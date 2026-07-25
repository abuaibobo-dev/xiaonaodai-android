package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)
    
    @Update
    suspend fun update(memory: MemoryEntity)
    
    @Delete
    suspend fun delete(memory: MemoryEntity)
    
    @Query("SELECT * FROM memory WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntity?
    
    @Query("SELECT * FROM memory WHERE category = :category ORDER BY updatedAt DESC")
    suspend fun getByCategory(category: String): List<MemoryEntity>
    
    @Query("SELECT * FROM memory WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): MemoryEntity?
    
    @Query("SELECT * FROM memory ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memory WHERE category = :category ORDER BY updatedAt DESC")
    fun observeByCategory(category: String): Flow<List<MemoryEntity>>
    
    @Query("DELETE FROM memory WHERE category = :category AND createdAt < :beforeTime")
    suspend fun deleteOldByCategory(category: String, beforeTime: Long)
    
    @Query("SELECT COUNT(*) FROM memory")
    suspend fun count(): Int
}
