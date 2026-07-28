package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EverOS 用户画像 (Profile)
 *
 * 从多次交互中提炼出的稳定画像条目，
 * confidence 表示置信度（0.0 - 1.0），
 * validFrom / validUntil 定义有效时间窗口。
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val key: String,
    val value: String,
    val confidence: Float,
    val validFrom: Long,
    val validUntil: Long?,
    val updatedAt: Long
)
