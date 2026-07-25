package com.meitu.generator.data.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 隐私模式管理器
 * 控制是否在本地处理敏感内容
 */
@Singleton
class PrivacyModeManager @Inject constructor(
    @Named("securePrefs") private val securePrefs: SharedPreferences
) {
    companion object {
        private const val KEY_PRIVACY_MODE = "privacy_mode_enabled"
    }

    private val _privacyModeEnabled = MutableStateFlow(
        securePrefs.getBoolean(KEY_PRIVACY_MODE, false)
    )
    val privacyModeEnabled: StateFlow<Boolean> = _privacyModeEnabled.asStateFlow()

    fun setPrivacyMode(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
        _privacyModeEnabled.value = enabled
    }
}
