package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY updatedAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isActive = 1 LIMIT 1")
    fun getActivePreset(): Flow<PresetEntity?>

    @Query("SELECT * FROM presets WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePresetSync(): PresetEntity?

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity): Long

    @Update
    suspend fun update(preset: PresetEntity)

    @Delete
    suspend fun delete(preset: PresetEntity)

    @Query("UPDATE presets SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE presets SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: Long): PresetEntity?
}
