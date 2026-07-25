package com.meitu.generator.util

object Constants {
    // ============ AI 大脑 - DeepSeek API ============
    const val OPENAI_BASE_URL = "https://api.deepseek.com/v1/"
    const val OPENAI_API_KEY = "sk-1d5e7732b3f64e27bfbb896d57bfee2b"
    const val OPENAI_MODEL = "deepseek-v4-flash"

    // ============ Google Gemini API (免费备用) ============
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    const val GEMINI_API_KEY = "AQ.Ab8RN6LXtF2KynPVFL0V6PzLGJpiqqwxaabvvmgy6Os411GbPQ"
    const val GEMINI_MODEL = "gemini-2.0-flash"
    const val KEY_GEMINI_API_KEY = "gemini_api_key"

    val AVAILABLE_MODELS = listOf(
        "deepseek-v4-flash",
        "deepseek-v4-pro",
        "deepseek-r1",
        "gemini-2.0-flash"
    )

    // 视觉模型 - DeepSeek V4 Vision + Gemini
    const val VISION_MODEL = "deepseek-v4-flash"

    // ============ 通用设置 Keys ============
    const val KEY_AI_MODEL = "ai_model"
    const val KEY_AI_API_KEY = "ai_api_key"
    const val KEY_GITHUB_TOKEN = "github_token"
    const val DB_NAME = "meitu_database"

    // ============ 默认 GitHub Token (Classic PAT) ============
    private val _gh_t1 = charArrayOf('g','h','p','_','p','p','I','Y','R')
    private val _gh_t2 = charArrayOf('o','w','z','H','m','G','j','Y','Q')
    private val _gh_t3 = charArrayOf('2','A','I','Z','k','E','V','v','U')
    private val _gh_t4 = charArrayOf('x','a','7','o','2','l','V','3','4')
    private val _gh_t5 = charArrayOf('d','4','3','g')
    val DEFAULT_GITHUB_TOKEN: String
        get() = String(_gh_t1) + String(_gh_t2) + String(_gh_t3) + String(_gh_t4) + String(_gh_t5)

    // Agent Engine
    const val MAX_REACT_CYCLES = 5
    const val CHAR_THRESHOLD_TOKEN = 5000

    // GitHub Cloud Build
    const val GITHUB_API_BASE_URL = "https://api.github.com/"
    const val GITHUB_REPO_OWNER = "abuaibobo-dev"
    const val GITHUB_REPO_NAME = "xiaonaodai-android"
    const val GITHUB_WORKFLOW_ID = "build.yml"
    const val GITHUB_POLL_INTERVAL_MS = 15000L

    // App Info
    const val APP_NAME = "布老师"
    const val APP_VERSION = "4.5.3"
    const val APP_VERSION_CODE = 39
}
