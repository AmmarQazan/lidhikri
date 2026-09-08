package com.greendome.adhkar.util

import android.app.LocaleManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
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

    fun applyAppLocales(context: Context, languageCode: String) {
        val code = AppLanguages.coerce(languageCode)
        applyLauncherLabel(context.applicationContext, code)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val tag = AppLanguages.systemLocaleTag(code)
        val manager = context.getSystemService(LocaleManager::class.java) ?: return
        val current = manager.applicationLocales.toLanguageTags()
        if (current.equals(tag, ignoreCase = true)) return
        manager.applicationLocales = LocaleList.forLanguageTags(tag)
    }

    fun applyLauncherLabel(context: Context, languageCode: String) {
        val pm = context.packageManager
        val pkg = context.packageName
        val enabled = AppLanguages.launcherAliasClass(languageCode)
        for (code in AppLanguages.codes) {
            val className = AppLanguages.launcherAliasClass(code)
            val state = if (className == enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, className),
                    state,
                    PackageManager.DONT_KILL_APP,
                )
            } catch (_: IllegalArgumentException) {
                // Alias missing from an older install until the new manifest is applied.
            }
        }
    }

    fun string(context: Context, languageCode: String, @StringRes id: Int): String {
        val locale = AppLanguages.locale(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config).getString(id)
    }
}
