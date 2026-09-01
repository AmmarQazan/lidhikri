package com.greendome.adhkar.data

import android.content.Context
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.service.AfterPrayerAlarmScheduler
import com.greendome.adhkar.service.CollectionAlarmScheduler
import com.greendome.adhkar.service.HomeGeofenceScheduler
import com.greendome.adhkar.service.ReminderScheduler
import com.greendome.adhkar.service.VehicleActivityScheduler
import com.greendome.adhkar.util.TasbihWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepository(
    private val db: AdhkarDatabase,
    private val context: Context
) {
    private val collectionDao = db.collectionDao()
    private val itemDao = db.azkarItemDao()

    fun observeCollections(): Flow<List<AdhkarCollectionEntity>> = collectionDao.observeAll()

    fun observeItems(collectionId: String): Flow<List<AzkarItemEntity>> =
        itemDao.observeByCollection(collectionId)

    fun observeAllItems(): Flow<List<AzkarItemEntity>> =
        itemDao.observeAll().map { items ->
            items.filter { it.collectionId != AzkarFavorites.COLLECTION_ID }
        }

    suspend fun getCollection(id: String) = collectionDao.getById(id)

    suspend fun getItems(collectionId: String) = itemDao.getByCollection(collectionId)

    suspend fun saveCollection(collection: AdhkarCollectionEntity) {
        val previousMorning = if (collection.id == "morning") collectionDao.getById("morning") else null
        val normalized = collection.copy(
            autoPlayEnabled = collection.autoPlayAllowed && collection.autoPlayEnabled,
            weekDaysMask = when (collection.id) {
                FridayAzkar.COLLECTION_ID -> FridayAzkar.fridayOnlyMask()
                BlessedDaysAzkar.COLLECTION_ID -> 127
                else -> collection.weekDaysMask
            },
            dayMode = when (collection.id) {
                FridayAzkar.COLLECTION_ID -> CollectionDayMode.WEEKDAYS
                BlessedDaysAzkar.COLLECTION_ID -> CollectionDayMode.ITEM_HIJRI
                else -> collection.dayMode
            },
        )
        collectionDao.insert(normalized)
        if (previousMorning != null) {
            followMorningForBlessedDays(
                morningHour = normalized.scheduleHour,
                morningMinute = normalized.scheduleMinute,
                previousMorningHour = previousMorning.scheduleHour,
                previousMorningMinute = previousMorning.scheduleMinute,
            )
        }
        rescheduleAllAlarms()
    }

    suspend fun initializeTasbihWindowIfNeeded() {
        val settings = SettingsRepository(context)
        if (settings.hasTasbihWindowSet) return
        if (settings.autoAzkarEnabled) {
            val morning = collectionDao.getById("morning")
            val sleep = collectionDao.getById("sleep")
            if (morning != null && sleep != null) {
                settings.setTasbihWindow(
                    morning.scheduleHour,
                    morning.scheduleMinute,
                    sleep.scheduleHour,
                    sleep.scheduleMinute
                )
                return
            }
        }
        settings.setTasbihWindow(
            TasbihWindow.FALLBACK_START_HOUR,
            TasbihWindow.FALLBACK_START_MINUTE,
            TasbihWindow.FALLBACK_END_HOUR,
            TasbihWindow.FALLBACK_END_MINUTE
        )
    }

    suspend fun applyOnboardingAzkarSchedule(
        enabled: Boolean,
        morningHour: Int,
        morningMinute: Int,
        sleepHour: Int,
        sleepMinute: Int,
        enabledCollectionIds: Set<String> = AutoAzkarCatalog.defaultEnabledClockIds(),
        afterPrayerEnabled: Boolean = false,
        afterAdhanEnabled: Boolean = AutoAzkarCatalog.spec(AdhanAzkar.COLLECTION_ID)?.defaultEnabled == true,
        homeAzkarEnabled: Boolean = AutoAzkarCatalog.spec(HomeAzkar.COLLECTION_ID)?.defaultEnabled == true,
        homeLocation: com.greendome.adhkar.prayer.PrayerLocation? = null,
        ridingAzkarEnabled: Boolean = AutoAzkarCatalog.spec(RidingAzkar.COLLECTION_ID)?.defaultEnabled == true,
    ) {
        if (enabled) {
            val settings = SettingsRepository(context)
            settings.afterPrayerFromSalahEnabled = afterPrayerEnabled
            settings.afterAdhanAzkarEnabled = afterAdhanEnabled
            settings.homeAzkarEnabled = homeAzkarEnabled
            settings.ridingAzkarEnabled = ridingAzkarEnabled
            homeLocation?.let { settings.setHomeLocation(it) }
            AzkarFavorites.ensureCollection(
                db,
                autoPlayEnabled = AzkarFavorites.COLLECTION_ID in enabledCollectionIds
            )
            val selectableIds = AutoAzkarCatalog.onboardingSpecs()
                .filter { it.trigger == AutoAzkarCatalog.Trigger.CLOCK }
                .map { it.id }
                .toSet()
            collectionDao.getAll().forEach { collection ->
                if (!collection.autoPlayAllowed) return@forEach
                var next = collection.copy(
                    autoPlayEnabled = if (collection.id in selectableIds) {
                        collection.id in enabledCollectionIds
                    } else {
                        collection.autoPlayEnabled
                    }
                )
                if (collection.id == "morning") {
                    next = next.copy(
                        scheduleHour = morningHour.coerceIn(0, 23),
                        scheduleMinute = morningMinute.coerceIn(0, 59)
                    )
                } else if (collection.id == "sleep") {
                    next = next.copy(
                        scheduleHour = sleepHour.coerceIn(0, 23),
                        scheduleMinute = sleepMinute.coerceIn(0, 59)
                    )
                } else if (collection.id == BlessedDaysAzkar.COLLECTION_ID) {
                    val (hour, minute) = BlessedDaysAzkar.daytimeFromMorning(morningHour, morningMinute)
                    next = next.copy(scheduleHour = hour, scheduleMinute = minute)
                }
                collectionDao.update(next)
            }
        }
        rescheduleAllAlarms()
    }

    suspend fun rescheduleAllAlarms() {
        val settings = SettingsRepository(context)
        val autoAzkarOn = settings.autoAzkarEnabled
        val itemsByCollection = itemDao.getAll().groupBy { it.collectionId }
        collectionDao.getAll().forEach { collection ->
            if (autoAzkarOn && collection.autoPlayAllowed && collection.autoPlayEnabled) {
                CollectionAlarmScheduler.schedule(
                    context,
                    collection,
                    itemsByCollection[collection.id].orEmpty(),
                )
            } else {
                CollectionAlarmScheduler.cancel(context, collection.id)
            }
        }
        if (settings.isServiceEnabled) {
            ReminderScheduler.scheduleNext(context)
        }
        AfterPrayerAlarmScheduler.reschedule(context)
        HomeGeofenceScheduler.register(context)
        VehicleActivityScheduler.register(context)
    }

    suspend fun saveItem(item: AzkarItemEntity): Long {
        val id = if (item.id == 0L) itemDao.insert(item) else {
            itemDao.update(item)
            item.id
        }
        rescheduleAllAlarms()
        return id
    }

    suspend fun deleteItem(id: Long) {
        itemDao.delete(id)
        rescheduleAllAlarms()
    }

    fun observeFavoriteSourceIds(): Flow<Set<Long>> =
        itemDao.observeFavoriteSourceIds(AzkarFavorites.COLLECTION_ID)
            .map { ids -> ids.filterNotNull().toSet() }

    suspend fun toggleFavorite(item: AzkarItemEntity) {
        if (item.collectionId == AzkarFavorites.COLLECTION_ID) {
            item.sourceItemId?.let { removeFavorite(it) }
            return
        }
        val existing = itemDao.getBySourceItemId(AzkarFavorites.COLLECTION_ID, item.id)
        if (existing != null) {
            removeFavorite(item.id)
        } else {
            addFavorite(item)
        }
    }

    private suspend fun addFavorite(sourceItem: AzkarItemEntity) {
        ensureFavoritesCollection()
        val sortOrder = itemDao.countByCollection(AzkarFavorites.COLLECTION_ID) + 1
        itemDao.insert(
            AzkarItemEntity(
                collectionId = AzkarFavorites.COLLECTION_ID,
                textAr = sourceItem.textAr,
                virtueAr = sourceItem.virtueAr,
                repeatCount = sourceItem.repeatCount,
                sortOrder = sortOrder,
                sourceItemId = sourceItem.id
            )
        )
        rescheduleAllAlarms()
    }

    private suspend fun removeFavorite(sourceItemId: Long) {
        itemDao.deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, sourceItemId)
        if (itemDao.countByCollection(AzkarFavorites.COLLECTION_ID) == 0) {
            CollectionAlarmScheduler.cancel(context, AzkarFavorites.COLLECTION_ID)
        }
        rescheduleAllAlarms()
    }

    private suspend fun followMorningForBlessedDays(
        morningHour: Int,
        morningMinute: Int,
        previousMorningHour: Int,
        previousMorningMinute: Int,
    ) {
        val blessed = collectionDao.getById(BlessedDaysAzkar.COLLECTION_ID) ?: return
        if (!BlessedDaysAzkar.shouldFollowMorning(
                blessed.scheduleHour,
                blessed.scheduleMinute,
                previousMorningHour,
                previousMorningMinute,
            )
        ) {
            return
        }
        val (hour, minute) = BlessedDaysAzkar.daytimeFromMorning(morningHour, morningMinute)
        if (blessed.scheduleHour == hour && blessed.scheduleMinute == minute) return
        collectionDao.update(blessed.copy(scheduleHour = hour, scheduleMinute = minute))
    }

    private suspend fun ensureFavoritesCollection() {
        AzkarFavorites.ensureCollection(db)
    }
}
