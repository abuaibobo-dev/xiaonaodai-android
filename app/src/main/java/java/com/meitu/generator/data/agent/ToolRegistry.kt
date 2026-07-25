package com.meitu.generator.data.agent

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 工具注册表 - 管理所有可用工具
 * 启动时注册所有工具，按名称查找
 */
@Singleton
class ToolRegistry @Inject constructor() {
    private val tools = mutableMapOf<String, Tool>()
    
    /** 注册一个工具 */
    fun register(tool: Tool) {
        tools[tool.name] = tool
    }
    
    /** 批量注册 */
    fun registerAll(toolList: List<Tool>) {
        toolList.forEach { tools[it.name] = it }
    }
    
    /** 按名称获取工具 */
    fun get(name: String): Tool? = tools[name]
    
    /** 获取所有已注册工具 */
    fun getAll(): List<Tool> = tools.values.toList()
    
    /** 获取所有工具的 function calling 声明（供 Gemini 使用） */
    fun getFunctionDeclarations(): List<com.google.gson.JsonObject> {
        return tools.values.map { tool ->
            com.google.gson.JsonObject().apply {
                addProperty("name", tool.name)
                addProperty("description", tool.description)
                add("parameters", tool.parametersSchema)
            }
        }
    }
    
    /** 检查工具是否已注册 */
    fun has(name: String): Boolean = tools.containsKey(name)
    
    /** 获取工具数量 */
    fun size(): Int = tools.size
}
