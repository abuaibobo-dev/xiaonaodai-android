package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: Long = 0,
    val presetName: String = "",
    val targetCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val status: Int = 0, // 0=running, 1=completed, 2=cancelled
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = 0,
    val durationSeconds: Long = 0
)
