package com.howmylook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme()

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E293B),
    onPrimary = Color.White,
    secondary = Color(0xFFDB2777),
    background = Color(0xFFFFF6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFFFF1F7),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFF3D8E7),
)

@Composable
fun HowMyLookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
