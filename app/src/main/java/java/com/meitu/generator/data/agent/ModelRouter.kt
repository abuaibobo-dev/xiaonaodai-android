package com.meitu.generator.data.agent

/**
 * 智能模型路由器 v2.0
 * 降级链：Agnes-2.5-Flash → Hy3(OpenRouter) → Gemini 2.5 Flash → Groq Llama 4 Scout → DeepSeek V4 Flash（最终备用）
 */
object ModelRouter {

    /** ModelRouter 降级链顺序 */
    val FALLBACK_CHAIN = listOf(
        "agnes-2.5-flash",
        "tencent/hy3:free",
        "gemini-2.5-flash",
        "llama-4-scout",
        "deepseek-v4-flash"
    )

    /**
     * 根据查询内容智能选择模型
     * @param query 用户输入
     * @param hasImage 是否包含图片
     * @param deepThinking 是否开启深度思考
     * @return 推荐的模型ID
     */
    fun selectModel(query: String, hasImage: Boolean = false, deepThinking: Boolean = false): String {
        val lowerQuery = query.lowercase()

        // 1. 图片分析任务 → Gemini（多模态最强）
        if (hasImage) {
            return "gemini-2.5-flash"
        }

        // 2. 长文本处理（>2000字） → Hy3 (OpenRouter) 长上下文
        if (query.length > 2000) {
            return "tencent/hy3:free"
        }

        // 3. 深度推理/数学/逻辑分析 → Hy3 (OpenRouter)
        if (isReasoningTask(lowerQuery) || deepThinking) {
            return "tencent/hy3:free"
        }

        // 4. 快速简单问答 → Groq Llama 4 Scout（极速响应）
        if (isSimpleQuery(lowerQuery)) {
            return "llama-4-scout"
        }

        // 5. 默认 → Agnes-2.5-Flash（主力，永久免费）
        return "agnes-2.5-flash"
    }

    /**
     * 判断是否为推理任务
     */
    private fun isReasoningTask(query: String): Boolean {
        val keywords = listOf(
            "推理", "分析", "证明", "逻辑", "为什么", "原因", "解释",
            "数学", "计算", "方程", "公式", "定理", "证明",
            "深度", "详细", "全面", "系统", "深入",
            "思考", "think", "reason", "analyze", "prove"
        )
        return keywords.any { query.contains(it) }
    }

    /**
     * 判断是否为简单查询
     */
    private fun isSimpleQuery(query: String): Boolean {
        return query.length < 50 && (
            query.contains("什么") ||
            query.contains("是谁") ||
            query.contains("多少") ||
            query.contains("怎么") ||
            query.startsWith("你好") ||
            query.startsWith("hi") ||
            query.startsWith("hello")
        )
    }

    /**
     * 获取模型显示名称
     */
    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "auto" -> "自动模式"
            "agnes-2.5-flash" -> "Agnes 2.5 Flash"
            "tencent/hy3:free" -> "腾讯混元 Hy3"
            "baidu/cobuddy:free" -> "百度 Cobuddy"
            "nvidia/nemotron-3-ultra:free" -> "NVIDIA Nemotron 3 Ultra"
            "gemini-3.5-flash" -> "Gemini 3.5 Flash"
            "gemini-3.1-flash-lite" -> "Gemini 3.1 Flash-Lite"
            "gemini-2.5-flash" -> "Gemini 2.5 Flash"
            "gemini-2.5-flash-lite" -> "Gemini 2.5 Flash-Lite"
            "openai/gpt-oss-120b" -> "GPT-OSS 120B"
            "openai/gpt-oss-20b" -> "GPT-OSS 20B"
            "deepseek-r1-distill-llama-70b" -> "DeepSeek R1 Distill"
            "moonshotai/kimi-k2-instruct" -> "Kimi K2"
            "llama-4-scout" -> "Llama 4 Scout"
            "Meta-Llama-3.3-70B-Instruct" -> "Llama 3.3 70B (SN)"
            "gpt-oss-120b" -> "GPT-OSS 120B (SN)"
            "DeepSeek-V3.1" -> "DeepSeek V3.1"
            "gemma-4-31B-it" -> "Gemma 4 31B"
            "meta-llama/Llama-3.3-70B-Instruct" -> "Llama 3.3 70B (HF)"
            "deepseek-v4-flash" -> "DeepSeek V4 Flash"
            "deepseek-v4-pro" -> "DeepSeek V4 Pro"
            else -> modelId
        }
    }

    /**
     * 获取模型选择原因（用于状态栏显示）
     */
    fun getSelectionReason(query: String, hasImage: Boolean, deepThinking: Boolean): String {
        val lowerQuery = query.lowercase()

        return when {
            hasImage -> "📷 图片分析 → Gemini 2.5 Flash"
            query.length > 2000 -> "📚 长文本 → 混元 Hy3"
            isReasoningTask(lowerQuery) || deepThinking -> "🧠 深度推理 → 混元 Hy3"
            isSimpleQuery(lowerQuery) -> "⚡ 快问快答 → Llama 4 Scout"
            else -> "✨ Agnes 2.5 Flash 处理"
        }
    }
}
