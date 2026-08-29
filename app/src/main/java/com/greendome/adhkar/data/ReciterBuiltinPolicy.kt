package com.greendome.adhkar.data

object ReciterBuiltinPolicy {
    fun isAppDefaultBuiltin(reciterId: Long, nameAr: String): Boolean =
        reciterId == SubaihatReciterSeed.RECITER_ID || nameAr.contains("صبيحات")

    /**
     * المدمج يختاره المدير. لا نرقّي الافتراضي إلا إذا لم يُعلَّم أي قارئ كمدمج.
     */
    fun reciterIdToPromoteAsBuiltin(
        reciters: List<Triple<Long, String, Boolean>>,
    ): Long? {
        if (reciters.any { it.third }) return null
        return reciters.find { isAppDefaultBuiltin(it.first, it.second) }?.first
    }
}
