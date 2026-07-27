package com.meitu.generator.data.agent

import com.meitu.generator.data.agent.AgentMemory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 隐式偏好学习器 v3 - 从用户实际行为中学习偏好
 * 
 * 学习维度：
 * 1. 意图类型偏好 - 用户最常做什么类型的操作
 * 2. 模型偏好 - 用户常用的 AI 模型
 * 3. 功能开关偏好 - 深度思考/联网搜索的使用频率
 * 4. 回复风格偏好 - 用户期望简洁还是详细回复
 * 5. 时段活跃偏好 - 用户最活跃的时段
 * 6. 代码风格偏好 - 编程语言、框架、代码组织方式
 * 7. 常用工具偏好 - 用户频繁使用的工具/命令
 * 
 * 阈值：连续2次相同行为即学习
 */
@Singleton
class PreferenceLearner @Inject constructor(
    private val agentMemory: AgentMemory
) {
    data class UserAction(
        val timestamp: Long = System.currentTimeMillis(),
        val intentType: String? = null,
        val modelUsed: String? = null,
        val deepThinkingOn: Boolean? = null,
        val webSearchOn: Boolean? = null,
        val hasImage: Boolean? = null,
        val responseLength: Int? = null,
        val hourOfDay: Int? = null,
        // v3 新增维度
        val codeLanguage: String? = null,
        val framework: String? = null,
        val codeStyle: String? = null,
        val toolsUsed: List<String>? = null,
        val userFeedback: String? = null
    )

    private val actionHistory = mutableListOf<UserAction>()
    private val MAX_HISTORY = 150

    fun recordAction(
        intentType: String? = null,
        modelUsed: String? = null,
        deepThinkingOn: Boolean? = null,
        webSearchOn: Boolean? = null,
        hasImage: Boolean? = null,
        responseLength: Int? = null,
        codeLanguage: String? = null,
        framework: String? = null,
        codeStyle: String? = null,
        toolsUsed: List<String>? = null,
        userFeedback: String? = null
    ) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        actionHistory.add(UserAction(
            timestamp = System.currentTimeMillis(),
            intentType = intentType,
            modelUsed = modelUsed,
            deepThinkingOn = deepThinkingOn,
            webSearchOn = webSearchOn,
            hasImage = hasImage,
            responseLength = responseLength,
            hourOfDay = hour,
            codeLanguage = codeLanguage,
            framework = framework,
            codeStyle = codeStyle,
            toolsUsed = toolsUsed,
            userFeedback = userFeedback
        ))
        if (actionHistory.size > MAX_HISTORY) {
            actionHistory.removeAt(0)
        }
    }

    fun recordQueryAction(query: String) {
        val intentType = when {
            query.contains("帮我做") || query.contains("做一个") || query.contains("开发") || query.contains("生成") -> "generate"
            query.contains("修改") || query.contains("改一下") || query.contains("更新") || query.contains("修复") -> "modify"
            query.contains("编译") || query.contains("构建") || query.contains("打包") -> "build"
            query.contains("状态") || query.contains("进度") -> "query"
            else -> "chat"
        }
        val codeLanguage = detectCodeLanguage(query)
        val framework = detectFramework(query)
        recordAction(intentType = intentType, codeLanguage = codeLanguage, framework = framework)
    }

    suspend fun analyzeAndLearn() {
        if (actionHistory.size < 2) return
        learnIntentPreference()
        learnModelPreference()
        learnFeatureTogglePreference()
        learnResponseLengthPreference()
        learnActiveHours()
        learnCodeStylePreference()
        learnToolPreference()
        learnFromFeedback()
    }

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

    private suspend fun learnModelPreference() {
        val models = actionHistory.mapNotNull { it.modelUsed }
        if (models.size < 2) return
        val counts = models.groupBy { it }.mapValues { it.value.size }
        val dominant = counts.maxByOrNull { it.value }
        if (dominant != null && dominant.value >= 2) {
            agentMemory.save("preferred_model", dominant.key, "preference")
        }
    }

    private suspend fun learnFeatureTogglePreference() {
        val deepThinkingStates = actionHistory.mapNotNull { it.deepThinkingOn }
        val webSearchStates = actionHistory.mapNotNull { it.webSearchOn }
        if (deepThinkingStates.size >= 2) {
            val onCount = deepThinkingStates.count { it }
            val ratio = onCount.toFloat() / deepThinkingStates.size
            if (ratio >= 0.7f) {
                agentMemory.save("prefer_deep_thinking", "true", "preference")
            } else if (ratio <= 0.3f) {
                agentMemory.save("prefer_deep_thinking", "false", "preference")
            }
        }
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

    private suspend fun learnResponseLengthPreference() {
        val lengths = actionHistory.mapNotNull { it.responseLength }
        if (lengths.size < 3) return
        val avgLength = lengths.average()
        val preference = when {
            avgLength < 200 -> "concise"
            avgLength > 800 -> "detailed"
            else -> "balanced"
        }
        agentMemory.save("preferred_response_style", preference, "preference")
    }

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

    private suspend fun learnCodeStylePreference() {
        val languages = actionHistory.mapNotNull { it.codeLanguage }
        if (languages.size >= 2) {
            val counts = languages.groupBy { it }.mapValues { it.value.size }
            val dominant = counts.maxByOrNull { it.value }
            if (dominant != null && dominant.value >= 2) {
                agentMemory.save("preferred_language", dominant.key, "preference")
            }
        }
        val frameworks = actionHistory.mapNotNull { it.framework }
        if (frameworks.size >= 2) {
            val counts = frameworks.groupBy { it }.mapValues { it.value.size }
            val dominant = counts.maxByOrNull { it.value }
            if (dominant != null && dominant.value >= 2) {
                agentMemory.save("preferred_framework", dominant.key, "preference")
            }
        }
        val styles = actionHistory.mapNotNull { it.codeStyle }
        if (styles.size >= 2) {
            val counts = styles.groupBy { it }.mapValues { it.value.size }
            val dominant = counts.maxByOrNull { it.value }
            if (dominant != null && dominant.value >= 2) {
                agentMemory.save("preferred_code_style", dominant.key, "preference")
            }
        }
    }

    private suspend fun learnToolPreference() {
        val allTools = actionHistory.flatMap { it.toolsUsed ?: emptyList() }
        if (allTools.size < 2) return
        val counts = allTools.groupBy { it }.mapValues { it.value.size }
        val topTools = counts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        if (topTools.isNotEmpty()) {
            agentMemory.save("preferred_tools", topTools.joinToString(","), "preference")
        }
    }

    private suspend fun learnFromFeedback() {
        val feedbacks = actionHistory.mapNotNull { it.userFeedback }
        if (feedbacks.size < 2) return
        val positiveRatio = feedbacks.count { it == "positive" }.toFloat() / feedbacks.size
        if (positiveRatio >= 0.7f) {
            agentMemory.save("current_approach_effective", "true", "preference")
        } else if (positiveRatio <= 0.3f) {
            agentMemory.save("current_approach_effective", "false", "preference")
        }
    }

    private fun detectCodeLanguage(query: String): String? {
        val lower = query.lowercase()
        return when {
            lower.contains("kotlin") || lower.contains("android") -> "kotlin"
            lower.contains("python") || lower.contains("py") -> "python"
            lower.contains("javascript") || lower.contains("js") || lower.contains("react") -> "javascript"
            lower.contains("typescript") || lower.contains("ts") -> "typescript"
            lower.contains("java") -> "java"
            lower.contains("go") || lower.contains("golang") -> "go"
            lower.contains("rust") -> "rust"
            lower.contains("c++") || lower.contains("cpp") -> "cpp"
            lower.contains("swift") -> "swift"
            else -> null
        }
    }

    private fun detectFramework(query: String): String? {
        val lower = query.lowercase()
        return when {
            lower.contains("android") -> "android"
            lower.contains("react") -> "react"
            lower.contains("vue") -> "vue"
            lower.contains("spring") -> "spring"
            lower.contains("django") -> "django"
            lower.contains("flask") -> "flask"
            lower.contains("flutter") -> "flutter"
            lower.contains("compose") -> "compose"
            else -> null
        }
    }

    suspend fun getLearnedPreferences(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        listOf(
            "preferred_intent",
            "preferred_model",
            "prefer_deep_thinking",
            "prefer_web_search",
            "preferred_response_style",
            "active_period",
            "preferred_language",
            "preferred_framework",
            "preferred_code_style",
            "preferred_tools",
            "current_approach_effective"
        ).forEach { key ->
            agentMemory.get(key)?.let { result[key] = it }
        }
        return result
    }
}
