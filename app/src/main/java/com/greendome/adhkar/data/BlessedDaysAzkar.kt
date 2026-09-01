package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.prayer.PrayerName
import com.greendome.adhkar.util.TasbihWindow

object BlessedDaysAzkar {
    const val COLLECTION_ID = "blessed_days"
    const val SORT_ORDER = 10
    const val RAMADAN = 9
    const val DHUL_HIJJAH = 12

    const val QADR_TEXT = "اللهم إنك عفوٌّ تحب العفو فاعف عني"
    const val ARAFAH_TEXT =
        "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير"
    const val TAKBIR_TEXT =
        "الله أكبر الله أكبر، لا إله إلا الله، والله أكبر الله أكبر، ولله الحمد"

    const val QADR_VIRTUE =
        "العشر الأواخر من رمضان — علّمه النبي ﷺ عائشة رضي الله عنها (الترمذي)."
    const val ARAFAH_VIRTUE =
        "يوم عرفة — خير الدعاء دعاء يوم عرفة، وهذا الذكر أفضل ما قيل فيه."
    const val TAKBIR_VIRTUE =
        "عشر ذي الحجة وأيام التشريق — التكبير المأثور عن ابن مسعود رضي الله عنه."

    const val DAYTIME_OFFSET_HOURS = 1

