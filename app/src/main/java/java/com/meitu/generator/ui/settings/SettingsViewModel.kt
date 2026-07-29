package com.meitu.generator.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.dao.LogDao
import com.meitu.generator.data.local.dao.TaskDao
import com.meitu.generator.data.security.PrivacyModeManager
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepo: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences,
    private val taskDao: TaskDao,
    private val logDao: LogDao,
    private val privacyModeManager: PrivacyModeManager
) : AndroidViewModel(application) {

    val currentBrainModel: StateFlow<String> = settingsRepo.getStringFlow(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.OPENAI_MODEL)

    val githubToken: StateFlow<String> = flow {
        val saved = securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: ""
        emit(saved.ifBlank { Constants.DEFAULT_GITHUB_TOKEN })
    }.stateIn(viewModelScope, SharingStarted.Lazily, Constants.DEFAULT_GITHUB_TOKEN)

    // ============ API Keys ============
    private val _deepseekApiKey = MutableStateFlow("")
    val deepseekApiKey: StateFlow<String> = _deepseekApiKey.asStateFlow()

    private val _googleApiKey = MutableStateFlow("")
    val googleApiKey: StateFlow<String> = _googleApiKey.asStateFlow()

    private val _openaiApiKey = MutableStateFlow("")
    val openaiApiKey: StateFlow<String> = _openaiApiKey.asStateFlow()

    private val _groqApiKey = MutableStateFlow("")
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    private val _siliconflowApiKey = MutableStateFlow("")
    val siliconflowApiKey: StateFlow<String> = _siliconflowApiKey.asStateFlow()

    private val _moonshotApiKey = MutableStateFlow("")
    val moonshotApiKey: StateFlow<String> = _moonshotApiKey.asStateFlow()

    private val _zhipuApiKey = MutableStateFlow("")
    val zhipuApiKey: StateFlow<String> = _zhipuApiKey.asStateFlow()

    // ============ GitHub 仓库配置 ============
    private val _githubRepoOwner = MutableStateFlow("")
    val githubRepoOwner: StateFlow<String> = _githubRepoOwner.asStateFlow()

    private val _githubRepoName = MutableStateFlow("")
    val githubRepoName: StateFlow<String> = _githubRepoName.asStateFlow()

    private val _githubWorkflowId = MutableStateFlow("")
    val githubWorkflowId: StateFlow<String> = _githubWorkflowId.asStateFlow()

    private val _githubUserWorkflowId = MutableStateFlow("")
    val githubUserWorkflowId: StateFlow<String> = _githubUserWorkflowId.asStateFlow()

    init {
        refreshApiKeys()
        refreshGithubConfig()
    }

    private fun refreshApiKeys() {
        _deepseekApiKey.value = securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: ""
        _googleApiKey.value = securePrefs.getString(Constants.KEY_GOOGLE_API_KEY, "") ?: ""
        _openaiApiKey.value = securePrefs.getString(Constants.KEY_OPENAI_API_KEY, "") ?: ""
        _groqApiKey.value = securePrefs.getString(Constants.KEY_GROQ_API_KEY, "") ?: ""
        _siliconflowApiKey.value = securePrefs.getString(Constants.KEY_SILICONFLOW_API_KEY, "") ?: ""
        _moonshotApiKey.value = securePrefs.getString(Constants.KEY_MOONSHOT_API_KEY, "") ?: ""
        _zhipuApiKey.value = securePrefs.getString(Constants.KEY_ZHIPU_API_KEY, "") ?: ""
    }

    private val _showClearConfirm = MutableStateFlow(false)
    val showClearConfirm: StateFlow<Boolean> = _showClearConfirm.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val privacyModeEnabled: StateFlow<Boolean> = privacyModeManager.privacyModeEnabled

    fun setBrainModel(model: String) {
        viewModelScope.launch { settingsRepo.setString(Constants.KEY_AI_MODEL, model) }
    }

    fun saveGithubToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_GITHUB_TOKEN, token).apply()
    }

    // ============ GitHub 仓库配置 ============
    private fun refreshGithubConfig() {
        viewModelScope.launch {
            _githubRepoOwner.value = settingsRepo.getString(Constants.KEY_GITHUB_REPO_OWNER, "")
            _githubRepoName.value = settingsRepo.getString(Constants.KEY_GITHUB_REPO_NAME, "")
            _githubWorkflowId.value = settingsRepo.getString(Constants.KEY_GITHUB_WORKFLOW_ID, "")
            _githubUserWorkflowId.value = settingsRepo.getString(Constants.KEY_GITHUB_USER_WORKFLOW_ID, "")
        }
    }

    fun saveGithubRepoOwner(owner: String) {
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_GITHUB_REPO_OWNER, owner)
            _githubRepoOwner.value = owner
            _toastMessage.value = if (owner.isNotBlank()) "✅ 仓库 owner 已保存" else "✅ 已恢复默认"
        }
    }

    fun saveGithubRepoName(name: String) {
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_GITHUB_REPO_NAME, name)
            _githubRepoName.value = name
            _toastMessage.value = if (name.isNotBlank()) "✅ 仓库名已保存" else "✅ 已恢复默认"
        }
    }

    fun saveGithubWorkflowId(id: String) {
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_GITHUB_WORKFLOW_ID, id)
            _githubWorkflowId.value = id
            _toastMessage.value = if (id.isNotBlank()) "✅ 主App workflow 已保存" else "✅ 已恢复默认"
        }
    }

    fun saveGithubUserWorkflowId(id: String) {
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_GITHUB_USER_WORKFLOW_ID, id)
            _githubUserWorkflowId.value = id
            _toastMessage.value = if (id.isNotBlank()) "✅ 用户项目 workflow 已保存" else "✅ 已恢复默认"
        }
    }

    fun saveDeepSeekApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_AI_API_KEY, key).apply()
        _deepseekApiKey.value = key
        _toastMessage.value = "✅ DeepSeek API Key 已保存"
    }

    fun saveGoogleApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_GOOGLE_API_KEY, key).apply()
        _googleApiKey.value = key
        _toastMessage.value = "✅ Google AI API Key 已保存"
    }

    fun saveOpenAIApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_OPENAI_API_KEY, key).apply()
        _openaiApiKey.value = key
        _toastMessage.value = "✅ OpenAI API Key 已保存"
    }

    fun saveGroqApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_GROQ_API_KEY, key).apply()
        _groqApiKey.value = key
        _toastMessage.value = "✅ Groq API Key 已保存"
    }

    fun saveSiliconFlowApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_SILICONFLOW_API_KEY, key).apply()
        _siliconflowApiKey.value = key
        _toastMessage.value = "✅ 硅基流动 API Key 已保存"
    }

    fun saveMoonshotApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_MOONSHOT_API_KEY, key).apply()
        _moonshotApiKey.value = key
        _toastMessage.value = "✅ Moonshot API Key 已保存"
    }

    fun saveZhipuApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_ZHIPU_API_KEY, key).apply()
        _zhipuApiKey.value = key
        _toastMessage.value = "✅ 智谱AI API Key 已保存"
    }

    fun clearCache() {
        viewModelScope.launch {
            logDao.trimLogs()
            _toastMessage.value = "缓存已清理"
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                settingsRepo.deleteSetting("chat_history_cache")
            } catch (_: Exception) {}
            _toastMessage.value = "对话历史已清空"
        }
    }

    fun showClearAllConfirm() { _showClearConfirm.value = true }
    fun dismissClearAll() { _showClearConfirm.value = false }

    fun clearAllData() {
        viewModelScope.launch {
            settingsRepo.initDefaults()
            _showClearConfirm.value = false
            _toastMessage.value = "所有数据已清空"
            refreshApiKeys()
        }
    }

    fun togglePrivacyMode() {
        privacyModeManager.setPrivacyMode(!privacyModeManager.privacyModeEnabled.value)
    }

    fun clearToast() { _toastMessage.value = null }
}
