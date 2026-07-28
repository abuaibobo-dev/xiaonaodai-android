package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val imageUri: String? = null,
    val reasoningContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
