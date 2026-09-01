package com.greendome.adhkar.util

enum class HomeSection(val id: String) {
    NEXT_PRAYER("next_prayer"),
    PRAYER_TIMES("prayer_times"),
    QIBLA("qibla"),
    AUTO_TASBIH("auto_tasbih"),
    AUTO_AZKAR("auto_azkar"),
    TODAY_STATS("today_stats");

    companion object {
        val DEFAULT_ORDER: List<String> = entries.map { it.id }
        val DEFAULT_HIDDEN: Set<String> = setOf(QIBLA.id)
        val EMBEDDED_IN_NEXT_PRAYER: Set<String> = setOf(AUTO_TASBIH.id, AUTO_AZKAR.id)
        /** Qibla is a header button; tasbih/azkar sit inside the next-prayer card. */
        val NOT_CUSTOMIZABLE: Set<String> = EMBEDDED_IN_NEXT_PRAYER + QIBLA.id

        fun fromId(id: String): HomeSection? = entries.find { it.id == id }
    }
}

object HomeLayout {
    fun merge(saved: List<String>): List<String> {
        if (saved.isEmpty()) return HomeSection.DEFAULT_ORDER
        val remaining = HomeSection.entries.map { it.id }.toMutableSet()
        val result = mutableListOf<String>()
        saved.forEach { id ->
            if (id in remaining) {
                result += id
                remaining.remove(id)
            }
        }
        HomeSection.DEFAULT_ORDER.filter { it in remaining }.forEach { result += it }
        return result
    }

    fun customizable(saved: List<String>): List<String> =
        merge(saved).filter { it !in HomeSection.NOT_CUSTOMIZABLE }

    fun hidden(saved: Set<String>, keyPresent: Boolean): Set<String> {
        val known = HomeSection.entries.map { it.id }.toSet()
        if (!keyPresent) return HomeSection.DEFAULT_HIDDEN
        return saved.filter { it in known }.toSet()
    }

    fun moved(ids: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (fromIndex == toIndex) return ids
        if (fromIndex !in ids.indices || toIndex !in ids.indices) return ids
        return ids.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }
}
