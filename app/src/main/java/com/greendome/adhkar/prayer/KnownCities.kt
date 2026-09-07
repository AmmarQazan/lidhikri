package com.greendome.adhkar.prayer

private fun city(
    lat: Double,
    lon: Double,
    name: String,
    country: String,
    code: String,
    tz: String,
) = PrayerLocation(lat, lon, name, country, code, tz)

val KNOWN_CITIES: List<PrayerLocation> = listOf(
    city(21.4225, 39.8262, "مكة المكرمة", "السعودية", "SA", "Asia/Riyadh"),
    city(24.5247, 39.5692, "المدينة المنورة", "السعودية", "SA", "Asia/Riyadh"),
    city(24.7136, 46.6753, "الرياض", "السعودية", "SA", "Asia/Riyadh"),
    city(21.5433, 39.1728, "جدة", "السعودية", "SA", "Asia/Riyadh"),
    city(26.4207, 50.0888, "الدمام", "السعودية", "SA", "Asia/Riyadh"),
    city(25.2048, 55.2708, "دبي", "الإمارات", "AE", "Asia/Dubai"),
    city(24.4539, 54.3773, "أبوظبي", "الإمارات", "AE", "Asia/Dubai"),
    city(29.3759, 47.9774, "الكويت", "الكويت", "KW", "Asia/Kuwait"),
    city(25.2854, 51.5310, "الدوحة", "قطر", "QA", "Asia/Qatar"),
    city(26.2285, 50.5860, "المنامة", "البحرين", "BH", "Asia/Bahrain"),
    city(23.5880, 58.3829, "مسقط", "عُمان", "OM", "Asia/Muscat"),
    city(30.0444, 31.2357, "القاهرة", "مصر", "EG", "Africa/Cairo"),
    city(31.2001, 29.9187, "الإسكندرية", "مصر", "EG", "Africa/Cairo"),
    city(31.9539, 35.9106, "عمّان", "الأردن", "JO", "Asia/Amman"),
    city(33.8938, 35.5018, "بيروت", "لبنان", "LB", "Asia/Beirut"),
    city(33.5138, 36.2765, "دمشق", "سوريا", "SY", "Asia/Damascus"),
    city(33.3152, 44.3661, "بغداد", "العراق", "IQ", "Asia/Baghdad"),
    city(31.7683, 35.2137, "القدس", "فلسطين", "PS", "Asia/Gaza"),
    city(31.5017, 34.4668, "غزة", "فلسطين", "PS", "Asia/Gaza"),
    city(41.0082, 28.9784, "إسطنبول", "تركيا", "TR", "Europe/Istanbul"),
    city(39.9334, 32.8597, "أنقرة", "تركيا", "TR", "Europe/Istanbul"),
    city(33.5731, -7.5898, "الدار البيضاء", "المغرب", "MA", "Africa/Casablanca"),
    city(34.0209, -6.8416, "الرباط", "المغرب", "MA", "Africa/Casablanca"),
    city(36.7538, 3.0588, "الجزائر", "الجزائر", "DZ", "Africa/Algiers"),
    city(36.8065, 10.1815, "تونس", "تونس", "TN", "Africa/Tunis"),
    city(32.8872, 13.1913, "طرابلس", "ليبيا", "LY", "Africa/Tripoli"),
    city(15.5007, 32.5599, "الخرطوم", "السودان", "SD", "Africa/Khartoum"),
    city(18.0735, -15.9582, "نواكشوط", "موريتانيا", "MR", "Africa/Nouakchott"),
    city(2.0469, 45.3182, "مقديشو", "الصومال", "SO", "Africa/Mogadishu"),
    city(11.5721, 43.1456, "جيبوتي", "جيبوتي", "DJ", "Africa/Djibouti"),
    city(-11.7172, 43.2473, "موروني", "جزر القمر", "KM", "Indian/Comoro"),
    city(24.8607, 67.0011, "كراتشي", "باكستان", "PK", "Asia/Karachi"),
    city(31.5204, 74.3587, "لاهور", "باكستان", "PK", "Asia/Karachi"),
    city(33.6844, 73.0479, "إسلام آباد", "باكستان", "PK", "Asia/Karachi"),
    city(23.8103, 90.4125, "دكا", "بنغلاديش", "BD", "Asia/Dhaka"),
    city(28.6139, 77.2090, "دلهي", "الهند", "IN", "Asia/Kolkata"),
    city(-6.2088, 106.8456, "جاكرتا", "إندونيسيا", "ID", "Asia/Jakarta"),
    city(-5.1477, 119.4327, "ماكاسار", "إندونيسيا", "ID", "Asia/Makassar"),
    city(3.1390, 101.6869, "كوالالمبور", "ماليزيا", "MY", "Asia/Kuala_Lumpur"),
    city(51.5074, -0.1278, "لندن", "بريطانيا", "GB", "Europe/London"),
    city(48.8566, 2.3522, "باريس", "فرنسا", "FR", "Europe/Paris"),
    city(52.3676, 4.9041, "أمستردام", "هولندا", "NL", "Europe/Amsterdam"),
    city(55.7558, 37.6173, "موسكو", "روسيا", "RU", "Europe/Moscow"),
    city(43.8563, 18.4131, "سراييفو", "البوسنة", "BA", "Europe/Sarajevo"),
    city(41.3275, 19.8187, "تيرانا", "ألبانيا", "AL", "Europe/Tirane"),
    city(42.6629, 21.1655, "بريشتينا", "كوسوفو", "XK", "Europe/Belgrade"),
    city(41.9981, 21.4254, "سكوبي", "مقدونيا", "MK", "Europe/Skopje"),
    city(42.4304, 19.2594, "بودغوريتسا", "الجبل الأسود", "ME", "Europe/Podgorica"),
    city(39.9042, 116.4074, "بكين", "الصين", "CN", "Asia/Shanghai"),
    city(43.8256, 87.6168, "أورومتشي", "الصين", "CN", "Asia/Urumqi"),
    city(40.7128, -74.0060, "نيويورك", "أمريكا", "US", "America/New_York"),
    city(34.0522, -118.2437, "لوس أنجلوس", "أمريكا", "US", "America/Los_Angeles"),
    city(41.8781, -87.6298, "شيكاغو", "أمريكا", "US", "America/Chicago"),
    city(43.6532, -79.3832, "تورونتو", "كندا", "CA", "America/Toronto"),
    city(49.2827, -123.1207, "فانكوفر", "كندا", "CA", "America/Vancouver"),
    city(-33.8688, 151.2093, "سيدني", "أستراليا", "AU", "Australia/Sydney"),
    city(-31.9505, 115.8605, "بيرث", "أستراليا", "AU", "Australia/Perth"),
    city(35.6762, 139.6503, "طوكيو", "اليابان", "JP", "Asia/Tokyo"),
    city(34.6937, 135.5023, "أوساكا", "اليابان", "JP", "Asia/Tokyo"),
    city(6.5244, 3.3792, "لاغوس", "نيجيريا", "NG", "Africa/Lagos"),
    city(9.0765, 7.3986, "أبوجا", "نيجيريا", "NG", "Africa/Lagos"),
    city(12.0022, 8.5920, "كانو", "نيجيريا", "NG", "Africa/Lagos"),
    city(-23.5505, -46.6333, "ساو باولو", "البرازيل", "BR", "America/Sao_Paulo"),
    city(-22.9068, -43.1729, "ريو دي جانيرو", "البرازيل", "BR", "America/Sao_Paulo"),
    city(-15.7975, -47.8919, "برازيليا", "البرازيل", "BR", "America/Sao_Paulo"),
    city(-3.1190, -60.0217, "ماناوس", "البرازيل", "BR", "America/Manaus"),
    city(-8.0476, -34.8770, "ريسيفي", "البرازيل", "BR", "America/Fortaleza"),
    city(-33.9249, 18.4241, "كيب تاون", "جنوب أفريقيا", "ZA", "Africa/Johannesburg"),
    city(-26.2041, 28.0473, "جوهانسبرغ", "جنوب أفريقيا", "ZA", "Africa/Johannesburg"),
    city(-29.8587, 31.0218, "ديربان", "جنوب أفريقيا", "ZA", "Africa/Johannesburg"),
    city(41.2995, 69.2401, "طشقند", "أوزبكستان", "UZ", "Asia/Tashkent"),
    city(43.2220, 76.8512, "ألماتي", "كازاخستان", "KZ", "Asia/Almaty"),
    city(51.1694, 71.4491, "أستانا", "كازاخستان", "KZ", "Asia/Almaty"),
    city(42.8746, 74.5698, "بيشكك", "قيرغيزستان", "KG", "Asia/Bishkek"),
    city(38.5598, 68.7738, "دوشانبي", "طاجيكستان", "TJ", "Asia/Dushanbe"),
    city(37.9601, 58.3261, "عشق آباد", "تركمانستان", "TM", "Asia/Ashgabat"),
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
    "ماكاسار" -> "Makassar"
    "كوالالمبور" -> "Kuala Lumpur"
    "لندن" -> "London"
    "باريس" -> "Paris"
    "أمستردام" -> "Amsterdam"
    "موسكو" -> "Moscow"
    "سراييفو" -> "Sarajevo"
    "تيرانا" -> "Tirana"
    "بريشتينا" -> "Pristina Prishtina"
    "سكوبي" -> "Skopje"
    "بودغوريتسا" -> "Podgorica"
    "بكين" -> "Beijing"
    "أورومتشي" -> "Urumqi Ürümqi"
    "نيويورك" -> "New York"
    "لوس أنجلوس" -> "Los Angeles"
    "شيكاغو" -> "Chicago"
    "تورونتو" -> "Toronto"
    "فانكوفر" -> "Vancouver"
    "سيدني" -> "Sydney"
    "بيرث" -> "Perth"
    "طوكيو" -> "Tokyo"
    "أوساكا" -> "Osaka"
    "لاغوس" -> "Lagos"
    "أبوجا" -> "Abuja"
    "كانو" -> "Kano"
    "ساو باولو" -> "Sao Paulo São Paulo"
    "ريو دي جانيرو" -> "Rio de Janeiro"
    "برازيليا" -> "Brasilia Brasília"
    "ماناوس" -> "Manaus"
    "ريسيفي" -> "Recife"
    "كيب تاون" -> "Cape Town"
    "جوهانسبرغ" -> "Johannesburg"
    "ديربان" -> "Durban"
    "طشقند" -> "Tashkent"
    "ألماتي" -> "Almaty"
    "أستانا" -> "Astana Nur-Sultan"
    "بيشكك" -> "Bishkek"
    "دوشانبي" -> "Dushanbe"
    "عشق آباد" -> "Ashgabat"
    else -> ""
}
