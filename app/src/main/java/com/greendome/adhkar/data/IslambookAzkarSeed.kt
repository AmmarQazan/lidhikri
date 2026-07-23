package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity

private data class ItemSeed(val text: String, val virtue: String = "", val repeat: Int = 1)

object IslambookAzkarSeed {
    const val QURANTIME_RECITER_ID = 2L

    suspend fun seed(db: AdhkarDatabase) {
        if (db.collectionDao().getById("morning") != null) return

        val collections = listOf(
            collection("morning", "أذكار الصباح", "Morning adhkar", 1, 6, 0, autoPlayAllowed = true),
            collection("evening", "أذكار المساء", "Evening adhkar", 2, 19, 0, autoPlayAllowed = true),
            collection("after_prayer", "أذكار بعد الصلاة المفروضة", "After obligatory prayer", 3, 12, 30),
            collection("sleep", "أذكار النوم والأحلام", "Sleep adhkar", 4, 22, 0, autoPlayAllowed = true),
            collection("wake_up", "أذكار الاستيقاظ من النوم", "Waking up", 5, 6, 0, autoPlayAllowed = true),
            collection("adhan", "أذكار عند سماع الأذان", "When hearing the adhan", 6, 12, 0),
            collection("home", "أذكار دخول وخروج المنزل", "Entering and leaving home", 7, 8, 0)
        )
        db.collectionDao().insertAll(collections)

        insertQurantimeCollection(db, "morning", QurantimeAzkarData.morningItems())
        insertQurantimeCollection(db, "evening", QurantimeAzkarData.eveningItems())
        db.azkarItemDao().insertAll(afterPrayerItems())
        db.azkarItemDao().insertAll(sleepItems())
        db.azkarItemDao().insertAll(wakeUpItems())
        db.azkarItemDao().insertAll(adhanItems())
        db.azkarItemDao().insertAll(homeItems())
        ensureQurantimeReciter(db)
        seedQurantimeAudio(db)
    }

    /** استبدال أذكار الصباح والمساء بما يطابق QuranTime (نص + صوت). */
    suspend fun resyncMorningEvening(db: AdhkarDatabase) {
        ensureQurantimeReciter(db)
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

    private suspend fun ensureQurantimeReciter(db: AdhkarDatabase) {
        if (db.reciterDao().getById(QURANTIME_RECITER_ID) == null) {
            db.reciterDao().insert(
                ReciterEntity(
                    id = QURANTIME_RECITER_ID,
                    nameAr = "QuranTime",
                    nameEn = "QuranTime",
                    nameFr = "QuranTime",
                    nameEs = "QuranTime",
                    isBuiltin = true
                )
            )
        }
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
                val existing = db.reciterAzkarAudioDao().get(entity.id, QURANTIME_RECITER_ID)
                if (existing == null) {
                    db.reciterAzkarAudioDao().insert(
                        ReciterAzkarAudioEntity(
                            reciterId = QURANTIME_RECITER_ID,
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
        minute: Int,
        autoPlayAllowed: Boolean = false
    ) = AdhkarCollectionEntity(
        id = id,
        titleAr = titleAr,
        titleEn = titleEn,
        sortOrder = order,
        scheduleHour = hour,
        scheduleMinute = minute,
        autoPlayAllowed = autoPlayAllowed,
        autoPlayEnabled = autoPlayAllowed,
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

    private fun afterPrayerItems(): List<AzkarItemEntity> = toEntities("after_prayer", listOf(
        ItemSeed("أَسْتَغْفِرُ اللهَ.", repeat = 3),
        ItemSeed("اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ."),
        ItemSeed("لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.", repeat = 33),
        ItemSeed("سُبْحَانَ اللهِ.", repeat = 33),
        ItemSeed("الْحَمْدُ لِلَّهِ.", repeat = 33),
        ItemSeed("اللهُ أَكْبَرُ.", repeat = 33),
        ItemSeed("لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.")
    ))

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

    private fun adhanItems(): List<AzkarItemEntity> = toEntities("adhan", listOf(
        ItemSeed("تقول مثل ما يقول المؤذن إلا في حيعلتين فيقول: لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ."),
        ItemSeed("اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ.")
    ))

    private fun homeItems(): List<AzkarItemEntity> = toEntities("home", listOf(
        ItemSeed("بِسْمِ اللهِ وَلَجْنَا، وَبِسْمِ اللهِ خَرَجْنَا، وَعَلَى رَبِّنَا تَوَكَّلْنَا.", "عند الدخول والخروج."),
        ItemSeed("اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ الْمَوْلِجِ وَخَيْرَ الْمَخْرَجِ …", "عند الدخول."),
        ItemSeed("بِسْمِ اللهِ، تَوَكَّلْتُ عَلَى اللهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ.", "عند الخروج."),
        ItemSeed("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ …", "عند الخروج.")
    ))
}
