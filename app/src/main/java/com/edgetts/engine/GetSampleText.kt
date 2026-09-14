package com.edgetts.engine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

class GetSampleText : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val language = intent.getStringExtra("language")
        val country = intent.getStringExtra("country")
        val locale = when {
            !language.isNullOrBlank() && !country.isNullOrBlank() -> Locale(language, country)
            !language.isNullOrBlank() -> Locale(language)
            else -> Locale.US
        }

        val sample = when (locale.language.lowercase(Locale.ROOT)) {
            "es" -> "Hola. Esta es una muestra del motor Edge TTS."
            "fr" -> "Bonjour. Ceci est un exemple du moteur Edge TTS."
            "de" -> "Hallo. Dies ist ein Beispiel der Edge-TTS-Engine."
            "zh" -> "你好。这是 Edge TTS 引擎的示例。"
            "ja" -> "こんにちは。これは Edge TTS エンジンのサンプルです。"
            "ko" -> "안녕하세요. 이것은 Edge TTS 엔진 샘플입니다."
            else -> getString(R.string.sample_text)
        }

        val result = Intent()
        result.putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, sample)
        setResult(RESULT_OK, result)
        finish()
    }
}
