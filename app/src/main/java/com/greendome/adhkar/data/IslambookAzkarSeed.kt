package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.util.TasbihWindow

private data class ItemSeed(
    val text: String,
    val virtue: String = "",
    val repeat: Int = 1,
    val detect: String = "",
)

object IslambookAzkarSeed {

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
        afterPrayerClosingSurahs().forEach { seed ->
            val needle = SubaihatReciterSeed.normalizeAr(seed.detect)
            val matches = db.azkarItemDao().getByCollection("after_prayer")
                .filter { SubaihatReciterSeed.normalizeAr(it.textAr).contains(needle) }
                .sortedBy { it.sortOrder }
            deleteAzkarItems(db, matches.drop(1))
            if (matches.isNotEmpty()) return@forEach
            val items = db.azkarItemDao().getByCollection("after_prayer")
            val nextOrder = (items.maxOfOrNull { it.sortOrder } ?: 0) + 1
            db.azkarItemDao().insert(
                AzkarItemEntity(
                    collectionId = "after_prayer",
                    textAr = seed.text,
                    virtueAr = seed.virtue,
                    repeatCount = seed.repeat,
                    sortOrder = nextOrder,
                )
            )
        }
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

    private fun afterPrayerItems(): List<AzkarItemEntity> = toEntities(
        "after_prayer",
        listOf(
            ItemSeed("أَسْتَغْفِرُ اللهَ.", repeat = 3),
            ItemSeed("اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ."),
            ItemSeed("لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.", repeat = 33),
            ItemSeed("سُبْحَانَ اللهِ.", repeat = 33),
            ItemSeed("الْحَمْدُ لِلَّهِ.", repeat = 33),
            ItemSeed("اللهُ أَكْبَرُ.", repeat = 33),
            ItemSeed("لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ."),
        ) + afterPrayerClosingSurahs()
    )

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
