package com.meitu.generator.data.agent

import com.meitu.generator.data.local.dao.MemoryDao
import com.meitu.generator.data.local.entity.MemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语义缓存 v2 - 基于关键词提取 + 意图分类的智能匹配
 * 
 * 改进：
 * 1. 中文分词优化 - 使用滑动窗口 bigram + 停用词过滤
 * 2. 关键词权重 - 动词/名词权重高于助词
 * 3. 意图分类匹配 - 先匹配意图类型，再匹配内容相似度
 * 4. 阈值降低 - 从85%降到60%，提高命中率
 * 5. 缓存淘汰策略 - 基于命中率+时效性
 */
@Singleton
class SemanticCache @Inject constructor(
    private val memoryDao: MemoryDao
) {
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.60  // 从0.85降到0.60
        private const val CATEGORY = "semantic_cache"
        private const val MAX_CACHE_SIZE = 100
    }

    // ============ 停用词表（中文高频无意义词） ============
    private val stopWords = setOf(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一",
        "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
        "没有", "看", "好", "自己", "这", "他", "她", "吗", "呢", "什么", "那",
        "怎么", "如何", "请问", "可以", "帮我", "帮", "给", "把", "用", "吗",
        "啊", "哦", "嗯", "吧", "呀", "啦", "嘛", "么"
    )

    // ============ 意图关键词 → 意图标签 ============
    private val intentKeywords = mapOf(
        "build" to listOf("编译", "构建", "打包", "apk", "build", "出包"),
        "generate" to listOf("生成", "创建", "写一个", "开发", "做一个", "做个", "设计"),
        "modify" to listOf("修改", "改", "更新", "修复", "bug", "调整", "优化", "升级"),
        "search" to listOf("搜索", "查", "找", "搜", "最新", "新闻", "价格"),
        "analyze" to listOf("分析", "解读", "看", "图片", "看看", "理解"),
        "chat" to listOf("你好", "hi", "hello", "谢谢", "帮助", "怎么用")
    )

    /**
     * 查找缓存
     */
    suspend fun lookup(input: String): String? {
        val cached = memoryDao.getByCategory(CATEGORY)
        if (cached.isEmpty()) return null

        val inputKeywords = extractKeywords(input)
        val inputIntent = classifyIntent(input)

        // 先在同意图类型中查找，再跨意图查找
        val candidates = cached.sortedByDescending { entry ->
            val cachedQuery = entry.key.removePrefix("cache_")
            val cachedIntent = extractStoredIntent(entry.value)
            val contentSim = weightedSimilarity(inputKeywords, extractKeywords(cachedQuery))
            val intentBonus = if (inputIntent == cachedIntent) 0.2f else 0f
            contentSim + intentBonus
        }

        for (entry in candidates) {
            val cachedQuery = entry.key.removePrefix("cache_")
            val cachedIntent = extractStoredIntent(entry.value)
            val contentSim = weightedSimilarity(inputKeywords, extractKeywords(cachedQuery))
            val intentBonus = if (inputIntent == cachedIntent) 0.2f else 0f
            val totalSim = contentSim + intentBonus

            if (totalSim >= SIMILARITY_THRESHOLD) {
                // 命中 - 更新计数
                val data = parseStoredValue(entry.value)
                val output = data["output"] ?: continue
                val hitCount = (data["hitCount"]?.toIntOrNull() ?: 0) + 1
                val toolUsed = data["tool"] ?: ""
                val intent = data["intent"] ?: ""

                memoryDao.update(entry.copy(
                    value = buildStoredValue(output, toolUsed, hitCount, intent),
                    updatedAt = System.currentTimeMillis()
                ))
                return output
            }
        }
        return null
    }

    /**
     * 存入缓存
     */
    suspend fun store(input: String, output: String, toolUsed: String = "") {
        val intent = classifyIntent(input)
        val key = "cache_${input.take(200)}"
        val value = buildStoredValue(output, toolUsed, 0, intent)

        // 检查是否已存在
        val existing = memoryDao.getByCategory(CATEGORY).find { it.key == key }
        if (existing != null) {
            memoryDao.update(existing.copy(value = value, updatedAt = System.currentTimeMillis()))
        } else {
            // 缓存数量限制
            val allCached = memoryDao.getByCategory(CATEGORY)
            if (allCached.size >= MAX_CACHE_SIZE) {
                // 清理最低命中率的缓存
                val sorted = allCached.sortedBy { entry ->
                    val data = parseStoredValue(entry.value)
                    data["hitCount"]?.toIntOrNull() ?: 0
                }
                sorted.take(10).forEach { memoryDao.delete(it) }
            }
            memoryDao.insert(MemoryEntity(
                key = key,
                value = value,
                category = CATEGORY
            ))
        }
    }

    /**
     * 清理低命中率缓存
     */
    suspend fun cleanup() {
        val cached = memoryDao.getByCategory(CATEGORY)
        val now = System.currentTimeMillis()
        for (entry in cached) {
            val data = parseStoredValue(entry.value)
            val hitCount = data["hitCount"]?.toIntOrNull() ?: 0
            val age = now - entry.createdAt
            // 超过7天且命中<2 → 清理
            if (age > 7 * 24 * 3600 * 1000L && hitCount < 2) {
                memoryDao.delete(entry)
            }
        }
    }

    // ============ 关键词提取 ============

    /**
     * 提取关键词 - 中文使用 bigram + 停用词过滤，英文按空格分词
     */
    private fun extractKeywords(text: String): Map<String, Float> {
        val keywords = mutableMapOf<String, Float>()
        val lower = text.lowercase().trim()

        // 英文词提取
        val englishWords = lower.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }
        englishWords.forEach { word ->
            if (word !in stopWords) {
                keywords[word] = (keywords[word] ?: 0f) + 1.0f
            }
        }

        // 中文 bigram 提取
        val chineseChars = lower.filter { it.code > 127 }
        if (chineseChars.length >= 2) {
            for (i in 0 until chineseChars.length - 1) {
                val bigram = chineseChars.substring(i, i + 2)
                if (bigram !in stopWords && bigram.any { it.code > 127 }) {
                    keywords[bigram] = (keywords[bigram] ?: 0f) + 1.0f
                }
            }
            // 单字（仅保留有意义的）
            val meaningfulChars = setOf("编", "译", "修", "改", "生", "成", "创", "建", "搜", "索", "图", "片", "分", "析", "代", "码", "功", "能", "接", "口", "变", "量", "函", "数", "类", "方", "法")
            chineseChars.forEach { c ->
                if (c.toString() in meaningfulChars) {
                    keywords[c.toString()] = (keywords[c.toString()] ?: 0f) + 0.5f
                }
            }
        }

        // 意图关键词加权
        for ((_, kws) in intentKeywords) {
            for (kw in kws) {
                if (lower.contains(kw.lowercase())) {
                    keywords["intent:$kw"] = 2.0f  // 意图关键词权重翻倍
                }
            }
        }

        return keywords
    }

    // ============ 意图分类 ============

    /**
     * 基于关键词匹配分类意图
     */
    private fun classifyIntent(text: String): String {
        val lower = text.lowercase()
        var bestIntent = "chat"
        var bestScore = 0

        for ((intent, keywords) in intentKeywords) {
            val score = keywords.count { lower.contains(it.lowercase()) }
            if (score > bestScore) {
                bestScore = score
                bestIntent = intent
            }
        }
        return bestIntent
    }

    // ============ 相似度计算 ============

    /**
     * 加权余弦相似度
     */
    private fun weightedSimilarity(a: Map<String, Float>, b: Map<String, Float>): Float {
        if (a.isEmpty() && b.isEmpty()) return 1.0f
        if (a.isEmpty() || b.isEmpty()) return 0.0f

        val allKeys = a.keys + b.keys
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (key in allKeys) {
            val va = a[key] ?: 0f
            val vb = b[key] ?: 0f
            dotProduct += va * vb
            normA += va * va
            normB += vb * vb
        }

        val denominator = Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())
        if (denominator == 0.0) return 0f
        return (dotProduct / denominator).toFloat()
    }

    // ============ 存储格式 ============
    // 格式: output|||tool|||hitCount|||intent
    // 用 ||| 分隔避免与内容冲突

    private fun buildStoredValue(output: String, tool: String, hitCount: Int, intent: String): String {
        return "$output|||$tool|||$hitCount|||$intent"
    }

    private fun parseStoredValue(value: String): Map<String, String> {
        val parts = value.split("|||")
        return mapOf(
            "output" to (parts.getOrNull(0) ?: ""),
            "tool" to (parts.getOrNull(1) ?: ""),
            "hitCount" to (parts.getOrNull(2) ?: "0"),
            "intent" to (parts.getOrNull(3) ?: "chat")
        )
    }

    private fun extractStoredIntent(value: String): String {
        return parseStoredValue(value)["intent"] ?: "chat"
    }
}
