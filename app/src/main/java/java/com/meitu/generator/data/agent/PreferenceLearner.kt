package com.meitu.generator.data.agent

import com.meitu.generator.data.agent.AgentMemory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 隐式偏好学习器 v2 - 从用户实际行为中学习偏好
 * 
 * 学习维度：
 * 1. 意图类型偏好 - 用户最常做什么类型的操作
 * 2. 模型偏好 - 用户常用的 AI 模型
 * 3. 功能开关偏好 - 深度思考/联网搜索的使用频率
 * 4. 对话长度偏好 - 用户期望简洁还是详细回复
 * 5. 时段活跃偏好 - 用户最活跃的时段
 * 
 * 阈值：连续2次相同行为即学习（原来是3次太慢）
 */
@Singleton
class PreferenceLearner @Inject constructor(
    private val agentMemory: AgentMemory
) {
    // ============ 行为记录 ============
    data class UserAction(
        val timestamp: Long = System.currentTimeMillis(),
        val intentType: String? = null,       // chat, generate, modify, build, query
        val modelUsed: String? = null,         // deepseek-v4-flash, deepseek-r1, etc.
        val deepThinkingOn: Boolean? = null,   // 深度思考是否开启
        val webSearchOn: Boolean? = null,      // 联网搜索是否开启
        val hasImage: Boolean? = null,         // 是否发送了图片
        val responseLength: Int? = null,       // AI回复长度（用于学习用户偏好）
        val hourOfDay: Int? = null             // 活跃时段
    )

    private val actionHistory = mutableListOf<UserAction>()
    private val MAX_HISTORY = 100

    /**
     * 记录一次用户交互行为
     */
    fun recordAction(
        intentType: String? = null,
        modelUsed: String? = null,
        deepThinkingOn: Boolean? = null,
        webSearchOn: Boolean? = null,
        hasImage: Boolean? = null,
        responseLength: Int? = null
    ) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        actionHistory.add(UserAction(
            intentType = intentType,
            modelUsed = modelUsed,
            deepThinkingOn = deepThinkingOn,
            webSearchOn = webSearchOn,
            hasImage = hasImage,
            responseLength = responseLength,
            hourOfDay = hour
        ))
        if (actionHistory.size > MAX_HISTORY) {
            actionHistory.removeAt(0)
        }
    }

    /**
     * 兼容旧接口 - 从查询文本推断意图并记录
     */
    fun recordAction(query: String) {
        val intentType = when {
            query.contains("帮我做") || query.contains("做一个") || query.contains("开发") || query.contains("生成") -> "generate"
            query.contains("修改") || query.contains("改一下") || query.contains("更新") || query.contains("修复") -> "modify"
            query.contains("编译") || query.contains("构建") || query.contains("打包") -> "build"
            query.contains("状态") || query.contains("进度") -> "query"
            else -> "chat"
        }
        recordAction(intentType = intentType)
    }

    /**
     * 分析并学习偏好（每次对话结束后异步调用）
     */
    suspend fun analyzeAndLearn() {
        if (actionHistory.size < 2) return

        learnIntentPreference()
        learnModelPreference()
        learnFeatureTogglePreference()
        learnResponseLengthPreference()
        learnActiveHours()
    }

    /**
     * 学习意图类型偏好
     */
    private suspend fun learnIntentPreference() {
        val intents = actionHistory.mapNotNull { it.intentType }
        if (intents.size < 2) return

        val counts = intents.groupBy { it }.mapValues { it.value.size }
        val dominant = counts.maxByOrNull { it.value }
        if (dominant != null && dominant.value >= 2) {
            val ratio = dominant.value.toFloat() / intents.size
            if (ratio >= 0.5f) {
                agentMemory.save("preferred_intent", dominant.key, "preference")
            }
        }
    }

    /**
     * 学习模型偏好
     */
    private suspend fun learnModelPreference() {
        val models = actionHistory.mapNotNull { it.modelUsed }
        if (models.size < 2) return

        val counts = models.groupBy { it }.mapValues { it.value.size }
        val dominant = counts.maxByOrNull { it.value }
        if (dominant != null && dominant.value >= 2) {
            agentMemory.save("preferred_model", dominant.key, "preference")
        }
    }

    /**
     * 学习功能开关偏好（深度思考/联网搜索）
     */
    private suspend fun learnFeatureTogglePreference() {
        val deepThinkingStates = actionHistory.mapNotNull { it.deepThinkingOn }
        val webSearchStates = actionHistory.mapNotNull { it.webSearchOn }

        // 深度思考偏好
        if (deepThinkingStates.size >= 2) {
            val onCount = deepThinkingStates.count { it }
            val ratio = onCount.toFloat() / deepThinkingStates.size
            if (ratio >= 0.7f) {
                agentMemory.save("prefer_deep_thinking", "true", "preference")
            } else if (ratio <= 0.3f) {
                agentMemory.save("prefer_deep_thinking", "false", "preference")
            }
        }

        // 联网搜索偏好
        if (webSearchStates.size >= 2) {
            val onCount = webSearchStates.count { it }
            val ratio = onCount.toFloat() / webSearchStates.size
            if (ratio >= 0.7f) {
                agentMemory.save("prefer_web_search", "true", "preference")
            } else if (ratio <= 0.3f) {
                agentMemory.save("prefer_web_search", "false", "preference")
            }
        }
    }

    /**
     * 学习回复长度偏好
     */
    private suspend fun learnResponseLengthPreference() {
        val lengths = actionHistory.mapNotNull { it.responseLength }
        if (lengths.size < 3) return

        val avgLength = lengths.average()
        val preference = when {
            avgLength < 200 -> "concise"      // 用户偏好简洁回复
            avgLength > 800 -> "detailed"      // 用户偏好详细回复
            else -> "balanced"                 // 平衡
        }
        agentMemory.save("preferred_response_style", preference, "preference")
    }

    /**
     * 学习活跃时段
     */
    private suspend fun learnActiveHours() {
        val hours = actionHistory.mapNotNull { it.hourOfDay }
        if (hours.size < 3) return

        val counts = hours.groupBy { it }.mapValues { it.value.size }
        val peakHour = counts.maxByOrNull { it.value }?.key
        if (peakHour != null) {
            val period = when (peakHour) {
                in 6..11 -> "morning"
                in 12..17 -> "afternoon"
                in 18..22 -> "evening"
                else -> "night"
            }
            agentMemory.save("active_period", period, "preference")
        }
    }

    /**
     * 获取当前学习到的偏好摘要（供 AgentEngine 注入 system prompt）
     */
    suspend fun getLearnedPreferences(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        listOf(
            "preferred_intent",
            "preferred_model",
            "prefer_deep_thinking",
            "prefer_web_search",
            "preferred_response_style",
            "active_period"
        ).forEach { key ->
            agentMemory.get(key)?.let { result[key] = it }
        }
        return result
    }
}
