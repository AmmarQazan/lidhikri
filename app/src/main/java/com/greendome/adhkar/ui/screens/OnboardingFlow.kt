package com.greendome.adhkar.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.CreamBackground
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.arabicFontFamily

private const val ONBOARDING_STEPS = 6

data class OnboardingResult(
    val language: String,
    val arabicFontStyle: ArabicFontStyle,
    val themeMode: AppThemeMode
)

@Composable
fun OnboardingFlow(
    initialLanguage: String,
    initialFontStyle: ArabicFontStyle,
    initialThemeMode: AppThemeMode,
    initialStep: Int = 0,
    onLanguageChange: (String) -> Unit,
    onStepChange: (Int) -> Unit,
    onComplete: (OnboardingResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(initialStep.coerceIn(0, ONBOARDING_STEPS - 1)) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var selectedFont by remember { mutableStateOf(initialFontStyle) }
    var selectedTheme by remember { mutableStateOf(initialThemeMode) }

    fun finish() {
        onComplete(
            OnboardingResult(
                language = selectedLanguage,
                arabicFontStyle = selectedFont,
                themeMode = selectedTheme
            )
        )
    }

    fun goNext() {
        if (step == 1 && selectedLanguage != initialLanguage) {
            onStepChange(step + 1)
            onLanguageChange(selectedLanguage)
            return
        }
        val nextStep = step + 1
        if (step < ONBOARDING_STEPS - 1) {
            step = nextStep
            onStepChange(nextStep)
        } else {
            finish()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (step) {
                0 -> OnboardingWelcomeStep()
                1 -> OnboardingLanguageStep(
                    selected = selectedLanguage,
                    onSelect = { selectedLanguage = it }
                )
                2 -> OnboardingFontStep(
                    selected = selectedFont,
                    onSelect = { selectedFont = it }
                )
                3 -> OnboardingThemeStep(
                    selected = selectedTheme,
                    onSelect = { selectedTheme = it }
                )
                4 -> OnboardingSettingsGuideStep()
                5 -> OnboardingReadyStep()
            }
        }

        OnboardingFooter(
            step = step,
            totalSteps = ONBOARDING_STEPS,
            isLastStep = step == ONBOARDING_STEPS - 1,
            onNext = { goNext() },
            onSkip = { finish() }
        )
    }
}

@Composable
private fun OnboardingWelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_sabbih),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(160.dp)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            color = GreenPrimaryDark,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = GreenPrimaryDark.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldDome)
        )
    }
}

@Composable
private fun OnboardingLanguageStep(
    selected: String,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "ar" to "العربية",
        "en" to "English",
        "fr" to "Français",
        "es" to "Español"
    )

    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_language_title),
        subtitle = stringResource(R.string.onboarding_language_subtitle)
    ) {
        languages.forEach { (code, label) ->
            OnboardingSelectionRow(
                label = label,
                selected = selected == code,
                onSelect = { onSelect(code) }
            )
        }
    }
}

@Composable
private fun OnboardingFontStep(
    selected: ArabicFontStyle,
    onSelect: (ArabicFontStyle) -> Unit
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_font_title),
        subtitle = stringResource(R.string.onboarding_font_subtitle)
    ) {
        ArabicFontStyle.entries.forEach { style ->
            OnboardingFontOptionRow(
                style = style,
                selected = selected == style,
                onSelect = { onSelect(style) }
            )
        }
    }
}

@Composable
private fun OnboardingThemeStep(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_theme_title),
        subtitle = stringResource(R.string.onboarding_theme_subtitle)
    ) {
        AppThemeModeSelector(
            selected = selected,
            onSelected = onSelect
        )
    }
}

@Composable
private fun OnboardingSettingsGuideStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = GreenPrimaryDark,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = GreenPrimaryDark.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OnboardingFeatureRow(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.onboarding_settings_tasbih_title),
            subtitle = stringResource(R.string.onboarding_settings_tasbih_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.Schedule,
            title = stringResource(R.string.onboarding_settings_schedule_title),
            subtitle = stringResource(R.string.onboarding_settings_schedule_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.ToggleOff,
            title = stringResource(R.string.onboarding_settings_disable_title),
            subtitle = stringResource(R.string.onboarding_settings_disable_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.onboarding_settings_favorites_title),
            subtitle = stringResource(R.string.onboarding_settings_favorites_subtitle)
        )
    }
}

@Composable
private fun OnboardingReadyStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineSmall,
            color = GreenPrimaryDark,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = GreenPrimaryDark.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        ArabicText(
            text = stringResource(R.string.onboarding_font_preview),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                lineHeight = 38.sp
            ),
            color = GreenPrimaryDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))
        OnboardingFeatureRow(
            icon = Icons.Default.TouchApp,
            title = stringResource(R.string.onboarding_feature_tasbih_title),
            subtitle = stringResource(R.string.onboarding_feature_tasbih_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.onboarding_feature_azkar_title),
            subtitle = stringResource(R.string.onboarding_feature_azkar_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.onboarding_feature_reminder_title),
            subtitle = stringResource(R.string.onboarding_feature_reminder_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.onboarding_feature_misbaha_title),
            subtitle = stringResource(R.string.onboarding_feature_misbaha_subtitle)
        )
    }
}

@Composable
private fun OnboardingStepScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = GreenPrimaryDark,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = GreenPrimaryDark.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun OnboardingSelectionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val background = if (selected) Color.White else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) GreenPrimaryDark else Color(0xFF555555),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary)
            )
        }
    }
}

@Composable
private fun OnboardingFontOptionRow(
    style: ArabicFontStyle,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val background = if (selected) Color.White else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = stringResource(onboardingFontStyleLabelRes(style)),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) GreenPrimary else Color.Gray
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_font_preview),
            style = TextStyle(
                fontFamily = arabicFontFamily(style),
                fontSize = 22.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Start
            ),
            color = GreenPrimaryDark,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldDome,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimaryDark.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun OnboardingFooter(
    step: Int,
    totalSteps: Int,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingPageIndicator(
            currentStep = step,
            totalSteps = totalSteps,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLastStep) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_next),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val active = index == currentStep
            Box(
                modifier = Modifier
                    .then(
                        if (active) Modifier
                            .width(22.dp)
                            .height(8.dp)
                        else Modifier.size(8.dp)
                    )
                    .clip(CircleShape)
                    .background(
                        if (active) GreenPrimary else GreenPrimary.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

private fun onboardingFontStyleLabelRes(style: ArabicFontStyle): Int = when (style) {
    ArabicFontStyle.DEFAULT -> R.string.font_style_default
    ArabicFontStyle.UTHMANI_1 -> R.string.font_style_uthmani_1
    ArabicFontStyle.UTHMANI_2 -> R.string.font_style_uthmani_2
    ArabicFontStyle.INDOPAK_1 -> R.string.font_style_indopak_1
    ArabicFontStyle.INDOPAK_2 -> R.string.font_style_indopak_2
    ArabicFontStyle.BENGALI -> R.string.font_style_bengali
}
