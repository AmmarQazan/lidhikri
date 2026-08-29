package com.greendome.adhkar.data

import com.greendome.adhkar.data.local.AdhkarDatabase

object CatalogRecovery {
    fun isOfficialCatalogEmpty(
        defaultDhikrCount: Int,
        officialCollectionCount: Int
    ): Boolean = defaultDhikrCount <= 0 || officialCollectionCount <= 0

    fun shouldSkipBuiltinSeed(
        remoteContentVersion: Int,
        catalogEmpty: Boolean
    ): Boolean = remoteContentVersion > 0 && !catalogEmpty

    fun shouldForceRemoteSync(catalogEmpty: Boolean): Boolean = catalogEmpty

    /**
     * نعيد تطبيق الحزمة إذا اختلفت البصمة، حتى لو رقم الإصدار متساوياً.
     * الاعتماد على الإصدار وحده يتخطى التحديث إذا نُسخ الرقم من نسخة احتياطية
     * أو إذا حُذفت عناصر بعد المزامنة.
     */
    fun shouldSkipRemoteApply(
        force: Boolean,
        remoteSha256: String,
        appliedSha256: String
    ): Boolean {
        if (force) return false
        val remote = remoteSha256.trim()
        val applied = appliedSha256.trim()
        return remote.isNotEmpty() && remote.equals(applied, ignoreCase = true)
    }

    fun needsBuiltinBackfill(
        hasMorningCollection: Boolean,
        defaultShortTasbihCount: Int
    ): Boolean = !hasMorningCollection || defaultShortTasbihCount <= 0
}

suspend fun AdhkarDatabase.isOfficialCatalogEmpty(): Boolean =
    CatalogRecovery.isOfficialCatalogEmpty(
        defaultDhikrCount = dhikrDao().getDefaults().size,
        officialCollectionCount = collectionDao().getAll().count {
            it.id != AzkarFavorites.COLLECTION_ID
        }
    )
