package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity

/**
 * أذكار دخول المنزل وخروجه من حصن المسلم.
 * التشغيل التلقائي بالموقع لا بالساعة.
 */
object HomeAzkar {
    const val COLLECTION_ID = "home"
    const val SORT_ORDER = 7
    const val DEFAULT_RADIUS_METERS = 150f

    fun hasCoordinates(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    enum class Event { ENTER, EXIT }

    fun entity(
        autoPlayEnabled: Boolean = false,
    ) = AdhkarCollectionEntity(
        id = COLLECTION_ID,
        titleAr = "أذكار دخول وخروج المنزل",
        titleEn = "Entering and leaving home",
        sortOrder = SORT_ORDER,
        autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
        autoPlayEnabled = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID) && autoPlayEnabled,
        scheduleHour = 8,
        scheduleMinute = 0,
        useTtsAutoPlay = true,
    )

    fun items(): List<ItemSeed> = listOf(
        ItemSeed(
            id = "enter_walajna",
            event = Event.ENTER,
            text = "بِسْمِ اللهِ وَلَجْنَا، وَبِسْمِ اللهِ خَرَجْنَا، وَعَلَى رَبِّنَا تَوَكَّلْنَا.",
            virtue = "عند دخول المنزل. ثم يسلّم على أهله.",
            detect = "ولجنا",
            absent = "المولج",
        ),
        ItemSeed(
            id = "enter_mawlaj",
            event = Event.ENTER,
            text = "اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ الْمَوْلِجِ وَخَيْرَ الْمَخْرَجِ، بِسْمِ اللهِ وَلَجْنَا، وَبِسْمِ اللهِ خَرَجْنَا، وَعَلَى اللهِ رَبِّنَا تَوَكَّلْنَا.",
            virtue = "عند دخول المنزل.",
            detect = "المولج",
        ),
        ItemSeed(
            id = "exit_tawakkalt",
            event = Event.EXIT,
            text = "بِسْمِ اللهِ، تَوَكَّلْتُ عَلَى اللهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ.",
            virtue = "عند الخروج. يُقال له: هُدِيتَ وَكُفِيتَ وَوُقِيتَ، وتنحّى عنه الشيطان.",
            detect = "توكلت",
        ),
        ItemSeed(
            id = "exit_adilla",
            event = Event.EXIT,
            text = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ، أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ.",
            virtue = "عند الخروج من المنزل.",
            detect = "اضل",
        ),
    )

    fun idsFor(event: Event): Set<String> =
        items().filter { it.event == event }.map { it.id }.toSet()

    fun toEntities(): List<AzkarItemEntity> =
        items().mapIndexed { index, seed -> seed.toEntity(index + 1) }

    fun pickRandom(
        stored: List<AzkarItemEntity>,
        event: Event,
        selectedIds: Set<String>,
    ): AzkarItemEntity? {
        val allowed = items().filter { it.event == event && it.id in selectedIds }
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
                    titleAr = "أذكار دخول وخروج المنزل",
                    titleEn = existing.titleEn.ifBlank { "Entering and leaving home" },
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
        val event: Event,
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
