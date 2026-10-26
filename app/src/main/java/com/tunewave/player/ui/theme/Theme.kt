package com.tunewave.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    secondary = Color(0xFF00B4A2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B8BFF),
    secondary = Color(0xFF4DD8C8),
)

/**
 * Phase 3 theme.
 *
 * On Android 12+ (API 31+), Material You dynamic colour is used when
 * [dynamicColor] is true, so the app follows the wallpaper colours.
 * On older versions it falls back to the fixed TuneWave colours above.
 */
@Composable
fun TuneWaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
