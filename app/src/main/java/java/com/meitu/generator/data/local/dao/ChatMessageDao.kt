package com.meitu.generator.data.local.dao

import androidx.room.*
import com.meitu.generator.data.local.entity.ChatMessageEntity

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE isSystem = 0 ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE isSystem = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 50): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE isSystem = 0 AND sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesBySession(sessionId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE isSystem = 0 AND sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesBySession(sessionId: Long, limit: Int = 50): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>): List<Long>

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("""
        SELECT sessionId, MIN(timestamp) as firstTimestamp, 
               (SELECT text FROM chat_messages WHERE sessionId = m.sessionId AND isUser = 1 ORDER BY timestamp ASC LIMIT 1) as firstUserMessage
        FROM chat_messages m 
        WHERE isSystem = 0 AND sessionId > 0
        GROUP BY sessionId 
        ORDER BY MAX(timestamp) DESC
    """)
    suspend fun getSessionSummaries(): List<SessionSummary>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCountBySession(sessionId: Long): Int
}

data class SessionSummary(
    val sessionId: Long,
    val firstTimestamp: Long,
    val firstUserMessage: String?
)
