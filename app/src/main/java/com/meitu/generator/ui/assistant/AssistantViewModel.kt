package com.meitu.generator.ui.assistant

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.remote.CozeApiClient
import com.meitu.generator.data.remote.StreamEvent
import com.meitu.generator.data.local.dao.ChatMessageDao
import com.meitu.generator.data.local.dao.SessionSummary
import com.meitu.generator.data.local.entity.ChatMessageEntity
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val cozeClient: CozeApiClient,
    @Named("securePrefs") private val securePrefs: SharedPreferences,
    private val chatMessageDao: ChatMessageDao
) : AndroidViewModel(application) {

    // ============ Chat Messages ============
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(
            text = "你好！我是布老师，你的专属 AI 助手 🧠\n\n我可以陪你聊天、回答问题、分析图片。\n\n💡 有什么想法直接说，我来帮你",
            isUser = false
        ))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // ============ 会话管理 ============
    private var currentSessionId: Long = System.currentTimeMillis()
    private var cozeConversationId: String? = null

    // ============ 对话名称 ============
    private val _conversationName = MutableStateFlow("新对话")
    val conversationName: StateFlow<String> = _conversationName.asStateFlow()

    fun setConversationName(name: String) {
        _conversationName.value = name
    }

    fun autoGenerateConversationName(firstMessage: String) {
        if (_conversationName.value == "新对话" && firstMessage.isNotBlank()) {
            val name = if (firstMessage.length > 15) firstMessage.take(15) + "..." else firstMessage
            _conversationName.value = name
        }
    }

    // ============ 历史对话列表 ============
    private val _sessionList = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessionList: StateFlow<List<SessionSummary>> = _sessionList.asStateFlow()

    fun loadSessionList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sessions = chatMessageDao.getSessionSummaries()
                _sessionList.value = sessions
            }
        }
    }

    fun switchToSession(sessionId: Long) {
        currentSessionId = sessionId
        cozeConversationId = null  // 切换会话时重置 Coze 对话 ID
        viewModelScope.launch {
            loadMessagesFromDb()
            // 恢复对话名称
            val messages = _messages.value
            val firstUserMsg = messages.firstOrNull { it.isUser }
            _conversationName.value = if (firstUserMsg != null) {
                if (firstUserMsg.text.length > 15) firstUserMsg.text.take(15) + "..." else firstUserMsg.text
            } else {
                "历史对话"
            }
        }
    }

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun setInputText(text: String) {
        _inputText.value = text
    }

    // ============ 附件（图片） ============
    private val _pendingImageUri = MutableStateFlow<String?>(null)
    val pendingImageUri: StateFlow<String?> = _pendingImageUri.asStateFlow()

    fun setPendingImageUri(uri: String?) {
        _pendingImageUri.value = uri
    }

    // ============ Loading ============
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ============ 状态 ============
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // ============ 当前流式任务的 Job ============
    private var streamJob: Job? = null

    // ============ Coze 配置状态 ============
    private val _isCozeConfigured = MutableStateFlow(true)
    val isCozeConfigured: StateFlow<Boolean> = _isCozeConfigured.asStateFlow()

    // ============ 当前 AI 通道 ============
    private val _currentChannel = MutableStateFlow(Constants.CHANNEL_COZE)
    val currentChannel: StateFlow<String> = _currentChannel.asStateFlow()

    init {
        checkCozeConfig()
        loadChannel()
        loadSessionList()
    }

    private fun loadChannel() {
        val channel = securePrefs.getString(Constants.KEY_AI_CHANNEL, Constants.CHANNEL_COZE) ?: Constants.CHANNEL_COZE
        _currentChannel.value = channel
    }

    /**
     * 获取所有可用通道列表，用于循环切换
     */
    private fun getAvailableChannels(): List<String> {
        val channels = mutableListOf(Constants.CHANNEL_COZE, Constants.CHANNEL_DEEPSEEK)
        val customJson = securePrefs.getString(Constants.KEY_CUSTOM_API_LIST, "[]") ?: "[]"
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.meitu.generator.ui.settings.CustomApiConfig>>() {}.type
            val customList: List<com.meitu.generator.ui.settings.CustomApiConfig> = com.google.gson.Gson().fromJson(customJson, type)
            for (c in customList) {
                channels.add("${Constants.CHANNEL_CUSTOM_PREFIX}${c.id}")
            }
        } catch (_: Exception) {}
        return channels
    }

    /**
     * 获取当前通道的显示标签
     */
    fun getChannelLabel(): String {
        val ch = _currentChannel.value
        return when {
            ch == Constants.CHANNEL_COZE -> "🧠 Coze"
            ch == Constants.CHANNEL_DEEPSEEK -> "DeepSeek"
            ch.startsWith(Constants.CHANNEL_CUSTOM_PREFIX) -> {
                val id = ch.removePrefix(Constants.CHANNEL_CUSTOM_PREFIX)
                val customJson = securePrefs.getString(Constants.KEY_CUSTOM_API_LIST, "[]") ?: "[]"
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<com.meitu.generator.ui.settings.CustomApiConfig>>() {}.type
                    val list: List<com.meitu.generator.ui.settings.CustomApiConfig> = com.google.gson.Gson().fromJson(customJson, type)
                    val config = list.find { it.id == id }
                    "${config?.emoji ?: "🔌"} ${config?.name ?: "自定义"}"
                } catch (_: Exception) { "🔌 自定义" }
            }
            else -> "🧠 Coze"
        }
    }

    /**
     * 循环切换到下一个通道
     */
    fun cycleChannel() {
        val channels = getAvailableChannels()
        val currentIdx = channels.indexOf(_currentChannel.value)
        val nextIdx = if (currentIdx < 0 || currentIdx >= channels.size - 1) 0 else currentIdx + 1
        val nextChannel = channels[nextIdx]
        securePrefs.edit().putString(Constants.KEY_AI_CHANNEL, nextChannel).apply()
        _currentChannel.value = nextChannel
    }

    private fun checkCozeConfig() {
        viewModelScope.launch {
            val savedPat = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
            val savedBotId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
            // 使用默认值回退：用户未配置时自动使用内置的 PAT 和 Bot ID
            val effectivePat = savedPat.ifBlank { Constants.DEFAULT_COZE_PAT }
            val effectiveBotId = savedBotId.ifBlank { Constants.DEFAULT_COZE_BOT_ID }
            _isCozeConfigured.value = effectivePat.isNotBlank() && effectiveBotId.isNotBlank()
        }
    }

    fun refreshCozeConfig() {
        checkCozeConfig()
        loadChannel()
    }

    // ============ 发送消息 ============
    fun sendMessage() {
        val text = _inputText.value.trim()
        val imageUri = _pendingImageUri.value

        if (text.isEmpty() && imageUri == null) return

        val channel = _currentChannel.value
        if (channel == Constants.CHANNEL_COZE && !_isCozeConfigured.value) {
            _statusMessage.value = "请先在设置中配置 Coze PAT 和 Bot ID"
            return
        }
        if (channel == Constants.CHANNEL_DEEPSEEK) {
            val apiKey = securePrefs.getString(Constants.KEY_DEEPSEEK_API_KEY, "") ?: ""
            if (apiKey.isBlank()) {
                _statusMessage.value = "请先在设置中配置 DeepSeek API Key"
                return
            }
        }

        // 生成对话名称
        autoGenerateConversationName(text.ifEmpty { "[图片]" })

        // 添加用户消息到列表
        val userMessage = ChatMessage(
            text = text.ifEmpty { "[图片]" },
            isUser = true,
            imageUri = imageUri
        )
        _messages.value = (_messages.value + userMessage).takeLast(100)
        _inputText.value = ""
        _pendingImageUri.value = null
        _isLoading.value = true

        // 保存用户消息到数据库
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatMessageDao.insert(ChatMessageEntity(
                    text = userMessage.text,
                    isUser = true,
                    imageUri = userMessage.imageUri,
                    timestamp = userMessage.timestamp,
                    sessionId = currentSessionId
                ))
                loadSessionList()
            } catch (_: Exception) {}
        }

        // 构建消息内容（如果有图片，先分析图片）
        viewModelScope.launch {
            var messageContent = text
            if (imageUri != null) {
                _statusMessage.value = "🖼️ 正在处理图片..."
                val imageDescription = processImage(imageUri)
                messageContent = if (text.isNotEmpty()) {
                    "$text\n\n[用户发送了一张图片，图片内容描述: $imageDescription]"
                } else {
                    "[用户发送了一张图片，图片内容描述: $imageDescription]"
                }
            }

            // 根据通道选择调用
            when {
                channel == Constants.CHANNEL_DEEPSEEK -> streamChatFromDeepSeek(messageContent)
                channel.startsWith(Constants.CHANNEL_CUSTOM_PREFIX) -> {
                    val customId = channel.removePrefix(Constants.CHANNEL_CUSTOM_PREFIX)
                    val customJson = securePrefs.getString(Constants.KEY_CUSTOM_API_LIST, "[]") ?: "[]"
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<com.meitu.generator.ui.settings.CustomApiConfig>>() {}.type
                        val list: List<com.meitu.generator.ui.settings.CustomApiConfig> = com.google.gson.Gson().fromJson(customJson, type)
                        val config = list.find { it.id == customId }
                        if (config != null) {
                            streamChatFromCustom(config)
                        } else {
                            _statusMessage.value = "❌ 自定义 API 配置不存在"
                            _isLoading.value = false
                        }
                    } catch (_: Exception) {
                        _statusMessage.value = "❌ 读取自定义配置失败"
                        _isLoading.value = false
                    }
                }
                else -> streamChatFromCoze(messageContent)
            }
        }
    }

    // ============ DeepSeek 直连 ============
    private suspend fun streamChatFromDeepSeek(message: String) {
        var aiMessageId: Long? = null
        var fullResponse = StringBuilder()

        try {
            val apiKey = securePrefs.getString(Constants.KEY_DEEPSEEK_API_KEY, "") ?: ""
            val systemPrompt = securePrefs.getString("deepseek_system_prompt", "") ?: ""
            val dsModel = securePrefs.getString(Constants.KEY_DEEPSEEK_MODEL, "deepseek-v4-flash") ?: "deepseek-v4-flash"

            val history = buildHistoryFromMessages()

            cozeClient.streamDeepSeekChat(
                apiKey = apiKey,
                message = message,
                model = dsModel,
                systemPrompt = systemPrompt.ifBlank { null },
                history = history
            ).collect { event ->
                when (event) {
                    is StreamEvent.Status -> {
                        _statusMessage.value = event.message
                    }
                    is StreamEvent.Delta -> {
                        if (aiMessageId == null) {
                            aiMessageId = System.nanoTime()
                            val newMsg = ChatMessage(id = aiMessageId!!, text = "", isUser = false)
                            _messages.value = (_messages.value + newMsg).takeLast(100)
                        }
                        fullResponse.append(event.text)
                        val id = aiMessageId!!
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == id) msg.copy(text = fullResponse.toString()) else msg
                        }
                    }
                    is StreamEvent.Done -> {
                        _statusMessage.value = null
                        _isLoading.value = false
                        if (aiMessageId != null && fullResponse.isNotEmpty()) {
                            saveAiMessage(fullResponse.toString())
                        } else if (aiMessageId == null) {
                            // 没有收到任何内容，创建一条提示
                            val id = System.nanoTime()
                            val errMsg = ChatMessage(id = id, text = "❌ AI 未返回内容，请重试", isUser = false)
                            _messages.value = (_messages.value + errMsg).takeLast(100)
                        }
                    }
                    is StreamEvent.Error -> {
                        _statusMessage.value = null
                        _isLoading.value = false
                        if (aiMessageId != null && fullResponse.isNotEmpty()) {
                            saveAiMessage(fullResponse.toString())
                        } else {
                            val id = System.nanoTime()
                            val errMsg = ChatMessage(id = id, text = "❌ ${event.message}", isUser = false)
                            _messages.value = (_messages.value + errMsg).takeLast(100)
                        }
                    }
                    is StreamEvent.TokenUsage -> {
                        accumulateTokenUsage(event.total, event.input, event.output)
                    }
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = null
            val errorMsg = "❌ DeepSeek 连接失败: ${e.message ?: "未知错误"}"
            if (aiMessageId != null && fullResponse.isNotEmpty()) {
                saveAiMessage(fullResponse.toString())
            } else {
                val id = System.nanoTime()
                val errMsg = ChatMessage(id = id, text = errorMsg, isUser = false)
                _messages.value = (_messages.value + errMsg).takeLast(100)
            }
        }
    }

    // ============ 自定义 API 流式对话 ============
    private suspend fun streamChatFromCustom(config: com.meitu.generator.ui.settings.CustomApiConfig, message: String) {
        var aiMessageId: Long? = null
        var fullResponse = StringBuilder()

        try {
            val systemPrompt = securePrefs.getString("deepseek_system_prompt", "") ?: ""
            val history = buildHistoryFromMessages()

            cozeClient.streamCustomChat(
                apiKey = config.apiKey,
                message = message,// 确保传入实际用户消息
                model = config.model,
                baseUrl = config.baseUrl,
                systemPrompt = systemPrompt.ifBlank { null },
                history = history
            ).collect { event ->
                when (event) {
                    is StreamEvent.Status -> {
                        _statusMessage.value = event.message
                    }
                    is StreamEvent.Delta -> {
                        if (aiMessageId == null) {
                            aiMessageId = System.nanoTime()
                            val newMsg = ChatMessage(id = aiMessageId!!, text = "", isUser = false)
                            _messages.value = (_messages.value + newMsg).takeLast(100)
                        }
                        fullResponse.append(event.text)
                        val id = aiMessageId!!
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == id) msg.copy(text = fullResponse.toString()) else msg
                        }
                    }
                    is StreamEvent.Done -> {
                        _statusMessage.value = null
                        _isLoading.value = false
                        if (aiMessageId != null && fullResponse.isNotEmpty()) {
                            saveAiMessage(fullResponse.toString())
                        } else if (aiMessageId == null) {
                            val id = System.nanoTime()
                            val errMsg = ChatMessage(id = id, text = "❌ API 未返回内容，请检查配置", isUser = false)
                            _messages.value = (_messages.value + errMsg).takeLast(100)
                        }
                    }
                    is StreamEvent.Error -> {
                        _statusMessage.value = null
                        _isLoading.value = false
                        if (aiMessageId != null && fullResponse.isNotEmpty()) {
                            saveAiMessage(fullResponse.toString())
                        } else {
                            val id = System.nanoTime()
                            val errMsg = ChatMessage(id = id, text = "❌ ${event.message}", isUser = false)
                            _messages.value = (_messages.value + errMsg).takeLast(100)
                        }
                    }
                    is StreamEvent.TokenUsage -> {
                        accumulateTokenUsage(event.total, event.input, event.output)
                    }
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = null
            val errorMsg = "❌ 自定义 API 连接失败: ${e.message ?: "未知错误"}"
            if (aiMessageId != null && fullResponse.isNotEmpty()) {
                saveAiMessage(fullResponse.toString())
            } else {
                val id = System.nanoTime()
                val errMsg = ChatMessage(id = id, text = errorMsg, isUser = false)
                _messages.value = (_messages.value + errMsg).takeLast(100)
            }
        }
    }

    // ============ Coze 流式对话 ============
    private suspend fun streamChatFromCoze(message: String) {
        var aiMessageId: Long? = null
        var fullResponse = StringBuilder()

        try {
            val savedPat = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
            val savedBotId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
            val pat = savedPat.ifBlank { Constants.DEFAULT_COZE_PAT }
            val botId = savedBotId.ifBlank { Constants.DEFAULT_COZE_BOT_ID }

            val client = CozeApiClient(
                baseUrl = Constants.COZE_API_BASE_URL,
                pat = pat,
                botId = botId,
                httpClient = provideTempHttpClient()
            )

            client.streamChat(
                message = message,
                conversationId = cozeConversationId
            ).collect { event ->
                when (event) {
                    is StreamEvent.Status -> {
                        _statusMessage.value = event.message
                    }
                    is StreamEvent.Delta -> {
                        if (aiMessageId == null) {
                            aiMessageId = System.nanoTime()
                            val newMsg = ChatMessage(id = aiMessageId!!, text = "", isUser = false)
                            _messages.value = (_messages.value + newMsg).takeLast(100)
                        }
                        fullResponse.append(event.text)
                        val id = aiMessageId!!
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == id) msg.copy(text = fullResponse.toString()) else msg
                        }
                    }
                    is StreamEvent.Done -> {
                        cozeConversationId = event.conversationId
                        _statusMessage.value = null
                        _isLoading.value = false

                        if (aiMessageId != null && fullResponse.isNotEmpty()) {
                            saveAiMessage(fullResponse.toString())
                        } else if (aiMessageId == null) {
                            val id = System.nanoTime()
                            val errMsg = ChatMessage(id = id, text = "❌ AI 未返回内容，请重试", isUser = false)
                            _messages.value = (_messages.value + errMsg).takeLast(100)
                        }
                    }
                    is StreamEvent.Error -> {
                        _statusMessage.value = null
                        _isLoading.value = false
                        if (aiMessageId != null && fullResponse.isNotEmpty()) {
                            saveAiMessage(fullResponse.toString())
                        } else {
                            val id = System.nanoTime()
                            val errMsg = ChatMessage(id = id, text = "❌ ${event.message}", isUser = false)
                            _messages.value = (_messages.value + errMsg).takeLast(100)
                        }
                    }
                    is StreamEvent.TokenUsage -> {
                        accumulateTokenUsage(event.total, event.input, event.output)
                    }
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = null
            val errorMsg = "❌ 连接失败: ${e.message ?: "未知错误"}"
            if (aiMessageId != null && fullResponse.isNotEmpty()) {
                saveAiMessage(fullResponse.toString())
            } else {
                val id = System.nanoTime()
                val errMsg = ChatMessage(id = id, text = errorMsg, isUser = false)
                _messages.value = (_messages.value + errMsg).takeLast(100)
            }
        }
    }

    private fun provideTempHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private suspend fun saveAiMessage(text: String) {
        withContext(Dispatchers.IO) {
            try {
                chatMessageDao.insert(ChatMessageEntity(
                    text = text,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    sessionId = currentSessionId
                ))
            } catch (_: Exception) {}
        }
    }

    // ============ 构建历史对话 ============
    private fun buildHistoryFromMessages(): List<Pair<String, String>> {
        val history = mutableListOf<Pair<String, String>>()
        val chatMessages = _messages.value.filter { !it.isSystem }
        // 取最近的对话（排除空的AI占位消息）
        for (msg in chatMessages) {
            if (msg.text.isNotBlank()) {
                history.add(Pair(if (msg.isUser) "user" else "assistant", msg.text))
            }
        }
        return history
    }

    // ============ Token 消耗累计（分通道，支持自定义） ============
    companion object {
        // Coze 通道
        private const val KEY_COZE_TOTAL_TOKENS = "coze_total_tokens"
        private const val KEY_COZE_TOTAL_INPUT = "coze_total_input"
        private const val KEY_COZE_TOTAL_OUTPUT = "coze_total_output"
        private const val KEY_COZE_TOTAL_MESSAGES = "coze_total_messages"
        // DeepSeek 通道
        private const val KEY_DS_TOTAL_TOKENS = "ds_total_tokens"
        private const val KEY_DS_TOTAL_INPUT = "ds_total_input"
        private const val KEY_DS_TOTAL_OUTPUT = "ds_total_output"
        private const val KEY_DS_TOTAL_MESSAGES = "ds_total_messages"
        // 自定义通道前缀
        private const val KEY_CUSTOM_PREFIX = "custom:"
    }

    private fun accumulateTokenUsage(total: Int, input: Int, output: Int) {
        val channel = _currentChannel.value
        val editor = securePrefs.edit()

        val prefix = when {
            channel == Constants.CHANNEL_COZE -> "coze"
            channel == Constants.CHANNEL_DEEPSEEK -> "ds"
            channel.startsWith(KEY_CUSTOM_PREFIX) -> channel // custom:{id}
            else -> "coze"
        }

        editor.putInt("${prefix}_total_tokens", securePrefs.getInt("${prefix}_total_tokens", 0) + total)
        editor.putInt("${prefix}_total_input", securePrefs.getInt("${prefix}_total_input", 0) + input)
        editor.putInt("${prefix}_total_output", securePrefs.getInt("${prefix}_total_output", 0) + output)
        editor.putInt("${prefix}_total_messages", securePrefs.getInt("${prefix}_total_messages", 0) + 1)
        editor.apply()
    }

    private suspend fun processImage(uri: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                    val base64 = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
                    "图片已处理（${bitmap.width}x${bitmap.height}）"
                } else {
                    "图片无法解析"
                }
            } catch (e: Exception) {
                "图片处理失败: ${e.message}"
            }
        }
    }

    // ============ 新建对话 ============
    fun newConversation() {
        streamJob?.cancel()
        currentSessionId = System.currentTimeMillis()
        cozeConversationId = null
        _conversationName.value = "新对话"
        _messages.value = listOf(ChatMessage(
            text = "你好！我是布老师，你的专属 AI 助手 🧠\n\n我可以陪你聊天、回答问题、分析图片。\n\n💡 有什么想法直接说，我来帮你",
            isUser = false
        ))
        _isLoading.value = false
        _statusMessage.value = null
        loadSessionList()
    }

    // ============ 清空当前对话 ============
    fun clearCurrentChat() {
        streamJob?.cancel()
        _messages.value = listOf(ChatMessage(
            text = "对话已清空，开始新的对话吧 💬",
            isUser = false
        ))
        _isLoading.value = false
        _statusMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatMessageDao.deleteBySession(currentSessionId)
            } catch (_: Exception) {}
        }
    }

    // ============ 从数据库加载消息 ============
    private fun loadMessagesFromDb() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val entities = chatMessageDao.getMessagesBySession(currentSessionId)
                val chatMessages = entities.map { entity ->
                    ChatMessage(
                        text = entity.text,
                        isUser = entity.isUser,
                        imageUri = entity.imageUri,
                        timestamp = entity.timestamp
                    )
                }
                if (chatMessages.isNotEmpty()) {
                    _messages.value = chatMessages
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
