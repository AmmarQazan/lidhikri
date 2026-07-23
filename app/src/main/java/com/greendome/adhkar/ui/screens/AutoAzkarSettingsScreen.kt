package com.greendome.adhkar.ui.screens



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.unit.dp

import com.greendome.adhkar.R

import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.PopupSettingsTarget



@Composable

fun AutoAzkarSettingsScreen(

    settings: SettingsRepository,

    autoAzkarEnabled: Boolean,

    autoAzkarRandom: Boolean,

    onToggleAutoAzkar: (Boolean) -> Unit,

    onAutoAzkarRandomChange: (Boolean) -> Unit,

    onBack: () -> Unit,

    modifier: Modifier = Modifier

) {

    var autoAzkarOn by remember { mutableStateOf(autoAzkarEnabled) }
    var autoAzkarRandomMode by remember { mutableStateOf(autoAzkarRandom) }



    SettingsSubScreenScaffold(

        title = stringResource(R.string.settings_auto_azkar_title),

        onBack = onBack,

        modifier = modifier

    ) {

        LazyColumn(

            modifier = Modifier.fillMaxWidth(),

            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            item {

                SectionTitle(

                    title = stringResource(R.string.auto_azkar_section),

                    subtitle = stringResource(R.string.auto_azkar_hint)

                )

            }

            item {

                SettingSwitch(

                    label = if (autoAzkarOn) stringResource(R.string.auto_azkar_on)

                    else stringResource(R.string.auto_azkar_off),

                    checked = autoAzkarOn,

                    onChange = {

                        autoAzkarOn = it

                        onToggleAutoAzkar(it)

                    }

                )

            }

            item { AutoAzkarModeLabel() }

            item {

                AutoAzkarModeSelector(

                    randomMode = autoAzkarRandomMode,

                    onRandomModeChange = {

                        autoAzkarRandomMode = it

                        onAutoAzkarRandomChange(it)

                    }

                )

            }

            item { AutoAzkarScheduleExplainer() }

            item {

                SectionTitle(

                    title = stringResource(R.string.azkar_popup_settings_section),

                    subtitle = stringResource(R.string.azkar_popup_settings_hint)

                )

            }

            item { PopupAppearanceSettings(settings = settings, target = PopupSettingsTarget.AZKAR) }

        }

    }

}


