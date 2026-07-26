package com.greendome.adhkar.sync

import androidx.room.withTransaction
import com.greendome.adhkar.data.AzkarFavorites
import com.greendome.adhkar.data.local.AdhkarCollectionEntity
import com.greendome.adhkar.data.local.AdhkarDatabase
import com.greendome.adhkar.data.local.AzkarItemEntity
import com.greendome.adhkar.data.local.DhikrEntity
import com.greendome.adhkar.data.local.ReciterAudioEntity
import com.greendome.adhkar.data.local.ReciterAzkarAudioEntity
import com.greendome.adhkar.data.local.ReciterEntity
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType
import org.json.JSONArray
import org.json.JSONObject

class RemoteContentApplier(private val db: AdhkarDatabase) {

    suspend fun apply(bundle: RemoteContentBundle) {
        db.withTransaction {
            db.catalogSyncDao().clearCatalogForSync(AzkarFavorites.COLLECTION_ID)

            insertReciters(bundle.reciters)
            insertDhikr(bundle.dhikr)
            insertCollections(bundle.collections)
            insertAzkarItems(bundle.azkarItems)
            insertReciterAudio(bundle.reciterAudio)
            insertReciterAzkarAudio(bundle.reciterAzkarAudio)
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
                    isBuiltin = o.optBoolean("isBuiltin", true),
                    isActive = o.optBoolean("isActive", true)
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
            db.dhikrDao().insert(
                DhikrEntity(
                    id = o.getLong("id"),
                    textAr = o.getString("textAr"),
                    textEn = o.optString("textEn", ""),
                    textFr = o.optString("textFr", ""),
                    textEs = o.optString("textEs", ""),
                    category = enumValue(o.optString("category", DhikrCategory.GENERAL.name)),
                    repeatCount = o.optInt("repeatCount", 1),
                    isEnabled = o.optBoolean("isEnabled", true),
                    isDefault = o.optBoolean("isDefault", true),
                    isLongForm = o.optBoolean("isLongForm", false),
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
                    sortOrder = o.optInt("sortOrder", 0),
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
                    sortOrder = o.optInt("sortOrder", 0),
                    autoPlayAllowed = o.optBoolean("autoPlayAllowed", false),
                    autoPlayEnabled = o.optBoolean("autoPlayEnabled", false),
                    scheduleHour = o.optInt("scheduleHour", 7),
                    scheduleMinute = o.optInt("scheduleMinute", 0),
                    weekDaysMask = o.optInt("weekDaysMask", 127),
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
                    sourceItemId = o.optLong("sourceItemId", -1L).takeIf { it >= 0L }
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

    private inline fun <reified T : Enum<T>> enumValue(raw: String): T =
        runCatching { enumValueOf<T>(raw) }.getOrElse {
            enumValueOf(T::class.java.enumConstants!!.first().name)
        }
}
