package com.greendome.adhkar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.greendome.adhkar.data.model.AudioSourceType
import com.greendome.adhkar.data.model.DhikrCategory
import com.greendome.adhkar.data.model.ReciterVoiceScope
import com.greendome.adhkar.data.model.CollectionDayMode
import com.greendome.adhkar.data.model.ScheduleType

class Converters {
    @TypeConverter fun fromCategory(v: DhikrCategory) = v.name
    @TypeConverter fun toCategory(v: String) = try { DhikrCategory.valueOf(v) } catch (_: Exception) { DhikrCategory.GENERAL }
    @TypeConverter fun fromAudio(v: AudioSourceType) = v.name
    @TypeConverter fun toAudio(v: String) = AudioSourceType.valueOf(v)
    @TypeConverter fun fromSchedule(v: ScheduleType) = v.name
    @TypeConverter fun toSchedule(v: String) = try { ScheduleType.valueOf(v) } catch (_: Exception) { ScheduleType.ALWAYS }
    @TypeConverter fun fromDayMode(v: CollectionDayMode) = v.name
    @TypeConverter fun toDayMode(v: String) = try { CollectionDayMode.valueOf(v) } catch (_: Exception) { CollectionDayMode.WEEKDAYS }
    @TypeConverter fun fromVoiceScope(v: ReciterVoiceScope) = v.name
    @TypeConverter fun toVoiceScope(v: String) = try { ReciterVoiceScope.valueOf(v) } catch (_: Exception) { ReciterVoiceScope.BOTH }
}

@Database(
    entities = [
        DhikrEntity::class,
        ReciterEntity::class,
        ReciterAudioEntity::class,
        ReciterAzkarAudioEntity::class,
        AdhanAudioEntity::class,
        DailyStatsEntity::class,
        AdhkarCollectionEntity::class,
        AzkarItemEntity::class,
        PendingPublishChangeEntity::class
    ],
        version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AdhkarDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun reciterDao(): ReciterDao
    abstract fun reciterAudioDao(): ReciterAudioDao
    abstract fun reciterAzkarAudioDao(): ReciterAzkarAudioDao
    abstract fun adhanAudioDao(): AdhanAudioDao
    abstract fun statsDao(): StatsDao
    abstract fun collectionDao(): CollectionDao
    abstract fun azkarItemDao(): AzkarItemDao
    abstract fun catalogSyncDao(): CatalogSyncDao
    abstract fun pendingPublishDao(): PendingPublishDao

    companion object {
        @Volatile private var instance: AdhkarDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dhikr ADD COLUMN textTr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dhikr ADD COLUMN textUr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dhikr ADD COLUMN textId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE dhikr ADD COLUMN textHi TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reciter ADD COLUMN nameTr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reciter ADD COLUMN nameUr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reciter ADD COLUMN nameId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reciter ADD COLUMN nameHi TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN titleFr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN titleEs TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN titleTr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN titleUr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN titleId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN titleHi TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_publish_change (
                        changeKey TEXT NOT NULL PRIMARY KEY,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        entityKey TEXT NOT NULL,
                        action TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reciter ADD COLUMN voiceScope TEXT NOT NULL DEFAULT 'BOTH'")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN dayMode TEXT NOT NULL DEFAULT 'WEEKDAYS'")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN hijriMonth INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN hijriDayStart INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE adhkar_collection ADD COLUMN hijriDayEnd INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN scheduleHour INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN scheduleMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN prayerAnchor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN prayerOffsetMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN skipQuietWindow INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN hijriMonth INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN hijriDayStart INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE azkar_item ADD COLUMN hijriDayEnd INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS adhan_audio (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nameAr TEXT NOT NULL,
                        nameEn TEXT NOT NULL DEFAULT '',
                        localPath TEXT,
                        remoteUrl TEXT,
                        suitableForFajr INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        isDownloaded INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN assetPath TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN muezzinAr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN muezzinEn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN countryAr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN countryEn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN cityAr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN cityEn TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN maqamAr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE adhan_audio ADD COLUMN maqamEn TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AdhkarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdhkarDatabase::class.java,
                    "adhkar.db"
                ).addMigrations(
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                )
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
