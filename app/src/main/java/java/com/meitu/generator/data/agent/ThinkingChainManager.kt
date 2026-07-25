package com.meitu.generator.data.agent

/**
 * 思维链管理器 - 跟踪 AI 的思考步骤
 */
class ThinkingChainManager {
    
    enum class StepType {
        UNDERSTANDING,  // 理解问题
        PLANNING,       // 制定计划
        RETRIEVING,     // 检索信息
        REASONING,      // 推理分析
        DECIDING,       // 做出决策
        EXECUTING,      // 执行操作
        VERIFYING,      // 验证结果
        RESPONDING      // 生成回复
    }
    
    data class ThinkingStep(
        val type: StepType,
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private val _steps = mutableListOf<ThinkingStep>()
    val steps: List<ThinkingStep> get() = _steps.toList()
    
    var isActive: Boolean = false
        private set
    
    fun start() {
        _steps.clear()
        isActive = true
    }
    
    fun addStep(type: StepType, content: String) {
        _steps.add(ThinkingStep(type, content))
    }
    
    fun finish() {
        isActive = false
    }
    
    fun reset() {
        _steps.clear()
        isActive = false
    }
}
