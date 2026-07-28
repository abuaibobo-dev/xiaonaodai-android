package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * EverOS 记忆单元 (MemCell)
 *
 * 每条记忆由三部分组成：
 * - episode: 简短叙事（第三人称），便于 LLM 直接引用
 * - facts: 原子事实列表（JSON 数组），用于细粒度检索
 * - foresight: 前瞻性推断（JSON 对象），含 valid_from / valid_until 时间窗口
 *
 * embedding 使用简化版 hash-based 向量（ByteArray），后续可替换为真实模型
 */
@Entity(
    tableName = "memory_cells",
    indices = [
        Index(value = ["sceneId"]),
        Index(value = ["createdAt"])
    ]
)
data class EverMemoryEntity(
    @PrimaryKey val id: String,
    val episode: String,
    val facts: String,
    val foresight: String,
    val metadata: String,
    val embedding: ByteArray?,
    val sceneId: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EverMemoryEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
