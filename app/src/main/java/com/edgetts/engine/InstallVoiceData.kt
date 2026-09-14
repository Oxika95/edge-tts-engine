package com.edgetts.engine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.edgetts.engine.ui.MainActivity

/**
 * Edge voices need no download; open the app settings UI instead.
 */
class InstallVoiceData : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
