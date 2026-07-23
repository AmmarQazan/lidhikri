package com.greendome.adhkar.data

import android.content.Context
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.service.CollectionAlarmScheduler
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

    suspend fun getCollection(id: String) = collectionDao.getById(id)

    suspend fun getItems(collectionId: String) = itemDao.getByCollection(collectionId)

    suspend fun saveCollection(collection: AdhkarCollectionEntity) {
        val normalized = collection.copy(
            autoPlayEnabled = collection.autoPlayAllowed && collection.autoPlayEnabled
        )
        collectionDao.insert(normalized)
        rescheduleAllAlarms()
    }

    suspend fun rescheduleAllAlarms() {
        collectionDao.getAll().forEach { collection ->
            if (collection.autoPlayAllowed && collection.autoPlayEnabled) {
                CollectionAlarmScheduler.schedule(context, collection)
            } else {
                CollectionAlarmScheduler.cancel(context, collection.id)
            }
        }
    }

    suspend fun saveItem(item: AzkarItemEntity): Long {
        return if (item.id == 0L) itemDao.insert(item) else {
            itemDao.update(item)
            item.id
        }
    }

    suspend fun deleteItem(id: Long) = itemDao.delete(id)

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
            collectionDao.deleteById(AzkarFavorites.COLLECTION_ID)
            CollectionAlarmScheduler.cancel(context, AzkarFavorites.COLLECTION_ID)
        } else {
            rescheduleAllAlarms()
        }
    }

    private suspend fun ensureFavoritesCollection() {
        if (collectionDao.getById(AzkarFavorites.COLLECTION_ID) != null) return
        collectionDao.insert(
            AdhkarCollectionEntity(
                id = AzkarFavorites.COLLECTION_ID,
                titleAr = "الأذكار المفضلة",
                titleEn = "Favorite adhkar",
                sortOrder = 0,
                autoPlayAllowed = true,
                autoPlayEnabled = true,
                scheduleHour = 7,
                scheduleMinute = 0,
                weekDaysMask = 127,
                useTtsAutoPlay = true
            )
        )
    }
}
