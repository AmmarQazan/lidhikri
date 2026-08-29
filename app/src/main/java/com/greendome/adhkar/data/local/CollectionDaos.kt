package com.greendome.adhkar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM adhkar_collection ORDER BY sortOrder")
    fun observeAll(): Flow<List<AdhkarCollectionEntity>>

    @Query("SELECT * FROM adhkar_collection WHERE id = :id")
    suspend fun getById(id: String): AdhkarCollectionEntity?

    @Query("SELECT * FROM adhkar_collection WHERE autoPlayEnabled = 1 AND autoPlayAllowed = 1")
    suspend fun getAutoEnabled(): List<AdhkarCollectionEntity>

    @Query("SELECT * FROM adhkar_collection ORDER BY sortOrder")
    suspend fun getAll(): List<AdhkarCollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<AdhkarCollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: AdhkarCollectionEntity)

    @Update
    suspend fun update(collection: AdhkarCollectionEntity)

    @Query("UPDATE adhkar_collection SET autoPlayEnabled = 1")
    suspend fun enableAllAutoPlay()

    @Query("DELETE FROM adhkar_collection WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AzkarItemDao {
    @Query("SELECT * FROM azkar_item WHERE collectionId = :collectionId ORDER BY sortOrder, id")
    fun observeByCollection(collectionId: String): Flow<List<AzkarItemEntity>>

    @Query("SELECT * FROM azkar_item WHERE collectionId = :collectionId ORDER BY sortOrder, id")
    suspend fun getByCollection(collectionId: String): List<AzkarItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AzkarItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AzkarItemEntity): Long

    @Update
    suspend fun update(item: AzkarItemEntity)

    @Query("SELECT * FROM azkar_item WHERE id = :id")
    suspend fun getById(id: Long): AzkarItemEntity?

    @Query("DELETE FROM azkar_item WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM azkar_item WHERE collectionId = :collectionId")
    suspend fun deleteByCollection(collectionId: String)

    @Query("SELECT id FROM azkar_item WHERE collectionId = :collectionId")
    suspend fun getIdsByCollection(collectionId: String): List<Long>

    @Query("SELECT * FROM azkar_item ORDER BY sortOrder, id")
    suspend fun getAll(): List<AzkarItemEntity>

    @Query("SELECT * FROM azkar_item ORDER BY collectionId, sortOrder, id")
    fun observeAll(): Flow<List<AzkarItemEntity>>

    @Query("SELECT sourceItemId FROM azkar_item WHERE collectionId = :collectionId AND sourceItemId IS NOT NULL")
    fun observeFavoriteSourceIds(collectionId: String): Flow<List<Long>>

    @Query("SELECT * FROM azkar_item WHERE collectionId = :collectionId AND sourceItemId = :sourceItemId LIMIT 1")
    suspend fun getBySourceItemId(collectionId: String, sourceItemId: Long): AzkarItemEntity?

    @Query("DELETE FROM azkar_item WHERE collectionId = :collectionId AND sourceItemId = :sourceItemId")
    suspend fun deleteBySourceItemId(collectionId: String, sourceItemId: Long)

    @Query("SELECT COUNT(*) FROM azkar_item WHERE collectionId = :collectionId")
    suspend fun countByCollection(collectionId: String): Int
}
