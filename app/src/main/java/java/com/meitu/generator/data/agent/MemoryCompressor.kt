package com.meitu.generator.data.agent

import android.content.SharedPreferences
import com.meitu.generator.data.local.dao.MemoryDao
import com.meitu.generator.data.local.entity.MemoryEntity
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.dto.OpenAIMessage
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 记忆压缩器 - 对话历史超过20轮时自动生成摘要
 * v3.0: 使用 OpenAI 兼容 API 替代 Gemini
 */
@Singleton
class MemoryCompressor @Inject constructor(
    private val memoryDao: MemoryDao,
    private val openAIService: OpenAIService,
    private val settingsRepository: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) {
    companion object {
        private const val MAX_HISTORY_ENTRIES = 20
        private const val CATEGORY_HISTORY = "history"
        private const val CATEGORY_SUMMARY = "history_summary"
    }
    
    suspend fun compressIfNeeded() {
        val history = memoryDao.getByCategory(CATEGORY_HISTORY)
        if (history.size <= MAX_HISTORY_ENTRIES) return
        compress(history)
    }
    
    private suspend fun compress(history: List<MemoryEntity>) {
        val historyText = history.take(MAX_HISTORY_ENTRIES).joinToString("\n") { 
            "${it.key}: ${it.value}" 
        }
        
        try {
            val prompt = """请将以下对话历史压缩为简洁的偏好摘要，格式如：
"用户喜欢XX风格，常用XX分辨率，偏好XX模型"

对话历史：
$historyText

只输出摘要文本，不要解释。"""

            val model = settingsRepository.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
            val effectiveModel = if (model == "auto" || model.isBlank()) "agnes-2.5-flash" else model
            val apiKey = (securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: "").ifBlank { Constants.OPENAI_API_KEY }

            val request = OpenAIRequest(
                model = effectiveModel,
                messages = listOf(
                    OpenAIMessage(role = "system", content = "你是记忆压缩助手，将对话历史压缩为简洁偏好摘要。"),
                    OpenAIMessage(role = "user", content = prompt)
                ),
                temperature = 0.3,
                max_tokens = 300
            )
            val response = openAIService.chatCompletions(
                request = request,
                authorization = "Bearer $apiKey"
            )
            val summary = response.choices?.firstOrNull()?.message?.content
            
            if (!summary.isNullOrEmpty()) {
                memoryDao.insert(MemoryEntity(
                    key = "history_summary_${System.currentTimeMillis()}",
                    value = summary,
                    category = CATEGORY_SUMMARY
                ))
                history.dropLast(5).forEach { memoryDao.delete(it) }
            }
        } catch (e: Exception) {
            history.dropLast(MAX_HISTORY_ENTRIES / 2).forEach { memoryDao.delete(it) }
        }
    }
}
