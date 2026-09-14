package com.edgetts.engine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1565C0)
private val BlueDark = Color(0xFF90CAF9)
private val Teal = Color(0xFF00897B)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Teal,
    tertiary = Color(0xFF5C6BC0),
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFF9FA8DA),
)

@Composable
fun EdgeTtsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
