package com.greendome.adhkar.sync

import androidx.room.withTransaction
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.BlessedDaysAzkar
import com.greendome.adhkar.data.BundledAdhanSeed
import com.greendome.adhkar.data.FridayAzkar
import com.greendome.adhkar.data.local.AdhanAudioEntity
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.dhikrEnabledForAutoTasbihImport
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ReciterVoiceScope
import com.greendome.adhkar.data.model.ScheduleType
import org.json.JSONArray
import org.json.JSONObject

class RemoteContentApplier(private val db: AdhkarDatabase) {

    suspend fun apply(bundle: RemoteContentBundle) {
        db.withTransaction {
            db.catalogSyncDao().clearCatalogForSync(
                listOf(
                    AzkarFavorites.COLLECTION_ID,
                    FridayAzkar.COLLECTION_ID,
                    BlessedDaysAzkar.COLLECTION_ID,
                )
            )

            insertReciters(bundle.reciters)
            insertDhikr(bundle.dhikr)
            insertCollections(bundle.collections)
            insertAzkarItems(bundle.azkarItems)
            insertReciterAudio(bundle.reciterAudio)
            insertReciterAzkarAudio(bundle.reciterAzkarAudio)
            insertAdhanAudio(bundle.adhanAudio)
        }
    }

    private suspend fun insertReciters(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            db.reciterDao().insert(
                ReciterEntity(
                    id = o.getLong("id"),
                    nameAr = o.getString("nameAr"),
                    nameEn = o.optString("nameEn", ""),
                    nameFr = o.optString("nameFr", ""),
                    nameEs = o.optString("nameEs", ""),
                    nameTr = o.optString("nameTr", ""),
                    nameUr = o.optString("nameUr", ""),
                    nameId = o.optString("nameId", ""),
                    nameHi = o.optString("nameHi", ""),
                    isBuiltin = o.optBoolean("isBuiltin", false),
                    isActive = o.optBoolean("isActive", true),
                    voiceScope = enumValue(o.optString("voiceScope", ReciterVoiceScope.BOTH.name)),
                )
            )
        }
    }

    private suspend fun insertDhikr(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val remoteAudio = o.optString("remoteAudioUrl", "").takeIf { it.isNotBlank() }
            val audioPath = o.optString("audioPath", "").takeIf { it.isNotBlank() }
            val sourceType = enumValue<AudioSourceType>(o.optString("audioSourceType", AudioSourceType.NONE.name))
            val resolvedType = when {
                remoteAudio != null -> AudioSourceType.DOWNLOAD
                sourceType == AudioSourceType.BUILTIN && audioPath != null -> AudioSourceType.DOWNLOAD
                else -> sourceType
            }
            val category = enumValue<DhikrCategory>(o.optString("category", DhikrCategory.GENERAL.name))
            val isLongForm = o.optBoolean("isLongForm", false)
            val isDefault = o.optBoolean("isDefault", true)
            val sortOrder = o.optInt("sortOrder", 0)
            db.dhikrDao().insert(
                DhikrEntity(
                    id = o.getLong("id"),
                    textAr = o.getString("textAr"),
                    textEn = o.optString("textEn", ""),
                    textFr = o.optString("textFr", ""),
                    textEs = o.optString("textEs", ""),
                    textTr = o.optString("textTr", ""),
                    textUr = o.optString("textUr", ""),
                    textId = o.optString("textId", ""),
                    textHi = o.optString("textHi", ""),
                    category = category,
                    repeatCount = o.optInt("repeatCount", 1),
                    isEnabled = dhikrEnabledForAutoTasbihImport(
                        category = category,
                        isLongForm = isLongForm,
                        isDefault = isDefault,
                        sortOrder = sortOrder,
                        remoteEnabled = o.optBoolean("isEnabled", true),
                    ),
                    isDefault = isDefault,
                    isLongForm = isLongForm,
                    audioSourceType = resolvedType,
                    audioPath = remoteAudio ?: audioPath,
                    reciterId = o.optLong("reciterId", -1L).takeIf { it >= 0L },
                    remoteAudioUrl = remoteAudio,
                    isDownloaded = o.optBoolean("isDownloaded", false) && remoteAudio == null,
                    displayPopup = o.optBoolean("displayPopup", true),
                    displayNotification = o.optBoolean("displayNotification", false),
                    displayLockScreen = o.optBoolean("displayLockScreen", true),
                    displayAudioOnly = o.optBoolean("displayAudioOnly", false),
                    displayAudioText = o.optBoolean("displayAudioText", true),
                    sortOrder = sortOrder,
                    scheduleType = enumValue(o.optString("scheduleType", ScheduleType.ALWAYS.name)),
                    timeStartHour = o.optInt("timeStartHour", -1),
                    timeStartMinute = o.optInt("timeStartMinute", 0),
                    timeEndHour = o.optInt("timeEndHour", -1),
                    timeEndMinute = o.optInt("timeEndMinute", 0),
                    hijriMonth = o.optInt("hijriMonth", -1),
                    hijriDayStart = o.optInt("hijriDayStart", -1),
                    hijriDayEnd = o.optInt("hijriDayEnd", -1),
                    scheduleLabelAr = o.optString("scheduleLabelAr", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
    }

    private suspend fun insertCollections(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            db.collectionDao().insert(
                AdhkarCollectionEntity(
                    id = o.getString("id"),
                    titleAr = o.getString("titleAr"),
                    titleEn = o.optString("titleEn", ""),
                    titleFr = o.optString("titleFr", ""),
                    titleEs = o.optString("titleEs", ""),
                    titleTr = o.optString("titleTr", ""),
                    titleUr = o.optString("titleUr", ""),
                    titleId = o.optString("titleId", ""),
                    titleHi = o.optString("titleHi", ""),
                    sortOrder = o.optInt("sortOrder", 0),
                    autoPlayAllowed = o.optBoolean("autoPlayAllowed", false),
                    autoPlayEnabled = o.optBoolean("autoPlayEnabled", false),
                    scheduleHour = o.optInt("scheduleHour", 7),
                    scheduleMinute = o.optInt("scheduleMinute", 0),
                    weekDaysMask = o.optInt("weekDaysMask", 127),
                    dayMode = enumValue(o.optString("dayMode", CollectionDayMode.WEEKDAYS.name)),
                    hijriMonth = o.optInt("hijriMonth", -1),
                    hijriDayStart = o.optInt("hijriDayStart", -1),
                    hijriDayEnd = o.optInt("hijriDayEnd", -1),
                    useTtsAutoPlay = o.optBoolean("useTtsAutoPlay", true)
                )
            )
        }
    }

    private suspend fun insertAzkarItems(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            db.azkarItemDao().insert(
                AzkarItemEntity(
                    id = o.getLong("id"),
                    collectionId = o.getString("collectionId"),
                    textAr = o.getString("textAr"),
                    virtueAr = o.optString("virtueAr", ""),
                    repeatCount = o.optInt("repeatCount", 1),
                    sortOrder = o.optInt("sortOrder", 0),
                    sourceItemId = o.optLong("sourceItemId", -1L).takeIf { it >= 0L },
                    scheduleHour = o.optInt("scheduleHour", -1),
                    scheduleMinute = o.optInt("scheduleMinute", 0),
                    prayerAnchor = o.optString("prayerAnchor", ""),
                    prayerOffsetMinutes = o.optInt("prayerOffsetMinutes", 0),
                    skipQuietWindow = o.optBoolean("skipQuietWindow", false),
                    hijriMonth = o.optInt("hijriMonth", -1),
                    hijriDayStart = o.optInt("hijriDayStart", -1),
                    hijriDayEnd = o.optInt("hijriDayEnd", -1),
                )
            )
        }
    }

    private suspend fun insertReciterAudio(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val remoteUrl = o.optString("remoteUrl", "").takeIf { it.isNotBlank() }
            db.reciterAudioDao().insert(
                ReciterAudioEntity(
                    id = o.getLong("id"),
                    reciterId = o.getLong("reciterId"),
                    dhikrId = o.getLong("dhikrId"),
                    localPath = null,
                    remoteUrl = remoteUrl,
                    assetPath = null,
                    isDownloaded = remoteUrl == null && o.optString("localPath", "").isNotBlank()
                )
            )
        }
    }

    private suspend fun insertReciterAzkarAudio(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val remoteUrl = o.optString("remoteUrl", "").takeIf { it.isNotBlank() }
            db.reciterAzkarAudioDao().insert(
                ReciterAzkarAudioEntity(
                    id = o.getLong("id"),
                    reciterId = o.getLong("reciterId"),
                    azkarItemId = o.getLong("azkarItemId"),
                    localPath = null,
                    remoteUrl = remoteUrl,
                    assetPath = null,
                    isDownloaded = remoteUrl == null && o.optString("localPath", "").isNotBlank()
                )
            )
        }
    }

    private suspend fun insertAdhanAudio(array: JSONArray) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val id = o.getLong("id")
            val remoteUrl = o.optString("remoteUrl", "").takeIf { it.isNotBlank() }
            val existing = db.adhanAudioDao().getById(id)
            val incomingAsset = o.optString("assetPath", "").takeIf { it.isNotBlank() }
            val bundled = BundledAdhanSeed.specs.any { it.id == id }
            db.adhanAudioDao().insert(
                AdhanAudioEntity(
                    id = id,
                    nameAr = o.getString("nameAr"),
                    nameEn = o.optString("nameEn", ""),
                    muezzinAr = o.optString("muezzinAr", ""),
                    muezzinEn = o.optString("muezzinEn", ""),
                    countryAr = o.optString("countryAr", ""),
                    countryEn = o.optString("countryEn", ""),
                    cityAr = o.optString("cityAr", ""),
                    cityEn = o.optString("cityEn", ""),
                    maqamAr = o.optString("maqamAr", ""),
                    maqamEn = o.optString("maqamEn", ""),
                    localPath = existing?.localPath,
                    remoteUrl = remoteUrl,
                    assetPath = incomingAsset ?: existing?.assetPath?.takeIf { bundled },
                    suitableForFajr = jsonFlag(o, "suitableForFajr", false),
                    isActive = jsonFlag(o, "isActive", true),
                    sortOrder = o.optInt("sortOrder", i),
                    isDownloaded = existing?.isDownloaded == true &&
                        !existing.localPath.isNullOrBlank() &&
                        existing.remoteUrl == remoteUrl
                )
            )
        }
    }

    private fun jsonFlag(o: JSONObject, key: String, default: Boolean): Boolean {
        if (!o.has(key) || o.isNull(key)) return default
        return when (val value = o.opt(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> default
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String): T =
        runCatching { enumValueOf<T>(raw) }.getOrElse {
            enumValueOf(T::class.java.enumConstants!!.first().name)
        }
}
