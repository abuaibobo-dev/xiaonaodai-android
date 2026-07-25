package com.meitu.generator.data.agent

/**
 * Token 消耗估算器 - 在 ReAct 循环中监控总字符消耗
 * 单次对话总字符超过5000时，自动触发记忆压缩
 */
object TokenEstimator {
    private const val CHAR_THRESHOLD = 5000
    
    private var totalChars = 0
    
    /** 记录一轮循环的输入/输出字符数 */
    fun account(input: String, output: String) {
        totalChars += input.length + output.length
    }
    
    /** 当前总消耗是否超过阈值 */
    fun shouldCompress(): Boolean = totalChars > CHAR_THRESHOLD
    
    /** 获取当前总字符数 */
    fun getTotalChars(): Int = totalChars
    
    /** 重置（新对话开始时） */
    fun reset() {
        totalChars = 0
    }
    
    /** 估算 Token 数（中文约1.5字/token，英文约4字/token） */
    fun estimateTokens(): Int {
        return (totalChars * 0.6).toInt()  // 粗估
    }
}
