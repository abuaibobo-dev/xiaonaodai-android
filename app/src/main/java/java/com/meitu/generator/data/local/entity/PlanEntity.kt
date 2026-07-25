package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 执行计划实体 - 支持复杂任务的分步执行和断点续传
 */
@Entity(
    tableName = "plan",
    indices = [Index(value = ["sessionId"]), Index(value = ["status"])]
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val steps: String,        // JSON array: [{"step":1,"tool":"image_generate","done":false},...]
    val currentStep: Int = 0,
    val status: String = "pending",  // pending, running, completed, failed
    val originalQuery: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
