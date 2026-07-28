package com.meitu.generator.data.agent

/**
 * 智能模型路由器 v4.0
 * 主力：DeepSeek Chat
 * 降级链：DeepSeek Chat → OpenRouter Nemotron → SambaNova Llama 3.3
 */
object ModelRouter {

    /** ModelRouter 降级链顺序 */
    val FALLBACK_CHAIN = listOf(
        "deepseek-chat",
        "nvidia/nemotron-3-super-120b-a12b:free",
        "Meta-Llama-3.3-70B-Instruct"
    )

    /**
     * 根据查询内容智能选择模型
     */
    fun selectModel(query: String, hasImage: Boolean = false, deepThinking: Boolean = false): String {
        val lowerQuery = query.lowercase()

        // 1. 图片分析任务 → OpenRouter 视觉模型
        if (hasImage) {
            return "nvidia/nemotron-nano-12b-v2-vl:free"
        }

        // 2. 长文本处理（>2000字） → DeepSeek（大上下文）
        if (query.length > 2000) {
            return "deepseek-chat"
        }

        // 3. 深度推理/数学/逻辑分析 → DeepSeek Reasoner
        if (isReasoningTask(lowerQuery) || deepThinking) {
            return "deepseek-reasoner"
        }

        // 4. 快速简单问答 → DeepSeek Chat（响应快）
        if (isSimpleQuery(lowerQuery)) {
            return "deepseek-chat"
        }

        // 5. 默认 → DeepSeek Chat（主力）
        return "deepseek-chat"
    }

    private fun isReasoningTask(query: String): Boolean {
        val keywords = listOf(
            "推理", "分析", "证明", "逻辑", "为什么", "原因", "解释",
            "数学", "计算", "方程", "公式", "定理",
            "深度", "详细", "全面", "系统", "深入",
            "思考", "think", "reason", "analyze", "prove"
        )
        return keywords.any { query.contains(it) }
    }

    private fun isSimpleQuery(query: String): Boolean {
        return query.length < 50 && (
            query.contains("什么") || query.contains("是谁") ||
            query.contains("多少") || query.contains("怎么") ||
            query.startsWith("你好") || query.startsWith("hi") ||
            query.startsWith("hello")
        )
    }

    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "auto" -> "自动模式"
            "deepseek-chat" -> "DeepSeek Chat"
            "deepseek-reasoner" -> "DeepSeek Reasoner"
            "nvidia/nemotron-3-super-120b-a12b:free" -> "Nemotron 120B"
            "nvidia/nemotron-nano-12b-v2-vl:free" -> "Nemotron VL 12B (视觉)"
            "Meta-Llama-3.3-70B-Instruct" -> "Llama 3.3 70B"
            else -> modelId
        }
    }

    fun getSelectionReason(query: String, hasImage: Boolean, deepThinking: Boolean): String {
        val lowerQuery = query.lowercase()
        return when {
            hasImage -> "📷 图片分析 → 视觉模型"
            query.length > 2000 -> "📚 长文本 → DeepSeek Chat"
            isReasoningTask(lowerQuery) || deepThinking -> "🧠 深度推理 → DeepSeek Reasoner"
            isSimpleQuery(lowerQuery) -> "⚡ 快问快答 → DeepSeek Chat"
            else -> "✨ DeepSeek Chat 处理"
        }
    }
}
