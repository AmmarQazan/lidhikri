package com.greendome.adhkar.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.service.AzkarCollectionPlayService
import com.greendome.adhkar.ui.theme.AppArabicFont
import com.greendome.adhkar.ui.theme.GreenDomeTheme
import com.greendome.adhkar.util.LocaleHelper

class OverlayActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapWithSavedLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val lockScreenMode = intent.getBooleanExtra(EXTRA_LOCK_SCREEN, false)
        if (lockScreenMode) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        val settings = SettingsRepository(this)
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        val isAutoAzkar = intent.getBooleanExtra(EXTRA_AUTO_AZKAR, false)
        val sectionTitle = intent.getStringExtra(EXTRA_SECTION_TITLE).orEmpty()
        val appearance = if (isAutoAzkar) settings.azkarPopupAppearance() else settings.tasbihPopupAppearance()
        val autoDismissSeconds = if (isAutoAzkar) {
            settings.azkarPopupAutoDismissSeconds
        } else {
            settings.tasbihPopupAutoDismissSeconds
        }
        setContent {
            GreenDomeTheme(themeMode = settings.appThemeMode) {
                AppArabicFont(settings.arabicFontStyle) {
                    if (isAutoAzkar) {
                        AutoAzkarOverlayCard(
                            sectionTitle = sectionTitle,
                            text = text,
                            appearance = appearance,
                            autoDismissSeconds = autoDismissSeconds,
                            onDismiss = { finish() },
                            onStopAuto = {
                                stopAutoAzkarPlayback()
                                finish()
                            }
                        )
                    } else {
                        DhikrOverlayCard(
                            text = text,
                            appearance = appearance,
                            autoDismissSeconds = autoDismissSeconds,
                            onDismiss = { finish() },
                            dimBackground = !lockScreenMode
                        )
                    }
                }
            }
        }
    }

    private fun stopAutoAzkarPlayback() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, AzkarCollectionPlayService::class.java).apply {
                action = AzkarCollectionPlayService.ACTION_STOP_AUTO_AZKAR
            }
        )
    }

    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_DHIKR_ID = "extra_dhikr_id"
        const val EXTRA_AUTO_AZKAR = "extra_auto_azkar"
        const val EXTRA_SECTION_TITLE = "extra_section_title"
        const val EXTRA_LOCK_SCREEN = "extra_lock_screen"

        fun lockScreenIntent(context: Context, dhikrId: Long, text: String) =
            Intent(context, OverlayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_DHIKR_ID, dhikrId)
                putExtra(EXTRA_LOCK_SCREEN, true)
            }

        fun autoAzkarIntent(context: Context, sectionTitle: String, text: String) =
            Intent(context, OverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_AUTO_AZKAR, true)
                putExtra(EXTRA_SECTION_TITLE, sectionTitle)
            }
    }
}
