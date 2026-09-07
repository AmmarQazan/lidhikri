package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.util.TasbihWindow

object IslambookAzkarSeed {

    internal data class ItemSeed(
        val text: String,
        val virtue: String = "",
        val repeat: Int = 1,
        val detect: String = "",
        val absent: List<String> = emptyList(),
        val matchRepeat: Boolean = false,
        val exact: Boolean = false,
    ) {
        fun matches(textAr: String, repeatCount: Int = repeat): Boolean {
            val needle = SubaihatReciterSeed.normalizeAr(detect)
            if (needle.isEmpty()) return false
            val n = SubaihatReciterSeed.normalizeAr(textAr)
            val hit = if (exact) n == needle || n == needle.trim('.') else n.contains(needle)
            if (!hit) return false
            if (absent.any { extra ->
                    val a = SubaihatReciterSeed.normalizeAr(extra)
                    a.isNotEmpty() && n.contains(a)
                }
            ) {
                return false
            }
            if (matchRepeat && repeatCount != repeat) return false
            return true
        }
    }

    suspend fun seed(db: AdhkarDatabase) {
        if (db.collectionDao().getById("morning") != null) return

        val collections = listOf(
            collection(
                "morning", "أذكار الصباح", "Morning adhkar", 1,
                TasbihWindow.DEFAULT_MORNING_HOUR, TasbihWindow.DEFAULT_MORNING_MINUTE
            ),
            collection("evening", "أذكار المساء", "Evening adhkar", 2, 19, 0),
            collection("after_prayer", "أذكار بعد الصلاة المفروضة", "After obligatory prayer", 3, 12, 30),
            collection(
                "sleep", "أذكار النوم والأحلام", "Sleep adhkar", 4,
                TasbihWindow.DEFAULT_SLEEP_HOUR, TasbihWindow.DEFAULT_SLEEP_MINUTE
            ),
            collection(
                "wake_up", "أذكار الاستيقاظ من النوم", "Waking up", 5,
                TasbihWindow.DEFAULT_WAKE_HOUR, TasbihWindow.DEFAULT_WAKE_MINUTE
            ),
            AdhanAzkar.entity(),
            collection(HomeAzkar.COLLECTION_ID, "أذكار دخول وخروج المنزل", "Entering and leaving home", 7, 8, 0),
            collection(RidingAzkar.COLLECTION_ID, "أذكار الركوب", "Riding adhkar", RidingAzkar.SORT_ORDER, 8, 0),
        )
        db.collectionDao().insertAll(collections)
        AzkarFavorites.ensureCollection(db)

        insertQurantimeCollection(db, "morning", QurantimeAzkarData.morningItems())
        insertQurantimeCollection(db, "evening", QurantimeAzkarData.eveningItems())
        db.azkarItemDao().insertAll(afterPrayerItems())
        db.azkarItemDao().insertAll(sleepItems())
        db.azkarItemDao().insertAll(wakeUpItems())
        db.azkarItemDao().insertAll(AdhanAzkar.toEntities())
        db.azkarItemDao().insertAll(HomeAzkar.toEntities())
        db.azkarItemDao().insertAll(RidingAzkar.toEntities())
        ReciterLibrariesMigration.ensureMixedVoicesReciter(db)
        seedQurantimeAudio(db)
    }

    suspend fun ensureAfterPrayerClosingSurahs(db: AdhkarDatabase) {
        if (db.collectionDao().getById("after_prayer") == null) return
        dedupeAfterPrayerByText(db)
        afterPrayerCatalog().forEachIndexed { index, seed ->
            val desiredOrder = index + 1
            val matches = db.azkarItemDao().getByCollection("after_prayer")
                .filter { seed.matches(it.textAr, it.repeatCount) }
                .sortedBy { it.sortOrder }
            deleteAzkarItems(db, matches.drop(1))
            val match = matches.firstOrNull()
            if (match == null) {
                db.azkarItemDao().insert(
                    AzkarItemEntity(
                        collectionId = "after_prayer",
                        textAr = seed.text,
                        virtueAr = seed.virtue,
                        repeatCount = seed.repeat,
                        sortOrder = desiredOrder,
                    )
                )
            } else if (
                match.textAr != seed.text ||
                match.virtueAr != seed.virtue ||
                match.repeatCount != seed.repeat ||
                match.sortOrder != desiredOrder
            ) {
                db.azkarItemDao().update(
                    match.copy(
                        textAr = seed.text,
                        virtueAr = seed.virtue,
                        repeatCount = seed.repeat,
                        sortOrder = desiredOrder,
                    )
                )
            }
        }
        removeDuplicateShortTahlil33(db)
    }

    private suspend fun removeDuplicateShortTahlil33(db: AdhkarDatabase) {
        val extras = db.azkarItemDao().getByCollection("after_prayer").filter { item ->
            if (item.repeatCount != 33) return@filter false
            val n = SubaihatReciterSeed.normalizeAr(item.textAr)
            n.contains("وحده لا شريك") &&
                listOf("لا مانع", "لا حول", "يحيي", "نعبد").none { extra -> n.contains(SubaihatReciterSeed.normalizeAr(extra)) }
        }
        deleteAzkarItems(db, extras)
    }

