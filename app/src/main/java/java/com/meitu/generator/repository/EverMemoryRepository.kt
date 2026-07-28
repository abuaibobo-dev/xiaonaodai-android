package com.meitu.generator.repository

import com.meitu.generator.data.evermemory.BM25Retriever
import com.meitu.generator.data.evermemory.SimpleEmbedder
import com.meitu.generator.data.evermemory.SemanticClusterer
import com.meitu.generator.data.local.dao.EverMemoryDao
import com.meitu.generator.data.local.entity.EverMemoryEntity
import com.meitu.generator.data.local.entity.EverMemorySceneEntity
import com.meitu.generator.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EverOS 语义记忆 Repository
 *
 * 核心职责：
 * 1. 添加记忆单元 → 自动向量嵌入 + 语义聚类
 * 2. 混合检索 → BM25 关键词匹配 + 向量余弦相似度，RRF 融合排序
 * 3. 上下文记忆 → 根据当前对话上下文检索相关记忆，构建 prompt
 * 4. Profile 进化 → 从聚类结果中提炼稳定的用户画像
 */
@Singleton
class EverMemoryRepository @Inject constructor(
    private val dao: EverMemoryDao,
    private val embedder: SimpleEmbedder,
    private val bm25: BM25Retriever,
    private val clusterer: SemanticClusterer
) {

    companion object {
        /** BM25 与向量检索的 RRF 融合常数 */
        private const val RRF_K = 60
        /** 向量检索的最低相似度阈值 */
        private const val VECTOR_SIMILARITY_THRESHOLD = 0.2f
        /** Profile 进化时每个 cluster 至少需要的 cell 数量 */
        private const val MIN_CELLS_FOR_PROFILE = 3
        /** 最大记忆 token 预算（用于 getRelevantMemories） */
        private const val DEFAULT_MAX_TOKENS = 1000
    }

    // ============ 1. 添加记忆单元 ============

    /**
     * 添加一条记忆单元
     *
     * 流程：
     * 1. 生成向量嵌入
     * 2. 创建 MemCell 实体
     * 3. 语义聚类 → 归入已有 Scene 或创建新 Scene
     * 4. 持久化到数据库
     *
     * @param episode 简短叙事（第三人称）
     * @param facts 原子事实列表
     * @param foresight 前瞻性推断（可选）
     * @return 新创建的 MemCell ID
     */
    suspend fun addMemoryCell(
        episode: String,
        facts: List<String>,
        foresight: Map<String, Any>? = null
    ): String = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        val cellId = "mem_${now}_${episode.hashCode().toUInt()}"

        // 1. 生成向量嵌入
        val embedding = embedder.embed(episode)

        // 2. 构建 JSON 字段
        val factsJson = facts.joinToString(",", prefix = "[\"", postfix = "\"]") { it.replace("\"", "\\\"") }
        val foresightJson = if (foresight != null) {
            buildForesightJson(foresight)
        } else {
            "{}"
        }
        val metadataJson = buildString {
            append("{")
            append("\"timestamp\":$now,")
            append("\"source\":\"agent_interaction\",")
            append("\"fact_count\":${facts.size}")
            append("}")
        }

        // 3. 语义聚类
        val existingScenes = dao.getAllScenes()
        val cell = EverMemoryEntity(
            id = cellId,
            episode = episode,
            facts = factsJson,
            foresight = foresightJson,
            metadata = metadataJson,
            embedding = embedding,
            sceneId = null, // 聚类后更新
            createdAt = now,
            updatedAt = now
        )

        val matchedScene = clusterer.cluster(cell, existingScenes)

        // 4. 持久化
        val cellWithScene = cell.copy(sceneId = matchedScene.id, updatedAt = System.currentTimeMillis())
        dao.insertMemoryCell(cellWithScene)
        dao.insertScene(matchedScene)

        cellId
    }

    // ============ 2. 混合检索 ============

    /**
     * 混合检索：BM25 + 向量余弦相似度，RRF 融合排序
     *
     * @param query 查询文本
     * @param topK 返回前 K 个结果
     * @return 按相关性降序排列的记忆单元列表
     */
    suspend fun search(query: String, topK: Int = 5): List<EverMemoryEntity> =
        withContext(Dispatchers.Default) {
            val allCells = dao.getAllCells()
            if (allCells.isEmpty()) return@withContext emptyList()

            // --- BM25 关键词检索 ---
            val documents = allCells.map { it.episode + " " + it.facts }
            val bm25Results = bm25.search(query, documents, topK = topK * 2)

            // 构建 BM25 得分映射：cellId -> rank（1-based）
            val bm25RankMap = mutableMapOf<String, Int>()
            bm25Results.forEachIndexed { index, (doc, _) ->
                val cellIndex = documents.indexOf(doc)
                if (cellIndex >= 0) {
                    bm25RankMap[allCells[cellIndex].id] = index + 1
                }
            }

            // --- 向量余弦相似度检索 ---
            val queryVector = embedder.embedToFloats(query)
            val vectorResults = allCells.mapNotNull { cell ->
                val cellVector = cell.embedding?.let { embedder.bytesToFloats(it) }
                    ?: embedder.embedToFloats(cell.episode)
                val similarity = embedder.cosineSimilarity(queryVector, cellVector)
                if (similarity >= VECTOR_SIMILARITY_THRESHOLD) {
                    cell.id to similarity
                } else null
            }.sortedByDescending { it.second }
                .take(topK * 2)

            // --- RRF 融合排序 ---
            val rrfScores = mutableMapOf<String, Float>()

            // BM25 RRF 分数
            for ((cellId, rank) in bm25RankMap) {
                rrfScores[cellId] = (rrfScores[cellId] ?: 0f) + (1.0f / (RRF_K + rank))
            }

            // 向量 RRF 分数
            vectorResults.forEachIndexed { index, (cellId, _) ->
                val rank = index + 1
                rrfScores[cellId] = (rrfScores[cellId] ?: 0f) + (1.0f / (RRF_K + rank))
            }

            // 按 RRF 分数排序，取 topK
            val cellMap = allCells.associateBy { it.id }
            return@withContext rrfScores.entries
                .sortedByDescending { it.value }
                .take(topK)
                .mapNotNull { (cellId, _) -> cellMap[cellId] }
        }

    // ============ 3. 获取相关记忆（构建 prompt） ============

    /**
     * 根据当前上下文获取相关记忆，用于注入 prompt
     *
     * @param context 当前对话上下文/用户输入
     * @param maxTokens 最大 token 预算（近似估算：1 token ≈ 1.5 中文字符）
     * @return 相关记忆列表
     */
    suspend fun getRelevantMemories(
        context: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): List<EverMemoryEntity> = withContext(Dispatchers.Default) {
        val searchResults = search(context, topK = 10)
        if (searchResults.isEmpty()) return@withContext emptyList()

        // 按 token 预算截断
        val result = mutableListOf<EverMemoryEntity>()
        var usedTokens = 0
        val maxChars = (maxTokens * 1.5).toInt() // 粗略估算

        for (cell in searchResults) {
            val cellChars = cell.episode.length + cell.facts.length
            if (usedTokens + cellChars > maxChars && result.isNotEmpty()) break
            result.add(cell)
            usedTokens += cellChars
        }

        result
    }

    // ============ 4. Profile 进化 ============

    /**
     * 从聚类结果中提炼/更新用户画像
     *
     * 流程：
     * 1. 遍历所有 Scene（聚类）
     * 2. 对每个 Scene 中的 facts 进行统计，提取高频事实
     * 3. 更新 UserProfile 条目（带置信度）
     */
    suspend fun evolveProfile(): Unit = withContext(Dispatchers.Default) {
        val scenes = dao.getAllScenes()
        val allCells = dao.getAllCells()

        if (allCells.isEmpty()) return@withContext

        // 收集所有 facts
        val factCounter = mutableMapOf<String, Int>()
        val factToCell = mutableMapOf<String, EverMemoryEntity>()

        for (cell in allCells) {
            val facts = parseJsonArray(cell.facts)
            for (fact in facts) {
                val normalized = fact.trim().lowercase()
                factCounter[normalized] = (factCounter[normalized] ?: 0) + 1
                factToCell[normalized] = cell
            }
        }

        // 过滤出出现次数 >= MIN_CELLS_FOR_PROFILE 的事实
        val totalCells = allCells.size
        val profileEntries = factCounter.entries
            .filter { it.value >= MIN_CELLS_FOR_PROFILE }
            .map { (fact, count) ->
                val confidence = count.toFloat() / totalCells
                UserProfileEntity(
                    key = "profile_${fact.hashCode().toUInt()}",
                    value = fact,
                    confidence = confidence.coerceIn(0f, 1f),
                    validFrom = System.currentTimeMillis(),
                    validUntil = null,
                    updatedAt = System.currentTimeMillis()
                )
            }

        // 写入数据库
        if (profileEntries.isNotEmpty()) {
            dao.upsertProfiles(profileEntries)
        }
    }

    // ============ 查询辅助方法 ============

    /**
     * 获取所有记忆场景
     */
    suspend fun getAllScenes(): List<EverMemorySceneEntity> = dao.getAllScenes()

    /**
     * 获取所有用户画像
     */
    suspend fun getAllProfiles(): List<UserProfileEntity> = dao.getAllProfiles()

    /**
     * 获取指定场景下的记忆单元
     */
    suspend fun getCellsByScene(sceneId: String): List<EverMemoryEntity> =
        dao.getCellsByScene(sceneId)

    /**
     * 获取所有记忆单元
     */
    suspend fun getAllCells(): List<EverMemoryEntity> = dao.getAllCells()

    /**
     * 获取记忆单元总数
     */
    suspend fun getCellCount(): Int = dao.countCells()

    // ============ 内部工具方法 ============

    /**
     * 构建 foresight 的 JSON 字符串
     */
    private fun buildForesightJson(foresight: Map<String, Any>): String {
        val sb = StringBuilder("{")
        val entries = foresight.entries.toList()
        entries.forEachIndexed { index, (key, value) ->
            sb.append("\"$key\":")
            when (value) {
                is String -> sb.append("\"$value\"")
                is Number -> sb.append(value)
                is Boolean -> sb.append(value)
                else -> sb.append("\"$value\"")
            }
            if (index < entries.size - 1) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

    /**
     * 简单解析 JSON 数组字符串
     */
    private fun parseJsonArray(json: String): List<String> {
        return json.removePrefix("[").removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
            .filter { it.isNotBlank() }
    }
}
