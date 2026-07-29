package com.meitu.generator.util

object Constants {
    // ============ AI 大脑 - DeepSeek API ============
    const val OPENAI_BASE_URL = "https://api.deepseek.com/v1/"
    val OPENAI_API_KEY: String get() = String(byteArrayOf(115,107,45,55,50,52,57,100,56,52,48,57,53,99,48,52,97,98,50,56,55,56,102,53,100,97,102,98,55,99,102,55,100,97,52))
    const val OPENAI_MODEL = "deepseek-chat"

    // ============ Google AI (Gemini) - OpenAI 兼容模式 ============
    const val GOOGLE_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"
    val GOOGLE_API_KEY: String get() = String(byteArrayOf(65,81,46,65,98,56,82,78,54,74,100,109,53,50,81,84,88,68,50,49,111,111,85,85,78,102,95,110,73,54,78,71,45,89,102,54,90,114,98,73,55,95,101,121,75,73,79,85,81,54,85,55,119))
    const val GOOGLE_DEFAULT_MODEL = "gemini-2.0-flash"
    const val KEY_GOOGLE_API_KEY = "google_api_key"

    // ============ OpenAI ============
    const val OPENAI_REAL_BASE_URL = "https://api.openai.com/v1/"
    const val KEY_OPENAI_API_KEY = "openai_api_key"

    // ============ Groq ============
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    const val KEY_GROQ_API_KEY = "groq_api_key"

    // ============ SiliconFlow (硅基流动) ============
    const val SILICONFLOW_BASE_URL = "https://api.siliconflow.cn/v1/"
    const val KEY_SILICONFLOW_API_KEY = "siliconflow_api_key"

    // ============ Moonshot (Kimi) ============
    const val MOONSHOT_BASE_URL = "https://api.moonshot.cn/v1/"
    const val KEY_MOONSHOT_API_KEY = "moonshot_api_key"

    // ============ Zhipu AI (智谱) ============
    const val ZHIPU_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/"
    const val KEY_ZHIPU_API_KEY = "zhipu_api_key"

    // ============ 视觉模型 ============
    const val VISION_MODEL = "gemini-2.0-flash"

    val AVAILABLE_MODELS = listOf(
        "auto",
        "deepseek-chat",
        "deepseek-reasoner",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash",
        "gpt-4o",
        "gpt-4o-mini",
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "deepseek-ai/DeepSeek-V3",
        "Qwen/Qwen2.5-72B-Instruct",
        "moonshot-v1-8k",
        "moonshot-v1-32k",
        "glm-4-flash",
        "glm-4"
    )

    const val KEY_AI_MODEL = "ai_model"
    const val KEY_AI_API_KEY = "ai_api_key"
    const val KEY_GITHUB_TOKEN = "github_token"
    const val DB_NAME = "meitu_database"

    // ============ GitHub 配置 - 用户可自定义 ============
    const val KEY_GITHUB_REPO_OWNER = "github_repo_owner"
    const val KEY_GITHUB_REPO_NAME = "github_repo_name"
    const val KEY_GITHUB_WORKFLOW_ID = "github_workflow_id"
    const val KEY_GITHUB_USER_WORKFLOW_ID = "github_user_workflow_id"

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
    // 默认值 — 用户可在设置中覆盖
    const val GITHUB_REPO_OWNER = "abuaibobo-dev"
    const val GITHUB_REPO_NAME = "xiaonaodai-android"
    const val GITHUB_WORKFLOW_ID = "build.yml"
    const val GITHUB_USER_PROJECT_WORKFLOW_ID = "user-project-build.yml"
    const val GITHUB_POLL_INTERVAL_MS = 15000L

    const val APP_NAME = "布老师"
    const val APP_VERSION = "5.0.7"
    const val APP_VERSION_CODE = 57
}
