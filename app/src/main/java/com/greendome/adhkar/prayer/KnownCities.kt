package com.greendome.adhkar.prayer

val KNOWN_CITIES: List<PrayerLocation> = listOf(
    PrayerLocation(21.4225, 39.8262, "مكة المكرمة", "السعودية", "SA"),
    PrayerLocation(24.5247, 39.5692, "المدينة المنورة", "السعودية", "SA"),
    PrayerLocation(24.7136, 46.6753, "الرياض", "السعودية", "SA"),
    PrayerLocation(21.5433, 39.1728, "جدة", "السعودية", "SA"),
    PrayerLocation(26.4207, 50.0888, "الدمام", "السعودية", "SA"),
    PrayerLocation(25.2048, 55.2708, "دبي", "الإمارات", "AE"),
    PrayerLocation(24.4539, 54.3773, "أبوظبي", "الإمارات", "AE"),
    PrayerLocation(29.3759, 47.9774, "الكويت", "الكويت", "KW"),
    PrayerLocation(25.2854, 51.5310, "الدوحة", "قطر", "QA"),
    PrayerLocation(26.2285, 50.5860, "المنامة", "البحرين", "BH"),
    PrayerLocation(23.5880, 58.3829, "مسقط", "عُمان", "OM"),
    PrayerLocation(30.0444, 31.2357, "القاهرة", "مصر", "EG"),
    PrayerLocation(31.2001, 29.9187, "الإسكندرية", "مصر", "EG"),
    PrayerLocation(31.9539, 35.9106, "عمّان", "الأردن", "JO"),
    PrayerLocation(33.8938, 35.5018, "بيروت", "لبنان", "LB"),
    PrayerLocation(33.5138, 36.2765, "دمشق", "سوريا", "SY"),
    PrayerLocation(33.3152, 44.3661, "بغداد", "العراق", "IQ"),
    PrayerLocation(31.7683, 35.2137, "القدس", "فلسطين", "PS"),
    PrayerLocation(31.5017, 34.4668, "غزة", "فلسطين", "PS"),
    PrayerLocation(41.0082, 28.9784, "إسطنبول", "تركيا", "TR"),
    PrayerLocation(39.9334, 32.8597, "أنقرة", "تركيا", "TR"),
    PrayerLocation(33.5731, -7.5898, "الدار البيضاء", "المغرب", "MA"),
    PrayerLocation(34.0209, -6.8416, "الرباط", "المغرب", "MA"),
    PrayerLocation(36.7538, 3.0588, "الجزائر", "الجزائر", "DZ"),
    PrayerLocation(36.8065, 10.1815, "تونس", "تونس", "TN"),
    PrayerLocation(32.8872, 13.1913, "طرابلس", "ليبيا", "LY"),
    PrayerLocation(15.5007, 32.5599, "الخرطوم", "السودان", "SD"),
    PrayerLocation(18.0735, -15.9582, "نواكشوط", "موريتانيا", "MR"),
    PrayerLocation(2.0469, 45.3182, "مقديشو", "الصومال", "SO"),
    PrayerLocation(11.5721, 43.1456, "جيبوتي", "جيبوتي", "DJ"),
    PrayerLocation(-11.7172, 43.2473, "موروني", "جزر القمر", "KM"),
    PrayerLocation(24.8607, 67.0011, "كراتشي", "باكستان", "PK"),
    PrayerLocation(31.5204, 74.3587, "لاهور", "باكستان", "PK"),
    PrayerLocation(33.6844, 73.0479, "إسلام آباد", "باكستان", "PK"),
    PrayerLocation(23.8103, 90.4125, "دكا", "بنغلاديش", "BD"),
    PrayerLocation(28.6139, 77.2090, "دلهي", "الهند", "IN"),
    PrayerLocation(-6.2088, 106.8456, "جاكرتا", "إندونيسيا", "ID"),
    PrayerLocation(3.1390, 101.6869, "كوالالمبور", "ماليزيا", "MY"),
    PrayerLocation(51.5074, -0.1278, "لندن", "بريطانيا", "GB"),
    PrayerLocation(48.8566, 2.3522, "باريس", "فرنسا", "FR"),
    PrayerLocation(40.7128, -74.0060, "نيويورك", "أمريكا", "US")
)

fun searchKnownCities(query: String): List<PrayerLocation> {
    val needle = query.trim()
    if (needle.length < 2) return emptyList()
    return KNOWN_CITIES.filter { city ->
        city.cityName.contains(needle, ignoreCase = true) ||
            city.countryName.contains(needle, ignoreCase = true) ||
            city.countryCode.contains(needle, ignoreCase = true) ||
            latinAlias(city).contains(needle, ignoreCase = true)
    }
}

private fun latinAlias(city: PrayerLocation): String = when (city.cityName) {
    "مكة المكرمة" -> "Makkah Mecca"
    "المدينة المنورة" -> "Madinah Medina"
    "الرياض" -> "Riyadh"
    "جدة" -> "Jeddah"
    "الدمام" -> "Dammam"
    "دبي" -> "Dubai"
    "أبوظبي" -> "Abu Dhabi"
    "الكويت" -> "Kuwait"
    "الدوحة" -> "Doha"
    "المنامة" -> "Manama"
    "مسقط" -> "Muscat"
    "القاهرة" -> "Cairo"
    "الإسكندرية" -> "Alexandria"
    "عمّان" -> "Amman"
    "بيروت" -> "Beirut"
    "دمشق" -> "Damascus"
    "بغداد" -> "Baghdad"
    "القدس" -> "Jerusalem"
    "غزة" -> "Gaza"
    "إسطنبول" -> "Istanbul"
    "أنقرة" -> "Ankara"
    "الدار البيضاء" -> "Casablanca"
    "الرباط" -> "Rabat"
    "الجزائر" -> "Algiers"
    "تونس" -> "Tunis"
    "طرابلس" -> "Tripoli"
    "الخرطوم" -> "Khartoum"
    "نواكشوط" -> "Nouakchott"
    "مقديشو" -> "Mogadishu"
    "جيبوتي" -> "Djibouti"
    "موروني" -> "Moroni"
    "كراتشي" -> "Karachi"
    "لاهور" -> "Lahore"
    "إسلام آباد" -> "Islamabad"
    "دكا" -> "Dhaka"
    "دلهي" -> "Delhi"
    "جاكرتا" -> "Jakarta"
    "كوالالمبور" -> "Kuala Lumpur"
    "لندن" -> "London"
    "باريس" -> "Paris"
    "نيويورك" -> "New York"
    else -> ""
}
