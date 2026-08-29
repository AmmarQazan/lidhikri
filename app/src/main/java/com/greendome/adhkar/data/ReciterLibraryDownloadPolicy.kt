package com.greendome.adhkar.data

object ReciterLibraryDownloadPolicy {
    fun shouldDownload(isBuiltin: Boolean, reciterId: Long, optedInIds: Set<Long>): Boolean =
        isBuiltin || reciterId in optedInIds

    fun shouldConfirmLibraryDownload(
        isBuiltin: Boolean,
        alreadyOptedIn: Boolean,
        pendingCount: Int,
    ): Boolean = !isBuiltin && !alreadyOptedIn && pendingCount > 0

    fun allowedReciterIds(
        reciters: List<Pair<Long, Boolean>>,
        optedInIds: Set<Long>,
    ): Set<Long> = reciters
        .filter { (id, builtin) -> shouldDownload(builtin, id, optedInIds) }
        .map { it.first }
        .toSet()
}
