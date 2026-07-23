package com.greendome.adhkar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "adhkar_collection")
data class AdhkarCollectionEntity(
    @PrimaryKey val id: String,
    val titleAr: String,
    val titleEn: String = "",
    val sortOrder: Int = 0,
    /** يحدده المدير: هل يُسمح لهذا القسم بالتشغيل التلقائي أصلاً */
    val autoPlayAllowed: Boolean = false,
    val autoPlayEnabled: Boolean = false,
    val scheduleHour: Int = 7,
    val scheduleMinute: Int = 0,
    /** بت لكل يوم: الأحد=1، الاثنين=2 … السبت=64 — الكل=127 */
    val weekDaysMask: Int = 127,
    val useTtsAutoPlay: Boolean = true
)

@Entity(tableName = "azkar_item")
data class AzkarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: String,
    val textAr: String,
    val virtueAr: String = "",
    val repeatCount: Int = 1,
    val sortOrder: Int = 0,
    /** معرّف الذكر الأصلي عند نسخه إلى قائمة المفضلة */
    val sourceItemId: Long? = null
)
