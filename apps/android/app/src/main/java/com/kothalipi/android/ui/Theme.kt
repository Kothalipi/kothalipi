package com.kothalipi.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF006A4E)
val Record = Color(0xFFF42A41)

@Composable
fun KothalipiTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFF3DDBA8), onPrimary = Color(0xFF00382A),
        background = Color(0xFF101714), surface = Color(0xFF17201C), surfaceVariant = Color(0xFF1F2B26),
        onBackground = Color(0xFFE8F1EC), onSurface = Color(0xFFE8F1EC), onSurfaceVariant = Color(0xFF93A39B),
        error = Color(0xFFFF8A94),
    ) else lightColorScheme(
        primary = Green, onPrimary = Color.White,
        background = Color(0xFFFAF9F6), surface = Color.White, surfaceVariant = Color(0xFFF1EEE8),
        onBackground = Color(0xFF14231C), onSurface = Color(0xFF14231C), onSurfaceVariant = Color(0xFF5B6B63),
        error = Color(0xFFB3261E),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
