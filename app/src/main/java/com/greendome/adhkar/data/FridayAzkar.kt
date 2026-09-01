package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.util.CollectionScheduleHelper

object FridayAzkar {
    const val COLLECTION_ID = "friday"
    const val DEFAULT_HOUR = 8
    const val DEFAULT_MINUTE = 0
    const val SORT_ORDER = 9

    const val SALAWAT_TEXT = "اللهم صل وسلم وبارك على نبينا محمد"
    const val HOUR_TEXT =
        "قال صلى الله عليه وسلم: «فيه ساعة لا يوافقها عبد مسلم وهو قائم يصلي يسأل الله شيئاً إلا أعطاه إياه». صدق رسول الله"
    const val KAHF_TEXT =
        "قال صلى الله عليه وسلم: «من قرأ سورة الكهف يوم الجمعة أضاء له من النور ما بين الجمعتين». صدق رسول الله"

    const val SALAWAT_VIRTUE = "يُستحب الإكثار منها طوال يوم الجمعة."
    const val HOUR_VIRTUE = "ساعة الإجابة؛ أرجحها آخر ساعة بعد العصر إلى المغرب."
    const val KAHF_VIRTUE = "تُقرأ من مغرب الخميس إلى مغرب الجمعة."

    fun fridayOnlyMask(): Int = CollectionScheduleHelper.fridayOnlyMask()

    fun entity(
        autoPlayEnabled: Boolean = AutoAzkarCatalog.entityDefaultEnabled(COLLECTION_ID),
        scheduleHour: Int = DEFAULT_HOUR,
        scheduleMinute: Int = DEFAULT_MINUTE,
    ) = AdhkarCollectionEntity(
        id = COLLECTION_ID,
        titleAr = "أذكار يوم الجمعة",
        titleEn = "Friday adhkar",
        sortOrder = SORT_ORDER,
        autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
        autoPlayEnabled = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID) && autoPlayEnabled,
        scheduleHour = scheduleHour,
        scheduleMinute = scheduleMinute,
        weekDaysMask = fridayOnlyMask(),
        dayMode = CollectionDayMode.WEEKDAYS,
        useTtsAutoPlay = true,
    )

    fun items(): List<ItemSeed> = listOf(
        ItemSeed(
            text = SALAWAT_TEXT,
            virtue = SALAWAT_VIRTUE,
            detect = "صل وسلم وبارك على نبينا محمد",
            scheduleHour = 11,
            scheduleMinute = 0,
            prayerAnchor = PrayerName.DHUHR.name,
            prayerOffsetMinutes = -30,
        ),
        ItemSeed(
            text = HOUR_TEXT,
            virtue = HOUR_VIRTUE,
            detect = "ساعة لا يوافقها",
            scheduleHour = 16,
            scheduleMinute = 30,
            prayerAnchor = PrayerName.MAGHRIB.name,
            prayerOffsetMinutes = -60,
            skipQuietWindow = true,
        ),
        ItemSeed(
            text = KAHF_TEXT,
            virtue = KAHF_VIRTUE,
            detect = "سورة الكهف",
        ),
    )

    suspend fun ensure(db: AdhkarDatabase) {
        val existing = db.collectionDao().getById(COLLECTION_ID)
        val allowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID)
        if (existing == null) {
            db.collectionDao().insert(entity())
        } else {
            db.collectionDao().update(
                existing.copy(
                    titleAr = "أذكار يوم الجمعة",
                    titleEn = existing.titleEn.ifBlank { "Friday adhkar" },
                    sortOrder = SORT_ORDER,
                    autoPlayAllowed = allowed,
                    autoPlayEnabled = allowed && existing.autoPlayEnabled,
                    weekDaysMask = fridayOnlyMask(),
                    dayMode = CollectionDayMode.WEEKDAYS,
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
                match.sortOrder != desiredOrder ||
                match.scheduleHour != seed.scheduleHour ||
                match.scheduleMinute != seed.scheduleMinute ||
                match.prayerAnchor != seed.prayerAnchor ||
                match.prayerOffsetMinutes != seed.prayerOffsetMinutes ||
                match.skipQuietWindow != seed.skipQuietWindow
            ) {
                db.azkarItemDao().update(
                    match.copy(
                        textAr = seed.text,
                        virtueAr = seed.virtue,
                        sortOrder = desiredOrder,
                        scheduleHour = seed.scheduleHour,
                        scheduleMinute = seed.scheduleMinute,
                        prayerAnchor = seed.prayerAnchor,
                        prayerOffsetMinutes = seed.prayerOffsetMinutes,
                        skipQuietWindow = seed.skipQuietWindow,
                    )
                )
            }
        }
        val keepDetects = seeds.map { it.detect }
        db.azkarItemDao().getByCollection(COLLECTION_ID)
            .filter { item -> keepDetects.none { detect -> item.textAr.contains(detect) } }
            .forEach { leftover ->
                db.azkarItemDao().deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, leftover.id)
                db.azkarItemDao().delete(leftover.id)
            }
    }

    data class ItemSeed(
        val text: String,
        val virtue: String,
        val detect: String,
        val scheduleHour: Int = -1,
        val scheduleMinute: Int = 0,
        val prayerAnchor: String = "",
        val prayerOffsetMinutes: Int = 0,
        val skipQuietWindow: Boolean = false,
    ) {
        fun matches(textAr: String): Boolean = textAr.contains(detect)

        fun toEntity(sortOrder: Int) = AzkarItemEntity(
            collectionId = COLLECTION_ID,
            textAr = text,
            virtueAr = virtue,
            repeatCount = 1,
            sortOrder = sortOrder,
            scheduleHour = scheduleHour,
            scheduleMinute = scheduleMinute,
            prayerAnchor = prayerAnchor,
            prayerOffsetMinutes = prayerOffsetMinutes,
            skipQuietWindow = skipQuietWindow,
        )
    }
}
