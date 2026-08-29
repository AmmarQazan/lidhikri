package com.greendome.adhkar.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greendome.adhkar.R
import com.greendome.adhkar.data.AutoAzkarCatalog
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.data.model.ArabicFontStyle
import com.greendome.adhkar.data.model.ClockHourFormat
import com.greendome.adhkar.ui.theme.ArabicText
import com.greendome.adhkar.ui.theme.CreamBackground
import com.greendome.adhkar.ui.theme.GoldDome
import com.greendome.adhkar.ui.theme.GreenPrimary
import com.greendome.adhkar.ui.theme.GreenPrimaryDark
import com.greendome.adhkar.ui.theme.arabicFontFamily
import com.greendome.adhkar.ui.theme.formatLocalizedDigits
import com.greendome.adhkar.ui.theme.stringResourceDigits
import com.greendome.adhkar.prayer.LocationMode
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.util.AppLanguages
import com.greendome.adhkar.util.LocaleHelper
import com.greendome.adhkar.util.TasbihWindow
import com.greendome.adhkar.util.formatClockHourCompact

private const val ONBOARDING_STEPS = 8

data class OnboardingResult(
    val language: String,
    val arabicFontStyle: ArabicFontStyle,
    val themeMode: AppThemeMode,
    val autoTasbihEnabled: Boolean = true,
    val autoAzkarEnabled: Boolean = false,
    val morningHour: Int = TasbihWindow.DEFAULT_MORNING_HOUR,
    val morningMinute: Int = TasbihWindow.DEFAULT_MORNING_MINUTE,
    val sleepHour: Int = TasbihWindow.DEFAULT_SLEEP_HOUR,
    val sleepMinute: Int = TasbihWindow.DEFAULT_SLEEP_MINUTE,
    val tasbihStartHour: Int = TasbihWindow.FALLBACK_START_HOUR,
    val tasbihStartMinute: Int = TasbihWindow.FALLBACK_START_MINUTE,
    val tasbihEndHour: Int = TasbihWindow.FALLBACK_END_HOUR,
    val tasbihEndMinute: Int = TasbihWindow.FALLBACK_END_MINUTE,
    val respectPrayerTime: Boolean = false,
    val prayerLocation: PrayerLocation? = null,
    val prayerLocationMode: LocationMode = LocationMode.MANUAL,
    val enabledAzkarCollectionIds: Set<String> = AutoAzkarCatalog.defaultEnabledClockIds(),
    val afterPrayerAzkarEnabled: Boolean = false,
    val clockHourFormat: ClockHourFormat = ClockHourFormat.HOUR_24
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
    azkarCollections: List<AdhkarCollectionEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(initialStep.coerceIn(0, ONBOARDING_STEPS - 1)) }
    var selectedLanguage by remember { mutableStateOf(AppLanguages.coerce(initialLanguage)) }
    var selectedFont by remember { mutableStateOf(initialFontStyle) }
    var selectedTheme by remember { mutableStateOf(initialThemeMode) }
    var wantAutoTasbih by remember { mutableStateOf(true) }
    var wantAutoAzkar by remember { mutableStateOf(false) }
    var morningHour by remember { mutableIntStateOf(TasbihWindow.DEFAULT_MORNING_HOUR) }
    var morningMinute by remember { mutableIntStateOf(TasbihWindow.DEFAULT_MORNING_MINUTE) }
    var sleepHour by remember { mutableIntStateOf(TasbihWindow.DEFAULT_SLEEP_HOUR) }
    var sleepMinute by remember { mutableIntStateOf(TasbihWindow.DEFAULT_SLEEP_MINUTE) }
    var tasbihStartHour by remember { mutableIntStateOf(TasbihWindow.FALLBACK_START_HOUR) }
    var tasbihStartMinute by remember { mutableIntStateOf(TasbihWindow.FALLBACK_START_MINUTE) }
    var tasbihEndHour by remember { mutableIntStateOf(TasbihWindow.FALLBACK_END_HOUR) }
    var tasbihEndMinute by remember { mutableIntStateOf(TasbihWindow.FALLBACK_END_MINUTE) }
    var wantRespectPrayer by remember { mutableStateOf(false) }
    var afterPrayerAzkar by remember { mutableStateOf(false) }
    var enabledAzkarIds by remember { mutableStateOf(AutoAzkarCatalog.defaultEnabledClockIds()) }
    var clockHourFormat by remember { mutableStateOf(ClockHourFormat.HOUR_24) }
    var prayerLocation by remember { mutableStateOf<PrayerLocation?>(null) }
    var prayerLocationMode by remember { mutableStateOf(LocationMode.MANUAL) }

    fun syncTasbihDefaults(enabled: Boolean) {
        if (enabled) {
            tasbihStartHour = morningHour
            tasbihStartMinute = morningMinute
            tasbihEndHour = sleepHour
            tasbihEndMinute = sleepMinute
        } else {
            tasbihStartHour = TasbihWindow.FALLBACK_START_HOUR
            tasbihStartMinute = TasbihWindow.FALLBACK_START_MINUTE
            tasbihEndHour = TasbihWindow.FALLBACK_END_HOUR
            tasbihEndMinute = TasbihWindow.FALLBACK_END_MINUTE
        }
    }

    fun finish() {
        onComplete(
            OnboardingResult(
                language = selectedLanguage,
                arabicFontStyle = selectedFont,
                themeMode = selectedTheme,
                autoTasbihEnabled = wantAutoTasbih,
                autoAzkarEnabled = wantAutoAzkar,
                morningHour = morningHour,
                morningMinute = morningMinute,
                sleepHour = sleepHour,
                sleepMinute = sleepMinute,
                tasbihStartHour = tasbihStartHour,
                tasbihStartMinute = tasbihStartMinute,
                tasbihEndHour = tasbihEndHour,
                tasbihEndMinute = tasbihEndMinute,
                respectPrayerTime = wantAutoTasbih && wantRespectPrayer,
                prayerLocation = prayerLocation,
                prayerLocationMode = prayerLocationMode,
                enabledAzkarCollectionIds = enabledAzkarIds,
                afterPrayerAzkarEnabled = afterPrayerAzkar,
                clockHourFormat = clockHourFormat
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

    fun goBack() {
        if (step > 0) {
            val prevStep = step - 1
            step = prevStep
            onStepChange(prevStep)
        }
    }

    BackHandler(enabled = step > 0) { goBack() }

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
                4 -> OnboardingTasbihStep(
                    wantAutoTasbih = wantAutoTasbih,
                    onWantAutoTasbihChange = { enabled ->
                        wantAutoTasbih = enabled
                        if (!enabled) wantRespectPrayer = false
                        if (enabled && wantAutoAzkar) {
                            syncTasbihDefaults(true)
                        } else if (!enabled) {
                            syncTasbihDefaults(false)
                        }
                    },
                    tasbihStartHour = tasbihStartHour,
                    tasbihStartMinute = tasbihStartMinute,
                    tasbihEndHour = tasbihEndHour,
                    tasbihEndMinute = tasbihEndMinute,
                    wantRespectPrayer = wantRespectPrayer,
                    onWantRespectPrayerChange = {
                        wantRespectPrayer = it
                        if (it) afterPrayerAzkar = true
                    },
                    prayerLocation = prayerLocation,
                    onPrayerLocationPicked = { loc, mode ->
                        prayerLocation = loc
                        prayerLocationMode = mode
                    }
                )
                5 -> OnboardingRemindersStep(
                    language = selectedLanguage,
                    azkarCollections = azkarCollections,
                    wantAutoAzkar = wantAutoAzkar,
                    autoTasbihEnabled = wantAutoTasbih,
                    onWantAutoAzkarChange = {
                        wantAutoAzkar = it
                        if (wantAutoTasbih) syncTasbihDefaults(it)
                    },
                    enabledAzkarIds = enabledAzkarIds,
                    onEnabledAzkarIdsChange = { enabledAzkarIds = it },
                    afterPrayerAzkar = afterPrayerAzkar,
                    onAfterPrayerAzkarChange = { afterPrayerAzkar = it },
                    prayerTimesOn = wantAutoTasbih && wantRespectPrayer,
                    morningHour = morningHour,
                    morningMinute = morningMinute,
                    onMorningHourChange = {
                        morningHour = it
                        if (wantAutoAzkar && wantAutoTasbih) tasbihStartHour = it
                    },
                    onMorningMinuteChange = {
                        morningMinute = it
                        if (wantAutoAzkar && wantAutoTasbih) tasbihStartMinute = it
                    },
                    sleepHour = sleepHour,
                    sleepMinute = sleepMinute,
                    onSleepHourChange = {
                        sleepHour = it
                        if (wantAutoAzkar && wantAutoTasbih) tasbihEndHour = it
                    },
                    onSleepMinuteChange = {
                        sleepMinute = it
                        if (wantAutoAzkar && wantAutoTasbih) tasbihEndMinute = it
                    },
                    clockHourFormat = clockHourFormat,
                    onClockHourFormatChange = { clockHourFormat = it }
                )
                6 -> OnboardingSettingsGuideStep()
                7 -> OnboardingReadyStep(
                    autoTasbihEnabled = wantAutoTasbih,
                    autoAzkarEnabled = wantAutoAzkar
                )
            }
        }

        OnboardingFooter(
            step = step,
            totalSteps = ONBOARDING_STEPS,
            isLastStep = step == ONBOARDING_STEPS - 1,
            canGoBack = step > 0,
            onNext = { goNext() },
            onBack = { goBack() },
            onSkip = { finish() }
        )
    }
}

