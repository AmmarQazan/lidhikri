package com.greendome.adhkar.util

import android.content.Context
import com.greendome.adhkar.data.local.AzkarItemEntity
import kotlin.random.Random

object AzkarDailyPicker {
    private const val PREFS = "azkar_daily_picker"

    fun pick(
        context: Context,
        collectionId: String,
        items: List<AzkarItemEntity>,
        randomMode: Boolean
    ): AzkarItemEntity? {
        if (items.isEmpty()) return null
        if (randomMode) return items.random()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "seq_$collectionId"
        val index = prefs.getInt(key, 0) % items.size
        prefs.edit().putInt(key, index + 1).apply()
        return items[index]
    }
}
