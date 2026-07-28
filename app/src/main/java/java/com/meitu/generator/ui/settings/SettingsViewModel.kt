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

    init { }

    val currentBrainModel: StateFlow<String> = settingsRepo.getStringFlow(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        .stateIn(viewModelScope, SharingStarted.Lazily, Constants.OPENAI_MODEL)

    val githubToken: StateFlow<String> = flow {
        val saved = securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: ""
        emit(saved.ifBlank { Constants.DEFAULT_GITHUB_TOKEN })
    }.stateIn(viewModelScope, SharingStarted.Lazily, Constants.DEFAULT_GITHUB_TOKEN)

    // ============ API Keys（仅保留可用平台） ============
    private val _deepseekApiKey = MutableStateFlow("")
    val deepseekApiKey: StateFlow<String> = _deepseekApiKey.asStateFlow()

    private val _openrouterApiKey = MutableStateFlow("")
    val openrouterApiKey: StateFlow<String> = _openrouterApiKey.asStateFlow()

    private val _sambanovaApiKey = MutableStateFlow("")
    val sambanovaApiKey: StateFlow<String> = _sambanovaApiKey.asStateFlow()

    init {
        refreshApiKeys()
    }

    private fun refreshApiKeys() {
        _deepseekApiKey.value = securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: ""
        _openrouterApiKey.value = securePrefs.getString(Constants.KEY_OPENROUTER_API_KEY, "") ?: ""
        _sambanovaApiKey.value = securePrefs.getString(Constants.KEY_SAMBANOVA_API_KEY, "") ?: ""
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

    fun saveDeepSeekApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_AI_API_KEY, key).apply()
        _deepseekApiKey.value = key
        _toastMessage.value = "✅ DeepSeek API Key 已保存"
    }

    fun saveOpenRouterApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_OPENROUTER_API_KEY, key).apply()
        _openrouterApiKey.value = key
        _toastMessage.value = "✅ OpenRouter API Key 已保存"
    }

    fun saveSambaNovaApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_SAMBANOVA_API_KEY, key).apply()
        _sambanovaApiKey.value = key
        _toastMessage.value = "✅ SambaNova API Key 已保存"
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
