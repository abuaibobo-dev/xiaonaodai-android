package com.meitu.generator.data.agent

/**
 * 智能模型路由器 v3.0
 * 降级链：OpenRouter Nemotron → OpenRouter Gemma → SambaNova Llama 3.3
 * 仅保留已验证可用的免费模型
 */
object ModelRouter {

    /** ModelRouter 降级链顺序（仅可用模型） */
    val FALLBACK_CHAIN = listOf(
        "deepseek-chat",
        "nvidia/nemotron-3-super-120b-a12b:free",
        "google/gemma-3-27b-it:free",
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

        // 2. 代码相关任务 → DeepSeek（代码能力强）
        if (isCodeTask(lowerQuery)) {
            return "deepseek-chat"
        }

        // 3. 长文本处理（>2000字） → DeepSeek（大上下文）
        if (query.length > 2000) {
            return "deepseek-chat"
        }

        // 4. 深度推理/数学/逻辑分析 → DeepSeek
        if (isReasoningTask(lowerQuery) || deepThinking) {
            return "deepseek-chat"
        }

        // 5. 快速简单问答 → SambaNova Llama 3.3（极速响应，省额度）
        if (isSimpleQuery(lowerQuery)) {
            return "Meta-Llama-3.3-70B-Instruct"
        }

        // 6. 默认 → DeepSeek（主力代码模型）
        return "deepseek-chat"
    }

    private fun isCodeTask(query: String): Boolean {
        val keywords = listOf(
            "代码", "编程", "函数", "变量", "类", "接口", "bug", "debug",
            "compile", "编译", "部署", "重构", "api", "函数", "方法",
            "代码审查", "code review", "写代码", "开发", "修复", "实现"
        )
        return keywords.any { query.contains(it) }
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
            "deepseek-chat" -> "DeepSeek V3"
            "nvidia/nemotron-3-super-120b-a12b:free" -> "Nemotron 120B"
            "google/gemma-3-27b-it:free" -> "Gemma 3 27B"
            "openai/gpt-oss-20b:free" -> "GPT-OSS 20B"
            "inclusionai/ling-3.0-flash:free" -> "Ling 3.0 Flash"
            "nvidia/nemotron-3-nano-30b-a3b:free" -> "Nemotron Nano 30B"
            "nvidia/nemotron-nano-12b-v2-vl:free" -> "Nemotron VL 12B (视觉)"
            "Meta-Llama-3.3-70B-Instruct" -> "Llama 3.3 70B"
            "DeepSeek-V3.1" -> "DeepSeek V3.1"
            "gemma-4-31B-it" -> "Gemma 4 31B"
            "gpt-oss-120b" -> "GPT-OSS 120B"
            else -> modelId
        }
    }

    fun getSelectionReason(query: String, hasImage: Boolean, deepThinking: Boolean): String {
        val lowerQuery = query.lowercase()
        return when {
            hasImage -> "📷 图片分析 → 视觉模型"
            isCodeTask(lowerQuery) -> "💻 代码任务 → DeepSeek"
            query.length > 2000 -> "📚 长文本 → DeepSeek"
            isReasoningTask(lowerQuery) || deepThinking -> "🧠 深度推理 → DeepSeek"
            isSimpleQuery(lowerQuery) -> "⚡ 快问快答 → Llama 3.3"
            else -> "✨ DeepSeek 处理"
        }
    }
}
