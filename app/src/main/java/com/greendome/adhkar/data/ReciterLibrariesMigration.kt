package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.ReciterEntity
import java.io.File

/**
 * مكتبات القرّاء:
 * ناصر الدين صبيحات هو الافتراضي عند أول تثبيت إن لم يختر المدير غيره.
 * المدمج يحدده المدير لأي قارئ.
 * مكتبة حسان قازان تُحذف ولا تُعاد.
 */
object ReciterLibrariesMigration {
    const val MIXED_VOICES_ID = 1L
    const val LEGACY_QURANTIME_ID = 2L
    const val NAME_AR = "أصوات متنوعة"
    const val NAME_EN = "Various Voices"
    const val NAME_FR = "Voix variées"
    const val NAME_ES = "Voces variadas"
    const val NAME_TR = "Çeşitli sesler"
    const val NAME_UR = "متنوع آوازیں"
    const val NAME_ID = "Berbagai suara"
    const val NAME_HI = "विविध आवाज़ें"

    fun mixedVoicesEntity(): ReciterEntity = ReciterEntity(
        id = MIXED_VOICES_ID,
        nameAr = NAME_AR,
        nameEn = NAME_EN,
        nameFr = NAME_FR,
        nameEs = NAME_ES,
        nameTr = NAME_TR,
        nameUr = NAME_UR,
        nameId = NAME_ID,
        nameHi = NAME_HI,
        isBuiltin = false,
        isActive = true
    )

    suspend fun apply(db: AdhkarDatabase, settings: SettingsRepository) {
        purgeHassanQazan(db, settings)
        mergeLegacyLibraries(db, settings)
        enforceDefaultBuiltinReciter(db)
    }

    suspend fun ensureMixedVoicesReciter(db: AdhkarDatabase): Long {
        val existing = db.reciterDao().getById(MIXED_VOICES_ID)
        val entity = mixedVoicesEntity()
        if (existing == null) {
            db.reciterDao().insert(entity)
        } else if (
            existing.nameAr != NAME_AR ||
            existing.nameEn != NAME_EN ||
            existing.nameFr != NAME_FR ||
            existing.nameEs != NAME_ES ||
            existing.nameTr != NAME_TR ||
            existing.nameUr != NAME_UR ||
            existing.nameId != NAME_ID ||
            existing.nameHi != NAME_HI ||
            !existing.isActive
        ) {
            db.reciterDao().update(
                existing.copy(
                    nameAr = NAME_AR,
                    nameEn = NAME_EN,
                    nameFr = NAME_FR,
                    nameEs = NAME_ES,
                    nameTr = NAME_TR,
                    nameUr = NAME_UR,
                    nameId = NAME_ID,
                    nameHi = NAME_HI,
                    isActive = true
                )
            )
        }
        return MIXED_VOICES_ID
    }

    private suspend fun purgeHassanQazan(db: AdhkarDatabase, settings: SettingsRepository) {
        val hassanIds = db.reciterDao().getAll()
            .filter { isHassanQazan(it) && !isKeptLibrary(it) }
            .map { it.id }
            .toSet()
        if (hassanIds.isEmpty()) return
        remapIfSelected(settings, hassanIds, SubaihatReciterSeed.RECITER_ID)
        hassanIds.forEach { id -> purgeReciter(db, id, deleteLocalFiles = true) }
    }

    private suspend fun mergeLegacyLibraries(db: AdhkarDatabase, settings: SettingsRepository) {
        ensureMixedVoicesReciter(db)
        val sources = db.reciterDao().getAll().filter { shouldMergeIntoMixedVoices(it) }
        if (sources.isEmpty()) return
        val sourceIds = sources.map { it.id }.toSet()
        sources.forEach { source ->
            moveAudio(db, fromId = source.id, toId = MIXED_VOICES_ID)
        }
        remapIfSelected(settings, sourceIds, MIXED_VOICES_ID)
        sourceIds.forEach { id -> purgeReciter(db, id, deleteLocalFiles = false) }
    }

    suspend fun enforceDefaultBuiltinReciter(db: AdhkarDatabase) {
        val all = db.reciterDao().getAll()
        val promoteId = ReciterBuiltinPolicy.reciterIdToPromoteAsBuiltin(
            all.map { Triple(it.id, it.nameAr, it.isBuiltin) },
        ) ?: return
        val reciter = all.find { it.id == promoteId } ?: return
        db.reciterDao().update(reciter.copy(isBuiltin = true))
    }

    private suspend fun moveAudio(db: AdhkarDatabase, fromId: Long, toId: Long) {
        val targetDhikr = db.reciterAudioDao().getByReciter(toId).map { it.dhikrId }.toSet()
        db.reciterAudioDao().getByReciter(fromId).forEach { audio ->
            if (audio.dhikrId !in targetDhikr) {
                db.reciterAudioDao().insert(audio.copy(id = 0L, reciterId = toId))
            }
        }
        val targetAzkar = db.reciterAzkarAudioDao().getByReciter(toId).map { it.azkarItemId }.toSet()
        db.reciterAzkarAudioDao().getByReciter(fromId).forEach { audio ->
            if (audio.azkarItemId !in targetAzkar) {
                db.reciterAzkarAudioDao().insert(audio.copy(id = 0L, reciterId = toId))
            }
        }
    }

    private suspend fun purgeReciter(
        db: AdhkarDatabase,
        id: Long,
        deleteLocalFiles: Boolean
    ) {
        db.reciterAudioDao().getByReciter(id).forEach { audio ->
            if (deleteLocalFiles) deleteLocal(audio.localPath)
        }
        db.reciterAzkarAudioDao().getByReciter(id).forEach { audio ->
            if (deleteLocalFiles) deleteLocal(audio.localPath)
        }
        db.reciterAudioDao().deleteByReciter(id)
        db.reciterAzkarAudioDao().deleteByReciter(id)
        db.reciterDao().deleteById(id)
    }

    private fun remapIfSelected(
        settings: SettingsRepository,
        removedIds: Set<Long>,
        fallbackId: Long
    ) {
        if (settings.selectedReciterId in removedIds) {
            settings.selectedReciterId = fallbackId
        }
        if (settings.azkarSelectedReciterId in removedIds) {
            settings.azkarSelectedReciterId = fallbackId
        }
    }

    private fun shouldMergeIntoMixedVoices(reciter: ReciterEntity): Boolean {
        if (reciter.id == MIXED_VOICES_ID) return false
        if (isKeptLibrary(reciter)) return false
        if (isHassanQazan(reciter)) return false
        val ar = reciter.nameAr
        val en = reciter.nameEn
        return ar.contains("المسبحة الصوتية") ||
            ar.contains("masba7a", ignoreCase = true) ||
            en.contains("masba7a", ignoreCase = true) ||
            ar.equals("QuranTime", ignoreCase = true) ||
            en.equals("QuranTime", ignoreCase = true)
    }

    private fun isHassanQazan(reciter: ReciterEntity): Boolean {
        if (reciter.nameAr.contains("حسان")) return true
        val blob = listOf(
            reciter.nameAr, reciter.nameEn, reciter.nameFr, reciter.nameEs,
            reciter.nameTr, reciter.nameUr, reciter.nameId, reciter.nameHi
        )
            .joinToString(" ")
            .lowercase()
        return blob.contains("hassan") &&
            (blob.contains("qazan") || blob.contains("kazan"))
    }

    private fun isKeptLibrary(reciter: ReciterEntity): Boolean =
        ReciterBuiltinPolicy.isAppDefaultBuiltin(reciter.id, reciter.nameAr)

    private fun deleteLocal(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
        }
    }
}
