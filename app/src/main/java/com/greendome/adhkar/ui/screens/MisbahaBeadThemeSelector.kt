package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.MisbahaBeadTheme
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GoldLight
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.widget.ElectronicMisbahaColors

data class MisbahaBeadPalette(
    val stringColor: Color,
    val activeHighlight: Color,
    val activeShadow: Color,
    val centerHighlight: Color,
    val centerShadow: Color,
    val inactiveLight: Color,
    val inactiveDark: Color,
    val previewHighlight: Color,
    val previewShadow: Color,
)

fun MisbahaBeadTheme.palette(): MisbahaBeadPalette = when (this) {
    MisbahaBeadTheme.CLASSIC -> MisbahaBeadPalette(
        stringColor = Color(0xFF4A90C2),
        activeHighlight = Color(0xFF8FD4B8),
        activeShadow = GreenPrimaryDark,
        centerHighlight = GoldLight,
        centerShadow = GoldDome,
        inactiveLight = Color(0xFFD8ECE4),
        inactiveDark = Color(0xFF9BB8AD),
        previewHighlight = Color(0xFF8FD4B8),
        previewShadow = GreenPrimaryDark,
    )
    MisbahaBeadTheme.ROYAL -> MisbahaBeadPalette(
        stringColor = Color(0xFFC9A227),
        activeHighlight = Color(0xFFB388FF),
        activeShadow = Color(0xFF4A148C),
        centerHighlight = Color(0xFFFFE082),
        centerShadow = Color(0xFFB8860B),
        inactiveLight = Color(0xFFE8DFF5),
        inactiveDark = Color(0xFF9E8BB8),
        previewHighlight = Color(0xFFB388FF),
        previewShadow = Color(0xFF4A148C),
    )
    MisbahaBeadTheme.DESERT -> MisbahaBeadPalette(
        stringColor = Color(0xFFA67C52),
        activeHighlight = Color(0xFFE8C39E),
        activeShadow = Color(0xFF8B5A2B),
        centerHighlight = Color(0xFFFFCC80),
        centerShadow = Color(0xFFBF360C),
        inactiveLight = Color(0xFFF3E5D8),
        inactiveDark = Color(0xFFC4A882),
        previewHighlight = Color(0xFFE8C39E),
        previewShadow = Color(0xFF8B5A2B),
    )
    MisbahaBeadTheme.EMERALD -> MisbahaBeadPalette(
        stringColor = Color(0xFF2E7D32),
        activeHighlight = Color(0xFF69F0AE),
        activeShadow = Color(0xFF1B5E20),
        centerHighlight = Color(0xFFFFF59D),
        centerShadow = Color(0xFF827717),
        inactiveLight = Color(0xFFD7F5E3),
        inactiveDark = Color(0xFF7EAE90),
        previewHighlight = Color(0xFF69F0AE),
        previewShadow = Color(0xFF1B5E20),
    )
    MisbahaBeadTheme.OCEAN -> MisbahaBeadPalette(
        stringColor = Color(0xFF0277BD),
        activeHighlight = Color(0xFF4FC3F7),
        activeShadow = Color(0xFF01579B),
        centerHighlight = Color(0xFF80DEEA),
        centerShadow = Color(0xFF006064),
        inactiveLight = Color(0xFFD6EEF8),
        inactiveDark = Color(0xFF7FA8BD),
        previewHighlight = Color(0xFF4FC3F7),
        previewShadow = Color(0xFF01579B),
    )
    MisbahaBeadTheme.AMBER -> MisbahaBeadPalette(
        stringColor = Color(0xFFEF6C00),
        activeHighlight = Color(0xFFFFD54F),
        activeShadow = Color(0xFFE65100),
        centerHighlight = Color(0xFFFFECB3),
        centerShadow = Color(0xFFFF6F00),
        inactiveLight = Color(0xFFFFF3D6),
        inactiveDark = Color(0xFFC9A86A),
        previewHighlight = Color(0xFFFFD54F),
        previewShadow = Color(0xFFE65100),
    )
    MisbahaBeadTheme.WOOD -> MisbahaBeadPalette(
        stringColor = Color(0xFF6D4C41),
        activeHighlight = Color(0xFFBCAAA4),
        activeShadow = Color(0xFF3E2723),
        centerHighlight = Color(0xFFD7CCC8),
        centerShadow = Color(0xFF5D4037),
        inactiveLight = Color(0xFFEDE0D8),
        inactiveDark = Color(0xFFA1887F),
        previewHighlight = Color(0xFFBCAAA4),
        previewShadow = Color(0xFF3E2723),
    )
    MisbahaBeadTheme.SILVER -> MisbahaBeadPalette(
        stringColor = Color(0xFF78909C),
        activeHighlight = Color(0xFFECEFF1),
        activeShadow = Color(0xFF546E7A),
        centerHighlight = Color(0xFFFFFDE7),
        centerShadow = Color(0xFFB0BEC5),
        inactiveLight = Color(0xFFF5F7F8),
        inactiveDark = Color(0xFFB0BEC5),
        previewHighlight = Color(0xFFECEFF1),
        previewShadow = Color(0xFF546E7A),
    )
    MisbahaBeadTheme.RUBY -> MisbahaBeadPalette(
        stringColor = Color(0xFF8E24AA),
        activeHighlight = Color(0xFFEF5350),
        activeShadow = Color(0xFFB71C1C),
        centerHighlight = Color(0xFFFFCDD2),
        centerShadow = Color(0xFF880E4F),
        inactiveLight = Color(0xFFF8E0E0),
        inactiveDark = Color(0xFFC48B8B),
        previewHighlight = Color(0xFFEF5350),
        previewShadow = Color(0xFFB71C1C),
    )
}