    private suspend fun dedupeAfterPrayerByText(db: AdhkarDatabase) {
        val grouped = db.azkarItemDao().getByCollection("after_prayer")
            .groupBy { SubaihatReciterSeed.normalizeAr(it.textAr) to it.repeatCount }
        grouped.values.forEach { group ->
            deleteAzkarItems(db, group.sortedBy { it.sortOrder }.drop(1))
        }
    }

    private suspend fun deleteAzkarItems(db: AdhkarDatabase, items: List<AzkarItemEntity>) {
        if (items.isEmpty()) return
        val removedIds = items.map { it.id }
        val favoriteIds = removedIds.mapNotNull { sourceId ->
            db.azkarItemDao().getBySourceItemId(AzkarFavorites.COLLECTION_ID, sourceId)?.id
        }
        val audioIds = removedIds + favoriteIds
        if (audioIds.isNotEmpty()) {
            db.reciterAzkarAudioDao().deleteByAzkarItemIds(audioIds)
        }
        items.forEach { item ->
            db.azkarItemDao().deleteBySourceItemId(AzkarFavorites.COLLECTION_ID, item.id)
            db.azkarItemDao().delete(item.id)
        }
    }

    /** استبدال أذكار الصباح والمساء بما يطابق QuranTime (نص + صوت). */
    suspend fun resyncMorningEvening(db: AdhkarDatabase) {
        ReciterLibrariesMigration.ensureMixedVoicesReciter(db)
        listOf("morning", "evening").forEach { collectionId ->
            val oldIds = db.azkarItemDao().getIdsByCollection(collectionId)
            if (oldIds.isNotEmpty()) {
                db.reciterAzkarAudioDao().deleteByAzkarItemIds(oldIds)
                db.azkarItemDao().deleteByCollection(collectionId)
            }
            val items = when (collectionId) {
                "morning" -> QurantimeAzkarData.morningItems()
                else -> QurantimeAzkarData.eveningItems()
            }
            insertQurantimeCollection(db, collectionId, items)
        }
        seedQurantimeAudio(db)
    }

    private suspend fun insertQurantimeCollection(
        db: AdhkarDatabase,
        collectionId: String,
        items: List<QurantimeItem>
    ) {
        val entities = items.mapIndexed { index, item ->
            AzkarItemEntity(
                collectionId = collectionId,
                textAr = item.text,
                virtueAr = item.virtue,
                repeatCount = item.repeat,
                sortOrder = index + 1
            )
        }
        db.azkarItemDao().insertAll(entities)
    }

    private suspend fun seedQurantimeAudio(db: AdhkarDatabase) {
        listOf("morning", "evening").forEach { collectionId ->
            val items = db.azkarItemDao().getByCollection(collectionId)
            val templates = when (collectionId) {
                "morning" -> QurantimeAzkarData.morningItems()
                else -> QurantimeAzkarData.eveningItems()
            }
            items.zip(templates).forEach { (entity, template) ->
                val reciterId = ReciterLibrariesMigration.MIXED_VOICES_ID
                val existing = db.reciterAzkarAudioDao().get(entity.id, reciterId)
                if (existing == null) {
                    db.reciterAzkarAudioDao().insert(
                        ReciterAzkarAudioEntity(
                            reciterId = reciterId,
                            azkarItemId = entity.id,
                            assetPath = template.audioAsset,
                            isDownloaded = true
                        )
                    )
                } else if (existing.assetPath != template.audioAsset) {
                    db.reciterAzkarAudioDao().insert(
                        existing.copy(
                            assetPath = template.audioAsset,
                            isDownloaded = true
                        )
                    )
                }
            }
        }
    }

    private fun collection(
        id: String,
        titleAr: String,
        titleEn: String,
        order: Int,
        hour: Int,
        minute: Int
    ) = AdhkarCollectionEntity(
        id = id,
        titleAr = titleAr,
        titleEn = titleEn,
        sortOrder = order,
        scheduleHour = hour,
        scheduleMinute = minute,
        autoPlayAllowed = AutoAzkarCatalog.entityAutoPlayAllowed(id),
        autoPlayEnabled = AutoAzkarCatalog.entityDefaultEnabled(id),
        useTtsAutoPlay = true
    )

    private fun toEntities(collectionId: String, seeds: List<ItemSeed>): List<AzkarItemEntity> =
        seeds.mapIndexed { index, seed ->
            AzkarItemEntity(
                collectionId = collectionId,
                textAr = seed.text,
                virtueAr = seed.virtue,
                repeatCount = seed.repeat,
                sortOrder = index + 1
            )
        }

    private fun afterPrayerItems(): List<AzkarItemEntity> = toEntities("after_prayer", afterPrayerCatalog())

