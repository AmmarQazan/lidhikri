package com.greendome.adhkar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.greendome.adhkar.data.model.AppThemeMode

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    secondary = GoldDome,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = CreamBackground,
    tertiary = GoldDark
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF0D2818),
    primaryContainer = GreenPrimaryDark,
    secondary = GoldLight,
    onSecondary = Color(0xFF1A1400),
    secondaryContainer = GoldDark,
    background = Color(0xFF1A1F1C),
    onBackground = Color(0xFFE8E4DC),
    surface = Color(0xFF242B27),
    onSurface = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF2E3632),
    tertiary = GoldLight
)

@Composable
fun AppFontScale(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val clamped = scale.coerceIn(0.8f, 1.5f)
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, clamped)
    ) {
        content()
    }
}

@Composable
fun AppCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

@Composable
fun AppMutedTextColor() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun AppAccentGreen(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.4f) GreenLight else GreenPrimaryDark

@Composable
fun GreenDomeTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
