package com.meitu.generator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * EverOS 记忆场景 (MemScene)
 *
 * 将语义相近的 MemCell 聚类到同一个 Scene，
 * centroidEmbedding 为该聚类中心的向量，用于快速相似度匹配。
 * cellIds 存储该场景下所有 MemCell 的 ID（JSON 数组）。
 */
@Entity(tableName = "memory_scenes")
data class EverMemorySceneEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val centroidEmbedding: ByteArray?,
    val cellIds: String,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EverMemorySceneEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
