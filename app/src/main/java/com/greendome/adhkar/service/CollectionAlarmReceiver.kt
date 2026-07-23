package com.greendome.adhkar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greendome.adhkar.data.local.AdhkarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CollectionAlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val collectionId = intent?.getStringExtra(EXTRA_COLLECTION_ID) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                val settings = com.greendome.adhkar.data.SettingsRepository(context)
                if (!settings.autoAzkarEnabled) return@launch
                val db = AdhkarDatabase.get(context)
                val collection = db.collectionDao().getById(collectionId) ?: return@launch
                if (!collection.autoPlayAllowed || !collection.autoPlayEnabled) return@launch

                val playIntent = Intent(context, AzkarCollectionPlayService::class.java).apply {
                    putExtra(AzkarCollectionPlayService.EXTRA_COLLECTION_ID, collectionId)
                }
                context.startForegroundService(playIntent)

                CollectionAlarmScheduler.schedule(context, collection)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "collection_id"
    }
}
