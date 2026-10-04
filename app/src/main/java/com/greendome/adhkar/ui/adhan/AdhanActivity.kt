package com.greendome.adhkar.ui.adhan

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
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
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.service.AdhanAlarmScheduler
import com.greendome.adhkar.service.AdhanAlertNotifier
import com.greendome.adhkar.service.AdhanPlaybackService
import com.greendome.adhkar.service.AzkarCollectionPlayService
import com.greendome.adhkar.service.ForegroundServiceStarts
import com.greendome.adhkar.service.PrayerPhoneSilent
import com.greendome.adhkar.ui.overlay.OverlayActivity
import com.greendome.adhkar.ui.overlay.OverlayWindow
import com.greendome.adhkar.ui.theme.AppArabicFont
import com.greendome.adhkar.ui.theme.GreenDomeTheme
import com.greendome.adhkar.util.AzkarDailyPicker
import com.greendome.adhkar.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdhanActivity : ComponentActivity() {

    private var openingAfterAdhanAzkar = false

    private val finished = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (openingAfterAdhanAzkar || isFinishing) return
            finish()
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
                        onOpenAzkar = { openAfterAdhanAzkar() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(finished) }
        super.onDestroy()
    }

    /**
     * النافذة تُفتح من هنا والنشاط ما زال في المقدمة.
     * إيقاف الأذان يبث انتهاء الأذان ويغلق هذه الشاشة، وفتح الأذكار بعده يرفضه أندرويد.
     */
    private fun openAfterAdhanAzkar() {
        if (openingAfterAdhanAzkar || isFinishing) return
        openingAfterAdhanAzkar = true
        lifecycleScope.launch {
            val itemId = showAfterAdhanAzkarWindow()
            AdhanPlaybackService.stopForAzkar(this@AdhanActivity)
            if (itemId != null) {
                ForegroundServiceStarts.start(
                    this@AdhanActivity,
                    Intent(this@AdhanActivity, AzkarCollectionPlayService::class.java).apply {
                        putExtra(
                            AzkarCollectionPlayService.EXTRA_COLLECTION_ID,
                            AdhanPlaybackService.ADHAN_AZKAR_ID,
                        )
                        putExtra(AzkarCollectionPlayService.EXTRA_ITEM_ID, itemId)
                        putExtra(AzkarCollectionPlayService.EXTRA_FORCE_PLAY, true)
                        putExtra(AzkarCollectionPlayService.EXTRA_SKIP_UI, true)
                    },
                )
            } else {
                PrayerPhoneSilent.enter(this@AdhanActivity)
            }
            if (!isFinishing) finish()
        }
    }

    private suspend fun showAfterAdhanAzkarWindow(): Long? {
        if (isFinishing) return null
        val settings = SettingsRepository(this)
        val db = AdhkarDatabase.get(this)
        val now = System.currentTimeMillis()
        val loaded = withContext(Dispatchers.IO) {
            val collection = db.collectionDao().getById(AdhanPlaybackService.ADHAN_AZKAR_ID)
            val items = db.azkarItemDao().getByCollection(AdhanPlaybackService.ADHAN_AZKAR_ID)
            collection to items
        }
        val (collection, items) = loaded
        if (isFinishing || collection == null || items.isEmpty()) return null
        val candidates = items.filter { !it.hasOwnHijri() || it.matchesHijri(now) }
        val picked = AzkarDailyPicker.pick(
            this,
            AdhanPlaybackService.ADHAN_AZKAR_ID,
            candidates,
            settings.autoAzkarRandomMode,
        ) ?: return null
        if (isFinishing) return null
        val title = collection.localizedTitle(settings.appLanguage)
        val text = picked.localizedText(settings.appLanguage)
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (OverlayWindow.hasPermission(this)) {
            OverlayWindow.showAutoAzkar(
                context = this,
                sectionTitle = title,
                text = text,
                onDismiss = { OverlayWindow.dismiss(applicationContext) },
                onStopAuto = {
                    AzkarCollectionPlayService.stopAutoAzkar(this)
                    OverlayWindow.dismiss(applicationContext)
                },
            )
        } else if (keyguard.isKeyguardLocked) {
            startActivity(OverlayActivity.lockScreenAutoAzkarIntent(this, title, text, picked.id))
        } else {
            startActivity(OverlayActivity.autoAzkarIntent(this, title, text))
        }
        return picked.id
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
