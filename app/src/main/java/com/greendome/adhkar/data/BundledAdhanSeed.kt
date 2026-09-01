package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.prayer.PrayerName

/** الأذان المضمّن: ضياء الدين لغير الفجر والعصر، والقصاص للفجر. أذان العصر الافتراضي: نايف فيده (كتالوج بعيد). */
object BundledAdhanSeed {
    const val DEFAULT_ID = 10001L
    const val FAJR_DEFAULT_ID = 10005L
    const val ASR_DEFAULT_ID = 22002L

    private val retiredAssetPaths = setOf(
        "adhan/kamal_almaroush_morocco.mp3",
        "adhan/bahjat_almazuri_iraq.mp3",
        "adhan/marouf_sharif_jordan.mp3",
    )

    data class Spec(
        val id: Long,
        val nameAr: String,
        val nameEn: String,
        val muezzinAr: String,
        val muezzinEn: String,
        val countryAr: String,
        val countryEn: String,
        val cityAr: String,
        val cityEn: String,
        val maqamAr: String = "",
        val maqamEn: String = "",
        val assetPath: String,
        val suitableForFajr: Boolean,
        val sortOrder: Int,
    )

    val specs = listOf(
        Spec(
            id = DEFAULT_ID,
            nameAr = "ضياء الدين بن نزار الدين — إندونيسيا",
            nameEn = "Dhiyauddin bin Nizaruddin — Indonesia",
            muezzinAr = "ضياء الدين بن نزار الدين",
            muezzinEn = "Dhiyauddin bin Nizaruddin",
            countryAr = "إندونيسيا",
            countryEn = "Indonesia",
            cityAr = "",
            cityEn = "",
            assetPath = "adhan/dhiyauddin_nizaruddin_indonesia.mp3",
            suitableForFajr = true,
            sortOrder = 0,
        ),
        Spec(
            id = FAJR_DEFAULT_ID,
            nameAr = "محمد بن مروان القصاص — المدينة المنورة (فجر)",
            nameEn = "Muhammad ibn Marwan Al-Qassas — Madinah (Fajr)",
            muezzinAr = "محمد بن مروان القصاص",
            muezzinEn = "Muhammad ibn Marwan Al-Qassas",
            countryAr = "السعودية",
            countryEn = "Saudi Arabia",
            cityAr = "المدينة المنورة",
            cityEn = "Madinah",
            assetPath = "adhan/muhammad_qassas_madinah_fajr.mp3",
            suitableForFajr = true,
            sortOrder = 1,
        ),
    )

    fun defaultId(prayer: PrayerName): Long = when (prayer) {
        PrayerName.FAJR -> FAJR_DEFAULT_ID
        PrayerName.ASR -> ASR_DEFAULT_ID
        else -> DEFAULT_ID
    }

    fun entities(): List<AdhanAudioEntity> = specs.map { it.toEntity() }

    suspend fun ensure(db: AdhkarDatabase) {
        val dao = db.adhanAudioDao()
        val existing = dao.getAll()
        specs.forEach { spec ->
            val current = existing.find { it.id == spec.id }
                ?: existing.find { it.assetPath == spec.assetPath }
            if (current == null) {
                dao.insert(spec.toEntity())
            } else if (current.id != spec.id) {
                dao.delete(current.id)
                dao.insert(spec.toEntity())
            } else {
                dao.update(
                    current.copy(
                        nameAr = spec.nameAr,
                        nameEn = spec.nameEn,
                        muezzinAr = spec.muezzinAr,
                        muezzinEn = spec.muezzinEn,
                        countryAr = spec.countryAr,
                        countryEn = spec.countryEn,
                        cityAr = spec.cityAr,
                        cityEn = spec.cityEn,
                        maqamAr = spec.maqamAr,
                        maqamEn = spec.maqamEn,
                        assetPath = spec.assetPath,
                        suitableForFajr = spec.suitableForFajr,
                        sortOrder = spec.sortOrder,
                        isActive = true,
                        isDownloaded = true,
                    )
                )
            }
        }
        val keepIds = specs.map { it.id }.toSet()
        existing.filter { row ->
            row.id !in keepIds && (
                row.assetPath in retiredAssetPaths ||
                    row.id in 10002L..10004L
                )
        }.forEach { dao.delete(it.id) }
    }

    private fun Spec.toEntity() = AdhanAudioEntity(
        id = id,
        nameAr = nameAr,
        nameEn = nameEn,
        muezzinAr = muezzinAr,
        muezzinEn = muezzinEn,
        countryAr = countryAr,
        countryEn = countryEn,
        cityAr = cityAr,
        cityEn = cityEn,
        maqamAr = maqamAr,
        maqamEn = maqamEn,
        assetPath = assetPath,
        suitableForFajr = suitableForFajr,
        isActive = true,
        sortOrder = sortOrder,
        isDownloaded = true,
    )
}
