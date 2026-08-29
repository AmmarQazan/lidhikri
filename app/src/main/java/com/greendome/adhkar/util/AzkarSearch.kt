package com.greendome.adhkar.util

data class AzkarSearchHit(
    val key: String,
    val textAr: String,
    val virtueAr: String = "",
    val sectionTitle: String,
    val extraSearchText: String = "",
    val collectionId: String? = null,
    val itemId: Long? = null,
    val isMyDhikr: Boolean = false,
)

object AzkarSearch {
    private val TASHKEEL = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val WHITESPACE = Regex("\\s+")

    fun normalize(text: String): String =
        text.replace(TASHKEEL, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ئ', 'ي')
            .replace('ؤ', 'و')
            .replace('ة', 'ه')
            .lowercase()
            .trim()

    fun matches(haystack: String, query: String): Boolean {
        val tokens = normalize(query).split(WHITESPACE).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false
        val hay = normalize(haystack)
        return tokens.all { hay.contains(it) }
    }

    fun search(query: String, sources: List<AzkarSearchHit>): List<AzkarSearchHit> {
        if (normalize(query).isEmpty()) return emptyList()
        return sources.filter { hit ->
            matches(
                "${hit.textAr} ${hit.virtueAr} ${hit.sectionTitle} ${hit.extraSearchText}",
                query
            )
        }
    }
}
