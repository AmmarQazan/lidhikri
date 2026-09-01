package com.greendome.adhkar.ui.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.greendome.adhkar.R
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.service.AdhanAlarmScheduler
import com.greendome.adhkar.service.AdhanAlertNotifier
import com.greendome.adhkar.service.AdhanPlaybackService
import com.greendome.adhkar.service.AzkarCollectionPlayService
import com.greendome.adhkar.ui.theme.AppArabicFont
import com.greendome.adhkar.ui.theme.GreenDomeTheme
import com.greendome.adhkar.util.LocaleHelper

class AdhanActivity : ComponentActivity() {

    private val finished = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isFinishing) finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapWithSavedLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        val prayer = runCatching {
            PrayerName.valueOf(intent?.getStringExtra(AdhanAlarmScheduler.EXTRA_PRAYER).orEmpty())
        }.getOrNull() ?: PrayerName.DHUHR
        val settings = SettingsRepository(this)
        ContextCompat.registerReceiver(
            this,
            finished,
            IntentFilter(AdhanPlaybackService.ACTION_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        setContent {
            GreenDomeTheme(themeMode = settings.appThemeMode) {
                AppArabicFont(settings.arabicFontStyle) {
                    AdhanScreen(
                        prayerName = AdhanAlertNotifier.prayerLabel(this, prayer),
                        onStop = {
                            AdhanPlaybackService.stop(this)
                            finish()
                        },
                        onOpenAzkar = {
                            AdhanPlaybackService.stop(this)
                            ContextCompat.startForegroundService(
                                this,
                                Intent(this, AzkarCollectionPlayService::class.java).apply {
                                    putExtra(
                                        AzkarCollectionPlayService.EXTRA_COLLECTION_ID,
                                        AdhanPlaybackService.ADHAN_AZKAR_ID
                                    )
                                    putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
                                }
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(finished) }
        super.onDestroy()
    }
}

@Composable
private fun AdhanScreen(
    prayerName: String,
    onStop: () -> Unit,
    onOpenAzkar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.adhan_now_text),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            prayerName,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.adhan_stop))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onOpenAzkar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.adhan_open_azkar))
        }
    }
}
