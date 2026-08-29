package com.greendome.adhkar.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.greendome.adhkar.AdhkarApplication
import com.greendome.adhkar.data.CollectionRepository
import com.greendome.adhkar.data.SeedData
import com.greendome.adhkar.widget.DhikrOfDayManager

class ContentUpdateController(
    private val app: AdhkarApplication,
    private val store: ContentUpdateStore = ContentUpdateStore(app),
) {
    var showDialog by mutableStateOf(false)
        private set
    var applying by mutableStateOf(false)
        private set
    var applyError by mutableStateOf<String?>(null)
        private set
    var remoteVersion by mutableIntStateOf(0)
        private set

    suspend fun autoPromptIfNeeded(): Boolean {
        val remote = RemoteContentSync.peekRemoteVersion() ?: return false
        if (
            !ContentUpdateEligibility.shouldPrompt(
                localVersion = app.settings.remoteContentVersion,
                remoteVersion = remote,
                snoozedVersion = store.snoozeVersion(),
                snoozeUntilMs = store.snoozeUntilMs(),
                nowMs = System.currentTimeMillis(),
            )
        ) {
            return false
        }
        remoteVersion = remote
        applyError = null
        showDialog = true
        return true
    }

    suspend fun apply() {
        if (applying) return
        applying = true
        applyError = null
        try {
            when (
                val result = RemoteContentSync.syncIfNeeded(
                    app,
                    app.settings,
                    app.database,
                    force = true,
                )
            ) {
                is RemoteSyncResult.Updated -> {
                    app.settings.seedVersion = maxOf(app.settings.seedVersion, 19)
                    SeedData(app.database, app.settings).seedIfEmpty()
                    CollectionRepository(app.database, app).rescheduleAllAlarms()
                    DhikrOfDayManager.refresh(app)
                    showDialog = false
                }
                is RemoteSyncResult.UpToDate -> showDialog = false
                is RemoteSyncResult.Failed -> applyError = result.reason
            }
        } finally {
            applying = false
        }
    }

    fun later() {
        if (applying) return
        if (remoteVersion > 0) store.snooze(remoteVersion)
        showDialog = false
    }
}
