package com.meitu.generator.ui.assistant

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.agent.AgentEngine
import com.meitu.generator.data.agent.IntentRouter
import com.meitu.generator.data.agent.ThinkingChainManager
import com.meitu.generator.data.remote.DeepSeekBalanceService
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.dto.OpenAIMessage
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.data.tools.BuildProgress
import com.meitu.generator.data.tools.BuildProgressCallback
import com.meitu.generator.data.local.dao.ChatMessageDao
import com.meitu.generator.data.local.dao.SessionSummary
import com.meitu.generator.data.local.entity.ChatMessageEntity
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named

data class TaskProgress(
    val status: String,
    val message: String,
    val progress: Float = 0f,
    val downloadUrl: String? = null
)


data class BalanceInfo(
    val totalBalance: String = "--",
    val toppedUp: String = "--",
    val used: String = "--",
    val available: Boolean = false,
    val currency: String = "CNY"
)

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val taskProgress: TaskProgress? = null,
    val imageUri: String? = null,
    val reasoningContent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val openAIService: OpenAIService,
    private val agentEngine: AgentEngine,
    @Named("securePrefs") private val securePrefs: SharedPreferences,
    private val deepSeekBalanceService: DeepSeekBalanceService,
    private val chatMessageDao: ChatMessageDao
) : AndroidViewModel(application) {

    // ============ Chat Messages ============
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(
            text = "你好！我是布老师，你的专属AI数字员工 🧠\n\n我能帮你：\n• 软件开发：写代码、改Bug、编译部署\n• 智能问答：聊天、翻译、深度分析\n• 联网搜索：实时信息、新闻、价格\n• 图片分析：识别内容、提取信息\n• 项目管理：创建、修改、迭代项目\n\n💡 有什么想法直接说，我来帮你实现",
            isUser = false
        ))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // ============ 会话管理 ============
    private var currentSessionId: Long = System.currentTimeMillis()

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
            // 恢复 conversationHistory
            conversationHistory.clear()
            messages.takeLast(10).forEach { msg ->
                conversationHistory.add(OpenAIMessage(role = if (msg.isUser) "user" else "assistant", content = msg.text))
            }
        }
    }


    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // ============ 附件（图片） ============
    private val _pendingImageUri = MutableStateFlow<String?>(null)
    val pendingImageUri: StateFlow<String?> = _pendingImageUri.asStateFlow()

    // ============ 余额信息 ============
    private val _balance = MutableStateFlow(BalanceInfo())
    val balance: StateFlow<BalanceInfo> = _balance.asStateFlow()

    private val _balanceLoading = MutableStateFlow(false)
    val balanceLoading: StateFlow<Boolean> = _balanceLoading.asStateFlow()

    // ============ AI 模型 ============
    private val _currentModel = MutableStateFlow(Constants.OPENAI_MODEL)
    val brainModel: StateFlow<String> = _currentModel.asStateFlow()
    val availableBrainModels = Constants.AVAILABLE_MODELS

    /** 已配置API Key的模型列表（按供应商过滤） */
    val configuredBrainModels: List<String>
        get() {
            val configured = mutableListOf("auto") // auto始终可用
            val supplierModelMap = mapOf(
                "deepseek" to listOf("deepseek-chat", "deepseek-reasoner"),
                "google" to listOf("gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-1.5-flash"),
                "openai" to listOf("gpt-4o", "gpt-4o-mini"),
                "groq" to listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
                "siliconflow" to listOf("deepseek-ai/DeepSeek-V3", "Qwen/Qwen2.5-72B-Instruct"),
                "moonshot" to listOf("moonshot-v1-8k", "moonshot-v1-32k"),
                "zhipu" to listOf("glm-4-flash", "glm-4")
            )
            val keyMap = mapOf(
                "deepseek" to Constants.KEY_AI_API_KEY,
                "google" to Constants.KEY_GOOGLE_API_KEY,
                "openai" to Constants.KEY_OPENAI_API_KEY,
                "groq" to Constants.KEY_GROQ_API_KEY,
                "siliconflow" to Constants.KEY_SILICONFLOW_API_KEY,
                "moonshot" to Constants.KEY_MOONSHOT_API_KEY,
                "zhipu" to Constants.KEY_ZHIPU_API_KEY
            )
            // DeepSeek始终可用（有内置Key）
            configured.addAll(supplierModelMap["deepseek"]!!)
            // 检查其他供应商
            for ((supplier, models) in supplierModelMap) {
                if (supplier == "deepseek") continue
                val key = keyMap[supplier] ?: continue
                val savedKey = securePrefs.getString(key, "") ?: ""
                if (savedKey.isNotBlank()) {
                    configured.addAll(models)
                }
            }
            return configured
        }

    // ============ Loading ============
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ============ Agent 实时状态 ============
    private val _agentStatus = MutableStateFlow<String?>(null)
    val agentStatus: StateFlow<String?> = _agentStatus.asStateFlow()

    // ============ 思维链（兼容 ThinkingChainIndicator）============
    private val thinkingChainManager = ThinkingChainManager()
    private val _thinkingChain = MutableStateFlow<List<ThinkingChainManager.ThinkingStep>>(emptyList())
    val thinkingChain: StateFlow<List<ThinkingChainManager.ThinkingStep>> = _thinkingChain.asStateFlow()
    private val _isThinkingChainActive = MutableStateFlow(false)
    val isThinkingChainActive: StateFlow<Boolean> = _isThinkingChainActive.asStateFlow()

    // ============ 深度思考内容 ============
    private val _thinkingContent = MutableStateFlow<String?>(null)
    val thinkingContent: StateFlow<String?> = _thinkingContent.asStateFlow()

    // ============ 模式开关 ============
    private val _deepThinkingEnabled = MutableStateFlow(false)
    val deepThinkingEnabled: StateFlow<Boolean> = _deepThinkingEnabled.asStateFlow()

    private val _webSearchEnabled = MutableStateFlow(false)
    val webSearchEnabled: StateFlow<Boolean> = _webSearchEnabled.asStateFlow()

    // ============ 错误提示（Snackbar，不混入聊天流） ============
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() { _errorMessage.value = null }

    fun toggleDeepThinking() {
        _deepThinkingEnabled.value = !_deepThinkingEnabled.value
    }

    fun toggleWebSearch() {
        _webSearchEnabled.value = !_webSearchEnabled.value
    }

    // ============ 打字动画防重复 ============
    private val _animatedMessageIds = mutableSetOf<Long>()
    val animatedMessageIds: Set<Long> get() = _animatedMessageIds

    fun markMessageAnimated(msgId: Long) {
        _animatedMessageIds.add(msgId)
    }

    // ============ Conversation History ============
    private val conversationHistory = mutableListOf<OpenAIMessage>()

    init {
        viewModelScope.launch {
            val savedModel = settingsRepo.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
            _currentModel.value = savedModel
            loadMessagesFromDb()
            loadSessionList()
        }
    }

    /**
     * 从数据库加载历史对话记录（仅当前会话）
     */
    private suspend fun loadMessagesFromDb() {
        try {
            val savedMessages = chatMessageDao.getRecentMessagesBySession(currentSessionId, 50)
            if (savedMessages.isNotEmpty()) {
                val welcomeMsg = ChatMessage(
                    text = "你好！我是布老师，你的专属AI数字员工 🧠\n\n我能帮你：\n• 软件开发：写代码、改Bug、编译部署\n• 智能问答：聊天、翻译、深度分析\n• 联网搜索：实时信息、新闻、价格\n• 图片分析：识别内容、提取信息\n• 项目管理：创建、修改、迭代项目\n\n💡 有什么想法直接说，我来帮你实现",
                    isUser = false
                )
                val loadedMessages = savedMessages.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        isUser = entity.isUser,
                        imageUri = entity.imageUri,
                        reasoningContent = entity.reasoningContent,
                        timestamp = entity.timestamp
                    )
                }
                _messages.value = listOf(welcomeMsg) + loadedMessages

                // 恢复 conversationHistory（最近10轮对话）
                conversationHistory.clear()
                savedMessages.takeLast(20).forEach { entity ->
                    conversationHistory.add(OpenAIMessage(
                        role = if (entity.isUser) "user" else "assistant",
                        content = entity.text
                    ))
                }
            }
        } catch (e: Exception) {
            // 加载失败不影响使用，静默处理
        }
    }



    fun refreshBalance() {
        viewModelScope.launch(Dispatchers.IO) {
            _balanceLoading.value = true
            try {
                val savedKey = securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: ""
                val apiKey = if (savedKey.isNotBlank()) savedKey else Constants.OPENAI_API_KEY
                val response = deepSeekBalanceService.getBalance("Bearer $apiKey")
                val cnyInfo = response.balanceInfos.find { it.currency == "CNY" }
                if (cnyInfo != null) {
                    val toppedUp = cnyInfo.toppedUpBalance.toFloatOrNull() ?: 0f
                    val total = cnyInfo.totalBalance.toFloatOrNull() ?: 0f
                    val used = toppedUp - total
                    _balance.value = BalanceInfo(
                        totalBalance = "%.2f".format(total),
                        toppedUp = "%.2f".format(toppedUp),
                        used = "%.2f".format(used),
                        available = response.isAvailable,
                        currency = "CNY"
                    )
                }
            } catch (e: Exception) {
                // 静默失败
            } finally {
                _balanceLoading.value = false
            }
        }
    }

    // ============ Input & Chat ============
    fun setInput(text: String) { _inputText.value = text }

    fun switchBrainModel(model: String) {
        _currentModel.value = model
        viewModelScope.launch { settingsRepo.setString(Constants.KEY_AI_MODEL, model) }
    }

    fun setPendingImage(uri: String?) {
        _pendingImageUri.value = uri
    }

    fun removePendingImage() {
        _pendingImageUri.value = null
    }

    fun sendInput() {
        val text = _inputText.value.trim()
        val imageUri = _pendingImageUri.value
        if (text.isEmpty() && imageUri == null) return
        _inputText.value = ""
        _pendingImageUri.value = null
        _thinkingContent.value = null
        processTextInput(text, imageUri)
    }

    private fun processTextInput(input: String, imageUri: String? = null) {
        val userMsg = ChatMessage(
            text = input.ifEmpty { "[图片]" },
            isUser = true,
            imageUri = imageUri
        )
        _messages.value = (_messages.value + userMsg).takeLast(100)
        _isLoading.value = true

        // 保存用户消息到数据库
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatMessageDao.insert(ChatMessageEntity(
                    text = userMsg.text,
                    isUser = true,
                    imageUri = userMsg.imageUri,
                    timestamp = userMsg.timestamp,
                    sessionId = currentSessionId
                ))
                loadSessionList()
            } catch (_: Exception) {}
        }

        val hasImage = imageUri != null
        val deepThinking = _deepThinkingEnabled.value
        val webSearch = _webSearchEnabled.value

        // 状态提示
        if (hasImage) {
            _agentStatus.value = "🖼️ 正在分析图片..."
        } else if (deepThinking) {
            _agentStatus.value = "🧠 深度思考中..."
        } else {
            _agentStatus.value = "🧠 正在思考..."
        }

        conversationHistory.add(OpenAIMessage(role = "user", content = input.ifEmpty { "[用户发送了一张图片]" }))

        viewModelScope.launch {
            try {
                var imageBase64: String? = null
                var imageMimeType: String? = null
                if (imageUri != null) {
                    _agentStatus.value = "🖼️ 正在处理图片..."
                    val result = withContext(Dispatchers.IO) {
                        encodeImageToBase64(imageUri)
                    }
                    imageBase64 = result?.first
                    imageMimeType = result?.second
                }

                // 进度消息：懒创建，只在编译任务真正触发时才显示
                var progressMsgId: Long? = null

                val progressCallback = BuildProgressCallback { progress ->
                    viewModelScope.launch {
                        // 首次回调时才创建进度消息
                        if (progressMsgId == null) {
                            progressMsgId = System.nanoTime()
                            val newMsg = ChatMessage(
                                id = progressMsgId!!,
                                text = progress.message,
                                isUser = false,
                                taskProgress = TaskProgress(
                                    status = progress.status,
                                    message = progress.message,
                                    progress = progress.progress,
                                    downloadUrl = progress.apkUri
                                )
                            )
                            _messages.value = (_messages.value + newMsg).takeLast(100)
                        } else {
                            val updatedMsg = ChatMessage(
                                id = progressMsgId!!,
                                text = progress.message,
                                isUser = false,
                                taskProgress = TaskProgress(
                                    status = progress.status,
                                    message = progress.message,
                                    progress = progress.progress,
                                    downloadUrl = progress.apkUri
                                ),
                                timestamp = _messages.value.find { it.id == progressMsgId!! }?.timestamp ?: System.currentTimeMillis()
                            )
                            _messages.value = _messages.value.map {
                                if (it.id == progressMsgId!!) updatedMsg else it
                            }
                        }
                        when (progress.status) {
                            "pushing" -> _agentStatus.value = "📤 正在推送代码到 GitHub..."
                            "building" -> _agentStatus.value = "🔨 正在 GitHub 编译中..."
                            "downloading" -> _agentStatus.value = "📥 正在下载 APK..."
                            "completed" -> _agentStatus.value = "✅ 编译完成，正在生成回复..."
                            "failed" -> _agentStatus.value = "❌ 编译失败"
                        }
                    }
                }

                val statusCallback = fun(status: String) {
                    _agentStatus.value = status
                }

                // 深度思考回调
                val thinkingCallback = fun(content: String) {
                    _thinkingContent.value = content
                }

                val response = agentEngine.run(
                    query = input,
                    imageBase64 = imageBase64,
                    imageMimeType = imageMimeType,
                    buildProgressCallback = progressCallback,
                    statusCallback = statusCallback,
                    deepThinkingEnabled = deepThinking,
                    webSearchEnabled = webSearch,
                    thinkingCallback = thinkingCallback,
                    conversationHistory = conversationHistory.toList()
                )

                // 获取思考内容
                val reasoning = _thinkingContent.value

                // === 错误检测：识别工具返回的错误信息，转为 Snackbar 而非聊天消息 ===
                val errorPatterns = listOf(
                    "[ERROR]", "[引擎错误]",
                    "推送失败:", "触发编译失败:", "编译失败", "APK 下载失败",
                    "云端编译异常", "代码生成失败", "无法从生成结果中提取代码",
                    "无法获取编译运行状态", "错误: projectCode",
                    "Payment Required", "HTTP 402", "402"
                )
                val isError = errorPatterns.any { response.contains(it, ignoreCase = true) }

                if (isError) {
                    // HTTP 402 等特殊错误显示友好提示
                    val isPaymentError = response.contains("402", ignoreCase = true) ||
                                         response.contains("Payment Required", ignoreCase = true)
                    val errorMsg = if (isPaymentError) {
                        "服务暂时不可用，已自动切换备用模型"
                    } else {
                        response.take(200)
                    }
                    _errorMessage.value = "⚠️ $errorMsg"
                    // 错误消息不加入聊天流，直接跳过
                    return@launch
                }

                if (progressMsgId != null) {
                    val currentProgressMsg = _messages.value.find { it.id == progressMsgId }
                    val hasApk = currentProgressMsg?.taskProgress?.downloadUrl != null

                    if (hasApk) {
                        val finalMsg = ChatMessage(
                            id = progressMsgId!!,
                            text = response,
                            isUser = false,
                            taskProgress = currentProgressMsg?.taskProgress,
                            reasoningContent = reasoning,
                            timestamp = currentProgressMsg?.timestamp ?: System.currentTimeMillis()
                        )
                        _messages.value = _messages.value.map {
                            if (it.id == progressMsgId!!) finalMsg else it
                        }
                    } else {
                        // 有进度消息但没有APK（编译失败或纯代码生成），保留进度消息+追加文本回复
                        _messages.value = (_messages.value + ChatMessage(
                            text = response,
                            isUser = false,
                            reasoningContent = reasoning
                        )).takeLast(100)
                    }
                } else {
                    // 没有编译任务，直接添加回复
                    _messages.value = (_messages.value + ChatMessage(
                        text = response,
                        isUser = false,
                        reasoningContent = reasoning
                    )).takeLast(100)
                }

                // 保存AI回复到数据库
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        chatMessageDao.insert(ChatMessageEntity(
                            text = response,
                            isUser = false,
                            reasoningContent = reasoning,
                            timestamp = System.currentTimeMillis(),
                            sessionId = currentSessionId
                        ))
                    } catch (_: Exception) {}
                }

                conversationHistory.add(OpenAIMessage(role = "assistant", content = response))
                if (conversationHistory.size > 20) {
                    conversationHistory.removeAt(0)
                    conversationHistory.removeAt(0)
                }
            } catch (e: Exception) {
                // P0修复：错误消息不混入聊天流，改为 Snackbar 提示
                _errorMessage.value = "❌ AI 调用失败: ${e.message?.take(200)}"
            } finally {
                _isLoading.value = false
                _agentStatus.value = null
                _thinkingContent.value = null
            }
        }
    }

    private fun encodeImageToBase64(uriString: String): Pair<String, String>? {
        return try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            val maxDim = 1024
            val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            } else 1f

            val scaledBitmap = if (scale < 1f) {
                val w = (bitmap.width * scale).toInt()
                val h = (bitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(bitmap, w, h, true)
            } else bitmap

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val bytes = baos.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Pair(base64, "image/jpeg")
        } catch (e: Exception) {
            null
        }
    }

    /** 清空对话：删除所有历史消息（不可恢复） */
    fun clearMessages() {
        _messages.value = listOf(ChatMessage(
            text = "对话已清空，有什么可以帮你的？",
            isUser = false
        ))
        conversationHistory.clear()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatMessageDao.deleteAll()
            } catch (_: Exception) {}
        }
    }

    /** 新建对话：保留历史数据，开启新会话 */
    fun newConversation() {
        currentSessionId = System.currentTimeMillis()
        _messages.value = listOf(ChatMessage(
            text = "你好！我是布老师，你的专属AI数字员工 🧠\n\n我能帮你：\n• 软件开发：写代码、改Bug、编译部署\n• 智能问答：聊天、翻译、深度分析\n• 联网搜索：实时信息、新闻、价格\n• 图片分析：识别内容、提取信息\n• 项目管理：创建、修改、迭代项目\n\n💡 有什么想法直接说，我来帮你实现",
            isUser = false
        ))
        _conversationName.value = "新对话"
        conversationHistory.clear()
        // 刷新历史对话列表
        loadSessionList()
    }
}
