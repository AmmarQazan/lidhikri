package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.sync.RemoteContentConfig

/**
 * مكتبة القارئ ناصر الدين صبيحات على Firebase Storage.
 * المسارات ثابتة حتى يمكن استبدال الملف لاحقاً دون تغيير التطبيق.
 */
object SubaihatReciterSeed {
    const val RECITER_ID = 10L
    const val STORAGE_PREFIX = "audio/subaihat"

    suspend fun ensure(db: AdhkarDatabase): Long {
        val reciterId = ensureReciter(db)
        val defaults = db.dhikrDao().getDefaults()
        dhikrMaps().forEach { spec ->
            val matches = defaults.filter { dhikr ->
                dhikr.isDefault &&
                    dhikr.category == spec.category &&
                    dhikr.sortOrder in spec.sortOrders
            }
            matches.forEach { linkDhikrAudio(db, reciterId, it.id, spec.file) }
        }
        azkarMaps().forEach { spec ->
            val items = db.azkarItemDao().getByCollection(spec.collectionId)
            matchAzkar(items, spec).forEach { linkAzkarAudio(db, reciterId, it.id, spec.file) }
        }
        return reciterId
    }

    private suspend fun ensureReciter(db: AdhkarDatabase): Long {
        db.reciterDao().getAll().find { reciter ->
            reciter.nameAr.contains("صبيحات")
        }?.let { existing ->
            return existing.id
        }

        val occupied = db.reciterDao().getById(RECITER_ID)
        val entity = ReciterEntity(
            id = if (occupied == null) RECITER_ID else 0L,
            nameAr = "ناصر الدين صبيحات",
            nameEn = "Naser Al-Din Subaihat",
            nameFr = "Naser Al-Din Subaihat",
            nameEs = "Naser Al-Din Subaihat",
            isBuiltin = true,
            isActive = true
        )
        val inserted = db.reciterDao().insert(entity)
        return if (occupied == null) RECITER_ID else inserted
    }

    private suspend fun linkDhikrAudio(
        db: AdhkarDatabase,
        reciterId: Long,
        dhikrId: Long,
        file: String
    ) {
        val url = RemoteContentConfig.publicDownloadUrl("$STORAGE_PREFIX/$file")
        val existing = db.reciterAudioDao().get(dhikrId, reciterId)
        if (existing == null) {
            db.reciterAudioDao().insert(
                ReciterAudioEntity(
                    reciterId = reciterId,
                    dhikrId = dhikrId,
                    remoteUrl = url,
                    isDownloaded = false
                )
            )
            return
        }
        if (existing.remoteUrl != url) {
            db.reciterAudioDao().insert(
                existing.copy(
                    remoteUrl = url,
                    assetPath = null,
                    localPath = null,
                    isDownloaded = false
                )
            )
        }
    }

    private suspend fun linkAzkarAudio(
        db: AdhkarDatabase,
        reciterId: Long,
        azkarItemId: Long,
        file: String
    ) {
        val url = RemoteContentConfig.publicDownloadUrl("$STORAGE_PREFIX/$file")
        val existing = db.reciterAzkarAudioDao().get(azkarItemId, reciterId)
        if (existing == null) {
            db.reciterAzkarAudioDao().insert(
                ReciterAzkarAudioEntity(
                    reciterId = reciterId,
                    azkarItemId = azkarItemId,
                    remoteUrl = url,
                    isDownloaded = false
                )
            )
            return
        }
        if (existing.remoteUrl != url) {
            db.reciterAzkarAudioDao().insert(
                existing.copy(
                    remoteUrl = url,
                    assetPath = null,
                    localPath = null,
                    isDownloaded = false
                )
            )
        }
    }

    private data class DhikrSpec(
        val category: DhikrCategory,
        val sortOrders: IntRange,
        val file: String
    ) {
        constructor(category: DhikrCategory, sortOrder: Int, file: String) :
            this(category, sortOrder..sortOrder, file)
    }

    private data class AzkarSpec(
        val collectionId: String,
        val file: String,
        val needles: List<String>,
        val exact: Boolean = false,
        val bindAll: Boolean = false,
        val absent: List<String> = emptyList()
    )

