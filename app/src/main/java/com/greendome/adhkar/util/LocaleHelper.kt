package com.greendome.adhkar.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale

object LocaleHelper {
    private const val PREFS = "adhkar_settings"
    private const val KEY = "app_language"

    fun getLanguage(context: Context): String =
        AppLanguages.coerce(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "ar") ?: "ar"
        )

    fun wrap(context: Context, languageCode: String): Context {
        val locale = AppLanguages.locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun wrapWithSavedLanguage(context: Context): Context =
        wrap(context, getLanguage(context))

    fun string(context: Context, languageCode: String, @StringRes id: Int): String {
        val locale = AppLanguages.locale(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config).getString(id)
    }
}
