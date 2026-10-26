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

// Light palette from the charcoal/lime/violet prototype the user
// supplied (light variant): pale background, white cards. Lime is
// darkened for readable contrast on light surfaces; violet stays
// as the secondary accent.
private val LightColors = lightColorScheme(
    primary = Color(0xFF4D7C0F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEFB8),
    onPrimaryContainer = Color(0xFF202617),
    secondary = Color(0xFF7C5CFF),
    secondaryContainer = Color(0xFFE7E0FF),
    onSecondaryContainer = Color(0xFF2A2440),
    background = Color(0xFFF4F3F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEBF3),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFECEBF3),
    surfaceContainerHighest = Color(0xFFDFDCE8),
    onSurface = Color(0xFF201E2A),
    onSurfaceVariant = Color(0xFF777486),
    outline = Color(0xFFDFDCE8),
)

// Dark palette adapted from the charcoal/lime/violet web prototype
// the user supplied: charcoal-black background, layered dark
// surfaces, electric lime primary accent, soft violet secondary.
// Used when dynamic colour is off (the default), so this look
// actually shows instead of the wallpaper colours.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6F36B),
    onPrimary = Color(0xFF1A2012),
    primaryContainer = Color(0xFF2A3319),
    onPrimaryContainer = Color(0xFFD9F7B0),
    secondary = Color(0xFFA48BFF),
    secondaryContainer = Color(0xFF2A2440),
    onSecondaryContainer = Color(0xFFDDD2FF),
    background = Color(0xFF101018),
    surface = Color(0xFF191923),
    surfaceVariant = Color(0xFF222230),
    surfaceContainer = Color(0xFF191923),
    surfaceContainerHigh = Color(0xFF222230),
    surfaceContainerHighest = Color(0xFF2D2D3B),
    onSurface = Color(0xFFF5F3FA),
    onSurfaceVariant = Color(0xFFA09EAF),
    outline = Color(0xFF2D2D3B),
)

/**
 * TuneWave theme.
 *
 * On Android 12+ (API 31+), Material You dynamic colour is used only
 * when [dynamicColor] is true; otherwise the app uses its own
 * charcoal/lime/violet palette above. Dynamic colour can be toggled in Settings.
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
