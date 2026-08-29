package com.greendome.adhkar.sync

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.PendingPublishChangeEntity
import kotlinx.coroutines.flow.Flow

object PendingPublishType {
    const val DHIKR = "DHIKR"
    const val RECITER = "RECITER"
    const val RECITER_AUDIO = "RECITER_AUDIO"
    const val RECITER_AZKAR_AUDIO = "RECITER_AZKAR_AUDIO"
    const val COLLECTION = "COLLECTION"
    const val AZKAR_ITEM = "AZKAR_ITEM"
}

object PendingPublishAction {
    const val UPSERT = "UPSERT"
    const val DELETE = "DELETE"
}

enum class AudioUploadKind {
    DHIKR,
    RECITER_AUDIO,
    RECITER_AZKAR_AUDIO
}

data class PendingPublishSet(
    val dhikrIds: Set<Long> = emptySet(),
    val reciterIds: Set<Long> = emptySet(),
    val reciterAudioIds: Set<Long> = emptySet(),
    val reciterAzkarAudioIds: Set<Long> = emptySet(),
    val collectionIds: Set<String> = emptySet(),
    val azkarItemIds: Set<Long> = emptySet()
) {
    val isEmpty: Boolean
        get() = dhikrIds.isEmpty() &&
            reciterIds.isEmpty() &&
            reciterAudioIds.isEmpty() &&
            reciterAzkarAudioIds.isEmpty() &&
            collectionIds.isEmpty() &&
            azkarItemIds.isEmpty()

    fun shouldUploadDhikrAudio(dhikrId: Long): Boolean = dhikrId in dhikrIds

    fun shouldUploadReciterAudio(id: Long): Boolean = id in reciterAudioIds

    fun shouldUploadReciterAzkarAudio(id: Long): Boolean = id in reciterAzkarAudioIds

    companion object {
        fun from(changes: List<PendingPublishChangeEntity>): PendingPublishSet {
            val dhikr = mutableSetOf<Long>()
            val reciters = mutableSetOf<Long>()
            val reciterAudio = mutableSetOf<Long>()
            val reciterAzkarAudio = mutableSetOf<Long>()
            val collections = mutableSetOf<String>()
            val azkarItems = mutableSetOf<Long>()
            for (change in changes) {
                val upsert = change.action != PendingPublishAction.DELETE
                when (change.entityType) {
                    PendingPublishType.DHIKR -> if (upsert) dhikr += change.entityId
                    PendingPublishType.RECITER -> reciters += change.entityId
                    PendingPublishType.RECITER_AUDIO -> if (upsert) reciterAudio += change.entityId
                    PendingPublishType.RECITER_AZKAR_AUDIO -> if (upsert) reciterAzkarAudio += change.entityId
                    PendingPublishType.COLLECTION -> collections += change.entityKey
                    PendingPublishType.AZKAR_ITEM -> if (upsert) azkarItems += change.entityId
                }
            }
            return PendingPublishSet(
                dhikrIds = dhikr,
                reciterIds = reciters,
                reciterAudioIds = reciterAudio,
                reciterAzkarAudioIds = reciterAzkarAudio,
                collectionIds = collections,
                azkarItemIds = azkarItems
            )
        }
    }
}

data class PendingPublishCounts(
    val dhikr: Int,
    val reciters: Int,
    val audio: Int,
    val collections: Int,
    val azkarItems: Int
) {
    val isEmpty: Boolean
        get() = dhikr == 0 && reciters == 0 && audio == 0 && collections == 0 && azkarItems == 0

    companion object {
        fun from(changes: List<PendingPublishChangeEntity>): PendingPublishCounts {
            var dhikr = 0
            var reciters = 0
            var audio = 0
            var collections = 0
            var azkarItems = 0
            for (change in changes) {
                when (change.entityType) {
                    PendingPublishType.DHIKR -> dhikr++
                    PendingPublishType.RECITER -> reciters++
                    PendingPublishType.RECITER_AUDIO,
                    PendingPublishType.RECITER_AZKAR_AUDIO -> audio++
                    PendingPublishType.COLLECTION -> collections++
                    PendingPublishType.AZKAR_ITEM -> azkarItems++
                }
            }
            return PendingPublishCounts(dhikr, reciters, audio, collections, azkarItems)
        }
    }
}

class PendingPublishRepository(private val db: AdhkarDatabase) {
    private val dao = db.pendingPublishDao()

    fun observeAll(): Flow<List<PendingPublishChangeEntity>> = dao.observeAll()

    suspend fun snapshot(): PendingPublishSet = PendingPublishSet.from(dao.getAll())

    suspend fun recordUpsert(type: String, entityId: Long, entityKey: String = "") {
        record(type, entityId, entityKey, PendingPublishAction.UPSERT)
    }

    suspend fun recordDelete(type: String, entityId: Long, entityKey: String = "") {
        record(type, entityId, entityKey, PendingPublishAction.DELETE)
    }

    suspend fun clear() = dao.clear()

    private suspend fun record(type: String, entityId: Long, entityKey: String, action: String) {
        dao.upsert(
            PendingPublishChangeEntity(
                changeKey = changeKey(type, entityId, entityKey),
                entityType = type,
                entityId = entityId,
                entityKey = entityKey,
                action = action,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    companion object {
        fun changeKey(type: String, entityId: Long, entityKey: String = ""): String =
            "$type:$entityId:$entityKey"
    }
}
