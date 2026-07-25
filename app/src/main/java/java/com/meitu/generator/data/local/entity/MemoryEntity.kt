package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Agent 记忆实体 - 存储用户偏好、历史操作、收藏的提示词等
 * 每次对话前注入相关记忆到 system prompt
 */
@Entity(
    tableName = "memory",
    indices = [
        Index(value = ["category"]),
        Index(value = ["key"])
    ]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,  // preference, history, favorite_prompt, context
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
