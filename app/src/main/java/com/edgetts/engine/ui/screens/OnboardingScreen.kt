package com.edgetts.engine.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.edgetts.engine.data.PrefsManager
import com.edgetts.engine.ui.Screen

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Edge TTS Engine", style = MaterialTheme.typography.headlineMedium)
        Text(
            "This app installs a system text-to-speech engine that uses Microsoft Edge neural voices over the network.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text("1. Open Android TTS settings", style = MaterialTheme.typography.titleMedium)
        Text(
            "Choose Edge TTS Engine as your preferred engine, then pick a language and voice.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text("2. Use it from any app", style = MaterialTheme.typography.titleMedium)
        Text(
            "Readers, accessibility tools, and other apps that use Android TTS will speak with Edge voices. An internet connection is required.",
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

        OutlinedButton(
            onClick = {
                prefs.isOnboardingComplete = true
                navController.navigate(Screen.Voices.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
