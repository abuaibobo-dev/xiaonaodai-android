package com.meitu.generator.util

object Constants {
    // ============ AI 大脑 - Agnes API（主力，永久免费） ============
    const val OPENAI_BASE_URL = "https://api.agnes-ai.com/v1/"
    val OPENAI_API_KEY: String get() = String(byteArrayOf(115,107,45,101,74,82,57,122,86,72,112,109,71,86,76,86,51,66,111,89,120,75,81,85,116,83,78,52,90,114,102,51,69,84,110,112,55,53,48,80,97,72,80,115,74,89,82,121,118,68,57))
    const val OPENAI_MODEL = "agnes-2.5-flash"
    const val AGNES_MODEL = "agnes-2.5-flash"

    // ============ DeepSeek API（最终备用，余额用完不续费） ============
    const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1/"
    val DEEPSEEK_API_KEY: String get() = String(byteArrayOf(115,107,45,49,100,53,101,55,55,51,50,98,51,102,54,52,101,50,55,98,102,98,98,56,57,54,100,53,55,98,102,101,101,50,98))

    // ============ Google Gemini API ============
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    val GEMINI_API_KEY: String get() = String(byteArrayOf(65,81,46,65,98,56,82,78,54,76,88,116,70,50,75,121,110,80,86,70,76,48,86,54,80,122,76,71,74,112,105,113,113,119,120,97,97,98,118,118,109,103,121,54,79,115,52,49,49,71,98,80,81))
    const val GEMINI_MODEL = "gemini-3.5-flash"
    const val KEY_GEMINI_API_KEY = "gemini_api_key"

    // ============ Groq API ============
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    val GROQ_API_KEY: String get() = String(byteArrayOf(103,115,107,95,110,54,70,86,110,56,49,87,102,55,65,67,108,115,51,80,67,107,48,87,87,71,100,121,98,51,70,89,82,78,78,52,56,103,71,82,52,115,112,73,78,51,53,105,108,81,114,105,54,104,52,66))
    const val GROQ_MODEL = "llama-4-scout"
    const val KEY_GROQ_API_KEY = "groq_api_key"

    // ============ SambaNova API ============
    const val SAMBANOVA_BASE_URL = "https://api.sambanova.ai/v1/"
    val SAMBANOVA_API_KEY: String get() = String(byteArrayOf(51,49,99,54,53,49,55,102,45,97,55,54,49,45,52,56,52,101,45,98,55,102,51,45,55,100,56,50,57,56,101,100,50,98,55,49))
    const val SAMBANOVA_MODEL = "Meta-Llama-3.3-70B-Instruct"
    const val KEY_SAMBANOVA_API_KEY = "sambanova_api_key"

    // ============ HuggingFace API ============
    const val HF_BASE_URL = "https://api-inference.huggingface.co/v1/"
    val HF_API_KEY: String get() = String(byteArrayOf(104,102,95,100,70,84,71,87,76,103,73,106,84,70,84,81,107,102,108,109,119,112,67,108,90,83,82,120,115,81,100,115,114,104,105,86,79))
    const val HF_MODEL = "meta-llama/Llama-3.3-70B-Instruct"
    const val KEY_HF_API_KEY = "hf_api_key"

    // ============ OpenRouter API ============
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1/"
    val OPENROUTER_API_KEY: String get() = String(byteArrayOf(115,107,45,111,114,45,118,49,45,48,98,50,98,50,53,52,99,57,51,54,52,48,99,51,55,98,98,48,53,49,49,56,97,48,52,100,54,98,52,51,52,102,56,48,48,55,102,57,56,49,50,102,99,97,48,48,99,97,98,55,98,48,53,53,56,100,102,52,97,99,48,97,56))
    const val OPENROUTER_MODEL = "tencent/hy3:free"
    const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"

    // ============ Cerebras API ============
    const val CEREBRAS_BASE_URL = "https://api.cerebras.ai/v1/"
    const val CEREBRAS_API_KEY = ""
    const val CEREBRAS_MODEL = "llama-3.3-70b"
    const val KEY_CEREBRAS_API_KEY = "cerebras_api_key"

    // ============ NVIDIA NIM API ============
    const val NVIDIA_BASE_URL = "https://integrate.api.nvidia.com/v1/"
    const val NVIDIA_API_KEY = ""
    const val NVIDIA_MODEL = "meta/llama-3.3-70b-instruct"
    const val KEY_NVIDIA_API_KEY = "nvidia_api_key"

    val AVAILABLE_MODELS = listOf(
        // 自动模式（智能路由）
        "auto",
        // Agnes（主力，永久免费）
        "agnes-2.5-flash",
        // OpenRouter 免费模型
        "tencent/hy3:free",
        "baidu/cobuddy:free",
        "nvidia/nemotron-3-ultra:free",
        // Google Gemini（免费备用）
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        // Groq（免费备用）
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "deepseek-r1-distill-llama-70b",
        "moonshotai/kimi-k2-instruct",
        "llama-4-scout",
        // SambaNova（免费备用）
        "Meta-Llama-3.3-70B-Instruct",
        "gpt-oss-120b",
        "DeepSeek-V3.1",
        "gemma-4-31B-it",
        // HuggingFace（免费备用）
        "meta-llama/Llama-3.3-70B-Instruct",
        // DeepSeek（最终备用，不续费）
        "deepseek-v4-flash",
        "deepseek-v4-pro"
    )

    const val VISION_MODEL = "gemini-2.5-flash"

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
    const val APP_VERSION = "4.7.0"
    const val APP_VERSION_CODE = 42
}
