package com.greendome.adhkar.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.prayer.AsrMadhabPref
import com.greendome.adhkar.prayer.CalculationMethodPref
import com.greendome.adhkar.prayer.DstMode
import com.greendome.adhkar.prayer.PrayerLocation
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.prayer.PrayerQuietWindows
import com.greendome.adhkar.prayer.PrayerTimesCalculator
import com.greendome.adhkar.prayer.TimezoneMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PrayerPhoneSilentEmulatorTest {
    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext
    private val settings = SettingsRepository(context)
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notifications = context.getSystemService(NotificationManager::class.java)

    private var savedSilent = false
    private var savedRespect = false
    private var savedMethod = CalculationMethodPref.AUTO
    private var savedMadhab = AsrMadhabPref.AUTO
    private var savedTzMode = TimezoneMode.AUTO
    private var savedTzId = ""
    private var savedDst = DstMode.AUTO
    private var savedLocation: PrayerLocation? = null
    private val savedDelays = linkedMapOf<PrayerName, Int>()

    @Before
    fun saveSettings() {
        savedSilent = settings.silentDuringFardPrayer
        savedRespect = settings.respectPrayerTime
        savedMethod = settings.prayerCalculationMethod
        savedMadhab = settings.prayerAsrMadhab
        savedTzMode = settings.prayerTimezoneMode
        savedTzId = settings.prayerTimezoneId
        savedDst = settings.prayerDstMode
        savedLocation = settings.prayerConfig().location
        PrayerName.entries.forEach { savedDelays[it] = settings.prayerAfterDelayMinutes(it) }
        if (settings.prayerPhoneSilentActive) {
            PrayerPhoneSilent.exit(context)
        }
    }

    @After
    fun restoreSettings() {
        PrayerPhoneSilent.exit(context)
        settings.silentDuringFardPrayer = savedSilent
        settings.respectPrayerTime = savedRespect
        settings.prayerCalculationMethod = savedMethod
        settings.prayerAsrMadhab = savedMadhab
        settings.prayerTimezoneMode = savedTzMode
        settings.prayerTimezoneId = savedTzId
        settings.prayerDstMode = savedDst
        val location = savedLocation
        if (location != null) settings.setPrayerLocation(location)
        savedDelays.forEach { (prayer, minutes) ->
            settings.setPrayerAfterDelayMinutes(prayer, minutes)
        }
        PrayerPhoneSilent.reschedule(context)
    }

    @Test
    fun logInstants() {
        configureRiyadh()
        val config = settings.prayerConfig()
        val probe = Instant.parse("2026-09-27T09:00:00Z").toEpochMilli()
        val day = PrayerTimesCalculator.timesFor(config, probe)
        checkNotNull(day) { "no prayer times" }
        val dhuhr = checkNotNull(day.timeOf(PrayerName.DHUHR))
        val delayMin = config.afterPrayerDelay(PrayerName.DHUHR, friday = false).toLong()
        val inside = dhuhr + 60_000L
        val outside = dhuhr + delayMin * 60_000L + 120_000L
        val now = System.currentTimeMillis()
        val activeEnd = PrayerQuietWindows.activePhoneSilentEndAt(config, now)
        probe(
            "now=$now insideEpoch=$inside outsideEpoch=$outside " +
                "dhuhr=$dhuhr delayMin=$delayMin activeEnd=${activeEnd ?: 0}"
        )
    }

    @Test
    fun insideWindowVibratesWithoutLeavingDnd() {
        check(PrayerPhoneSilent.hasPolicyAccess(context)) { "notification policy not granted" }
        configureRiyadh()
        val config = settings.prayerConfig()
        val end = PrayerQuietWindows.activePhoneSilentEndAt(config)
        assertNotNull("clock is outside the prayer window", end)
        val beforeRinger = audio.ringerMode
        val beforeZen = zenMode()
        val beforeFilter = notifications.currentInterruptionFilter
        probe("inside before ringer=$beforeRinger zen=$beforeZen filter=$beforeFilter end=$end")

        PrayerPhoneSilent.enter(context)

        val duringRinger = audio.ringerMode
        val duringZen = zenMode()
        val duringFilter = notifications.currentInterruptionFilter
        probe(
            "inside during ringer=$duringRinger zen=$duringZen filter=$duringFilter " +
                "active=${settings.prayerPhoneSilentActive}"
        )
        assertTrue(settings.prayerPhoneSilentActive)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, duringRinger)
        if (beforeZen == ZEN_OFF) {
            assertEquals("Do Not Disturb turned on", ZEN_OFF, duringZen)
        }
        if (beforeFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            assertEquals(
                "interruption filter changed",
                NotificationManager.INTERRUPTION_FILTER_ALL,
                duringFilter
            )
        }

        PrayerPhoneSilent.exit(context)
        probe(
            "inside after ringer=${audio.ringerMode} zen=${zenMode()} " +
                "filter=${notifications.currentInterruptionFilter} " +
                "active=${settings.prayerPhoneSilentActive}"
        )
        assertFalse(settings.prayerPhoneSilentActive)
        assertEquals(beforeRinger, audio.ringerMode)
        assertEquals(beforeZen, zenMode())
        assertEquals(beforeFilter, notifications.currentInterruptionFilter)
    }

    @Test
    fun outsideWindowDoesNotChangeRingerOrDnd() {
        check(PrayerPhoneSilent.hasPolicyAccess(context)) { "notification policy not granted" }
        configureRiyadh()
        val config = settings.prayerConfig()
        assertNull(
            "clock is still inside a prayer window",
            PrayerQuietWindows.activePhoneSilentEndAt(config)
        )
        val beforeRinger = audio.ringerMode
        val beforeZen = zenMode()
        val beforeFilter = notifications.currentInterruptionFilter
        probe("outside before ringer=$beforeRinger zen=$beforeZen filter=$beforeFilter")

        PrayerPhoneSilent.enter(context)

        probe(
            "outside after ringer=${audio.ringerMode} zen=${zenMode()} " +
                "filter=${notifications.currentInterruptionFilter} " +
                "active=${settings.prayerPhoneSilentActive}"
        )
        assertFalse(settings.prayerPhoneSilentActive)
        assertEquals(beforeRinger, audio.ringerMode)
        assertEquals(beforeZen, zenMode())
        assertEquals(beforeFilter, notifications.currentInterruptionFilter)
    }

    private fun configureRiyadh() {
        settings.respectPrayerTime = true
        settings.silentDuringFardPrayer = true
        settings.prayerCalculationMethod = CalculationMethodPref.UMM_AL_QURA
        settings.prayerAsrMadhab = AsrMadhabPref.SHAFI
        settings.prayerTimezoneMode = TimezoneMode.MANUAL
        settings.prayerTimezoneId = "Asia/Riyadh"
        settings.prayerDstMode = DstMode.AUTO
        settings.setPrayerLocation(
            PrayerLocation(
                latitude = 24.7136,
                longitude = 46.6753,
                cityName = "الرياض",
                countryName = "السعودية",
                countryCode = "SA",
                timezoneId = "Asia/Riyadh",
            )
        )
        PrayerName.entries.forEach { settings.setPrayerAfterDelayMinutes(it, 30) }
    }

    private fun zenMode(): Int =
        Settings.Global.getInt(context.contentResolver, "zen_mode", -1)

    private fun probe(message: String) {
        Log.i(TAG, message)
    }

    companion object {
        private const val TAG = "SILENT_PROBE"
        private const val ZEN_OFF = 0
    }
}
