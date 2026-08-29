package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase

object AzkarFavorites {
    const val COLLECTION_ID = "favorites"

    fun entity(autoPlayEnabled: Boolean = AutoAzkarCatalog.entityDefaultEnabled(COLLECTION_ID)) =
        AdhkarCollectionEntity(
            id = COLLECTION_ID,
            titleAr = "الأذكار المفضلة",
            titleEn = "Favorite adhkar",
            sortOrder = 0,
            autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
            autoPlayEnabled = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID) && autoPlayEnabled,
            scheduleHour = 7,
            scheduleMinute = 0,
            weekDaysMask = 127,
            useTtsAutoPlay = true
        )

    suspend fun ensureCollection(db: AdhkarDatabase, autoPlayEnabled: Boolean? = null) {
        val existing = db.collectionDao().getById(COLLECTION_ID)
        if (existing == null) {
            db.collectionDao().insert(entity(autoPlayEnabled ?: AutoAzkarCatalog.entityDefaultEnabled(COLLECTION_ID)))
            return
        }
        val allowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID)
        val enabled = autoPlayEnabled ?: existing.autoPlayEnabled
        if (existing.autoPlayAllowed != allowed || existing.autoPlayEnabled != (allowed && enabled)) {
            db.collectionDao().update(
                existing.copy(
                    autoPlayAllowed = allowed,
                    autoPlayEnabled = allowed && enabled
                )
            )
        }
    }
}
