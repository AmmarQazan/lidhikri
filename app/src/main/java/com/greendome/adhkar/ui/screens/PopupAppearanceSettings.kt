package com.greendome.adhkar.ui.screens



import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.Slider

import androidx.compose.material3.Text

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

import com.greendome.adhkar.data.model.PopupAppearance

import com.greendome.adhkar.data.model.PopupSettingsTarget

import com.greendome.adhkar.ui.overlay.OverlayWindow
import com.greendome.adhkar.ui.theme.stringResourceDigits



@Composable

fun PopupAppearanceSettings(

    settings: SettingsRepository,

    target: PopupSettingsTarget,

    modifier: Modifier = Modifier

) {

    val context = androidx.compose.ui.platform.LocalContext.current

    var positionX by remember(target) { mutableStateOf(settings.popupPositionX(target)) }

    var positionY by remember(target) { mutableStateOf(settings.popupPositionY(target)) }

    var boxWidth by remember(target) { mutableStateOf(settings.popupBoxWidthFraction(target)) }

    var popupFont by remember(target) { mutableStateOf(settings.popupFontScale(target)) }

    var autoDismiss by remember(target) {

        mutableStateOf(settings.popupAutoDismissSeconds(target).toFloat())

    }



    val maxAutoDismiss = when (target) {

        PopupSettingsTarget.TASBIH -> PopupAppearance.MAX_AUTO_DISMISS_SECONDS

        PopupSettingsTarget.AZKAR -> PopupAppearance.AZKAR_MAX_AUTO_DISMISS_SECONDS

    }



    val previewSample = when (target) {

        PopupSettingsTarget.TASBIH -> stringResource(R.string.popup_preview_sample)

        PopupSettingsTarget.AZKAR -> stringResource(R.string.azkar_popup_preview_sample)

    }



    Column(modifier = modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {

        OutlinedButton(

            onClick = {

                if (OverlayWindow.hasPermission(context)) {

                    when (target) {

                        PopupSettingsTarget.TASBIH -> OverlayWindow.showTasbih(context, previewSample)

                        PopupSettingsTarget.AZKAR -> OverlayWindow.showAzkarPreview(

                            context,

                            context.getString(R.string.azkar_popup_preview_section),

                            previewSample

                        )

                    }

                } else {

                    context.startActivity(

                        android.content.Intent(

                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,

                            android.net.Uri.parse("package:${context.packageName}")

                        )

                    )

                }

            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Text(

                if (OverlayWindow.hasPermission(context)) {

                    stringResource(R.string.popup_preview_on_device)

                } else {

                    stringResource(R.string.permission_overlay)

                }

            )

        }



        Text(stringResource(R.string.popup_position_x))

        Slider(

            value = positionX,

            onValueChange = {

                positionX = it

                settings.setPopupPositionX(target, it)

            }

        )

        Text(

            popupPositionLabel(positionX, horizontal = true),

            style = MaterialTheme.typography.bodySmall

        )



        Text(stringResource(R.string.popup_position_y))

        Slider(

            value = positionY,

            onValueChange = {

                positionY = it

                settings.setPopupPositionY(target, it)

            }

        )

        Text(

            popupPositionLabel(positionY, horizontal = false),

            style = MaterialTheme.typography.bodySmall

        )



        Text(stringResource(R.string.popup_box_width))

        Slider(

            value = boxWidth,

            onValueChange = {

                boxWidth = it

                settings.setPopupBoxWidthFraction(target, it)

            },

            valueRange = PopupAppearance.MIN_BOX_WIDTH..PopupAppearance.MAX_BOX_WIDTH

        )

        Text(

            stringResourceDigits(R.string.popup_box_width_value, (boxWidth * 100).toInt()),

            style = MaterialTheme.typography.bodySmall

        )



        Text(stringResource(R.string.popup_font_scale))

        Slider(

            value = popupFont,

            onValueChange = {

                popupFont = it

                settings.setPopupFontScale(target, it)

            },

            valueRange = PopupAppearance.MIN_FONT_SCALE..PopupAppearance.MAX_FONT_SCALE

        )

        Text(

            stringResourceDigits(R.string.popup_font_scale_value, (popupFont * 100).toInt()),

            style = MaterialTheme.typography.bodySmall

        )



        Text(stringResource(R.string.popup_auto_dismiss))

        Slider(

            value = autoDismiss,

            onValueChange = {

                val seconds = it.toInt().coerceIn(

                    PopupAppearance.MIN_AUTO_DISMISS_SECONDS,

                    maxAutoDismiss

                )

                autoDismiss = seconds.toFloat()

                settings.setPopupAutoDismissSeconds(target, seconds)

            },

            valueRange = PopupAppearance.MIN_AUTO_DISMISS_SECONDS.toFloat()..maxAutoDismiss.toFloat(),

            steps = maxAutoDismiss

        )

        Text(

            if (autoDismiss.toInt() == 0) {

                stringResource(R.string.popup_auto_dismiss_manual)

            } else {

                stringResourceDigits(R.string.popup_auto_dismiss_value, autoDismiss.toInt())

            },

            style = MaterialTheme.typography.bodySmall

        )



    }

}



private fun SettingsRepository.popupPositionX(target: PopupSettingsTarget): Float = when (target) {

    PopupSettingsTarget.TASBIH -> tasbihPopupPositionX

    PopupSettingsTarget.AZKAR -> azkarPopupPositionX

}



private fun SettingsRepository.popupPositionY(target: PopupSettingsTarget): Float = when (target) {

    PopupSettingsTarget.TASBIH -> tasbihPopupPositionY

    PopupSettingsTarget.AZKAR -> azkarPopupPositionY

}



private fun SettingsRepository.popupBoxWidthFraction(target: PopupSettingsTarget): Float = when (target) {

    PopupSettingsTarget.TASBIH -> tasbihPopupBoxWidthFraction

    PopupSettingsTarget.AZKAR -> azkarPopupBoxWidthFraction

}



private fun SettingsRepository.popupFontScale(target: PopupSettingsTarget): Float = when (target) {

    PopupSettingsTarget.TASBIH -> tasbihPopupFontScale

    PopupSettingsTarget.AZKAR -> azkarPopupFontScale

}



private fun SettingsRepository.setPopupPositionX(target: PopupSettingsTarget, value: Float) {

    when (target) {

        PopupSettingsTarget.TASBIH -> tasbihPopupPositionX = value

        PopupSettingsTarget.AZKAR -> azkarPopupPositionX = value

    }

}



private fun SettingsRepository.setPopupPositionY(target: PopupSettingsTarget, value: Float) {

    when (target) {

        PopupSettingsTarget.TASBIH -> tasbihPopupPositionY = value

        PopupSettingsTarget.AZKAR -> azkarPopupPositionY = value

    }

}



private fun SettingsRepository.setPopupBoxWidthFraction(target: PopupSettingsTarget, value: Float) {

    when (target) {

        PopupSettingsTarget.TASBIH -> tasbihPopupBoxWidthFraction = value

        PopupSettingsTarget.AZKAR -> azkarPopupBoxWidthFraction = value

    }

}



private fun SettingsRepository.setPopupFontScale(target: PopupSettingsTarget, value: Float) {

    when (target) {

        PopupSettingsTarget.TASBIH -> tasbihPopupFontScale = value

        PopupSettingsTarget.AZKAR -> azkarPopupFontScale = value

    }

}



private fun SettingsRepository.setPopupAutoDismissSeconds(target: PopupSettingsTarget, value: Int) {

    when (target) {

        PopupSettingsTarget.TASBIH -> tasbihPopupAutoDismissSeconds = value

        PopupSettingsTarget.AZKAR -> azkarPopupAutoDismissSeconds = value

    }

}



@Composable

private fun popupPositionLabel(value: Float, horizontal: Boolean): String {

    val percent = (value * 100).toInt()

    return if (horizontal) {

        stringResourceDigits(R.string.popup_position_x_value, percent)

    } else {

        stringResourceDigits(R.string.popup_position_y_value, percent)

    }

}


