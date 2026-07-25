package com.meitu.generator.data.agent

/**
 * Orchestrator 意图路由器
 * 分析用户输入，判断意图类型：CHAT / TASK_GENERATE / TASK_MODIFY / TASK_BUILD / QUERY
 */
object IntentRouter {
    enum class IntentType {
        CHAT,           // 纯聊天/问答
        TASK_GENERATE,  // 生成新项目
        TASK_MODIFY,    // 修改已有项目
        TASK_BUILD,     // 编译/构建
        QUERY           // 查询状态
    }

    data class IntentResult(
        val type: IntentType,
        val confidence: Float = 0.8f,
        val projectName: String? = null,
        val description: String? = null
    )

    fun classify(query: String): IntentResult {
        val lower = query.lowercase()
        return when {
            matchesAny(lower, listOf("帮我做", "做一个", "创建一个", "写一个", "生成一个", "开发一个", "做一个")) ->
                IntentResult(IntentType.TASK_GENERATE, description = query)
            matchesAny(lower, listOf("修改", "改一下", "更新", "给.*加个功能", "修复", "bug")) ->
                IntentResult(IntentType.TASK_MODIFY, description = query)
            matchesAny(lower, listOf("编译", "构建", "打包", "生成apk", "build")) ->
                IntentResult(IntentType.TASK_BUILD)
            matchesAny(lower, listOf("状态", "进度", "下载", "查看编译")) ->
                IntentResult(IntentType.QUERY)
            else -> IntentResult(IntentType.CHAT)
        }
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { 
            if (it.contains(".*")) {
                Regex(it).containsMatchIn(text)
            } else {
                text.contains(it)
            }
        }
    }
}
