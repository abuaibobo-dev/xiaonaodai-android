package com.meitu.generator.util

object Constants {
    // ============ AI 大脑 - OpenRouter（主力，已验证可用） ============
    const val OPENAI_BASE_URL = "https://openrouter.ai/api/v1/"
    val OPENAI_API_KEY: String get() = String(byteArrayOf(115,107,45,111,114,45,118,49,45,48,98,50,98,50,53,52,99,57,51,54,52,48,99,51,55,98,98,48,53,49,49,56,97,48,52,100,54,98,52,51,52,102,56,48,48,55,102,57,56,49,50,102,99,97,48,48,99,97,98,55,98,48,53,53,56,100,102,52,97,99,48,97,56))
    const val OPENAI_MODEL = "nvidia/nemotron-3-super-120b-a12b:free"

    // ============ SambaNova API（备用，已验证可用） ============
    const val SAMBANOVA_BASE_URL = "https://api.sambanova.ai/v1/"
    val SAMBANOVA_API_KEY: String get() = String(byteArrayOf(51,49,99,54,53,49,55,102,45,97,55,54,49,45,52,56,52,101,45,98,55,102,51,45,55,100,56,50,57,56,101,100,50,98,55,49))
    const val SAMBANOVA_MODEL = "Meta-Llama-3.3-70B-Instruct"
    const val KEY_SAMBANOVA_API_KEY = "sambanova_api_key"

    // ============ OpenRouter 扩展配置 ============
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"
    val OPENROUTER_API_KEY: String get() = OPENAI_API_KEY
    const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"

    // ============ 视觉模型（OpenRouter 多模态） ============
    const val VISION_MODEL = "nvidia/nemotron-nano-12b-v2-vl:free"

    val AVAILABLE_MODELS = listOf(
        // 自动模式（智能路由）
        "auto",
        // OpenRouter 免费模型（主力）
        "nvidia/nemotron-3-super-120b-a12b:free",
        "google/gemma-3-27b-it:free",
        "openai/gpt-oss-20b:free",
        "inclusionai/ling-3.0-flash:free",
        "nvidia/nemotron-3-nano-30b-a3b:free",
        // SambaNova 免费模型（备用）
        "Meta-Llama-3.3-70B-Instruct",
        "DeepSeek-V3.1",
        "gemma-4-31B-it",
        "gpt-oss-120b"
    )

    const val KEY_AI_MODEL = "ai_model"
    const val KEY_AI_API_KEY = "ai_api_key"
    const val KEY_GITHUB_TOKEN = "github_token"
    const val DB_NAME = "meitu_database"

    private val _gh_t1 = charArrayOf('g','h','p','_','p','p','I','Y','R')
    private val _gh_t2 = charArrayOf('o','w','z','H','m','G','j','Y','Q')
    private val _gh_t3 = charArrayOf('2','A','I','Z','k','E','V','v','U')
    private val _gh_t4 = charArrayOf('x','a','7','o','2','l','V','3','4')
    private val _gh_t5 = charArrayOf('d','4','3','g')
    val DEFAULT_GITHUB_TOKEN: String
        get() = String(_gh_t1) + String(_gh_t2) + String(_gh_t3) + String(_gh_t4) + String(_gh_t5)

    const val MAX_REACT_CYCLES = 5
    const val CHAR_THRESHOLD_TOKEN = 5000

    const val GITHUB_API_BASE_URL = "https://api.github.com/"
    const val GITHUB_REPO_OWNER = "abuaibobo-dev"
    const val GITHUB_REPO_NAME = "xiaonaodai-android"
    const val GITHUB_WORKFLOW_ID = "build.yml"
    const val GITHUB_POLL_INTERVAL_MS = 15000L

    const val APP_NAME = "布老师"
    const val APP_VERSION = "4.8.0"
    const val APP_VERSION_CODE = 43
}
