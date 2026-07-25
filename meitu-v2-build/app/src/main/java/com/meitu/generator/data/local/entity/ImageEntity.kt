package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val presetId: Long = 0,
    val taskId: Long = 0,
    val prompt: String = "",
    val model: String = "",
    val ratio: String = "",
    val quality: String = "",
    val localPath: String = "",
    val imgbbUrl: String = "",
    val imgbbDeleteUrl: String = "",
    val isFavorite: Boolean = false,
    val status: Int = 0, // 0=generating, 1=success, 2=failed
    val retryCount: Int = 0,
    val errorMessage: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
    val uploadedAt: Long = 0
)