    private fun dhikrMaps(): List<DhikrSpec> = listOf(
        DhikrSpec(DhikrCategory.GENERAL, 1, "tasbih/subhan_allah.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 2, "tasbih/alhamdulillah.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 3, "tasbih/tahlil.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 4, "tasbih/allahu_akbar.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 5, "tasbih/hawqala.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 6, "tasbih/baqiyat.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 7, "tasbih/istighfar.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 8, "tasbih/salawat.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 9, "tasbih/subhan_bihamd.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 10, "tasbih/subhan_azim.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 11, "tasbih/tawhid.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 12, "jawami/subhan_bihamd_adada.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 13, "tasbih/yunus.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 14, "tasbih/jalal.mp3"),
        DhikrSpec(DhikrCategory.GENERAL, 15, "tasbih/hasbi.mp3"),
        DhikrSpec(DhikrCategory.EID, 100..101, "tasbih/eid_takbir.mp3"),
        DhikrSpec(DhikrCategory.JAWAMI, 200, "jawami/subhan_bihamd_adada.mp3"),
        DhikrSpec(DhikrCategory.JAWAMI, 201, "jawami/subhan_adada_khalqih.mp3"),
    )

    private fun azkarMaps(): List<AzkarSpec> = listOf(
        AzkarSpec("morning", "morning/ayat_kursi.mp3", listOf("كرسي")),
        AzkarSpec("morning", "morning/ikhlas.mp3", listOf("الصمد")),
        AzkarSpec("morning", "morning/falaq.mp3", listOf("الفلق")),
        AzkarSpec("morning", "morning/nas.mp3", listOf("الوسواس")),
        AzkarSpec("morning", "morning/asbahna_mulk.mp3", listOf("اصبحنا", "سوء الكبر")),
        AzkarSpec("morning", "morning/bika_asbahna.mp3", listOf("بك اصبحنا")),
        AzkarSpec("morning", "morning/sayyid_istighfar.mp3", listOf("خلقتني")),
        AzkarSpec("morning", "morning/alim_ghayb.mp3", listOf("الغيب", "فاطر")),
        AzkarSpec("morning", "morning/ushhiduka.mp3", listOf("اصبحت اشهدك")),
        AzkarSpec("morning", "morning/shirk.mp3", listOf("اشرك")),
        AzkarSpec("morning", "morning/nima.mp3", listOf("اصبح بي من نعمه")),
        AzkarSpec("morning", "morning/afini.mp3", listOf("عافني")),
        AzkarSpec("morning", "morning/hamm.mp3", listOf("الهم")),
        AzkarSpec("morning", "morning/afw.mp3", listOf("العفو والعافيه")),
        AzkarSpec("morning", "morning/bismillah_la_yadurr.mp3", listOf("لا يضر")),
        AzkarSpec("morning", "morning/ya_hayy.mp3", listOf("يا حي")),
        AzkarSpec("morning", "morning/asbahna_alamin.mp3", listOf("رب العالمين")),
        AzkarSpec("morning", "morning/fitrah.mp3", listOf("فطره الاسلام")),
        AzkarSpec("morning", "morning/ilman.mp3", listOf("علما نافعا")),
        AzkarSpec("morning", "morning/salawat.mp3", listOf("بارك", "نبينا")),
        AzkarSpec("morning", "tasbih/subhan_bihamd.mp3", listOf("سبحان الله وبحمده"), exact = true),
        AzkarSpec("morning", "jawami/subhan_bihamd_adada.mp3", listOf("وبحمده", "عدد خلقه")),
        AzkarSpec("morning", "tasbih/tawhid.mp3", listOf("وحده لا شريك"), absent = listOf("اصبحنا", "امسينا", "اشهدك")),
        AzkarSpec("morning", "evening/man_qala_istighfar.mp3", listOf("فر من الزحف")),
        AzkarSpec("morning", "tasbih/istighfar.mp3", listOf("استغفر الله واتوب اليه"), exact = true),
        AzkarSpec("evening", "evening/ayat_kursi.mp3", listOf("كرسي")),
        AzkarSpec("evening", "evening/ikhlas.mp3", listOf("الصمد")),
        AzkarSpec("evening", "evening/falaq.mp3", listOf("الفلق")),
        AzkarSpec("evening", "evening/nas.mp3", listOf("الوسواس")),
        AzkarSpec("evening", "evening/amsayna_mulk.mp3", listOf("امسينا", "سوء الكبر")),
        AzkarSpec("evening", "evening/bika_amsayna.mp3", listOf("بك امسينا")),
        AzkarSpec("evening", "evening/sayyid_istighfar.mp3", listOf("خلقتني")),
        AzkarSpec("evening", "evening/alim_ghayb.mp3", listOf("الغيب", "فاطر")),
        AzkarSpec("evening", "evening/ushhiduka.mp3", listOf("امسيت اشهدك")),
        AzkarSpec("evening", "evening/shirk.mp3", listOf("اشرك")),
        AzkarSpec("evening", "evening/nima.mp3", listOf("امسى بي من نعمه")),
        AzkarSpec("evening", "evening/afini.mp3", listOf("عافني")),
        AzkarSpec("evening", "evening/hamm.mp3", listOf("الهم")),
        AzkarSpec("evening", "evening/afw.mp3", listOf("العفو والعافيه")),
        AzkarSpec("evening", "evening/bismillah_la_yadurr.mp3", listOf("لا يضر")),
        AzkarSpec("evening", "evening/ya_hayy.mp3", listOf("يا حي")),
        AzkarSpec("evening", "evening/fitrah.mp3", listOf("فطره الاسلام")),
        AzkarSpec("evening", "evening/man_qala_istighfar.mp3", listOf("استغفر الله العظيم")),
        AzkarSpec("evening", "evening/salawat.mp3", listOf("بارك", "نبينا")),
        AzkarSpec("evening", "tasbih/subhan_bihamd.mp3", listOf("سبحان الله وبحمده"), exact = true),
        AzkarSpec("evening", "tasbih/tawhid.mp3", listOf("وحده لا شريك"), absent = listOf("اصبحنا", "امسينا", "اشهدك")),
        AzkarSpec("after_prayer", "after_prayer/istighfar_3.mp3", listOf("استغفر الله"), exact = true),
        AzkarSpec("after_prayer", "after_prayer/salam.mp3", listOf("السلام")),
        AzkarSpec(
            "after_prayer",
            "after_prayer/tawhid.mp3",
            listOf("وحده لا شريك"),
            bindAll = true,
            absent = listOf("لا مانع", "لا حول", "نعبد"),
        ),
        AzkarSpec("after_prayer", "after_prayer/subhan.mp3", listOf("سبحان الله"), exact = true),
        AzkarSpec("after_prayer", "after_prayer/hamd.mp3", listOf("الحمد لله"), exact = true),
        AzkarSpec("after_prayer", "after_prayer/takbir.mp3", listOf("الله اكبر"), exact = true),
        AzkarSpec("after_prayer", "after_prayer/ayat_kursi.mp3", listOf("كرسي")),
        AzkarSpec("after_prayer", "after_prayer/ikhlas.mp3", listOf("الصمد")),
        AzkarSpec("after_prayer", "after_prayer/falaq.mp3", listOf("الفلق")),
        AzkarSpec("after_prayer", "after_prayer/nas.mp3", listOf("الوسواس")),
        AzkarSpec("sleep", "sleep/janbi.mp3", listOf("جنبي")),
        AzkarSpec("sleep", "sleep/khalaqta.mp3", listOf("خلقت نفسي")),
        AzkarSpec("sleep", "sleep/qini.mp3", listOf("قني عذابك")),
        AzkarSpec("sleep", "sleep/amutu.mp3", listOf("اموت")),
        AzkarSpec("sleep", "sleep/subhan.mp3", listOf("سبحان الله"), exact = true),
        AzkarSpec("sleep", "sleep/hamd.mp3", listOf("الحمد لله"), exact = true),
        AzkarSpec("sleep", "sleep/takbir.mp3", listOf("الله اكبر"), exact = true),
        AzkarSpec("wake_up", "wake_up/ahyana.mp3", listOf("احيانا")),
        AzkarSpec("wake_up", "wake_up/tawhid.mp3", listOf("وحده لا شريك")),
        AzkarSpec("wake_up", "wake_up/afani.mp3", listOf("عافاني")),
        AzkarSpec("adhan", "adhan/wasilah.mp3", listOf("الوسيله")),
        AzkarSpec("home", "home/walajna.mp3", listOf("ولجنا"), absent = listOf("المولج")),
        AzkarSpec("home", "home/mawlaj.mp3", listOf("المولج")),
        AzkarSpec("home", "home/tawakkalt.mp3", listOf("توكلت")),
        AzkarSpec("home", "home/adilla.mp3", listOf("اضل")),
        AzkarSpec("jawami", "jawami/subhan_bihamd_adada.mp3", listOf("وبحمده", "عدد خلقه")),
        AzkarSpec(
            "jawami",
            "jawami/subhan_adada_khalqih.mp3",
            listOf("عدد خلقه", "رضا"),
            absent = listOf("وبحمده")
        ),
    )

    private fun matchAzkar(items: List<AzkarItemEntity>, spec: AzkarSpec): List<AzkarItemEntity> {
        val needleNorm = spec.needles.map { normalizeAr(it) }
        val absentNorm = spec.absent.map { normalizeAr(it) }.filter { it.isNotEmpty() }
        val hits = items.filter { item ->
            val text = normalizeAr(item.textAr)
            if (absentNorm.any { text.contains(it) }) return@filter false
            if (spec.exact) {
                needleNorm.any { text == it || text == it.trim('.') }
            } else {
                needleNorm.all { text.contains(it) }
            }
        }
        return if (spec.bindAll) hits else hits.take(1)
    }

    internal fun matchedAzkarFile(collectionId: String, textAr: String): String? {
        val item = AzkarItemEntity(collectionId = collectionId, textAr = textAr)
        return azkarMaps().firstNotNullOfOrNull { spec ->
            if (spec.collectionId != collectionId) return@firstNotNullOfOrNull null
            if (matchAzkar(listOf(item), spec).isEmpty()) return@firstNotNullOfOrNull null
            spec.file
        }
    }

    internal fun normalizeAr(raw: String): String {
        val noMarks = raw.replace(TASHKEEL, "")
        val mapped = buildString(noMarks.length) {
            noMarks.forEach { ch ->
                append(
                    when (ch) {
                        'إ', 'أ', 'آ', 'ٱ' -> 'ا'
                        'ى' -> 'ي'
                        'ة' -> 'ه'
                        'ؤ' -> 'و'
                        'ئ' -> 'ي'
                        else -> ch
                    }
                )
            }
        }
        return mapped.replace(NON_ARABIC, "").trim()
    }

    private val TASHKEEL = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val NON_ARABIC = Regex("[^\\u0621-\\u064Aa-zA-Z0-9]+")
}
