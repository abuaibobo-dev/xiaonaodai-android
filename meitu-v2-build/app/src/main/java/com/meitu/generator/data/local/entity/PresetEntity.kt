package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val prompt: String,
    val negativePrompt: String = "",
    val tags: String = "[]",
    val ratio: String = "1:1",
    val model: String = "真实写实",
    val quality: String = "SD",
    val referenceImagePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
