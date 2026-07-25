package com.meitu.generator.util

object Constants {
    const val AGNES_BASE_URL = "https://apihub.agnes-ai.com/"
    const val AGNES_API_KEY = "sk-rNcRB5bYHiwCCK828hAcEjHzj7LuTPlgGRybB404cpRVvVEl"
    const val AGNES_MODEL = "agnes-image-2.1-flash"

    const val GEMINI_API_KEY = "AQ.Ab8RN6LXtF2KynPVFL0V6PzLGJpiqqwxaabvvmgy6Os411GbPQ"
    const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    const val IMGBB_ENDPOINT = "https://api.imgbb.com/1/upload"

    const val MAX_CONCURRENT = 3
    const val DEFAULT_INTERVAL = 4
    const val POLL_INTERVAL_MS = 2000L
    const val MAX_LOG_ENTRIES = 200

    const val DB_NAME = "meitu_database"
    const val IMAGES_DIR = "generated_images"

    // Settings keys
    const val KEY_SUBMIT_INTERVAL = "submit_interval"
    const val KEY_DEFAULT_MODEL = "default_model"
    const val KEY_DEFAULT_QUALITY = "default_quality"
    const val KEY_IMGBB_API_KEY = "imgbb_api_key"
    const val KEY_IMGBB_AUTO_UPLOAD = "imgbb_auto_upload"
    const val KEY_BATTERY_SAFE_MODE = "battery_safe_mode"
    const val KEY_AUTO_SAVE_ALBUM = "auto_save_to_album"
}
