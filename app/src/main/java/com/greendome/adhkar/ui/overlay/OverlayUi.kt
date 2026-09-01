package com.greendome.adhkar.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.PopupAppearance
import com.greendome.adhkar.ui.theme.AppFontScale
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark

@Composable
fun AutoAzkarOverlayCard(
    sectionTitle: String,
    text: String,
    appearance: PopupAppearance,
    onDismiss: () -> Unit,
    onStopAuto: () -> Unit,
    autoDismissSeconds: Int = PopupAppearance.AZKAR_DEFAULT_AUTO_DISMISS_SECONDS,
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(autoDismissSeconds) {
        if (autoDismissSeconds > 0) {
            delay(autoDismissSeconds * 1000L)
            onDismiss()
        }
    }
    LaunchedEffect(expanded) {
        if (expanded) scrollState.scrollTo(0)
    }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val maxExpandedHeight = (screenHeightDp * 0.82f).dp
    val boxWidthFraction = if (expanded) 0.98f else appearance.boxWidthFraction
    val bodyFontSize = if (expanded) 26.sp else 16.sp
    val bodyLineHeight = if (expanded) 42.sp else 26.sp
    val overlayFontScale = if (expanded) {
        (appearance.fontScale * 1.45f).coerceIn(0.8f, 2.2f)
    } else {
        appearance.fontScale
    }
    val alignment = BiasAlignment(
        horizontalBias = appearance.horizontalBias(),
        verticalBias = appearance.verticalBias()
    )
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, overlayFontScale)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = alignment
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = if (expanded) 8.dp else 16.dp,
                        vertical = if (expanded) 12.dp else 20.dp
                    )
                    .fillMaxWidth(boxWidthFraction)
                    .then(
                        if (expanded) Modifier.height(maxExpandedHeight) else Modifier
                    )
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(4.dp)
                        .background(GoldDome, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(10.dp))
                if (sectionTitle.isNotBlank()) {
                    Text(
                        text = sectionTitle,
                        style = if (expanded) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                        color = GoldDome,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    Spacer(Modifier.height(8.dp))
                }
                val textAreaModifier = if (expanded) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth()
                }
                Box(modifier = textAreaModifier) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (expanded) {
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        ArabicText(
                            text = text,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = bodyFontSize,
                                lineHeight = bodyLineHeight,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center,
                            color = GreenPrimaryDark,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = if (expanded) Int.MAX_VALUE else 6,
                            overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onStopAuto,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 34.dp, max = 38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.overlay_stop_auto_azkar),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { expanded = !expanded },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDome),
                        modifier = Modifier
                            .weight(1.15f)
                            .heightIn(min = 34.dp, max = 38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            contentDescription = stringResource(
                                if (expanded) R.string.overlay_collapse_popup
                                else R.string.overlay_expand_popup
                            ),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (expanded) R.string.overlay_collapse_popup
                                else R.string.overlay_expand_popup
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier
                            .heightIn(min = 34.dp, max = 38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.overlay_dismiss),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DhikrOverlayCard(
    text: String,
    appearance: PopupAppearance,
    onDismiss: () -> Unit,
    autoDismissSeconds: Int = PopupAppearance.TASBIH_DEFAULT_AUTO_DISMISS_SECONDS,
    dimBackground: Boolean = true,
    showDismissButton: Boolean = true
) {
    LaunchedEffect(autoDismissSeconds, showDismissButton) {
        if (showDismissButton && autoDismissSeconds > 0) {
            delay(autoDismissSeconds * 1000L)
            onDismiss()
        }
    }
    val alignment = BiasAlignment(
        horizontalBias = appearance.horizontalBias(),
        verticalBias = appearance.verticalBias()
    )
    AppFontScale(appearance.fontScale) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (dimBackground) Modifier.background(Color.Black.copy(alpha = 0.5f))
                    else Modifier
                ),
            contentAlignment = alignment
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .fillMaxWidth(appearance.boxWidthFraction)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(4.dp)
                        .background(GoldDome, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(12.dp))
                ArabicText(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    color = GreenPrimaryDark,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                if (showDismissButton) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .heightIn(min = 34.dp, max = 38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.overlay_dismiss),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
