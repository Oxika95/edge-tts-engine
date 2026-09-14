package com.edgetts.engine

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.edgetts.engine.edge.EdgeTtsVoices
import java.util.Locale

/**
 * Reports all Edge locales as installed so Android treats this engine as ready.
 */
class CheckVoiceData : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val available = ArrayList<String>()
        for (locale in EdgeTtsVoices.locales) {
            try {
                val lang = locale.isO3Language
                val country = try {
                    locale.isO3Country
                } catch (_: Exception) {
                    ""
                }
                if (lang.isNotEmpty()) {
                    available.add(if (country.isNotEmpty()) "$lang-$country" else lang)
                }
            } catch (_: Exception) {
                // Skip locales without ISO3 codes
            }
        }

        // Always include common English variants for older clients.
        for (extra in listOf(Locale.US, Locale.UK, Locale("en", "AU"), Locale("en", "IN"))) {
            try {
                val tag = "${extra.isO3Language}-${extra.isO3Country}"
                if (!available.contains(tag)) available.add(tag)
            } catch (_: Exception) {
                // ignore
            }
        }

        val result = Intent()
        result.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, available)
        result.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, arrayListOf())
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, result)
        finish()
    }
}
