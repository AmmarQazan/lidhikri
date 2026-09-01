package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity

/**
 * دعاء الوسيلة بعد الأذان. التشغيل عند انتهاء الأذان لا بالساعة.
 */
object AdhanAzkar {
    const val COLLECTION_ID = "adhan"
    const val SORT_ORDER = 6

    const val TITLE_AR = "أذكار ما بعد الأذان"
    const val TITLE_EN = "After-adhan adhkar"

    /** فاصل بعد انتهاء الأذان بالكامل قبل دعاء الوسيلة. */
    const val AFTER_END_DELAY_MS = 10_000L

    fun entity(
        autoPlayEnabled: Boolean = false,
    ) = AdhkarCollectionEntity(
        id = COLLECTION_ID,
        titleAr = TITLE_AR,
        titleEn = TITLE_EN,
        sortOrder = SORT_ORDER,
        autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
        autoPlayEnabled = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID) && autoPlayEnabled,
        scheduleHour = 12,
        scheduleMinute = 0,
        useTtsAutoPlay = true,
    )

    fun items(): List<ItemSeed> = listOf(
        ItemSeed(
            id = "wasilah",
            text = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ.",
            virtue = "يُقال بعد الأذان.",
            detect = "الوسيله",
        ),
    )

    fun toEntities(): List<AzkarItemEntity> =
        items().mapIndexed { index, seed -> seed.toEntity(index + 1) }

    suspend fun ensure(db: AdhkarDatabase) {
        val existing = db.collectionDao().getById(COLLECTION_ID)
        val allowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID)
        if (existing == null) {
            db.collectionDao().insert(entity())
        } else {
            db.collectionDao().update(
                existing.copy(
                    titleAr = TITLE_AR,
                    titleEn = TITLE_EN,
                    sortOrder = SORT_ORDER,
                    autoPlayAllowed = allowed,
                    autoPlayEnabled = allowed && existing.autoPlayEnabled,
                )
            )
        }
        syncItems(db)
    }

    private suspend fun syncItems(db: AdhkarDatabase) {
        val seeds = items()
        val current = db.azkarItemDao().getByCollection(COLLECTION_ID)
        seeds.forEachIndexed { index, seed ->
            val desiredOrder = index + 1
            val match = current.find { seed.matches(it.textAr) }
            if (match == null) {
                db.azkarItemDao().insert(seed.toEntity(desiredOrder))
            } else if (
                match.textAr != seed.text ||
                match.virtueAr != seed.virtue ||
                match.sortOrder != desiredOrder
            ) {
                db.azkarItemDao().update(
                    match.copy(
                        textAr = seed.text,
                        virtueAr = seed.virtue,
                        sortOrder = desiredOrder,
                    )
                )
            }
        }
        db.azkarItemDao().getByCollection(COLLECTION_ID)
            .filter { item -> seeds.none { seed -> seed.matches(item.textAr) } }
            .forEach { leftover ->
                db.reciterAzkarAudioDao().deleteByAzkarItemIds(listOf(leftover.id))
                db.azkarItemDao().deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, leftover.id)
                db.azkarItemDao().delete(leftover.id)
            }
    }

    data class ItemSeed(
        val id: String,
        val text: String,
        val virtue: String,
        val detect: String,
    ) {
        fun matches(textAr: String): Boolean =
            ContentI18n.normalizeAr(detect) in ContentI18n.normalizeAr(textAr)

        fun toEntity(sortOrder: Int) = AzkarItemEntity(
            collectionId = COLLECTION_ID,
            textAr = text,
            virtueAr = virtue,
            repeatCount = 1,
            sortOrder = sortOrder,
        )
    }
}
