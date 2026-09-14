package com.edgetts.engine.edge

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * Microsoft Edge read-aloud WebSocket client, ported from Readest's edgeTTS.ts.
 * Returns MP3 audio bytes for a text chunk.
 */
object EdgeTtsClient {
    // OkHttp WebSocket requests must use https://; the client upgrades to wss://.
    private const val EDGE_SPEECH_URL =
        "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
    private const val EDGE_API_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
    private const val CHROMIUM_MAJOR_VERSION = "143"
    private const val WIN_EPOCH_OFFSET_SEC = 11_644_473_600L
    private const val SYNTH_TIMEOUT_MS = 45_000L
    private const val CACHE_SIZE = 32
    private const val TAG = "EdgeTTS"

    private val clockMutex = Mutex()
    @Volatile private var clockSkewSeconds = 0.0
    @Volatile private var clockSynced = false
    private val cacheMutex = Mutex()
    private val inflightMutex = Mutex()
    private val cache = object : LinkedHashMap<String, ByteArray>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > CACHE_SIZE
        }
    }
    private val inflight = mutableMapOf<String, CompletableDeferred<ByteArray>>()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    /**
     * @param rate speech rate multiplier (1.0 = normal). Mapped into SSML prosody.
     * @param pitch speech pitch multiplier (1.0 = normal). Mapped into SSML prosody percent.
     */
    suspend fun synthesize(
        text: String,
        voice: EdgeTtsVoice,
        rate: Float = 1.0f,
        pitch: Float = 1.0f,
    ): ByteArray {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ByteArray(0)
        val rateKey = String.format(Locale.US, "%.2f", rate)
        val pitchKey = String.format(Locale.US, "%.2f", pitch)
        val key = "${voice.id}|$rateKey|$pitchKey|$trimmed"

        cacheMutex.withLock {
            cache[key]?.let { return it }
        }

        val (deferred, owner) = inflightMutex.withLock {
            val existing = inflight[key]
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<ByteArray>()
                inflight[key] = created
                created to true
            }
        }

        if (!owner) {
            return deferred.await()
        }

        syncClockIfNeeded()
        try {
            val audio = withTimeout(SYNTH_TIMEOUT_MS) {
                synthesizeUncached(trimmed, voice, rate, pitch)
            }
            cacheMutex.withLock { cache[key] = audio }
            deferred.complete(audio)
            return audio
        } catch (t: Throwable) {
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(t)
            }
            throw t
        } finally {
            inflightMutex.withLock { inflight.remove(key) }
        }
    }

    /** Exposed for unit tests. */
    fun generateSecMsGecForTest(nowMillis: Long = System.currentTimeMillis(), skewSeconds: Double = 0.0): String {
        var ticks = (nowMillis / 1000.0) + skewSeconds + WIN_EPOCH_OFFSET_SEC
        ticks -= ticks % 300.0
        ticks *= 10_000_000.0
        val payload = String.format(Locale.US, "%.0f%s", ticks, EDGE_API_TOKEN)
        return sha256Hex(payload)
    }

    private suspend fun synthesizeUncached(
        text: String,
        voice: EdgeTtsVoice,
        rate: Float,
        pitch: Float,
    ): ByteArray {
        return try {
            synthesizeOnce(text, voice, rate, pitch)
        } catch (e: EdgeTtsHttpException) {
            if (e.status == 403) {
                Log.w(TAG, "403 from Edge Speech, retrying with clock skew")
                synthesizeOnce(text, voice, rate, pitch)
            } else {
                throw e
            }
        }
    }

    private suspend fun syncClockIfNeeded() {
        if (clockSynced) return
        clockMutex.withLock {
            if (clockSynced) return
            try {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://www.bing.com")
                        .head()
                        .build()
                    client.newCall(request).execute().use { response ->
                        applyClockSkew(response.header("Date"))
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Clock sync failed: ${t.message}")
            } finally {
                clockSynced = true
            }
        }
    }

    private suspend fun synthesizeOnce(
        text: String,
        voice: EdgeTtsVoice,
        rate: Float,
        pitch: Float,
    ): ByteArray {
        val connectId = md5Hex(UUID.randomUUID().toString())
        val url = EDGE_SPEECH_URL.toHttpUrl().newBuilder()
            .addQueryParameter("ConnectionId", connectId)
            .addQueryParameter("TrustedClientToken", EDGE_API_TOKEN)
            .addQueryParameter("Sec-MS-GEC", generateSecMsGec())
            .addQueryParameter("Sec-MS-GEC-Version", "1-$CHROMIUM_FULL_VERSION")
            .build()
        val date = Date().toString()

        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                    " (KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36" +
                    " Edg/$CHROMIUM_MAJOR_VERSION.0.0.0"
            )
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", "muid=${generateMuid()};")
            .build()

        val config = sendFrame(
            headers = mapOf(
                "Content-Type" to "application/json; charset=utf-8",
                "Path" to "speech.config",
                "X-Timestamp" to date,
            ),
            body = """{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":false,"wordBoundaryEnabled":true},"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}""",
        )
        val ssml = sendFrame(
            headers = mapOf(
                "Content-Type" to "application/ssml+xml",
                "Path" to "ssml",
                "X-RequestId" to connectId,
                "X-Timestamp" to date,
            ),
            body = genSsml(voice.lang, text, voice.id, rate, pitch),
        )

        return suspendCancellableCoroutine { cont ->
            val audio = ArrayList<ByteArray>()
            var settled = false

            fun settle(block: () -> Unit) {
                if (settled) return
                settled = true
                block()
            }

            val webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(config)
                    webSocket.send(ssml)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val headers = parseHeaders(text)
                    if (headers["Path"] == "turn.end") {
                        webSocket.close(1000, null)
                        val bytes = concat(audio)
                        settle {
                            if (bytes.isEmpty()) {
                                cont.resumeWithException(IllegalStateException("No audio data received."))
                            } else {
                                cont.resume(bytes)
                            }
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val raw = bytes.toByteArray()
                    if (raw.size < 2) return
                    val headerLength = ((raw[0].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
                    val bodyStart = 2 + headerLength
                    if (raw.size > bodyStart) {
                        audio.add(raw.copyOfRange(bodyStart, raw.size))
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val status = response?.code
                    Log.e(TAG, "WebSocket failed status=$status message=${t.message}", t)
                    if (status == 403) {
                        applyClockSkew(response.header("Date"))
                    }
                    settle {
                        if (cont.isActive) {
                            cont.resumeWithException(
                                EdgeTtsHttpException(
                                    "Edge Speech failed: ${t.message}",
                                    status,
                                    t
                                )
                            )
                        }
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    settle {
                        val bytes = concat(audio)
                        if (bytes.isEmpty()) {
                            if (cont.isActive) {
                                cont.resumeWithException(IllegalStateException("WebSocket closed before audio completed."))
                            }
                        } else if (cont.isActive) {
                            cont.resume(bytes)
                        }
                    }
                }
            })

            cont.invokeOnCancellation {
                webSocket.cancel()
            }
        }
    }

    private fun genSsml(
        lang: String,
        text: String,
        voice: String,
        rate: Float,
        pitch: Float,
    ): String {
        val cleaned = escapeXml(text.replace(Regex("^<break\\b[^>]*>", RegexOption.IGNORE_CASE), ""))
        val rateAttr = formatRate(rate)
        val pitchAttr = formatPitch(pitch)
        return """
            <speak version="1.0" xml:lang="$lang">
              <voice name="$voice">
                <prosody rate="$rateAttr" pitch="$pitchAttr">
                    $cleaned
                </prosody>
              </voice>
            </speak>
        """.trimIndent()
    }

    /** Android TTS rate is typically 0.5–2.0; SSML uses percent or relative values. */
    private fun formatRate(rate: Float): String {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        val percent = ((clamped - 1.0f) * 100f).roundToInt()
        return if (percent >= 0) "+$percent%" else "$percent%"
    }

    private fun formatPitch(pitch: Float): String {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        val percent = ((clamped - 1.0f) * 100f).roundToInt()
        return if (percent >= 0) "+$percent%" else "$percent%"
    }

    private fun sendFrame(headers: Map<String, String>, body: String): String {
        val header = headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
        return "$header\r\n\r\n$body"
    }

    private fun parseHeaders(message: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        for (line in message.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) break
            val separator = trimmed.indexOf(':')
            if (separator == -1) continue
            headers[trimmed.substring(0, separator).trim()] = trimmed.substring(separator + 1).trim()
        }
        return headers
    }

    private fun concat(parts: List<ByteArray>): ByteArray {
        val size = parts.sumOf { it.size }
        val out = ByteArray(size)
        var offset = 0
        for (part in parts) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun applyClockSkew(dateHeader: String?) {
        val serverMillis = parseHttpDate(dateHeader) ?: return
        clockSkewSeconds += (serverMillis - System.currentTimeMillis()) / 1000.0
        Log.i(TAG, "Clock skew now ${clockSkewSeconds}s")
    }

    private fun parseHttpDate(dateHeader: String?): Long? {
        if (dateHeader.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(dateHeader)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun generateSecMsGec(): String {
        return generateSecMsGecForTest(System.currentTimeMillis(), clockSkewSeconds)
    }

    class EdgeTtsHttpException(
        message: String,
        val status: Int?,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    private fun generateMuid(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun md5Hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }
}
