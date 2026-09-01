package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity

/**
 * أذكار الركوب من حصن المسلم.
 * التشغيل التلقائي عند دخول المركبة لا بالساعة.
 */
object RidingAzkar {
    const val COLLECTION_ID = "riding"
    const val SORT_ORDER = 8

    fun entity(
        autoPlayEnabled: Boolean = false,
    ) = AdhkarCollectionEntity(
        id = COLLECTION_ID,
        titleAr = "أذكار الركوب",
        titleEn = "Riding adhkar",
        sortOrder = SORT_ORDER,
        autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
        autoPlayEnabled = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID) && autoPlayEnabled,
        scheduleHour = 8,
        scheduleMinute = 0,
        useTtsAutoPlay = true,
    )

    fun items(): List<ItemSeed> = listOf(
        ItemSeed(
            id = "ride_sakhkhara",
            text = "بِسْمِ اللهِ، الْحَمْدُ لِلَّهِ. سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ. الْحَمْدُ لِلَّهِ، الْحَمْدُ لِلَّهِ، الْحَمْدُ لِلَّهِ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَكَ اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ.",
            virtue = "عند ركوب المركبة. من حصن المسلم.",
            detect = "ظلمت نفسي",
        ),
        ItemSeed(
            id = "ride_ayah",
            text = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ.",
            virtue = "آية الركوب. تُقال عند ركوب المركبة.",
            detect = "منقلبون",
            absent = "ظلمت نفسي",
        ),
    )

    fun ids(): Set<String> = items().map { it.id }.toSet()

    fun toEntities(): List<AzkarItemEntity> =
        items().mapIndexed { index, seed -> seed.toEntity(index + 1) }

    fun pickRandom(
        stored: List<AzkarItemEntity>,
        selectedIds: Set<String>,
    ): AzkarItemEntity? {
        val allowed = items().filter { it.id in selectedIds }
        val matched = allowed.mapNotNull { seed -> stored.find { seed.matches(it.textAr) } }
        return matched.randomOrNull()
    }

    suspend fun ensure(db: AdhkarDatabase) {
        val existing = db.collectionDao().getById(COLLECTION_ID)
        val allowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID)
        if (existing == null) {
            db.collectionDao().insert(entity())
        } else {
            db.collectionDao().update(
                existing.copy(
                    titleAr = "أذكار الركوب",
                    titleEn = existing.titleEn.ifBlank { "Riding adhkar" },
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
                db.azkarItemDao().deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, leftover.id)
                db.azkarItemDao().delete(leftover.id)
            }
    }

    data class ItemSeed(
        val id: String,
        val text: String,
        val virtue: String,
        val detect: String,
        val absent: String = "",
    ) {
        fun matches(textAr: String): Boolean {
            val n = ContentI18n.normalizeAr(textAr)
            if (ContentI18n.normalizeAr(detect) !in n) return false
            if (absent.isNotBlank() && ContentI18n.normalizeAr(absent) in n) return false
            return true
        }

        fun toEntity(sortOrder: Int) = AzkarItemEntity(
            collectionId = COLLECTION_ID,
            textAr = text,
            virtueAr = virtue,
            repeatCount = 1,
            sortOrder = sortOrder,
        )
    }
}
