package com.meitu.generator.ui.settings

/**
 * 自定义 API 通道配置
 */
data class CustomApiConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val emoji: String = "🔌"
)