fun MisbahaBeadTheme.toElectronicColors(): ElectronicMisbahaColors {
    val palette = palette()
    val activeMid = palette.activeHighlight.toArgb()
    val activeDark = palette.activeShadow.toArgb()
    val white = 0xFFFFFFFF.toInt()
    return ElectronicMisbahaColors(
        beadActiveLight = ColorUtils.blendARGB(white, activeMid, 0.32f),
        beadActiveMid = activeMid,
        beadActiveDark = activeDark,
        beadInactiveLight = palette.inactiveLight.toArgb(),
        beadInactiveDark = palette.inactiveDark.toArgb(),
        centerInner = palette.centerHighlight.toArgb(),
        centerOuter = ColorUtils.blendARGB(
            palette.activeHighlight.toArgb(),
            palette.stringColor.toArgb(),
            0.4f,
        ),
        text = activeDark,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MisbahaBeadThemeSelector(
    selected: MisbahaBeadTheme,
    onSelected: (MisbahaBeadTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MisbahaBeadTheme.entries.forEach { theme ->
            MisbahaBeadThemeOption(
                theme = theme,
                selected = selected == theme,
                onSelect = { onSelected(theme) },
            )
        }
    }
}

@Composable
private fun MisbahaBeadThemeOption(
    theme: MisbahaBeadTheme,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val palette = theme.palette()
    val borderColor = if (selected) GreenPrimary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(palette.previewHighlight, palette.previewShadow),
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape),
        )
        Text(
            text = misbahaBeadThemeLabel(theme),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun misbahaBeadThemeLabel(theme: MisbahaBeadTheme): String = when (theme) {
    MisbahaBeadTheme.CLASSIC -> stringResource(R.string.misbaha_bead_theme_classic)
    MisbahaBeadTheme.ROYAL -> stringResource(R.string.misbaha_bead_theme_royal)
    MisbahaBeadTheme.DESERT -> stringResource(R.string.misbaha_bead_theme_desert)
    MisbahaBeadTheme.EMERALD -> stringResource(R.string.misbaha_bead_theme_emerald)
    MisbahaBeadTheme.OCEAN -> stringResource(R.string.misbaha_bead_theme_ocean)
    MisbahaBeadTheme.AMBER -> stringResource(R.string.misbaha_bead_theme_amber)
    MisbahaBeadTheme.WOOD -> stringResource(R.string.misbaha_bead_theme_wood)
    MisbahaBeadTheme.SILVER -> stringResource(R.string.misbaha_bead_theme_silver)
    MisbahaBeadTheme.RUBY -> stringResource(R.string.misbaha_bead_theme_ruby)
}
