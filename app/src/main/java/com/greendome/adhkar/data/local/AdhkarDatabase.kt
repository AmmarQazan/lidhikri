package com.greendome.adhkar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ScheduleType

class Converters {
    @TypeConverter fun fromCategory(v: DhikrCategory) = v.name
    @TypeConverter fun toCategory(v: String) = try { DhikrCategory.valueOf(v) } catch (_: Exception) { DhikrCategory.GENERAL }
    @TypeConverter fun fromAudio(v: AudioSourceType) = v.name
    @TypeConverter fun toAudio(v: String) = AudioSourceType.valueOf(v)
    @TypeConverter fun fromSchedule(v: ScheduleType) = v.name
    @TypeConverter fun toSchedule(v: String) = try { ScheduleType.valueOf(v) } catch (_: Exception) { ScheduleType.ALWAYS }
}

@Database(
    entities = [
        DhikrEntity::class,
        ReciterEntity::class,
        ReciterAudioEntity::class,
        ReciterAzkarAudioEntity::class,
        DailyStatsEntity::class,
        AdhkarCollectionEntity::class,
        AzkarItemEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AdhkarDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun reciterDao(): ReciterDao
    abstract fun reciterAudioDao(): ReciterAudioDao
    abstract fun reciterAzkarAudioDao(): ReciterAzkarAudioDao
    abstract fun statsDao(): StatsDao
    abstract fun collectionDao(): CollectionDao
    abstract fun azkarItemDao(): AzkarItemDao

    companion object {
        @Volatile private var instance: AdhkarDatabase? = null

        fun get(context: Context): AdhkarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdhkarDatabase::class.java,
                    "adhkar.db"
                ).fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