    internal fun afterPrayerCatalog(): List<ItemSeed> = listOf(
        ItemSeed(
            "أَسْتَغْفِرُ اللهَ.",
            "بعد السلام ثلاثاً. رواه مسلم.",
            repeat = 3,
            detect = "استغفر الله",
            absent = listOf("العظيم", "اتوب"),
            exact = true,
        ),
        ItemSeed(
            "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ.",
            "بعد الاستغفار. رواه مسلم.",
            detect = "السلام",
        ),
        ItemSeed(
            "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، اللَّهُمَّ لَا مَانِعَ لِمَا أَعْطَيْتَ، وَلَا مُعْطِيَ لِمَا مَنَعْتَ، وَلَا يَنْفَعُ ذَا الْجَدِّ مِنْكَ الْجَدُّ.",
            "بعد كل فريضة. رواه البخاري ومسلم.",
            detect = "لا مانع",
        ),
        ItemSeed(
            "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ، لَا إِلَهَ إِلَّا اللهُ وَلَا نَعْبُدُ إِلَّا إِيَّاهُ، لَهُ النِّعْمَةُ وَلَهُ الْفَضْلُ وَلَهُ الثَّنَاءُ الْحَسَنُ، لَا إِلَهَ إِلَّا اللهُ مُخْلِصِينَ لَهُ الدِّينَ وَلَوْ كَرِهَ الْكَافِرُونَ.",
            "بعد كل فريضة. رواه مسلم.",
            detect = "كره الكافرون",
        ),
        ItemSeed(
            "سُبْحَانَ اللهِ.",
            "دبر كل صلاة. رواه مسلم.",
            repeat = 33,
            detect = "سبحان الله",
            exact = true,
        ),
        ItemSeed(
            "الْحَمْدُ لِلَّهِ.",
            "دبر كل صلاة. رواه مسلم.",
            repeat = 33,
            detect = "الحمد لله",
            exact = true,
        ),
        ItemSeed(
            "اللهُ أَكْبَرُ.",
            "دبر كل صلاة. رواه مسلم.",
            repeat = 33,
            detect = "الله اكبر",
            exact = true,
        ),
        ItemSeed(
            "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            "تمام المائة بعد التسبيح. رواه مسلم.",
            repeat = 1,
            detect = "وحده لا شريك",
            absent = listOf("لا مانع", "لا حول", "يحيي", "نعبد", "الفضل"),
            matchRepeat = true,
        ),
        ItemSeed(
            "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            "بعد المغرب والفجر عشر مرات.",
            repeat = 10,
            detect = "يحيي ويميت",
        ),
        ItemSeed(
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْبُخْلِ، وَأَعُوذُ بِكَ مِنَ الْجُبْنِ، وَأَعُوذُ بِكَ مِنْ أَنْ أُرَدَّ إِلَى أَرْذَلِ الْعُمُرِ، وَأَعُوذُ بِكَ مِنْ فِتْنَةِ الدُّنْيَا، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ.",
            "بعد كل صلاة. رواه البخاري.",
            detect = "البخل",
            absent = listOf("الهم", "الحزن"),
        ),
        ItemSeed(
            "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ.",
            "بعد كل صلاة. رواه أبو داود وأحمد.",
            detect = "اعني على ذكرك",
        ),
    ) + afterPrayerClosingSurahs()

    private fun afterPrayerClosingSurahs(): List<ItemSeed> {
        val morning = QurantimeAzkarData.morningItems()
        return listOf(
            ItemSeed(
                morning[0].text,
                "من قرأ آية الكرسي دبر كل صلاة مكتوبة لم يمنعه من دخول الجنة إلا الموت.",
                repeat = 1,
                detect = "كرسي",
            ),
            ItemSeed(
                morning[1].text,
                "المعوذات بعد الصلاة.",
                repeat = 3,
                detect = "الصمد",
            ),
            ItemSeed(
                morning[2].text,
                "المعوذات بعد الصلاة.",
                repeat = 3,
                detect = "الفلق",
            ),
            ItemSeed(
                morning[3].text,
                "المعوذات بعد الصلاة.",
                repeat = 3,
                detect = "الوسواس",
            ),
        )
    }

    private fun sleepItems(): List<AzkarItemEntity> = toEntities("sleep", listOf(
        ItemSeed("بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي، وَبِكَ أَرْفَعُهُ، إِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ."),
        ItemSeed("اللَّهُمَّ إِنَّكَ خَلَقْتَ نَفْسِي وَأَنْتَ تَوَفَّاهَا، لَكَ مَمَاتُهَا وَمَحْيَاهَا …"),
        ItemSeed("اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ."),
        ItemSeed("بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا."),
        ItemSeed("سُبْحَانَ اللَّهِ.", repeat = 33),
        ItemSeed("الْحَمْدُ لِلَّهِ.", repeat = 33),
        ItemSeed("اللهُ أَكْبَرُ.", repeat = 34)
    ))

    private fun wakeUpItems(): List<AzkarItemEntity> = toEntities("wake_up", listOf(
        ItemSeed("الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ."),
        ItemSeed("لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.", repeat = 100),
        ItemSeed("الْحَمْدُ لِلَّهِ الَّذِي عَافَانِي فِي جَسَدِي وَرَدَّ عَلَيَّ رُوحِي وَأَذِنَ لِي بِذِكْرِهِ.")
    ))

}
