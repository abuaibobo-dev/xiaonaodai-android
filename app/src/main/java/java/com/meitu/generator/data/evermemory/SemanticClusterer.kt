package com.meitu.generator.data.evermemory

import com.meitu.generator.data.local.entity.EverMemoryEntity
import com.meitu.generator.data.local.entity.EverMemorySceneEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语义聚类器
 *
 * 将新的 MemCell 与现有 Scene 进行相似度匹配：
 * - 若与某个 Scene 的 centroid 相似度超过阈值，则归入该 Scene
 * - 否则创建新的 Scene
 *
 * 聚类后会自动更新 Scene 的 centroid（增量式质心计算）
 */
@Singleton
class SemanticClusterer @Inject constructor(
    private val embedder: SimpleEmbedder
) {

    companion object {
        /** 归入已有 Scene 的最低相似度阈值 */
        const val MERGE_THRESHOLD = 0.35f
        /** Scene 标题生成时的最大 episode 预览长度 */
        const val TITLE_MAX_LENGTH = 30
    }

    /**
     * 尝试将 MemCell 归入已有 Scene，或创建新 Scene
     *
     * @param memCell 待聚类的记忆单元
     * @param existingScenes 已有的所有 Scene 列表
     * @return 匹配到的 Scene（已更新 centroid），或新创建的 Scene
     */
    fun cluster(
        memCell: EverMemoryEntity,
        existingScenes: List<EverMemorySceneEntity>
    ): EverMemorySceneEntity {
        val cellEmbedding = memCell.embedding?.let { embedder.bytesToFloats(it) }
            ?: embedder.embedToFloats(memCell.episode)

        // 如果没有任何已有 Scene，直接创建新的
        if (existingScenes.isEmpty()) {
            return createNewScene(memCell, cellEmbedding)
        }

        // 计算与所有 Scene centroid 的相似度
        var bestMatch: Pair<EverMemorySceneEntity, Float>? = null

        for (scene in existingScenes) {
            val centroid = scene.centroidEmbedding?.let { embedder.bytesToFloats(it) }
                ?: continue
            val similarity = embedder.cosineSimilarity(cellEmbedding, centroid)

            if (bestMatch == null || similarity > bestMatch.second) {
                bestMatch = scene to similarity
            }
        }

        // 如果最佳匹配的相似度超过阈值，归入该 Scene
        if (bestMatch != null && bestMatch.second >= MERGE_THRESHOLD) {
            return updateSceneCentroid(bestMatch.first, memCell, cellEmbedding)
        }

        // 否则创建新 Scene
        return createNewScene(memCell, cellEmbedding)
    }

    /**
     * 创建新的 Scene
     */
    private fun createNewScene(
        memCell: EverMemoryEntity,
        cellEmbedding: FloatArray
    ): EverMemorySceneEntity {
        val title = generateTitle(memCell)
        return EverMemorySceneEntity(
            id = "scene_${System.currentTimeMillis()}_${memCell.id.takeLast(6)}",
            title = title,
            description = memCell.episode.take(200),
            centroidEmbedding = embedder.floatsToBytes(cellEmbedding),
            cellIds = "["${memCell.id}"]",
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * 更新 Scene 的 centroid（增量式质心）
     * 新 centroid = (旧 centroid × n + 新向量) / (n + 1)
     */
    private fun updateSceneCentroid(
        scene: EverMemorySceneEntity,
        memCell: EverMemoryEntity,
        cellEmbedding: FloatArray
    ): EverMemorySceneEntity {
        val oldCentroid = scene.centroidEmbedding?.let { embedder.bytesToFloats(it) }
        val cellCount = parseCellIds(scene.cellIds).size

        val newCentroid: FloatArray = if (oldCentroid != null && oldCentroid.size == cellEmbedding.size) {
            // 增量更新质心
            val n = cellCount.toFloat()
            FloatArray(cellEmbedding.size) { i ->
                (oldCentroid[i] * n + cellEmbedding[i]) / (n + 1)
            }
        } else {
            cellEmbedding.copyOf()
        }

        // L2 归一化新的 centroid
        val normalizedCentroid = l2Normalize(newCentroid)

        // 更新 cellIds
        val existingIds = parseCellIds(scene.cellIds).toMutableList()
        if (!existingIds.contains(memCell.id)) {
            existingIds.add(memCell.id)
        }
        val newCellIdsJson = existingIds.joinToString(",", prefix = "["", postfix = ""]")

        return scene.copy(
            centroidEmbedding = embedder.floatsToBytes(normalizedCentroid),
            cellIds = newCellIdsJson
        )
    }

    /**
     * 从 episode 生成 Scene 标题
     */
    private fun generateTitle(memCell: EverMemoryEntity): String {
        val episode = memCell.episode
        return if (episode.length <= TITLE_MAX_LENGTH) {
            episode
        } else {
            episode.take(TITLE_MAX_LENGTH) + "…"
        }
    }

    /**
     * 解析 cellIds JSON 数组
     */
    private fun parseCellIds(json: String): List<String> {
        // 简单解析：["id1","id2","id3"]
        return json.removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding(""") }
            .filter { it.isNotBlank() }
    }

    /**
     * L2 归一化
     */
    private fun l2Normalize(vector: FloatArray): FloatArray {
        var norm = 0f
        for (v in vector) norm += v * v
        norm = kotlin.math.sqrt(norm)
        return if (norm > 0f) vector.map { it / norm }.toFloatArray() else vector
    }

    /**
     * FloatArray 转 ByteArray（委托给 embedder）
     */
    private fun SimpleEmbedder.floatsToBytes(floats: FloatArray): ByteArray {
        val bytes = ByteArray(floats.size * 4)
        for (i in floats.indices) {
            val bits = floats[i].toBits()
            bytes[i * 4] = (bits shr 24).toByte()
            bytes[i * 4 + 1] = (bits shr 16).toByte()
            bytes[i * 4 + 2] = (bits shr 8).toByte()
            bytes[i * 4 + 3] = bits.toByte()
        }
        return bytes
    }
}
