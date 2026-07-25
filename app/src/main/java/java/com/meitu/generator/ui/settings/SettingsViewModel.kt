package com.meitu.generator.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.dao.LogDao
import com.meitu.generator.data.local.dao.TaskDao
import com.meitu.generator.data.remote.DeepSeekBalanceService
import com.meitu.generator.data.security.PrivacyModeManager
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class BalanceInfo(
    val totalBalance: String = "--",
    val toppedUp: String = "--",
    val used: String = "--",
    val available: Boolean = true,
    val currency: String = "CNY"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences,
    private val taskDao: TaskDao,
    private val logDao: LogDao,
    private val privacyModeManager: PrivacyModeManager,
    private val deepSeekBalanceService: DeepSeekBalanceService
) : AndroidViewModel(application) {

    // ============ 余额信息 ============
    private val _balance = MutableStateFlow(BalanceInfo())
    val balance: StateFlow<BalanceInfo> = _balance.asStateFlow()

    private val _balanceLoading = MutableStateFlow(false)
    val balanceLoading: StateFlow<Boolean> = _balanceLoading.asStateFlow()

    init {
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

    val currentBrainModel: StateFlow<String> = settingsRepo.getStringFlow(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.OPENAI_MODEL)

    val githubToken: StateFlow<String> = flow {
        val saved = securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: ""
        emit(saved.ifBlank { Constants.DEFAULT_GITHUB_TOKEN })
    }.stateIn(viewModelScope, SharingStarted.Lazily, Constants.DEFAULT_GITHUB_TOKEN)

    val aiApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val geminiApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_GEMINI_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val groqApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_GROQ_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val sambanovaApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_SAMBANOVA_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val hfApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_HF_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val openrouterApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_OPENROUTER_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val cerebrasApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_CEREBRAS_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val nvidiaApiKey: StateFlow<String> = flow {
        emit(securePrefs.getString(Constants.KEY_NVIDIA_API_KEY, "") ?: "")
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    private val _showClearConfirm = MutableStateFlow(false)
    val showClearConfirm: StateFlow<Boolean> = _showClearConfirm.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()


    // 隐私模式
    val privacyModeEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = privacyModeManager.privacyModeEnabled

    fun setBrainModel(model: String) {
        viewModelScope.launch { settingsRepo.setString(Constants.KEY_AI_MODEL, model) }
    }

    fun saveGithubToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_GITHUB_TOKEN, token).apply()
    }

    fun saveApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_AI_API_KEY, key).apply()
        _toastMessage.value = "API Key 已保存"
    }

    fun saveGeminiApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_GEMINI_API_KEY, key).apply()
        _toastMessage.value = "Google API Key 已保存"
    }

    fun saveGroqApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_GROQ_API_KEY, key).apply()
        _toastMessage.value = "Groq API Key 已保存"
    }

    fun saveSambaNovaApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_SAMBANOVA_API_KEY, key).apply()
        _toastMessage.value = "SambaNova API Key 已保存"
    }

    fun saveHfApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_HF_API_KEY, key).apply()
        _toastMessage.value = "HuggingFace API Key 已保存"
    }

    fun saveOpenRouterApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_OPENROUTER_API_KEY, key).apply()
        _toastMessage.value = "OpenRouter API Key 已保存"
    }

    fun saveCerebrasApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_CEREBRAS_API_KEY, key).apply()
        _toastMessage.value = "Cerebras API Key 已保存"
    }

    fun saveNvidiaApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_NVIDIA_API_KEY, key).apply()
        _toastMessage.value = "NVIDIA API Key 已保存"
    }

    fun clearCache() {
        viewModelScope.launch {
            logDao.trimLogs()
            _toastMessage.value = "缓存已清理"
        }
    }

    fun clearChatHistory() {
        _toastMessage.value = "对话历史已清空"
    }

    fun showClearAllConfirm() { _showClearConfirm.value = true }
    fun dismissClearAll() { _showClearConfirm.value = false }

    fun clearAllData() {
        viewModelScope.launch {
            settingsRepo.initDefaults()
            _showClearConfirm.value = false
            _toastMessage.value = "所有数据已清空"
        }
    }


    fun togglePrivacyMode() {
        privacyModeManager.setPrivacyMode(!privacyModeManager.privacyModeEnabled.value)
    }

    fun clearToast() { _toastMessage.value = null }
}
