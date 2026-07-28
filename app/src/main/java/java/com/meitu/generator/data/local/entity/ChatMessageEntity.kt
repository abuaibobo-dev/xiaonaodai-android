package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 聊天消息持久化实体
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["timestamp"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val imageUri: String? = null,
    val reasoningContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
