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
    const val CHANNEL_CUSTOM_PREFIX = "custom:"
    const val KEY_AI_CHANNEL = "ai_channel"
    const val KEY_CUSTOM_API_LIST = "custom_api_list"

    // 本地数据库名
    const val DB_NAME = "meitu_database"

    // ============ App 信息 ============
    const val APP_NAME = "布老师"
    const val APP_VERSION = "6.2.3"
    const val APP_VERSION_CODE = 65

    // ============ GitHub 配置 ============
    const val KEY_GITHUB_TOKEN = "github_token"
    const val GITHUB_API_BASE = "https://api.github.com"

    // ============ HuggingFace 配置 ============
    const val KEY_HF_TOKEN = "hf_token"
    const val HF_API_BASE = "https://huggingface.co/api"

    // ============ Server酱 / PushPlus 配置 ============
    const val KEY_SERVERCHAN_KEY = "serverchan_key"
    const val KEY_PUSHPLUS_TOKEN = "pushplus_token"

    // ============ 通用余额查询 ============
    const val KEY_BALANCE_CHECK_LIST = "balance_check_list"
}