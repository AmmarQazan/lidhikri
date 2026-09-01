package com.greendome.adhkar.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
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
    onSurfaceVariant = GreenPrimaryDark,
    surfaceVariant = CreamBackground,
    tertiary = GoldDark
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FCBAA),
    onPrimary = Color(0xFF0D2818),
    primaryContainer = GreenPrimaryDark,
    secondary = GoldLight,
    onSecondary = Color(0xFF1A1400),
    secondaryContainer = GoldDark,
    background = Color(0xFF1A1F1C),
    onBackground = Color(0xFFF5F1E9),
    surface = Color(0xFF242B27),
    onSurface = Color(0xFFF5F1E9),
    onSurfaceVariant = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF3A4540),
    surfaceTint = Color(0xFF242B27),
    tertiary = GoldLight
)

@Composable
fun AppFontScale(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val clamped = scale.coerceIn(APP_FONT_SCALE_MIN, APP_FONT_SCALE_MAX * APP_FONT_SCALE_BASELINE)
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, clamped)
    ) {
        content()
    }
}

fun AppCardShape() = RoundedCornerShape(14.dp)

@Composable
fun AppCardColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface
)

@Composable
fun AppCardElevation() = CardDefaults.cardElevation(defaultElevation = 2.dp)

@Composable
fun AppCardOutline() = BorderStroke(
    1.dp,
    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
)

@Composable
fun AppFilledButtonColors() = ButtonDefaults.buttonColors(
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
)

@Composable
fun AppMutedTextColor() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun AppAccentGreen(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.4f) {
        MaterialTheme.colorScheme.primary
    } else {
        GreenPrimaryDark
    }

/** نص البطاقة: كريمي في الليلي حتى لا يذوب في الخلفية الخضراء */
@Composable
fun AppOnCardColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.4f) {
        Color(0xFFF5F1E9)
    } else {
        TextPrimary
    }

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
