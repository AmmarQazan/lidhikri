package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.DhikrCategory

object JawamiAzkarSeed {
    const val COLLECTION_ID = "jawami"

    suspend fun seed(db: AdhkarDatabase, jawamiDhikrProvider: () -> List<com.greendome.adhkar.data.local.DhikrEntity>) {
        if (db.collectionDao().getById(COLLECTION_ID) == null) {
            db.collectionDao().insert(
                AdhkarCollectionEntity(
                    id = COLLECTION_ID,
                    titleAr = "جوامع التسبيح",
                    titleEn = "Jawami Tasbih",
                    sortOrder = 8,
                    autoPlayAllowed = false,
                    autoPlayEnabled = false,
                    useTtsAutoPlay = false,
                )
            )
        }

        val existingCount = db.azkarItemDao().countByCollection(COLLECTION_ID)
        if (existingCount > 0) return

        val defaults = db.dhikrDao().getDefaults()
        jawamiDhikrProvider().forEach { template ->
            val exists = defaults.any {
                it.isDefault && it.sortOrder == template.sortOrder && it.category == DhikrCategory.JAWAMI
            }
            if (!exists) db.dhikrDao().insert(template)
        }

        val jawamiItems = db.dhikrDao().getDefaults()
            .filter { it.category == DhikrCategory.JAWAMI }
            .sortedBy { it.sortOrder }

        if (jawamiItems.isEmpty()) return

        db.azkarItemDao().insertAll(
            jawamiItems.mapIndexed { index, dhikr ->
                AzkarItemEntity(
                    collectionId = COLLECTION_ID,
                    textAr = dhikr.textAr,
                    repeatCount = dhikr.repeatCount.coerceAtLeast(1),
                    sortOrder = index + 1,
                )
            }
        )
    }
}
