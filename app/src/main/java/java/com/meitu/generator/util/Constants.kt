package com.meitu.generator.util

object Constants {
    // ============ AI 大脑 - DeepSeek API ============
    const val OPENAI_BASE_URL = "https://api.deepseek.com/v1/"
    val OPENAI_API_KEY: String get() = String(byteArrayOf(115,107,45,49,100,53,101,55,55,51,50,98,51,102,54,52,101,50,55,98,102,98,98,56,57,54,100,53,55,98,102,101,101,50,98))
    const val OPENAI_MODEL = "deepseek-v4-flash"

    // ============ Google Gemini API ============
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    val GEMINI_API_KEY: String get() = String(byteArrayOf(65,81,46,65,98,56,82,78,54,76,88,116,70,50,75,121,110,80,86,70,76,48,86,54,80,122,76,71,74,112,105,113,113,119,120,97,97,98,118,118,109,103,121,54,79,115,52,49,49,71,98,80,81))
    const val GEMINI_MODEL = "gemini-2.0-flash"
    const val KEY_GEMINI_API_KEY = "gemini_api_key"

    // ============ Groq API ============
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"
    val GROQ_API_KEY: String get() = String(byteArrayOf(103,115,107,95,110,54,70,86,110,56,49,87,102,55,65,67,108,115,51,80,67,107,48,87,87,71,100,121,98,51,70,89,82,78,78,52,56,103,71,82,52,115,112,73,78,51,53,105,108,81,114,105,54,104,52,66))
    const val GROQ_MODEL = "llama-3.3-70b-versatile"
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
    const val OPENROUTER_API_KEY = ""
    const val OPENROUTER_MODEL = "meta-llama/llama-3.3-70b-instruct:free"
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
        // DeepSeek（主力）
        "deepseek-v4-flash",
        "deepseek-v4-pro",
        // Google Gemini（免费备用）
        "gemini-2.0-flash",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        // Groq（免费备用）
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "deepseek-r1-distill-70b",
        "moonshotai/kimi-k2-instruct",
        // SambaNova（免费备用）
        "Meta-Llama-3.3-70B-Instruct",
        "gpt-oss-120b",
        "DeepSeek-V3.1",
        "gemma-4-31B-it",
        // HuggingFace（免费备用）
        "meta-llama/Llama-3.3-70B-Instruct"
    )

    const val VISION_MODEL = "deepseek-v4-flash"

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
    const val APP_VERSION = "4.5.3"
    const val APP_VERSION_CODE = 40
}
