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

    // ============ Toast ============
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        _cozePat.value = securePrefs.getString(Constants.KEY_COZE_PAT, "") ?: ""
        viewModelScope.launch {
            _cozeBotId.value = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
        }
    }

    fun saveCozePat(pat: String) {
        securePrefs.edit().putString(Constants.KEY_COZE_PAT, pat).apply()
        _cozePat.value = pat
        _toastMessage.value = if (pat.isNotBlank()) "✅ PAT 已保存" else "✅ 已清除 PAT"
    }

    fun saveCozeBotId(botId: String) {
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_COZE_BOT_ID, botId)
            _cozeBotId.value = botId
            _toastMessage.value = if (botId.isNotBlank()) "✅ Bot ID 已保存" else "✅ 已清除 Bot ID"
        }
    }

    fun clearToast() { _toastMessage.value = null }
}
