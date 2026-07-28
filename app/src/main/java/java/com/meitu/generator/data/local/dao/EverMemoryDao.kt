package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.EverMemoryEntity
import com.meitu.generator.data.local.entity.EverMemorySceneEntity
import com.meitu.generator.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * EverOS 记忆系统 DAO
 */
@Dao
interface EverMemoryDao {

    // ============ MemoryCell 操作 ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryCell(cell: EverMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryCells(cells: List<EverMemoryEntity>)

    @Query("SELECT * FROM memory_cells WHERE id = :id")
    suspend fun getCellById(id: String): EverMemoryEntity?

    @Query("SELECT * FROM memory_cells WHERE sceneId = :sceneId ORDER BY createdAt DESC")
    suspend fun getCellsByScene(sceneId: String): List<EverMemoryEntity>

    @Query("SELECT * FROM memory_cells ORDER BY createdAt DESC")
    suspend fun getAllCells(): List<EverMemoryEntity>

    @Query("SELECT * FROM memory_cells ORDER BY createdAt DESC")
    fun observeAllCells(): Flow<List<EverMemoryEntity>>

    @Query("UPDATE memory_cells SET sceneId = :sceneId, updatedAt = :updatedAt WHERE id = :cellId")
    suspend fun updateSceneAssignment(cellId: String, sceneId: String?, updatedAt: Long)

    @Query("UPDATE memory_cells SET sceneId = :sceneId, updatedAt = :updatedAt WHERE id IN (:cellIds)")
    suspend fun updateSceneAssignmentBatch(cellIds: List<String>, sceneId: String?, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM memory_cells")
    suspend fun countCells(): Int

    @Query("DELETE FROM memory_cells WHERE id = :id")
    suspend fun deleteCell(id: String)

    // ============ MemoryScene 操作 ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: EverMemorySceneEntity)

    @Query("SELECT * FROM memory_scenes ORDER BY createdAt DESC")
    suspend fun getAllScenes(): List<EverMemorySceneEntity>

    @Query("SELECT * FROM memory_scenes WHERE id = :id")
    suspend fun getSceneById(id: String): EverMemorySceneEntity?

    @Update
    suspend fun updateScene(scene: EverMemorySceneEntity)

    @Query("DELETE FROM memory_scenes WHERE id = :id")
    suspend fun deleteScene(id: String)

    // ============ UserProfile 操作 ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<UserProfileEntity>)

    @Query("SELECT * FROM user_profiles ORDER BY confidence DESC")
    suspend fun getAllProfiles(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profiles WHERE `key` = :key")
    suspend fun getProfileByKey(key: String): UserProfileEntity?

    @Query("DELETE FROM user_profiles WHERE `key` = :key")
    suspend fun deleteProfile(key: String)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun countProfiles(): Int
}
