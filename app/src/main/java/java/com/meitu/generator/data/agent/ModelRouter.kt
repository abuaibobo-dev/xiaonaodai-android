package com.meitu.generator.data.agent

/**
 * 智能模型路由器 - 根据用户意图自动选择最合适的免费模型
 */
object ModelRouter {
    
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
        
        // 2. 深度推理/数学/逻辑分析 → DeepSeek R1 或 Groq DeepSeek-R1-Distill
        if (isReasoningTask(lowerQuery) || deepThinking) {
            return selectReasoningModel()
        }
        
        // 3. 写代码/编程任务 → 代码能力强的模型
        if (isCodingTask(lowerQuery)) {
            return selectCodingModel()
        }
        
        // 4. 中文写作/翻译/文案 → 中文优化模型
        if (isChineseTask(lowerQuery)) {
            return selectChineseModel()
        }
        
        // 5. 长文本处理（>2000字） → 大上下文窗口模型
        if (query.length > 2000) {
            return "gemini-2.5-flash" // 1M上下文
        }
        
        // 6. 快速简单问答 → 最快的模型
        if (isSimpleQuery(lowerQuery)) {
            return "llama-3.1-8b-instant" // Groq最快
        }
        
        // 7. 复杂任务/需要大模型 → SambaNova 405B
        if (isComplexTask(lowerQuery)) {
            return "Meta-Llama-3.1-405B-Instruct"
        }
        
        // 8. 默认 → DeepSeek V4 Flash（平衡）
        return "deepseek-v4-flash"
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
     * 选择推理模型
     */
    private fun selectReasoningModel(): String {
        // 优先使用 Groq DeepSeek-R1-Distill（推理专用，速度快）
        return "deepseek-r1-distill-70b"
    }
    
    /**
     * 判断是否为编程任务
     */
    private fun isCodingTask(query: String): Boolean {
        val keywords = listOf(
            "代码", "编程", "写个", "实现", "函数", "类", "方法",
            "app", "application", "程序", "软件", "开发",
            "kotlin", "java", "python", "javascript", "typescript",
            "android", "ios", "web", "api", "数据库", "sql",
            "bug", "错误", "修复", "调试", "debug",
            "code", "program", "function", "class", "method"
        )
        return keywords.any { query.contains(it) }
    }
    
    /**
     * 选择编程模型
     */
    private fun selectCodingModel(): String {
        // DeepSeek V3 在代码方面表现优秀
        return "DeepSeek-V3-0324"
    }
    
    /**
     * 判断是否为中文任务
     */
    private fun isChineseTask(query: String): Boolean {
        val keywords = listOf(
            "翻译", "作文", "文章", "文案", "诗歌", "小说",
            "故事", "剧本", "歌词", "成语", "古文", "文言文",
            "中文", "汉语", "chinese", "translate", "write"
        )
        return keywords.any { query.contains(it) }
    }
    
    /**
     * 选择中文优化模型
     */
    private fun selectChineseModel(): String {
        // Qwen3-32B 中文优化最好
        return "qwen3-32b"
    }
    
    /**
     * 判断是否为简单查询
     */
    private fun isSimpleQuery(query: String): Boolean {
        // 短文本 + 简单问题
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
     * 判断是否为复杂任务
     */
    private fun isComplexTask(query: String): Boolean {
        val keywords = listOf(
            "详细", "完整", "全面", "系统", "深入", "专业",
            "研究报告", "分析报告", "对比分析", "综合分析",
            "总结", "归纳", "概述", "综述"
        )
        return keywords.any { query.contains(it) } || query.length > 500
    }
    
    /**
     * 获取模型显示名称
     */
    fun getModelDisplayName(modelId: String): String {
        return when (modelId) {
            "deepseek-v4-flash" -> "DeepSeek V4 Flash"
            "deepseek-v4-pro" -> "DeepSeek V4 Pro"
            "deepseek-r1" -> "DeepSeek R1"
            "DeepSeek-V3-0324" -> "DeepSeek V3"
            "deepseek-r1-distill-70b" -> "DeepSeek R1 Distill"
            "gemini-2.0-flash" -> "Gemini 2.0 Flash"
            "gemini-2.5-flash" -> "Gemini 2.5 Flash"
            "gemini-2.5-flash-lite" -> "Gemini 2.5 Flash-Lite"
            "gemini-3-flash" -> "Gemini 3 Flash"
            "gemini-3.1-flash-lite" -> "Gemini 3.1 Flash-Lite"
            "llama-3.3-70b-versatile" -> "Llama 3.3 70B"
            "llama-3.1-8b-instant" -> "Llama 3.1 8B (最快)"
            "llama-4-scout-17b-16e-instruct" -> "Llama 4 Scout"
            "qwen3-32b" -> "Qwen3 32B (中文优化)"
            "Meta-Llama-3.1-405B-Instruct" -> "Llama 3.1 405B (最大)"
            "Meta-Llama-3.1-70B-Instruct" -> "Llama 3.1 70B"
            "Meta-Llama-3.1-8B-Instruct" -> "Llama 3.1 8B"
            "Qwen2.5-72B-Instruct" -> "Qwen2.5 72B"
            "Qwen2.5-Coder-32B-Instruct" -> "Qwen2.5 Coder"
            "DeepSeek-R1" -> "DeepSeek R1"
            "meta-llama/Llama-3.3-70B-Instruct" -> "Llama 3.3 70B (HF)"
            else -> modelId
        }
    }
    
    /**
     * 获取模型选择原因（用于调试和显示）
     */
    fun getSelectionReason(query: String, hasImage: Boolean, deepThinking: Boolean): String {
        val lowerQuery = query.lowercase()
        
        return when {
            hasImage -> "📷 图片分析任务 → Gemini 2.5 Flash（多模态最强）"
            isReasoningTask(lowerQuery) || deepThinking -> "🧠 推理任务 → DeepSeek R1 Distill（推理专用）"
            isCodingTask(lowerQuery) -> "💻 编程任务 → DeepSeek V3（代码能力强）"
            isChineseTask(lowerQuery) -> "📝 中文任务 → Qwen3 32B（中文优化）"
            query.length > 2000 -> "📚 长文本处理 → Gemini 2.5 Flash（1M上下文）"
            isSimpleQuery(lowerQuery) -> "⚡ 简单问答 → Llama 3.1 8B（最快）"
            isComplexTask(lowerQuery) -> "🔬 复杂任务 → Llama 3.1 405B（最大模型）"
            else -> "✨ 通用任务 → DeepSeek V4 Flash（平衡）"
        }
    }
}
