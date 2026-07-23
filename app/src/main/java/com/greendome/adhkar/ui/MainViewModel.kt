package com.greendome.adhkar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.greendome.adhkar.data.CollectionRepository
import com.greendome.adhkar.data.DailyStatsRepository
import com.greendome.adhkar.data.DhikrRepository
import com.greendome.adhkar.data.ReciterRepository
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.service.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val dhikrRepo = DhikrRepository(AdhkarDatabase.get(app))
    private val reciterRepo = ReciterRepository(AdhkarDatabase.get(app))

    private val collectionRepo = CollectionRepository(AdhkarDatabase.get(app), app)

    val dhikrList = dhikrRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val longFormList = dhikrRepo.observeLongForm().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reciters = reciterRepo.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReciters = reciterRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val azkarCollections = collectionRepo.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val statsRepo = DailyStatsRepository(AdhkarDatabase.get(app))

    private val _todayTasbihCount = MutableStateFlow(0)
    val todayTasbihCount: StateFlow<Int> = _todayTasbihCount.asStateFlow()

    private val _todayAzkarCount = MutableStateFlow(0)
    val todayAzkarCount: StateFlow<Int> = _todayAzkarCount.asStateFlow()

    private val _todayMisbahaCount = MutableStateFlow(0)
    val todayMisbahaCount: StateFlow<Int> = _todayMisbahaCount.asStateFlow()

    private val _minutesUntilNext = MutableStateFlow(0)
    val minutesUntilNext: StateFlow<Int> = _minutesUntilNext.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _todayTasbihCount.value = statsRepo.tasbihTodayCount()
            _todayAzkarCount.value = statsRepo.azkarTodayCount()
            _todayMisbahaCount.value = statsRepo.misbahaTodayCount()
            _minutesUntilNext.value = ReminderScheduler.minutesUntilNext(getApplication())
        }
    }

    private val _tasbihActivityLog = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tasbihActivityLog: StateFlow<Map<String, Int>> = _tasbihActivityLog.asStateFlow()

    fun recordMisbahaCount() {
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

    fun saveDhikr(entity: DhikrEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dhikrRepo.save(entity)
            onDone()
        }
    }

    fun deleteDhikr(id: Long) {
        viewModelScope.launch { dhikrRepo.deleteCustom(id) }
    }

    fun saveCollection(collection: AdhkarCollectionEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            collectionRepo.saveCollection(collection)
            onDone()
        }
    }

    fun saveAzkarItem(item: AzkarItemEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            collectionRepo.saveItem(item)
            onDone()
        }
    }

    fun deleteAzkarItem(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            collectionRepo.deleteItem(id)
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
            reciterRepo.save(entity)
            onDone()
        }
    }

    fun deleteReciter(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            reciterRepo.deleteReciter(id)
            onDone()
        }
    }

    fun reciterAudioFlow(reciterId: Long) = reciterRepo.observeAudioByReciter(reciterId)

    fun saveReciterAudio(entity: ReciterAudioEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            reciterRepo.saveReciterAudio(entity)
            onDone()
        }
    }

    fun deleteReciterAudio(id: Long, localPath: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            reciterRepo.deleteReciterAudio(id, localPath)
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
