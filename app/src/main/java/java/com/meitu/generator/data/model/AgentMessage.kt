package com.meitu.generator.data.model

/**
 * Agent 消息模型 - 支持 ReAct 循环中的多种消息类型
 */
data class AgentMessage(
    val id: Long = System.nanoTime(),
    val role: Role,
    val content: String,
    val toolCall: ToolCall? = null,
    val toolResult: ToolResult? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role {
        USER, ASSISTANT, SYSTEM, TOOL_CALL, TOOL_RESULT
    }
}

data class ToolCall(
    val toolName: String,
    val arguments: Map<String, Any>
)

data class ToolResult(
    val toolName: String,
    val success: Boolean,
    val output: String,
    val imageData: String? = null  // base64 or URL
)

/**
 * 技能定义 - JSON 描述的可注册技能
 */
data class SkillDefinition(
    val id: String,
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val enabled: Boolean = true,
    val tools: List<String>  // 包含的工具名列表
)

/**
 * 工具执行上下文 - 传递给工具的运行时信息
 */
data class ToolContext(
    val applicationContext: android.content.Context,
    val sessionId: String = System.currentTimeMillis().toString(),
    val memory: Map<String, String> = emptyMap()
)
