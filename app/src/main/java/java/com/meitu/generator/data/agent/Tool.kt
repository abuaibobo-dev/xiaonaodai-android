package com.meitu.generator.data.agent

import com.meitu.generator.data.model.ToolContext
import com.google.gson.JsonObject

/**
 * 工具接口 - 所有 Agent 工具必须实现此接口
 * 每个工具定义 name、description、parameters（JSON Schema 格式）
 * 供 Gemini function calling 使用
 */
interface Tool {
    /** 工具唯一标识名 */
    val name: String
    
    /** 工具描述（传给 LLM 帮助决策） */
    val description: String
    
    /** 工具参数的 JSON Schema 定义（Gemini function calling 格式） */
    val parametersSchema: JsonObject
    
    /** 
     * 执行工具
     * @param arguments LLM 传来的参数
     * @param context 运行时上下文
     * @return 执行结果字符串
     */
    suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String
}

/**
 * 工具执行结果
 */
data class ToolExecutionResult(
    val success: Boolean,
    val output: String,
    val imageData: String? = null
)
