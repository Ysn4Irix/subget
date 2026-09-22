package com.subget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightGoldAccent,
    onPrimary = Color.White,
    primaryContainer = LightGoldContainer,
    onPrimaryContainer = LightGoldOnContainer,
    secondary = LightElectricTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = LightEmeraldSuccess,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    outlineVariant = LightSurfaceBorderActive,
    error = LightRoseError,
    onError = Color.White,
    errorContainer = LightRoseContainer,
    onErrorContainer = Color(0xFF9F1239)
)

private val CinematicDarkColorScheme = darkColorScheme(
    primary = DarkGoldAccent,
    onPrimary = ObsidianDark,
    primaryContainer = DarkGoldAccentDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkElectricTeal,
    onSecondary = ObsidianDark,
    secondaryContainer = SurfaceBorderDark,
    onSecondaryContainer = DarkElectricTeal,
    tertiary = DarkEmeraldSuccess,
    onTertiary = ObsidianDark,
    background = ObsidianDark,
    onBackground = DarkTextPrimary,
    surface = SurfaceDark,
    onSurface = DarkTextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = DarkTextSecondary,
    outline = SurfaceBorderDark,
    outlineVariant = SurfaceBorderActive,
    error = DarkRoseError,
    onError = DarkTextPrimary,
    errorContainer = DarkRoseContainer,
    onErrorContainer = DarkTextPrimary
)

val LocalIsDarkTheme = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun SubgetTheme(
    darkTheme: Boolean = false, // Default is Light Mode as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CinematicDarkColorScheme else LightColorScheme

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

