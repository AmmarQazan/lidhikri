package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.model.DhikrCategory

object JawamiAzkarSeed {
    const val COLLECTION_ID = "jawami"

    suspend fun seed(db: AdhkarDatabase, jawamiDhikrProvider: () -> List<DhikrEntity>) {
        val alreadySeeded = db.collectionDao().getById(COLLECTION_ID) != null
        ensureCollection(db)
        if (alreadySeeded) return
        ensureDhikrTemplates(db, jawamiDhikrProvider())
        insertAzkarFromDhikr(db)
    }

    suspend fun keepOnlyWithAudio(db: AdhkarDatabase, @Suppress("UNUSED_PARAMETER") templates: List<DhikrEntity>) {
        ensureCollection(db)
        syncAzkarItemsFromDhikr(db)
    }

    suspend fun syncAzkarItemsFromDhikr(db: AdhkarDatabase) {
        resyncAzkarItems(db)
    }

    private suspend fun ensureCollection(db: AdhkarDatabase) {
        if (db.collectionDao().getById(COLLECTION_ID) == null) {
            db.collectionDao().insert(
                AdhkarCollectionEntity(
                    id = COLLECTION_ID,
                    titleAr = "جوامع التسبيح",
                    titleEn = "Jawami Tasbih",
                    sortOrder = 8,
                    autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
                    autoPlayEnabled = AutoAzkarCatalog.entityDefaultEnabled(COLLECTION_ID),
                    useTtsAutoPlay = false,
                )
            )
        }
    }

    private suspend fun ensureDhikrTemplates(db: AdhkarDatabase, templates: List<DhikrEntity>) {
        val defaults = db.dhikrDao().getDefaults()
        templates.forEach { template ->
            val exists = defaults.any {
                it.isDefault && it.sortOrder == template.sortOrder && it.category == DhikrCategory.JAWAMI
            }
            if (!exists) db.dhikrDao().insert(template)
        }
    }

    private suspend fun insertAzkarFromDhikr(db: AdhkarDatabase) {
        val jawamiItems = db.dhikrDao().getDefaults()
            .filter { it.category == DhikrCategory.JAWAMI }
            .sortedBy { it.sortOrder }
        if (jawamiItems.isEmpty()) return
        db.azkarItemDao().insertAll(
            jawamiItems.mapIndexed { index, dhikr ->
                azkarItem(dhikr, index + 1)
            }
        )
    }

    private suspend fun resyncAzkarItems(db: AdhkarDatabase) {
        val keepDhikr = db.dhikrDao().getDefaults()
            .filter { it.category == DhikrCategory.JAWAMI }
            .sortedBy { it.sortOrder }
        val keepTexts = keepDhikr.map { it.textAr }.toSet()
        val items = db.azkarItemDao().getByCollection(COLLECTION_ID)
        val toRemove = items.filter { it.textAr !in keepTexts }
        if (toRemove.isNotEmpty()) {
            val removedIds = toRemove.map { it.id }
            val favoriteIds = removedIds.mapNotNull { sourceId ->
                db.azkarItemDao().getBySourceItemId(AzkarFavorites.COLLECTION_ID, sourceId)?.id
            }
            val audioIds = removedIds + favoriteIds
            if (audioIds.isNotEmpty()) {
                db.reciterAzkarAudioDao().deleteByAzkarItemIds(audioIds)
            }
            toRemove.forEach { item ->
                db.azkarItemDao().deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, item.id)
                db.azkarItemDao().delete(item.id)
            }
        }
        val remaining = db.azkarItemDao().getByCollection(COLLECTION_ID)
        val remainingTexts = remaining.map { it.textAr }.toSet()
        val missing = keepDhikr.filter { it.textAr !in remainingTexts }
        if (missing.isNotEmpty()) {
            val startOrder = remaining.maxOfOrNull { it.sortOrder } ?: 0
            db.azkarItemDao().insertAll(
                missing.mapIndexed { index, dhikr ->
                    azkarItem(dhikr, startOrder + index + 1)
                }
            )
        }
        val ordered = db.azkarItemDao().getByCollection(COLLECTION_ID)
            .sortedWith(
                compareBy<AzkarItemEntity> { item ->
                    val idx = keepDhikr.indexOfFirst { it.textAr == item.textAr }
                    if (idx < 0) Int.MAX_VALUE else idx
                }.thenBy { it.sortOrder }
            )
        ordered.forEachIndexed { index, item ->
            val desired = index + 1
            if (item.sortOrder != desired) {
                db.azkarItemDao().update(item.copy(sortOrder = desired))
            }
        }
    }

    private fun azkarItem(dhikr: DhikrEntity, sortOrder: Int) = AzkarItemEntity(
        collectionId = COLLECTION_ID,
        textAr = dhikr.textAr,
        repeatCount = dhikr.repeatCount.coerceAtLeast(1),
        sortOrder = sortOrder,
    )
}
