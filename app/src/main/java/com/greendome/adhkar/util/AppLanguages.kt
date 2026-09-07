package com.greendome.adhkar.util

import java.util.Locale

data class AppLanguage(
    val code: String,
    val nativeName: String,
    val isRtl: Boolean,
    val enabled: Boolean,
)

object AppLanguages {
    val all: List<AppLanguage> = listOf(
        AppLanguage("ar", "العربية", true, enabled = true),
        AppLanguage("en", "English", false, enabled = true),
        AppLanguage("fr", "Français", false, enabled = true),
        AppLanguage("es", "Español", false, enabled = true),
        AppLanguage("tr", "Türkçe", false, enabled = true),
        AppLanguage("ur", "اردو", true, enabled = true),
        AppLanguage("id", "Bahasa Indonesia", false, enabled = true),
        AppLanguage("hi", "हिन्दी", false, enabled = true),
    )

    val codes: List<String> = all.map { it.code }

    fun nativeName(code: String): String =
        all.find { it.code == code }?.nativeName ?: code

    fun isRtl(code: String): Boolean =
        all.find { it.code == code }?.isRtl == true

    fun isEnabled(code: String): Boolean =
        all.find { it.code == code }?.enabled == true

    fun coerce(code: String): String =
        if (isEnabled(code)) code else "ar"

    fun locale(code: String): Locale = when (code) {
        "ar" -> Locale.forLanguageTag("ar-u-nu-latn")
        "fr" -> Locale.FRENCH
        "es" -> Locale.forLanguageTag("es")
        "tr" -> Locale.forLanguageTag("tr")
        "ur" -> Locale.forLanguageTag("ur")
        "id" -> Locale("in")
        "hi" -> Locale.forLanguageTag("hi")
        else -> Locale.ENGLISH
    }

    fun pickerPairs(): List<Pair<String, String>> =
        all.filter { it.enabled }.map { it.code to it.nativeName }
}
