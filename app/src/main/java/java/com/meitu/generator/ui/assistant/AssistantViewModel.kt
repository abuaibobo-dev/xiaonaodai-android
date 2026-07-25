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

data class BalanceInfo(
    val totalBalance: String = "--",
    val toppedUp: String = "--",
    val used: String = "--",
    val available: Boolean = true,
    val currency: String = "CNY"
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    private val openAIService: OpenAIService,
    private val agentEngine: AgentEngine,
    private val deepSeekBalanceService: DeepSeekBalanceService,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : AndroidViewModel(application) {

    // ============ Chat Messages ============
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(
            text = "你好！我是小脑袋 AI 智能体 🧠\n\n我可以帮你：\n• 聊天问答、写代码、翻译\n• 深度思考复杂问题\n• 联网搜索最新信息\n• 分析你发来的图片\n• 生成和修改项目\n• 云端编译 APK\n\n💡 试试输入框下方的模式切换！",
            isUser = false
        ))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // ============ 附件（图片） ============
    private val _pendingImageUri = MutableStateFlow<String?>(null)
    val pendingImageUri: StateFlow<String?> = _pendingImageUri.asStateFlow()

    // ============ AI 模型 ============
    private val _currentModel = MutableStateFlow(Constants.OPENAI_MODEL)
    val brainModel: StateFlow<String> = _currentModel.asStateFlow()
    val availableBrainModels = Constants.AVAILABLE_MODELS

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

    // ============ 余额信息 ============
    private val _balance = MutableStateFlow(BalanceInfo())
    val balance: StateFlow<BalanceInfo> = _balance.asStateFlow()

    private val _balanceLoading = MutableStateFlow(false)
    val balanceLoading: StateFlow<Boolean> = _balanceLoading.asStateFlow()

    // ============ Conversation History ============
    private val conversationHistory = mutableListOf<OpenAIMessage>()

    init {
        viewModelScope.launch {
            val savedModel = settingsRepo.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
            _currentModel.value = savedModel
        }
        refreshBalance()
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
                    thinkingCallback = thinkingCallback
                )

                // 获取思考内容
                val reasoning = _thinkingContent.value

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

                conversationHistory.add(OpenAIMessage(role = "assistant", content = response))
                if (conversationHistory.size > 20) {
                    conversationHistory.removeAt(0)
                    conversationHistory.removeAt(0)
                }
            } catch (e: Exception) {
                _messages.value = (_messages.value + ChatMessage(
                    text = "❌ AI 调用失败: ${e.message?.take(200)}",
                    isUser = false
                )).takeLast(100)
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

    fun clearMessages() {
        _messages.value = listOf(ChatMessage(
            text = "对话已清空，有什么可以帮你的？",
            isUser = false
        ))
        conversationHistory.clear()
    }
}
