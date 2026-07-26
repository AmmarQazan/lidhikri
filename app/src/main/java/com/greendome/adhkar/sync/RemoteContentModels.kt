package com.greendome.adhkar.sync

import org.json.JSONArray
import org.json.JSONObject

data class RemoteContentManifest(
    val version: Int,
    val contentPath: String,
    val sha256: String
) {
    companion object {
        fun parse(json: JSONObject): RemoteContentManifest = RemoteContentManifest(
            version = json.getInt("version"),
            contentPath = json.getString("contentPath"),
            sha256 = json.getString("sha256")
        )
    }
}

data class RemoteContentBundle(
    val version: Int,
    val dhikr: JSONArray,
    val reciters: JSONArray,
    val reciterAudio: JSONArray,
    val reciterAzkarAudio: JSONArray,
    val collections: JSONArray,
    val azkarItems: JSONArray
) {
    companion object {
        fun parse(json: JSONObject): RemoteContentBundle = RemoteContentBundle(
            version = json.getInt("version"),
            dhikr = json.getJSONArray("dhikr"),
            reciters = json.getJSONArray("reciters"),
            reciterAudio = json.getJSONArray("reciterAudio"),
            reciterAzkarAudio = json.getJSONArray("reciterAzkarAudio"),
            collections = json.getJSONArray("collections"),
            azkarItems = json.getJSONArray("azkarItems")
        )
    }
}

sealed class RemoteSyncResult {
    data object UpToDate : RemoteSyncResult()
    data class Updated(val version: Int) : RemoteSyncResult()
    data class Failed(val reason: String) : RemoteSyncResult()
}
