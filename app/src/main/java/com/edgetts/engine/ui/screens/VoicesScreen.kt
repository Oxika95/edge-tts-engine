package com.edgetts.engine.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edgetts.engine.data.PrefsManager
import com.edgetts.engine.edge.EdgeTtsVoice
import com.edgetts.engine.edge.EdgeTtsVoices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen() {
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }
    var selectedVoiceId by remember { mutableStateOf(prefs.currentVoice) }
    val locales = remember { EdgeTtsVoices.locales.sortedBy { it.toLanguageTag() } }
    var selectedLocale by remember {
        mutableStateOf(
            EdgeTtsVoices.fromId(selectedVoiceId)?.locale ?: Locale.US
        )
    }
    val voicesForLocale = remember(selectedLocale) {
        EdgeTtsVoices.matching(selectedLocale)
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context, { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }, context.packageName)
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    fun preview(voice: EdgeTtsVoice) {
        val engine = tts
        if (engine == null || !ttsReady) {
            Toast.makeText(context, "TTS not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val androidVoice = engine.voices?.firstOrNull { it.name == voice.id }
        if (androidVoice != null) {
            engine.voice = androidVoice
        } else {
            engine.language = voice.locale
        }
        engine.speak(
            "Hello. This is ${voice.name} from Edge TTS.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "edge-preview"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Edge voices") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Selected: $selectedVoiceId",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LocaleChipRow(
                locales = locales,
                selected = selectedLocale,
                onSelect = { selectedLocale = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(voicesForLocale, key = { it.id }) { voice ->
                    VoiceRow(
                        voice = voice,
                        selected = voice.id == selectedVoiceId,
                        onSelect = {
                            selectedVoiceId = voice.id
                            prefs.currentVoice = voice.id
                        },
                        onPreview = { preview(voice) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocaleChipRow(
    locales: List<Locale>,
    selected: Locale,
    onSelect: (Locale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferred = listOf("en-US", "en-GB", "en-AU", "es-ES", "fr-FR", "de-DE", "zh-CN", "ja-JP")
    val preferredLocales = preferred.mapNotNull { tag ->
        locales.firstOrNull { it.toLanguageTag().equals(tag, ignoreCase = true) }
    }
    val preferredKeys = preferredLocales.map { EdgeTtsVoices.localeKey(it) }.toSet()
    val ordered = preferredLocales + locales.filter {
        EdgeTtsVoices.localeKey(it) !in preferredKeys
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        modifier = modifier
    ) {
        items(ordered, key = { it.toLanguageTag() }) { locale ->
            FilterChip(
                selected = EdgeTtsVoices.localeKey(locale) == EdgeTtsVoices.localeKey(selected),
                onClick = { onSelect(locale) },
                label = { Text(locale.toLanguageTag()) }
            )
        }
    }
}

@Composable
private fun VoiceRow(
    voice: EdgeTtsVoice,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(voice.name, style = MaterialTheme.typography.titleMedium)
                Text(voice.id, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selected")
            }
            IconButton(onClick = onPreview) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
            }
        }
    }
}
