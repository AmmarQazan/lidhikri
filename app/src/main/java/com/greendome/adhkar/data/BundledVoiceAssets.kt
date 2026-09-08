package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase

/**
 * يعيد ربط أصوات صبيحات المدمجة بعد مزامنة السحابة،
 * ويزيل مسارات «أصوات متنوعة» التي لم تعد داخل الحزمة.
 */
object BundledVoiceAssets {
    suspend fun attach(db: AdhkarDatabase) {
        SubaihatReciterSeed.ensure(db)
        clearRetiredMixedVoiceAssets(db)
    }

    private suspend fun clearRetiredMixedVoiceAssets(db: AdhkarDatabase) {
        val reciterId = ReciterLibrariesMigration.MIXED_VOICES_ID
        db.reciterAudioDao().getByReciter(reciterId).forEach { audio ->
            if (!isRetiredMixedAsset(audio.assetPath)) return@forEach
            db.reciterAudioDao().insert(
                audio.copy(
                    assetPath = null,
                    isDownloaded = !audio.localPath.isNullOrBlank(),
                )
            )
        }
        db.reciterAzkarAudioDao().getByReciter(reciterId).forEach { audio ->
            if (!isRetiredMixedAsset(audio.assetPath)) return@forEach
            db.reciterAzkarAudioDao().insert(
                audio.copy(
                    assetPath = null,
                    isDownloaded = !audio.localPath.isNullOrBlank(),
                )
            )
        }
    }

    private fun isRetiredMixedAsset(path: String?): Boolean {
        val value = path?.replace('\\', '/') ?: return false
        return value.startsWith("audio/sou_") ||
            value.startsWith("audio/azkar/qt_")
    }
}
