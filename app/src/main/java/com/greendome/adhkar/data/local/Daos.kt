package com.greendome.adhkar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr WHERE isEnabled = 1 ORDER BY sortOrder, id")
    fun observeEnabled(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE isLongForm = 1 ORDER BY sortOrder, id")
    fun observeLongForm(): Flow<List<DhikrEntity>>

    @Query("SELECT * FROM dhikr WHERE id = :id")
    suspend fun getById(id: Long): DhikrEntity?

    @Query("SELECT * FROM dhikr WHERE isEnabled = 1 ORDER BY sortOrder, id")
    suspend fun getEnabledList(): List<DhikrEntity>

    @Query("SELECT * FROM dhikr WHERE isDefault = 1")
    suspend fun getDefaults(): List<DhikrEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DhikrEntity): Long

    @Update
    suspend fun update(entity: DhikrEntity)

    @Query("DELETE FROM dhikr WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustom(id: Long)

    @Query("DELETE FROM dhikr WHERE isDefault = 1")
    suspend fun deleteDefaults()

    @Query(
        """
        UPDATE dhikr SET
            displayPopup = 1,
            displayNotification = 0,
            displayLockScreen = 1,
            displayAudioOnly = 0,
            displayAudioText = 1
        WHERE isDefault = 1
        """
    )
    suspend fun resetDefaultDisplayModes()

    @Query("UPDATE dhikr SET isDownloaded = :downloaded, audioPath = :path WHERE id = :id")
    suspend fun updateDownloadState(id: Long, downloaded: Boolean, path: String?)
}

@Dao
interface ReciterDao {
    @Query("SELECT * FROM reciter WHERE isActive = 1 ORDER BY id")
    fun observeActive(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciter ORDER BY id")
    fun observeAll(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciter WHERE id = :id")
    suspend fun getById(id: Long): ReciterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReciterEntity): Long

    @Update
    suspend fun update(entity: ReciterEntity)

    @Query("DELETE FROM reciter WHERE id = :id AND isBuiltin = 0")
    suspend fun deleteCustom(id: Long)

    @Query("UPDATE reciter SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}

@Dao
interface ReciterAudioDao {
    @Query("SELECT * FROM reciter_audio WHERE dhikrId = :dhikrId AND reciterId = :reciterId LIMIT 1")
    suspend fun get(dhikrId: Long, reciterId: Long): ReciterAudioEntity?

    @Query("SELECT * FROM reciter_audio WHERE reciterId = :reciterId")
    fun observeByReciter(reciterId: Long): Flow<List<ReciterAudioEntity>>

    @Query("SELECT * FROM reciter_audio WHERE reciterId = :reciterId")
    suspend fun getByReciter(reciterId: Long): List<ReciterAudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReciterAudioEntity): Long

    @Query("DELETE FROM reciter_audio WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reciter_audio WHERE reciterId = :reciterId")
    suspend fun deleteByReciter(reciterId: Long)

    @Query("SELECT * FROM reciter_audio WHERE isDownloaded = 0 AND remoteUrl IS NOT NULL")
    suspend fun getPendingDownloads(): List<ReciterAudioEntity>

    @Query("UPDATE reciter_audio SET isDownloaded = 1, localPath = :path WHERE id = :id")
    suspend fun markDownloaded(id: Long, path: String)
}

@Dao
interface ReciterAzkarAudioDao {
    @Query("SELECT * FROM reciter_azkar_audio WHERE azkarItemId = :azkarItemId AND reciterId = :reciterId LIMIT 1")
    suspend fun get(azkarItemId: Long, reciterId: Long): ReciterAzkarAudioEntity?

    @Query("SELECT * FROM reciter_azkar_audio WHERE reciterId = :reciterId")
    fun observeByReciter(reciterId: Long): Flow<List<ReciterAzkarAudioEntity>>

    @Query("SELECT * FROM reciter_azkar_audio WHERE reciterId = :reciterId")
    suspend fun getByReciter(reciterId: Long): List<ReciterAzkarAudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReciterAzkarAudioEntity): Long

    @Query("DELETE FROM reciter_azkar_audio WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM reciter_azkar_audio WHERE reciterId = :reciterId")
    suspend fun deleteByReciter(reciterId: Long)

    @Query("DELETE FROM reciter_azkar_audio WHERE azkarItemId IN (:ids)")
    suspend fun deleteByAzkarItemIds(ids: List<Long>)

    @Query("SELECT * FROM reciter_azkar_audio WHERE isDownloaded = 0 AND remoteUrl IS NOT NULL")
    suspend fun getPendingDownloads(): List<ReciterAzkarAudioEntity>

    @Query("UPDATE reciter_azkar_audio SET isDownloaded = 1, localPath = :path WHERE id = :id")
    suspend fun markDownloaded(id: Long, path: String)
}

@Dao
interface StatsDao {
    @Query("SELECT playCount FROM daily_stats WHERE dateKey = :dateKey")
    suspend fun getCount(dateKey: String): Int?

    @Query("SELECT * FROM daily_stats WHERE dateKey LIKE :prefix || '%'")
    suspend fun getByKeyPrefix(prefix: String): List<DailyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyStatsEntity)

    @Query("UPDATE daily_stats SET playCount = playCount + 1 WHERE dateKey = :dateKey")
    suspend fun increment(dateKey: String)
}

@Dao
interface CatalogSyncDao {
    @Query("DELETE FROM reciter_azkar_audio")
    suspend fun deleteAllReciterAzkarAudio()

    @Query("DELETE FROM reciter_audio")
    suspend fun deleteAllReciterAudio()

    @Query("DELETE FROM azkar_item WHERE collectionId != :preserveCollectionId")
    suspend fun deleteCatalogAzkarExcept(preserveCollectionId: String)

    @Query("DELETE FROM adhkar_collection WHERE id != :preserveCollectionId")
    suspend fun deleteCatalogCollectionsExcept(preserveCollectionId: String)

    @Query("DELETE FROM dhikr WHERE isDefault = 1")
    suspend fun deleteDefaultDhikr()

    @Query("DELETE FROM reciter")
    suspend fun deleteAllReciters()

    @Transaction
    suspend fun clearCatalogForSync(preserveCollectionId: String) {
        deleteAllReciterAzkarAudio()
        deleteAllReciterAudio()
        deleteCatalogAzkarExcept(preserveCollectionId)
        deleteCatalogCollectionsExcept(preserveCollectionId)
        deleteDefaultDhikr()
        deleteAllReciters()
    }
}
