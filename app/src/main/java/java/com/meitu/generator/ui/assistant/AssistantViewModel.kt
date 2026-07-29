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
    private val _isCozeConfigured = MutableStateFlow(false)
    val isCozeConfigured: StateFlow<Boolean> = _isCozeConfigured.asStateFlow()

    init {
        checkCozeConfig()
        loadSessionList()
    }

    private fun checkCozeConfig() {
        viewModelScope.launch {
            val pat = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
            val botId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
            _isCozeConfigured.value = pat.isNotBlank() && botId.isNotBlank()
        }
    }

    fun refreshCozeConfig() {
        checkCozeConfig()
    }

    // ============ 发送消息 ============
    fun sendMessage() {
        val text = _inputText.value.trim()
        val imageUri = _pendingImageUri.value

        if (text.isEmpty() && imageUri == null) return
        if (!_isCozeConfigured.value) {
            _statusMessage.value = "请先在设置中配置 Coze PAT 和 Bot ID"
            return
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

            // 流式调用 Coze API
            streamChatFromCoze(messageContent)
        }
    }

    private suspend fun streamChatFromCoze(message: String) {
        // 创建空的 AI 回复消息占位
        val aiMessageId = System.nanoTime()
        val aiMessage = ChatMessage(
            id = aiMessageId,
            text = "",
            isUser = false
        )
        _messages.value = (_messages.value + aiMessage).takeLast(100)

        var fullResponse = StringBuilder()

        try {
            // 更新 CozeApiClient 的配置（可能用户在设置中更改了）
            val pat = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
            val botId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")

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
                        fullResponse.append(event.text)
                        // 更新 AI 消息内容
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == aiMessageId) msg.copy(text = fullResponse.toString()) else msg
                        }
                    }
                    is StreamEvent.Done -> {
                        cozeConversationId = event.conversationId
                        _statusMessage.value = null
                        _isLoading.value = false

                        // 保存到数据库
                        saveAiMessage(fullResponse.toString())
                    }
                    is StreamEvent.Error -> {
                        _statusMessage.value = null
                        _isLoading.value = false
                        if (fullResponse.isEmpty()) {
                            // 没有内容，显示错误
                            _messages.value = _messages.value.map { msg ->
                                if (msg.id == aiMessageId) msg.copy(text = "❌ ${event.message}") else msg
                            }
                        } else {
                            saveAiMessage(fullResponse.toString())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = null
            val errorMsg = "❌ 连接失败: ${e.message ?: "未知错误"}"
            if (fullResponse.isEmpty()) {
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == aiMessageId) msg.copy(text = errorMsg) else msg
                }
            } else {
                saveAiMessage(fullResponse.toString())
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