private data class OnboardingWelcomeCopy(
    val title: String,
    val subtitle: String
)

@Composable
private fun OnboardingWelcomeStep() {
    val context = LocalContext.current
    val copies = remember(context) {
        AppLanguages.codes.map { code ->
            OnboardingWelcomeCopy(
                title = LocaleHelper.string(context, code, R.string.onboarding_welcome_title),
                subtitle = LocaleHelper.string(context, code, R.string.onboarding_welcome_subtitle)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Image(
            painter = painterResource(R.drawable.logo_sabbih),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.56f)
                .height(128.dp)
        )
        Spacer(Modifier.height(20.dp))
        copies.forEachIndexed { index, copy ->
            if (index == 1) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoldDome)
                )
                Spacer(Modifier.height(16.dp))
            } else if (index > 1) {
                Spacer(Modifier.height(10.dp))
            }
            Text(
                text = copy.title,
                style = if (index == 0) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = GreenPrimaryDark,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (index <= 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = copy.subtitle,
                    style = if (index == 0) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = GreenPrimaryDark.copy(alpha = if (index == 0) 0.75f else 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = if (index == 0) 26.sp else 22.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingLanguageStep(
    selected: String,
    onSelect: (String) -> Unit
) {
    val languages = AppLanguages.pickerPairs()

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
private fun OnboardingTasbihStep(
    wantAutoTasbih: Boolean,
    onWantAutoTasbihChange: (Boolean) -> Unit,
    tasbihStartHour: Int,
    tasbihStartMinute: Int,
    tasbihEndHour: Int,
    tasbihEndMinute: Int,
    wantRespectPrayer: Boolean,
    onWantRespectPrayerChange: (Boolean) -> Unit,
    prayerLocation: PrayerLocation?,
    onPrayerLocationPicked: (PrayerLocation, LocationMode) -> Unit
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_auto_tasbih_title),
        subtitle = stringResource(R.string.onboarding_auto_tasbih_subtitle)
    ) {
        OnboardingSelectionRow(
            label = stringResource(R.string.onboarding_auto_azkar_yes),
            selected = wantAutoTasbih,
            onSelect = { onWantAutoTasbihChange(true) }
        )
        OnboardingSelectionRow(
            label = stringResource(R.string.onboarding_auto_azkar_no),
            selected = !wantAutoTasbih,
            onSelect = { onWantAutoTasbihChange(false) }
        )
        if (wantAutoTasbih) {
            val periodAm = stringResource(R.string.clock_period_am)
            val periodPm = stringResource(R.string.clock_period_pm)
            val startLabel = formatClockHourCompact(
                tasbihStartHour,
                tasbihStartMinute,
                periodAm,
                periodPm
            ).formatLocalizedDigits()
            val endLabel = formatClockHourCompact(
                tasbihEndHour,
                tasbihEndMinute,
                periodAm,
                periodPm
            ).formatLocalizedDigits()
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onboarding_tasbih_window_title),
                style = MaterialTheme.typography.titleMedium,
                color = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResourceDigits(
                    R.string.onboarding_tasbih_window_range,
                    startLabel,
                    endLabel
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_tasbih_window_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimaryDark.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onboarding_prayer_title),
                style = MaterialTheme.typography.titleMedium,
                color = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_prayer_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimaryDark.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OnboardingSelectionRow(
                label = stringResource(R.string.onboarding_prayer_yes),
                selected = wantRespectPrayer,
                onSelect = { onWantRespectPrayerChange(true) }
            )
            OnboardingSelectionRow(
                label = stringResource(R.string.onboarding_prayer_no),
                selected = !wantRespectPrayer,
                onSelect = { onWantRespectPrayerChange(false) }
            )
            if (wantRespectPrayer) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_prayer_what_happens),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenPrimaryDark.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                PrayerCityPicker(
                    current = prayerLocation,
                    onPicked = onPrayerLocationPicked
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OnboardingRemindersStep(
    language: String,
    azkarCollections: List<AdhkarCollectionEntity>,
    wantAutoAzkar: Boolean,
    autoTasbihEnabled: Boolean,
    onWantAutoAzkarChange: (Boolean) -> Unit,
    enabledAzkarIds: Set<String>,
    onEnabledAzkarIdsChange: (Set<String>) -> Unit,
    afterPrayerAzkar: Boolean,
    onAfterPrayerAzkarChange: (Boolean) -> Unit,
    prayerTimesOn: Boolean,
    morningHour: Int,
    morningMinute: Int,
    onMorningHourChange: (Int) -> Unit,
    onMorningMinuteChange: (Int) -> Unit,
    sleepHour: Int,
    sleepMinute: Int,
    onSleepHourChange: (Int) -> Unit,
    onSleepMinuteChange: (Int) -> Unit,
    clockHourFormat: ClockHourFormat,
    onClockHourFormatChange: (ClockHourFormat) -> Unit
) {
    OnboardingStepScaffold(
        title = stringResource(R.string.onboarding_auto_azkar_title),
        subtitle = stringResource(
            if (autoTasbihEnabled) R.string.onboarding_auto_azkar_subtitle
            else R.string.onboarding_auto_azkar_subtitle_no_tasbih
        )
    ) {
        OnboardingSelectionRow(
            label = stringResource(R.string.onboarding_auto_azkar_yes),
            selected = wantAutoAzkar,
            onSelect = { onWantAutoAzkarChange(true) }
        )
        OnboardingSelectionRow(
            label = stringResource(R.string.onboarding_auto_azkar_no),
            selected = !wantAutoAzkar,
            onSelect = { onWantAutoAzkarChange(false) }
        )
        if (wantAutoAzkar) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_azkar_sections_title),
                style = MaterialTheme.typography.titleSmall,
                color = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.onboarding_azkar_sections_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimaryDark.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
            AutoAzkarCatalog.onboardingSpecs().forEach { spec ->
                val checked = if (spec.trigger == AutoAzkarCatalog.Trigger.PRAYER) {
                    afterPrayerAzkar
                } else {
                    spec.id in enabledAzkarIds
                }
                val collection = azkarCollections.find { it.id == spec.id }
                val label = if (collection != null) {
                    azkarCollectionTitle(collection, language)
                } else {
                    AutoAzkarCatalog.displayTitle(spec.id, language)
                }
                OnboardingSectionToggle(
                    label = label,
                    checked = checked,
                    hint = if (spec.trigger == AutoAzkarCatalog.Trigger.PRAYER) {
                        stringResource(
                            if (prayerTimesOn) R.string.onboarding_azkar_after_prayer_hint
                            else R.string.onboarding_azkar_after_prayer_needs_prayer
                        )
                    } else {
                        null
                    },
                    onCheckedChange = { on ->
                        if (spec.trigger == AutoAzkarCatalog.Trigger.PRAYER) {
                            onAfterPrayerAzkarChange(on)
                        } else if (on) {
                            onEnabledAzkarIdsChange(enabledAzkarIds + spec.id)
                        } else {
                            onEnabledAzkarIdsChange(enabledAzkarIds - spec.id)
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            TimeOfDaySetting(
                label = stringResource(R.string.onboarding_morning_time),
                hour = morningHour,
                minute = morningMinute,
                onHourChange = onMorningHourChange,
                onMinuteChange = onMorningMinuteChange
            )
            Spacer(Modifier.height(8.dp))
            TimeOfDaySetting(
                label = stringResource(R.string.onboarding_sleep_time),
                hour = sleepHour,
                minute = sleepMinute,
                onHourChange = onSleepHourChange,
                onMinuteChange = onSleepMinuteChange
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.azkar_clock_format_label),
                style = MaterialTheme.typography.titleSmall,
                color = GreenPrimaryDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.azkar_clock_format_hint),
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimaryDark.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
            ClockHourFormatSelector(
                selected = clockHourFormat,
                onSelected = onClockHourFormatChange
            )
        }
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
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.onboarding_settings_azkar_title),
            subtitle = stringResource(R.string.onboarding_settings_azkar_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.onboarding_settings_prayer_title),
            subtitle = stringResource(R.string.onboarding_settings_prayer_subtitle)
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
        OnboardingFeatureRow(
            icon = Icons.Default.FormatSize,
            title = stringResource(R.string.onboarding_settings_display_title),
            subtitle = stringResource(R.string.onboarding_settings_display_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.TouchApp,
            title = stringResource(R.string.onboarding_settings_misbaha_title),
            subtitle = stringResource(R.string.onboarding_settings_misbaha_subtitle)
        )
        OnboardingFeatureRow(
            icon = Icons.Default.Widgets,
            title = stringResource(R.string.onboarding_settings_widgets_title),
            subtitle = stringResource(R.string.onboarding_settings_widgets_subtitle)
        )
    }
}

@Composable
private fun OnboardingReadyStep(
    autoTasbihEnabled: Boolean,
    autoAzkarEnabled: Boolean
) {
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
            text = stringResource(
                when {
                    autoTasbihEnabled && autoAzkarEnabled -> R.string.onboarding_ready_subtitle
                    autoTasbihEnabled -> R.string.onboarding_ready_subtitle_tasbih_only
                    autoAzkarEnabled -> R.string.onboarding_ready_subtitle_azkar_only
                    else -> R.string.onboarding_ready_subtitle_none
                }
            ),
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
    enabled: Boolean = true,
    onSelect: () -> Unit
) {
    val background = if (selected) Color.White else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                selected -> GreenPrimaryDark
                !enabled -> Color(0xFF555555).copy(alpha = 0.4f)
                else -> Color(0xFF555555)
            },
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
private fun OnboardingSectionToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else Color.Transparent)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = GreenPrimaryDark,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
            )
            if (!hint.isNullOrBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenPrimaryDark.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    canGoBack: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
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
                if (canGoBack) {
                    TextButton(onClick = onBack) {
                        Text(
                            text = stringResource(R.string.onboarding_back),
                            color = GreenPrimaryDark,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = if (canGoBack) Modifier else Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = Color.Gray,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (canGoBack) {
                        TextButton(onClick = onBack) {
                            Text(
                                text = stringResource(R.string.onboarding_back),
                                color = GreenPrimaryDark,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
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
