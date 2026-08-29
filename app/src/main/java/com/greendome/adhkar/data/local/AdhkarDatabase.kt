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
import com.greendome.adhkar.data.model.ScheduleType

class Converters {
    @TypeConverter fun fromCategory(v: DhikrCategory) = v.name
    @TypeConverter fun toCategory(v: String) = try { DhikrCategory.valueOf(v) } catch (_: Exception) { DhikrCategory.GENERAL }
    @TypeConverter fun fromAudio(v: AudioSourceType) = v.name
    @TypeConverter fun toAudio(v: String) = AudioSourceType.valueOf(v)
    @TypeConverter fun fromSchedule(v: ScheduleType) = v.name
    @TypeConverter fun toSchedule(v: String) = try { ScheduleType.valueOf(v) } catch (_: Exception) { ScheduleType.ALWAYS }
    @TypeConverter fun fromVoiceScope(v: ReciterVoiceScope) = v.name
    @TypeConverter fun toVoiceScope(v: String) = try { ReciterVoiceScope.valueOf(v) } catch (_: Exception) { ReciterVoiceScope.BOTH }
}

@Database(
    entities = [
        DhikrEntity::class,
        ReciterEntity::class,
        ReciterAudioEntity::class,
        ReciterAzkarAudioEntity::class,
        DailyStatsEntity::class,
        AdhkarCollectionEntity::class,
        AzkarItemEntity::class,
        PendingPublishChangeEntity::class
    ],
    version = 9,
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

        fun get(context: Context): AdhkarDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdhkarDatabase::class.java,
                    "adhkar.db"
                ).addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
