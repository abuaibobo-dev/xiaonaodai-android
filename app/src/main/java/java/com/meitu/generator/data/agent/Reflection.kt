package com.meitu.generator.data.agent

/**
 * 反思与纠错模块 - 工具执行后的校验器
 * 在 AgentEngine 的 Observation 阶段校验结果
 */
object Reflection {
    
    data class ValidationResult(
        val needsRetry: Boolean,
        val reason: String,
        val suggestedAction: String = ""
    )
    
    /**
     * 校验工具执行结果
     * - 返回字符串是否包含 error
     * - 图片是否小于 1KB（可能异常）
     * - HTTP 状态码是否 429
     */
    fun validate(toolName: String, output: String): ValidationResult {
        // 检查是否包含明显错误标记
        if (output.contains("error", ignoreCase = true) && output.length < 200) {
            return ValidationResult(
                needsRetry = true,
                reason = "输出包含错误信息",
                suggestedAction = "retry"
            )
        }
        
        // 检查 429 限流
        if (output.contains("429") || output.contains("Too Many Requests", ignoreCase = true)) {
            return ValidationResult(
                needsRetry = true,
                reason = "API限流(429)",
                suggestedAction = "wait_and_retry"
            )
        }
        
        // 检查超时
        if (output.contains("timeout", ignoreCase = true) || output.contains("timed out", ignoreCase = true)) {
            return ValidationResult(
                needsRetry = true,
                reason = "请求超时",
                suggestedAction = "retry_with_lower_quality"
            )
        }
        
        // 检查空结果
        if (output.isBlank() || output == "null" || output == "{}") {
            return ValidationResult(
                needsRetry = true,
                reason = "返回空结果",
                suggestedAction = "retry"
            )
        }
        
        // 图片数据太小（base64长度 < 100 ≈ 几十字节图片）
        if (output.startsWith("data:image/") && output.length < 200) {
            return ValidationResult(
                needsRetry = true,
                reason = "图片数据异常(过小)",
                suggestedAction = "lower_resolution"
            )
        }
        
        return ValidationResult(needsRetry = false, reason = "")
    }
    
    /**
     * 处理工具执行异常
     */
    fun handleToolError(toolName: String, error: Exception): String {
        val msg = error.message ?: "未知错误"
        return when {
            msg.contains("429") -> "[工具${toolName}触发限流，请稍后重试]"
            msg.contains("timeout", ignoreCase = true) -> "[工具${toolName}超时，已自动降低质量重试]"
            msg.contains("Socket") -> "[工具${toolName}网络异常，请检查连接]"
            msg.contains("401") || msg.contains("403") -> "[工具${toolName}认证失败]"
            else -> "[工具${toolName}执行失败: ${msg.take(80)}]"
        }
    }
}
