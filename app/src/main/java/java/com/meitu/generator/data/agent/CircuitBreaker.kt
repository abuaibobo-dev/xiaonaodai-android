package com.meitu.generator.data.agent

import com.meitu.generator.data.agent.AgentMemory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能熔断与备用池 - 维护免费提供商池
 * 当主源返回 429/503/超时时，自动无感切换备用源
 * 熔断状态记录到 AgentMemory
 */
@Singleton
class CircuitBreaker @Inject constructor(
    private val agentMemory: AgentMemory
) {
    data class ProviderState(
        val name: String,
        val available: Boolean = true,
        val failCount: Int = 0,
        val lastFailTime: Long = 0,
        val cooldownMs: Long = 60_000L  // 冷却1分钟
    )
    
    private val providerStates = mutableMapOf<String, ProviderState>()
    private val providerPools = mutableMapOf<String, List<String>>()  // toolName -> [primary, fallback, ...]
    
    init {
        // 初始化各工具的提供商池
        providerPools["background_remove"] = listOf("remove_bg", "pollinations")
        providerPools["image_upscale"] = listOf("pollinations", "span")
        providerPools["image_generate"] = listOf("pollinations", "agnes")
        providerPools["style_transfer"] = listOf("pollinations_kontext", "pollinations")
        providerPools["generate_video"] = listOf("pollinations_video")
        providerPools["text_to_speech"] = listOf("pollinations_audio")
    }
    
    /**
     * 获取当前可用的提供商
     * @param toolName 工具名
     * @return 第一个非熔断状态的提供商名
     */
    suspend fun getAvailableProvider(toolName: String): String {
        val pool = providerPools[toolName] ?: return "default"
        
        for (provider in pool) {
            val state = providerStates[provider]
            if (state == null || state.available) {
                return provider
            }
            // 检查是否过了冷却期
            if (System.currentTimeMillis() - state.lastFailTime > state.cooldownMs) {
                // 尝试恢复
                providerStates[provider] = state.copy(available = true, failCount = 0)
                return provider
            }
        }
        
        // 所有提供商都熔断了，返回最后一个（强制尝试）
        return pool.last()
    }
    
    /**
     * 报告提供商失败
     */
    fun reportFailure(provider: String) {
        val state = providerStates.getOrPut(provider) { ProviderState(name = provider) }
        val newFailCount = state.failCount + 1
        // 连续失败3次则熔断
        val shouldBreak = newFailCount >= 3
        providerStates[provider] = state.copy(
            available = !shouldBreak,
            failCount = newFailCount,
            lastFailTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 报告提供商成功
     */
    fun reportSuccess(provider: String) {
        providerStates[provider] = ProviderState(name = provider, available = true, failCount = 0)
    }
    
    /**
     * 获取提供商池描述
     */
    fun getPoolDescription(toolName: String): String {
        val pool = providerPools[toolName] ?: return "default"
        return pool.joinToString(" → ") { provider ->
            val state = providerStates[provider]
            if (state != null && !state.available) "$provider[熔断]" else provider
        }
    }
}