    fun daytimeFromMorning(hour: Int, minute: Int): Pair<Int, Int> {
        val total = hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59) + DAYTIME_OFFSET_HOURS * 60
        val wrapped = total.mod(24 * 60)
        return wrapped / 60 to wrapped % 60
    }

    fun shouldFollowMorning(
        currentHour: Int,
        currentMinute: Int,
        morningHour: Int,
        morningMinute: Int,
    ): Boolean {
        val (followedHour, followedMinute) = daytimeFromMorning(morningHour, morningMinute)
        val atMorning = currentHour == morningHour && currentMinute == morningMinute
        val atFollowed = currentHour == followedHour && currentMinute == followedMinute
        val atOldDefault = currentHour == TasbihWindow.DEFAULT_MORNING_HOUR &&
            currentMinute == TasbihWindow.DEFAULT_MORNING_MINUTE
        return atMorning || atFollowed || atOldDefault
    }

    fun entity(
        autoPlayEnabled: Boolean = AutoAzkarCatalog.entityDefaultEnabled(COLLECTION_ID),
        scheduleHour: Int = daytimeFromMorning(
            TasbihWindow.DEFAULT_MORNING_HOUR,
            TasbihWindow.DEFAULT_MORNING_MINUTE,
        ).first,
        scheduleMinute: Int = daytimeFromMorning(
            TasbihWindow.DEFAULT_MORNING_HOUR,
            TasbihWindow.DEFAULT_MORNING_MINUTE,
        ).second,
    ) = AdhkarCollectionEntity(
        id = COLLECTION_ID,
        titleAr = "أذكار الأيام المباركة",
        titleEn = "Blessed days adhkar",
        sortOrder = SORT_ORDER,
        autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID),
        autoPlayEnabled = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID) && autoPlayEnabled,
        scheduleHour = scheduleHour,
        scheduleMinute = scheduleMinute,
        weekDaysMask = 127,
        dayMode = CollectionDayMode.ITEM_HIJRI,
        useTtsAutoPlay = true,
    )

    fun items(): List<ItemSeed> = listOf(
        ItemSeed(
            text = QADR_TEXT,
            virtue = QADR_VIRTUE,
            detect = "عفو تحب العفو",
            hijriMonth = RAMADAN,
            hijriDayStart = 21,
            hijriDayEnd = 30,
            scheduleHour = 19,
            scheduleMinute = 30,
            prayerAnchor = PrayerName.MAGHRIB.name,
            prayerOffsetMinutes = 15,
            skipQuietWindow = true,
        ),
        ItemSeed(
            text = ARAFAH_TEXT,
            virtue = ARAFAH_VIRTUE,
            detect = "خير الدعاء دعاء يوم عرفة",
            hijriMonth = DHUL_HIJJAH,
            hijriDayStart = 9,
            hijriDayEnd = 9,
        ),
        ItemSeed(
            text = TAKBIR_TEXT,
            virtue = TAKBIR_VIRTUE,
            detect = "الله أكبر الله أكبر، لا إله إلا الله",
            hijriMonth = DHUL_HIJJAH,
            hijriDayStart = 1,
            hijriDayEnd = 13,
        ),
    )

    suspend fun ensure(db: AdhkarDatabase) {
        val existing = db.collectionDao().getById(COLLECTION_ID)
        val allowed = AutoAzkarCatalog.entityAutoPlayAllowed(COLLECTION_ID)
        val morning = db.collectionDao().getById("morning")
        val (daytimeHour, daytimeMinute) = if (morning != null) {
            daytimeFromMorning(morning.scheduleHour, morning.scheduleMinute)
        } else {
            daytimeFromMorning(
                TasbihWindow.DEFAULT_MORNING_HOUR,
                TasbihWindow.DEFAULT_MORNING_MINUTE,
            )
        }
        val followMorning = when {
            existing == null -> true
            morning == null -> false
            else -> shouldFollowMorning(
                existing.scheduleHour,
                existing.scheduleMinute,
                morning.scheduleHour,
                morning.scheduleMinute,
            )
        }
        if (existing == null) {
            db.collectionDao().insert(
                entity(scheduleHour = daytimeHour, scheduleMinute = daytimeMinute)
            )
        } else {
            db.collectionDao().update(
                existing.copy(
                    titleAr = "أذكار الأيام المباركة",
                    titleEn = existing.titleEn.ifBlank { "Blessed days adhkar" },
                    sortOrder = SORT_ORDER,
                    autoPlayAllowed = allowed,
                    autoPlayEnabled = allowed && existing.autoPlayEnabled,
                    weekDaysMask = 127,
                    dayMode = CollectionDayMode.ITEM_HIJRI,
                    hijriMonth = -1,
                    hijriDayStart = -1,
                    hijriDayEnd = -1,
                    scheduleHour = if (followMorning) daytimeHour else existing.scheduleHour,
                    scheduleMinute = if (followMorning) daytimeMinute else existing.scheduleMinute,
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
            val match = current.find { seed.matches(it.textAr) || seed.matchesVirtue(it.virtueAr) }
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
                match.skipQuietWindow != seed.skipQuietWindow ||
                match.hijriMonth != seed.hijriMonth ||
                match.hijriDayStart != seed.hijriDayStart ||
                match.hijriDayEnd != seed.hijriDayEnd
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
                        hijriMonth = seed.hijriMonth,
                        hijriDayStart = seed.hijriDayStart,
                        hijriDayEnd = seed.hijriDayEnd,
                    )
                )
            }
        }
        db.azkarItemDao().getByCollection(COLLECTION_ID)
            .filter { item -> seeds.none { seed -> seed.matches(item.textAr) || seed.matchesVirtue(item.virtueAr) } }
            .forEach { leftover ->
                db.azkarItemDao().deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, leftover.id)
                db.azkarItemDao().delete(leftover.id)
            }
    }

    data class ItemSeed(
        val text: String,
        val virtue: String,
        val detect: String,
        val hijriMonth: Int,
        val hijriDayStart: Int,
        val hijriDayEnd: Int,
        val scheduleHour: Int = -1,
        val scheduleMinute: Int = 0,
        val prayerAnchor: String = "",
        val prayerOffsetMinutes: Int = 0,
        val skipQuietWindow: Boolean = false,
    ) {
        fun matches(textAr: String): Boolean =
            ContentI18n.normalizeAr(detect) in ContentI18n.normalizeAr(textAr)

        fun matchesVirtue(virtueAr: String): Boolean =
            ContentI18n.normalizeAr(detect) in ContentI18n.normalizeAr(virtueAr)

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
            hijriMonth = hijriMonth,
            hijriDayStart = hijriDayStart,
            hijriDayEnd = hijriDayEnd,
        )
    }
}
