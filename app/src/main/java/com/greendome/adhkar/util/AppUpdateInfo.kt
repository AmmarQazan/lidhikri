package com.greendome.adhkar.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppUpdateInfo(
    val versionName: String,
    val updatedAtLabel: String
)

object AppUpdateInfoProvider {

    fun get(context: Context, lang: String): AppUpdateInfo {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return AppUpdateInfo(versionName = "—", updatedAtLabel = "—")
        }

        val versionName = packageInfo.versionName ?: "—"
        val updatedAtLabel = formatUpdateDate(packageInfo.lastUpdateTime, lang)
        return AppUpdateInfo(versionName = versionName, updatedAtLabel = updatedAtLabel)
    }

    private fun formatUpdateDate(timeMs: Long, lang: String): String {
        if (timeMs <= 0L) return "—"
        val locale = when (lang) {
            "ar" -> Locale.forLanguageTag("ar-u-nu-latn")
            "fr" -> Locale.FRENCH
            "es" -> Locale.forLanguageTag("es")
            else -> Locale.ENGLISH
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)
        return formatter.format(Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault()))
    }
}
