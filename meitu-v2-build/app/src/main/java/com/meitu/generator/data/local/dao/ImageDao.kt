package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY isFavorite DESC, generatedAt DESC LIMIT :limit OFFSET :offset")
    fun getImagesPaged(limit: Int, offset: Int): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE isFavorite = 1 ORDER BY generatedAt DESC LIMIT :limit OFFSET :offset")
    fun getFavoriteImagesPaged(limit: Int, offset: Int): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE generatedAt >= :startOfDay ORDER BY isFavorite DESC, generatedAt DESC LIMIT :limit OFFSET :offset")
    fun getTodayImagesPaged(startOfDay: Long, limit: Int, offset: Int): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE generatedAt >= :startOfMonth ORDER BY isFavorite DESC, generatedAt DESC LIMIT :limit OFFSET :offset")
    fun getMonthImagesPaged(startOfMonth: Long, limit: Int, offset: Int): Flow<List<ImageEntity>>

    @Query("SELECT COUNT(*) FROM images")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE status = 1")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE generatedAt >= :startOfDay AND status = 1")
    fun getTodayCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE generatedAt >= :startOfMonth AND status = 1")
    fun getMonthCount(startOfMonth: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE status = 2")
    fun getFailedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE generatedAt >= :startOfDay AND status = 2")
    fun getTodayFailedCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM images WHERE imgbbUrl != ''")
    fun getCloudBackupCount(): Flow<Int>

    @Query("SELECT AVG(CASE WHEN status = 1 AND generatedAt > 0 THEN (generatedAt - generatedAt) ELSE NULL END) FROM images")
    fun getAvgGenerationTime(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageEntity): Long

    @Update
    suspend fun update(image: ImageEntity)

    @Delete
    suspend fun delete(image: ImageEntity)

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getById(id: Long): ImageEntity?

    @Query("SELECT * FROM images WHERE taskId = :taskId")
    suspend fun getByTaskId(taskId: Long): ImageEntity?

    @Query("UPDATE images SET isFavorite = :fav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, fav: Boolean)
}
