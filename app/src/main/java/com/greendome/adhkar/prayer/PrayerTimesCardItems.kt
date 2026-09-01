package com.greendome.adhkar.prayer

internal const val PRAYER_TIMES_CARD_IMSAK = "imsak"
internal const val PRAYER_TIMES_CARD_SUNRISE = "sunrise"

internal fun prayerTimesCardKeys(): List<String> = buildList {
    add(PRAYER_TIMES_CARD_IMSAK)
    add(PrayerName.FAJR.name)
    add(PRAYER_TIMES_CARD_SUNRISE)
    PrayerName.entries.filter { it != PrayerName.FAJR }.mapTo(this) { it.name }
}

internal fun prayerTimesCardHighlightIndex(next: PrayerName?): Int {
    if (next == null) return -1
    return prayerTimesCardKeys().indexOf(next.name)
}
