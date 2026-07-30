package com.meitu.generator.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : ViewModel() {

    // ============ Coze 配置 ============
    private val _cozePat = MutableStateFlow("")
    val cozePat: StateFlow<String> = _cozePat.asStateFlow()

    private val _cozeBotId = MutableStateFlow("")
    val cozeBotId: StateFlow<String> = _cozeBotId.asStateFlow()

    private val _isUsingDefaultPat = MutableStateFlow(true)
    val isUsingDefaultPat: StateFlow<Boolean> = _isUsingDefaultPat.asStateFlow()

    private val _isUsingDefaultBotId = MutableStateFlow(true)
    val isUsingDefaultBotId: StateFlow<Boolean> = _isUsingDefaultBotId.asStateFlow()()

    // ============ Token 消耗统计 ============
    private val _totalTokens = MutableStateFlow(0)
    val totalTokens: StateFlow<Int> = _totalTokens.asStateFlow()

    private val _totalInputTokens = MutableStateFlow(0)
    val totalInputTokens: StateFlow<Int> = _totalInputTokens.asStateFlow()

    private val _totalOutputTokens = MutableStateFlow(0)
    val totalOutputTokens: StateFlow<Int> = _totalOutputTokens.asStateFlow()

    private val _totalMessages = MutableStateFlow(0)
    val totalMessages: StateFlow<Int> = _totalMessages.asStateFlow()

    // ============ Toast ============
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadConfig()
        loadTokenUsage()
    }

    private fun loadConfig() {
        val savedPat = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
        if (savedPat.isNotBlank()) {
            _cozePat.value = savedPat
            _isUsingDefaultPat.value = false
        } else {
            _cozePat.value = Constants.DEFAULT_COZE_PAT
            _isUsingDefaultPat.value = true
        }
        viewModelScope.launch {
            val savedBotId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
            if (savedBotId.isNotBlank()) {
                _cozeBotId.value = savedBotId
                _isUsingDefaultBotId.value = false
            } else {
                _cozeBotId.value = Constants.DEFAULT_COZE_BOT_ID
                _isUsingDefaultBotId.value = true
            }
        }
    }

    private fun loadTokenUsage() {
        _totalTokens.value = securePrefs.getInt("total_tokens_consumed", 0)
        _totalInputTokens.value = securePrefs.getInt("total_input_tokens", 0)
        _totalOutputTokens.value = securePrefs.getInt("total_output_tokens", 0)
        _totalMessages.value = securePrefs.getInt("total_ai_messages", 0)
    }

    fun refreshTokenUsage() {
        loadTokenUsage()
    }

    fun saveCozePat(pat: String) {
        securePrefs.edit().putString(Constants.KEY_COZE_PAT, pat).apply()
        _cozePat.value = pat
        _isUsingDefaultPat.value = false
        _toastMessage.value = if (pat.isNotBlank()) "✅ PAT 已保存" else "✅ 已恢复默认 PAT"
        if (pat.isBlank()) {
            _cozePat.value = Constants.DEFAULT_COZE_PAT
            _isUsingDefaultPat.value = true
        }
    }

    fun saveCozeBotId(botId: String) {
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_COZE_BOT_ID, botId)
            _cozeBotId.value = botId
            _isUsingDefaultBotId.value = false
            _toastMessage.value = if (botId.isNotBlank()) "✅ Bot ID 已保存" else "✅ 已恢复默认 Bot ID"
            if (botId.isBlank()) {
                _cozeBotId.value = Constants.DEFAULT_COZE_BOT_ID
                _isUsingDefaultBotId.value = true
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }
}
