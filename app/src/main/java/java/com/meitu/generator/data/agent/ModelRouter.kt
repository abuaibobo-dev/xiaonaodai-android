package com.meitu.generator.data.agent

import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能模型路由器 v5.0.2
 * 主力：DeepSeek Chat
 * 备选：Google Gemini 系列
 */
@Singleton
class ModelRouter @Inject constructor(
    private val settingsRepo: SettingsRepository
) {

    companion object {
        /** 模型名 → 显示名映射 */
        fun getModelDisplayName(modelId: String): String = when (modelId) {
            "auto" -> "自动模式"
            "deepseek-chat" -> "DeepSeek Chat"
            "deepseek-reasoner" -> "DeepSeek Reasoner"
            "gemini-2.0-flash" -> "Gemini 2.0 Flash"
            "gemini-2.0-flash-lite" -> "Gemini 2.0 Flash Lite"
            "gemini-1.5-flash" -> "Gemini 1.5 Flash"
            "gpt-4o" -> "GPT-4o"
            "gpt-4o-mini" -> "GPT-4o Mini"
            "llama-3.3-70b-versatile" -> "Llama 3.3 70B"
            "llama-3.1-8b-instant" -> "Llama 3.1 8B"
            "deepseek-ai/DeepSeek-V3" -> "DeepSeek V3 (硅基流动)"
            "Qwen/Qwen2.5-72B-Instruct" -> "Qwen 2.5 72B (硅基流动)"
            "moonshot-v1-8k" -> "Moonshot V1 8K"
            "moonshot-v1-32k" -> "Moonshot V1 32K"
            "glm-4-flash" -> "GLM-4 Flash"
            "glm-4" -> "GLM-4"
            else -> modelId.replace("-", " ").replace("_", " ")
                .replaceFirstChar { it.uppercase() }
        }

        /** 模型名 → 供应商名映射 */
        fun getModelSupplier(modelId: String): String = when (modelId) {
            "deepseek-chat", "deepseek-reasoner" -> "DeepSeek"
            "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-1.5-flash" -> "Google AI"
            "gpt-4o", "gpt-4o-mini" -> "OpenAI"
            "llama-3.3-70b-versatile", "llama-3.1-8b-instant" -> "Groq"
            "deepseek-ai/DeepSeek-V3", "Qwen/Qwen2.5-72B-Instruct" -> "硅基流动"
            "moonshot-v1-8k", "moonshot-v1-32k" -> "Moonshot"
            "glm-4-flash", "glm-4" -> "智谱AI"
            else -> "DeepSeek"
        }

        fun selectModel(query: String, hasImage: Boolean = false, deepThinking: Boolean = false): String {
            val lowerQuery = query.lowercase()

            if (hasImage) return "gemini-2.0-flash" // Gemini 支持多模态
            if (query.length > 2000) return "deepseek-chat"
            if (isReasoningTask(lowerQuery) || deepThinking) return "deepseek-reasoner"
            if (isSimpleQuery(lowerQuery)) return "deepseek-chat"
            return "deepseek-chat"
        }

        fun getSelectionReason(query: String, hasImage: Boolean, deepThinking: Boolean): String {
            val lowerQuery = query.lowercase()
            return when {
                hasImage -> "📷 图片分析 → Gemini Flash (多模态)"
                query.length > 2000 -> "📚 长文本 → DeepSeek Chat"
                isReasoningTask(lowerQuery) || deepThinking -> "🧠 深度推理 → DeepSeek Reasoner"
                isSimpleQuery(lowerQuery) -> "⚡ 快问快答 → DeepSeek Chat"
                else -> "✨ DeepSeek Chat 处理"
            }
        }

        private fun isReasoningTask(query: String): Boolean {
            val keywords = listOf("推理", "分析", "证明", "逻辑", "为什么", "原因", "解释", "数学", "计算", "方程", "公式", "定理", "深度", "详细", "全面", "系统", "深入", "思考", "think", "reason", "analyze", "prove")
            return keywords.any { query.contains(it) }
        }

        private fun isSimpleQuery(query: String): Boolean {
            return query.length < 50 && (query.contains("什么") || query.contains("是谁") || query.contains("多少") || query.contains("怎么") || query.startsWith("你好") || query.startsWith("hi") || query.startsWith("hello"))
        }
    }
}
