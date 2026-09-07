package com.greendome.adhkar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.greendome.adhkar.data.AdhanAudioRepository
import com.greendome.adhkar.data.CollectionRepository
import com.greendome.adhkar.data.DailyStatsRepository
import com.greendome.adhkar.data.DhikrRepository
import com.greendome.adhkar.data.ReciterRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.prayer.PrayerRespectGate
import com.greendome.adhkar.review.InAppReviewTracker
import com.greendome.adhkar.service.PrayerAlarms
import com.greendome.adhkar.service.ReminderScheduler
import com.greendome.adhkar.sync.PendingPublishRepository
import com.greendome.adhkar.sync.PendingPublishType
import com.greendome.adhkar.ui.screens.OnboardingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AdhkarDatabase.get(app)
    private val settings = SettingsRepository(app)
    private val dhikrRepo = DhikrRepository(db)
    private val reciterRepo = ReciterRepository(db)
    private val adhanAudioRepo = AdhanAudioRepository(db)
    private val collectionRepo = CollectionRepository(db, app)
    private val pendingPublishRepo = PendingPublishRepository(db)

    val dhikrList = dhikrRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val longFormList = dhikrRepo.observeLongForm().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reciters = reciterRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReciters = reciterRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAdhanAudio = adhanAudioRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val azkarCollections = collectionRepo.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAzkarItems = collectionRepo.observeAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _azkarHubOrder = MutableStateFlow(settings.azkarHubOrder)
    val azkarHubOrder: StateFlow<List<String>> = _azkarHubOrder.asStateFlow()
    val pendingPublishChanges = pendingPublishRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val statsRepo = DailyStatsRepository(db)

    private val _todayTasbihCount = MutableStateFlow(0)
    val todayTasbihCount: StateFlow<Int> = _todayTasbihCount.asStateFlow()

    private val _todayAzkarCount = MutableStateFlow(0)
    val todayAzkarCount: StateFlow<Int> = _todayAzkarCount.asStateFlow()

    private val _todayMisbahaCount = MutableStateFlow(0)
    val todayMisbahaCount: StateFlow<Int> = _todayMisbahaCount.asStateFlow()

    private val _minutesUntilNext = MutableStateFlow(0)
    val minutesUntilNext: StateFlow<Int> = _minutesUntilNext.asStateFlow()

    private val _isPrayerQuiet = MutableStateFlow(false)
    val isPrayerQuiet: StateFlow<Boolean> = _isPrayerQuiet.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _todayTasbihCount.value = statsRepo.tasbihTodayCount()
            _todayAzkarCount.value = statsRepo.azkarTodayCount()
            _todayMisbahaCount.value = statsRepo.misbahaTodayCount()
            _minutesUntilNext.value = ReminderScheduler.minutesUntilNext(getApplication())
            _isPrayerQuiet.value = PrayerRespectGate.isQuiet(getApplication())
        }
    }

    private val _tasbihActivityLog = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tasbihActivityLog: StateFlow<Map<String, Int>> = _tasbihActivityLog.asStateFlow()

    fun recordMisbahaCount() {
        InAppReviewTracker(getApplication()).addTasbih(1)
        viewModelScope.launch {
            statsRepo.incrementMisbahaToday()
            _todayMisbahaCount.value = statsRepo.misbahaTodayCount()
            loadTasbihActivityLog()
        }
    }

    fun loadTasbihActivityLog(monthsBack: Int = 3) {
        viewModelScope.launch {
            _tasbihActivityLog.value = statsRepo.tasbihActivityMap(monthsBack)
        }
    }

    fun settingsRepo() = settings

    fun saveDhikr(
        entity: DhikrEntity,
        markPendingPublish: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = dhikrRepo.save(entity)
            if (markPendingPublish) {
                pendingPublishRepo.recordUpsert(PendingPublishType.DHIKR, id)
            }
            onDone()
        }
    }

    fun deleteDhikr(id: Long, markPendingPublish: Boolean = false) {
        viewModelScope.launch {
            dhikrRepo.delete(id)
            if (markPendingPublish) {
                pendingPublishRepo.recordDelete(PendingPublishType.DHIKR, id)
            }
        }
    }

    fun saveCollection(
        collection: AdhkarCollectionEntity,
        markPendingPublish: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            collectionRepo.saveCollection(collection)
            if (markPendingPublish) {
                pendingPublishRepo.recordUpsert(PendingPublishType.COLLECTION, 0, collection.id)
            }
            onDone()
        }
    }

    fun setAutoAzkarEnabled(enabled: Boolean) {
        settings.autoAzkarEnabled = enabled
        viewModelScope.launch { collectionRepo.rescheduleAllAlarms() }
    }

    fun saveAzkarHubOrder(ids: List<String>) {
        settings.azkarHubOrder = ids
        _azkarHubOrder.value = ids
    }

    fun applyOnboardingReminders(result: OnboardingResult) {
        settings.autoAzkarEnabled = result.autoAzkarEnabled
        settings.azkarClockHourFormat = result.clockHourFormat
        settings.setTasbihWindow(
            result.tasbihStartHour,
            result.tasbihStartMinute,
            result.tasbihEndHour,
            result.tasbihEndMinute
        )
        viewModelScope.launch {
            settings.respectPrayerTime = result.respectPrayerTime
            settings.silentDuringFardPrayer = result.silentDuringFardPrayer
            result.prayerLocation?.let { settings.setPrayerLocation(it, result.prayerLocationMode) }
            if (result.prayerLocationMode == com.greendome.adhkar.prayer.LocationMode.GPS) {
                settings.prayerTravelAutoUpdate = true
            }
            collectionRepo.applyOnboardingAzkarSchedule(
                enabled = result.autoAzkarEnabled,
                morningHour = result.morningHour,
                morningMinute = result.morningMinute,
                sleepHour = result.sleepHour,
                sleepMinute = result.sleepMinute,
                enabledCollectionIds = result.enabledAzkarCollectionIds,
                afterPrayerEnabled = result.afterPrayerAzkarEnabled,
                afterAdhanEnabled = result.afterAdhanAzkarEnabled,
                homeAzkarEnabled = result.homeAzkarEnabled,
                homeLocation = result.homeLocation,
                ridingAzkarEnabled = result.ridingAzkarEnabled,
            )
            PrayerAlarms.rescheduleAll(getApplication())
        }
    }

    fun saveAzkarItem(item: AzkarItemEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = collectionRepo.saveItem(item)
            pendingPublishRepo.recordUpsert(PendingPublishType.AZKAR_ITEM, id)
            onDone()
        }
    }

    fun deleteAzkarItem(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            collectionRepo.deleteItem(id)
            pendingPublishRepo.recordDelete(PendingPublishType.AZKAR_ITEM, id)
            onDone()
        }
    }

    fun collectionItemsFlow(collectionId: String) = collectionRepo.observeItems(collectionId)

    fun favoriteSourceIdsFlow() = collectionRepo.observeFavoriteSourceIds()

    fun toggleAzkarFavorite(item: AzkarItemEntity) {
        viewModelScope.launch { collectionRepo.toggleFavorite(item) }
    }

    fun saveReciter(entity: ReciterEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = reciterRepo.save(entity)
            pendingPublishRepo.recordUpsert(PendingPublishType.RECITER, id)
            onDone()
        }
    }

    fun deleteReciter(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            reciterRepo.deleteReciter(id)
            pendingPublishRepo.recordDelete(PendingPublishType.RECITER, id)
            onDone()
        }
    }

    fun reciterAudioFlow(reciterId: Long) = reciterRepo.observeAudioByReciter(reciterId)

    fun reciterAzkarAudioFlow(reciterId: Long) = reciterRepo.observeAzkarAudioByReciter(reciterId)

    fun saveReciterAudio(entity: ReciterAudioEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = reciterRepo.saveReciterAudio(entity)
            pendingPublishRepo.recordUpsert(PendingPublishType.RECITER_AUDIO, id)
            onDone()
        }
    }

    fun deleteReciterAudio(id: Long, localPath: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            reciterRepo.deleteReciterAudio(id, localPath)
            pendingPublishRepo.recordDelete(PendingPublishType.RECITER_AUDIO, id)
            onDone()
        }
    }

    fun saveReciterAzkarAudio(entity: ReciterAzkarAudioEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = reciterRepo.saveReciterAzkarAudio(entity)
            pendingPublishRepo.recordUpsert(PendingPublishType.RECITER_AZKAR_AUDIO, id)
            onDone()
        }
    }

    fun deleteReciterAzkarAudio(id: Long, localPath: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            reciterRepo.deleteReciterAzkarAudio(id, localPath)
            pendingPublishRepo.recordDelete(PendingPublishType.RECITER_AZKAR_AUDIO, id)
            onDone()
        }
    }

    fun saveAdhanAudio(entity: AdhanAudioEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val id = adhanAudioRepo.save(entity)
            pendingPublishRepo.recordUpsert(PendingPublishType.ADHAN_AUDIO, id)
            onDone()
        }
    }

    fun deleteAdhanAudio(entity: AdhanAudioEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            adhanAudioRepo.delete(entity)
            pendingPublishRepo.recordDelete(PendingPublishType.ADHAN_AUDIO, entity.id)
            onDone()
        }
    }

    fun loginAdmin(pin: String): Boolean {
        val ok = settings.verifyAdminPin(pin)
        settings.isAdminLoggedIn = ok
        return ok
    }

    fun logoutAdmin() { settings.isAdminLoggedIn = false }
}
