package com.edgetts.engine

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.edgetts.engine.data.PrefsManager
import com.edgetts.engine.edge.EdgeTtsClient
import com.edgetts.engine.edge.EdgeTtsVoice
import com.edgetts.engine.edge.EdgeTtsVoices
import com.edgetts.engine.edge.Mp3Decoder
import com.edgetts.engine.edge.TextChunker
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/**
 * System-wide Android TTS engine backed by Microsoft Edge Speech neural voices.
 */
class EdgeTtsService : TextToSpeechService() {

    companion object {
        private const val TAG = "EdgeTtsService"
        private const val DEFAULT_SAMPLE_RATE = 24_000
    }

    private lateinit var prefs: PrefsManager
    private val synthMutex = Mutex()
    @Volatile private var stopRequested = false

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
        Log.i(TAG, "EdgeTtsService created, voices=${EdgeTtsVoices.all.size}")
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val locale = resolveLocale(lang, country) ?: return TextToSpeech.LANG_NOT_SUPPORTED
        val matches = EdgeTtsVoices.matching(locale)
        if (matches.isEmpty()) return TextToSpeech.LANG_NOT_SUPPORTED
        val exact = matches.any { EdgeTtsVoices.localeKey(it.locale) == EdgeTtsVoices.localeKey(locale) }
        return if (exact && !country.isNullOrBlank()) {
            TextToSpeech.LANG_COUNTRY_AVAILABLE
        } else {
            TextToSpeech.LANG_AVAILABLE
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetLanguage(): Array<String> {
        val voice = EdgeTtsVoices.fromId(prefs.currentVoice)
            ?: EdgeTtsVoices.fromId(PrefsManager.DEFAULT_VOICE)
            ?: EdgeTtsVoices.all.first()
        val locale = voice.locale
        return arrayOf(
            locale.language.ifEmpty { "en" },
            locale.country,
            ""
        )
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        val locale = resolveLocale(lang, country) ?: Locale.US
        val matches = EdgeTtsVoices.matching(locale)
        if (matches.isEmpty()) return PrefsManager.DEFAULT_VOICE

        val preferred = prefs.currentVoice
        if (matches.any { it.id == preferred }) return preferred

        val exact = matches.firstOrNull {
            EdgeTtsVoices.localeKey(it.locale) == EdgeTtsVoices.localeKey(locale)
        }
        return exact?.id ?: matches.first().id
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        if (voiceName.isNullOrBlank()) return TextToSpeech.ERROR
        return if (EdgeTtsVoices.fromId(voiceName) != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    }

    override fun onLoadVoice(voiceName: String?): Int {
        val voice = EdgeTtsVoices.fromId(voiceName ?: return TextToSpeech.ERROR)
            ?: return TextToSpeech.ERROR
        prefs.currentVoice = voice.id
        return TextToSpeech.SUCCESS
    }

    override fun onGetVoices(): List<Voice> {
        return EdgeTtsVoices.all.map { edge ->
            Voice(
                edge.id,
                edge.locale,
                Voice.QUALITY_VERY_HIGH,
                Voice.LATENCY_VERY_HIGH,
                false,
                emptySet()
            )
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        stopRequested = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        val text = request.charSequenceText?.toString().orEmpty()
        val reqId = System.identityHashCode(request)
        Log.i(TAG, "[$reqId] synthesize len=${text.length}")

        stopRequested = false
        if (text.isBlank()) {
            callback.done()
            return
        }

        val voice = resolveVoice(request)
        val rate = (request.speechRate / 100f).coerceIn(0.5f, 2.0f)
        val pitch = (request.pitch / 100f).coerceIn(0.5f, 2.0f)
        val chunks = TextChunker.chunk(text)

        try {
            runBlocking {
                synthMutex.withLock {
                    var started = false
                    for (chunk in chunks) {
                        if (stopRequested) break
                        val mp3 = EdgeTtsClient.synthesize(chunk, voice, rate, pitch)
                        if (stopRequested) break
                        val pcm = Mp3Decoder.decode(mp3)
                        if (!started) {
                            val sampleRate = if (pcm.sampleRate > 0) pcm.sampleRate else DEFAULT_SAMPLE_RATE
                            callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
                            started = true
                        }
                        writePcm(callback, pcm.pcm)
                    }
                    if (!started) {
                        callback.start(DEFAULT_SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT, 1)
                    }
                }
            }
            callback.done()
        } catch (t: Throwable) {
            Log.e(TAG, "[$reqId] synthesis failed", t)
            try {
                callback.error()
            } catch (_: Exception) {
                // callback may already be finished
            }
        }
    }

    private fun writePcm(callback: SynthesisCallback, pcm: ByteArray) {
        val max = callback.maxBufferSize.coerceAtLeast(2048)
        var offset = 0
        while (offset < pcm.size) {
            if (stopRequested) return
            val n = minOf(max, pcm.size - offset)
            val ret = callback.audioAvailable(pcm, offset, n)
            if (ret == TextToSpeech.ERROR) {
                stopRequested = true
                return
            }
            offset += n
        }
    }

    private fun resolveVoice(request: SynthesisRequest): EdgeTtsVoice {
        val requested = if (android.os.Build.VERSION.SDK_INT >= 21) request.voiceName else null
        if (!requested.isNullOrBlank()) {
            EdgeTtsVoices.fromId(requested)?.let { return it }
        }

        val locale = resolveLocale(request.language, request.country) ?: Locale.US
        val matches = EdgeTtsVoices.matching(locale)
        val preferred = prefs.currentVoice
        matches.firstOrNull { it.id == preferred }?.let { return it }
        EdgeTtsVoices.fromId(preferred)?.let { return it }
        return matches.firstOrNull()
            ?: EdgeTtsVoices.fromId(PrefsManager.DEFAULT_VOICE)
            ?: EdgeTtsVoices.all.first()
    }

    private fun resolveLocale(lang: String?, country: String?): Locale? {
        if (lang.isNullOrBlank()) return null
        val language = normalizeLang(lang)
        val c = country?.takeIf { it.isNotBlank() }?.let { normalizeCountry(it) }.orEmpty()
        return if (c.isEmpty()) Locale(language) else Locale(language, c)
    }

    private fun normalizeLang(lang: String): String {
        return when (lang.lowercase(Locale.ROOT)) {
            "eng" -> "en"
            "spa", "esl" -> "es"
            "fra", "fre" -> "fr"
            "deu", "ger" -> "de"
            "zho", "chi" -> "zh"
            "jpn" -> "ja"
            "kor" -> "ko"
            "por" -> "pt"
            "ita" -> "it"
            "rus" -> "ru"
            "ara" -> "ar"
            "hin" -> "hi"
            else -> if (lang.length == 3) {
                try {
                    Locale("", "").let {
                        // Prefer ISO2 if Android maps ISO3
                        Locale.forLanguageTag(lang).language.ifEmpty { lang.lowercase(Locale.ROOT) }
                    }
                } catch (_: Exception) {
                    lang.lowercase(Locale.ROOT)
                }
            } else {
                lang.lowercase(Locale.ROOT)
            }
        }
    }

    private fun normalizeCountry(country: String): String {
        return when (country.uppercase(Locale.ROOT)) {
            "USA" -> "US"
            "GBR" -> "GB"
            "AUS" -> "AU"
            "IND" -> "IN"
            "CAN" -> "CA"
            "ESP" -> "ES"
            "FRA" -> "FR"
            "DEU" -> "DE"
            "MEX" -> "MX"
            "BRA" -> "BR"
            "CHN" -> "CN"
            "TWN" -> "TW"
            "HKG" -> "HK"
            "JPN" -> "JP"
            "KOR" -> "KR"
            else -> country.uppercase(Locale.ROOT).take(2)
        }
    }
}
