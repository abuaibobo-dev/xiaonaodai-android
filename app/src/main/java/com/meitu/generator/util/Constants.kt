package com.meitu.generator.util

object Constants {
    // ============ Coze API 配置 ============
    const val COZE_API_BASE_URL = "https://api.coze.cn"
    const val KEY_COZE_PAT = "coze_pat"
    const val KEY_COZE_BOT_ID = "coze_bot_id"

    // 默认配置（开箱即用）
    const val DEFAULT_COZE_PAT = "pat_UpyDNzHznqt05EuVxLR6HovSWc31SqOxRINUKSmrxMm31R91OCczSIxnFdPFacTD"
    const val DEFAULT_COZE_BOT_ID = "7667987502787461163"

    // ============ DeepSeek API 配置 ============
    const val DEEPSEEK_API_BASE_URL = "https://api.deepseek.com"
    const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    const val KEY_DEEPSEEK_MODEL = "deepseek_model"
    val DEEPSEEK_MODELS = listOf("deepseek-v4-flash", "deepseek-v4-pro")

    // ============ AI 通道类型 ============
    const val CHANNEL_COZE = "coze"
    const val CHANNEL_DEEPSEEK = "deepseek"
    const val KEY_AI_CHANNEL = "ai_channel"

    // 本地数据库名
    const val DB_NAME = "meitu_database"

    // ============ App 信息 ============
    const val APP_NAME = "布老师"
    const val APP_VERSION = "6.2.1"
    const val APP_VERSION_CODE = 63
}
