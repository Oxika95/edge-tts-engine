package com.edgetts.engine.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edgetts.engine.BuildConfig
import com.edgetts.engine.data.PrefsManager
import com.edgetts.engine.edge.EdgeTtsVoices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("System TTS", style = MaterialTheme.typography.titleMedium)
            Text(
                "Open Android text-to-speech settings and choose Edge TTS Engine as the preferred engine. " +
                    "Other apps can then use Edge neural voices.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    try {
                        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open TTS settings")
            }

            Text("Current voice", style = MaterialTheme.typography.titleMedium)
            Text(prefs.currentVoice, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${EdgeTtsVoices.all.size} voices across ${EdgeTtsVoices.locales.size} locales",
                style = MaterialTheme.typography.bodySmall
            )

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "Edge TTS Engine is an independent Android system TTS provider. " +
                    "It is not affiliated with Microsoft, Readest, or NekoSpeak. " +
                    "Speech is synthesized over the network using Microsoft Edge’s public read-aloud endpoint, " +
                    "which is unofficial and may change or break.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
            Text("License: AGPL-3.0 (with MIT-derived UI/plumbing)", style = MaterialTheme.typography.bodySmall)

            OutlinedButton(
                onClick = {
                    prefs.isOnboardingComplete = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show setup tips again on next launch")
            }
        }
    }
}
