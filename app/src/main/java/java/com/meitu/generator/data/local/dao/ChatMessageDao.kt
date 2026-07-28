package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * 聊天消息 DAO
 */
@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>): List<Long>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ChatMessageEntity>
}
