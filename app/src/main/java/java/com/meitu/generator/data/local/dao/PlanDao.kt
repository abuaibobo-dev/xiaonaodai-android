package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PlanEntity): Long
    
    @Update
    suspend fun update(plan: PlanEntity)
    
    @Query("SELECT * FROM plan WHERE id = :id")
    suspend fun getById(id: Long): PlanEntity?
    
    @Query("SELECT * FROM plan WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBySession(sessionId: String): PlanEntity?
    
    @Query("SELECT * FROM plan WHERE status = 'running' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getRunningPlan(): PlanEntity?
    
    @Query("SELECT * FROM plan ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PlanEntity>>
    
    @Query("DELETE FROM plan WHERE status = 'completed' AND createdAt < :beforeTime")
    suspend fun deleteOldCompleted(beforeTime: Long)
}
