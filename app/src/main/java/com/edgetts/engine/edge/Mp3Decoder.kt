package com.edgetts.engine.edge

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File

/**
 * Decodes Edge MP3 (24 kHz mono) to PCM 16-bit little-endian for SynthesisCallback.
 */
object Mp3Decoder {
    private const val TAG = "Mp3Decoder"
    private const val TIMEOUT_US = 10_000L

    data class PcmAudio(
        val sampleRate: Int,
        val channelCount: Int,
        val pcm: ByteArray,
    )

    fun decode(mp3: ByteArray): PcmAudio {
        if (mp3.isEmpty()) {
            return PcmAudio(sampleRate = 24_000, channelCount = 1, pcm = ByteArray(0))
        }

        val temp = File.createTempFile("edge_tts_", ".mp3")
        try {
            temp.writeBytes(mp3)
            return decodeFile(temp)
        } finally {
            temp.delete()
        }
    }

    private fun decodeFile(file: File): PcmAudio {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex < 0 || format == null) {
            extractor.release()
            throw IllegalStateException("No audio track in MP3 payload")
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalStateException("Missing MIME")
        val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            24_000
        }
        val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            1
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmChunks = ArrayList<ByteArray>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inIndex)
                        if (inputBuffer == null) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            inputBuffer.clear()
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.d(TAG, "Output format changed: ${codec.outputFormat}")
                    }
                    outIndex >= 0 -> {
                        if (info.size > 0) {
                            val outBuffer = codec.getOutputBuffer(outIndex)
                            if (outBuffer != null) {
                                outBuffer.position(info.offset)
                                outBuffer.limit(info.offset + info.size)
                                val chunk = ByteArray(info.size)
                                outBuffer.get(chunk)
                                pcmChunks.add(chunk)
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        val total = pcmChunks.sumOf { it.size }
        val pcm = ByteArray(total)
        var offset = 0
        for (chunk in pcmChunks) {
            chunk.copyInto(pcm, offset)
            offset += chunk.size
        }

        // Ensure mono 16-bit PCM for TTS callback; downmix if needed.
        val mono = if (channelCount > 1) downmixToMono(pcm, channelCount) else pcm
        return PcmAudio(sampleRate = sampleRate, channelCount = 1, pcm = mono)
    }

    private fun downmixToMono(interleaved: ByteArray, channels: Int): ByteArray {
        if (channels <= 1) return interleaved
        val frameBytes = channels * 2
        val frames = interleaved.size / frameBytes
        val out = ByteArray(frames * 2)
        var inPos = 0
        var outPos = 0
        for (f in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) {
                val lo = interleaved[inPos].toInt() and 0xFF
                val hi = interleaved[inPos + 1].toInt()
                val sample = (hi shl 8) or lo
                sum += sample.toShort().toInt()
                inPos += 2
            }
            val avg = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[outPos++] = (avg and 0xFF).toByte()
            out[outPos++] = ((avg shr 8) and 0xFF).toByte()
        }
        return out
    }
}
