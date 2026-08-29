package com.greendome.adhkar.prayer

import android.content.Context
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TravelLocationUpdater {
    suspend fun maybeRefresh(context: Context) = withContext(Dispatchers.IO) {
        val settings = SettingsRepository(context)
        val config = settings.prayerConfig()
        if (!config.enabled || !config.travelAutoUpdate || config.locationMode != LocationMode.GPS) {
            return@withContext
        }
        if (!DeviceLocation.hasPermission(context)) return@withContext
        val now = System.currentTimeMillis()
        if (now - settings.prayerLastTravelCheckAt < PrayerConfig.TRAVEL_CHECK_INTERVAL_MS) {
            return@withContext
        }
        settings.prayerLastTravelCheckAt = now
        val current = DeviceLocation.lastKnown(context) ?: return@withContext
        val previous = config.location ?: return@withContext
        val moved = distanceMeters(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude
        )
        if (moved < PrayerConfig.TRAVEL_DISTANCE_METERS) return@withContext
        val resolved = CityLocator.reverse(context, current.latitude, current.longitude)
        settings.setPrayerLocation(resolved, LocationMode.GPS)
        ReminderScheduler.scheduleNext(context)
        AfterPrayerAlarmScheduler.reschedule(context)
    }
}
