package com.meitu.generator.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meitu.generator.data.local.dao.SettingsDao
import com.meitu.generator.data.local.entity.SettingEntity
import com.meitu.generator.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun getInt(key: String, default: Int): Int {
        return settingsDao.getSetting(key)?.value?.toIntOrNull() ?: default
    }

    suspend fun getString(key: String, default: String = ""): String {
        return settingsDao.getSetting(key)?.value ?: default
    }

    suspend fun getBoolean(key: String, default: Boolean): Boolean {
        return settingsDao.getSetting(key)?.value?.toBooleanStrictOrNull() ?: default
    }

    suspend fun setInt(key: String, value: Int) {
        settingsDao.upsert(SettingEntity(key, value.toString()))
    }

    suspend fun setString(key: String, value: String) {
        settingsDao.upsert(SettingEntity(key, value))
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        settingsDao.upsert(SettingEntity(key, value.toString()))
    }

    fun getIntFlow(key: String, default: Int): Flow<Int> {
        return settingsDao.getAllSettings().map { list ->
            list.find { it.key == key }?.value?.toIntOrNull() ?: default
        }
    }

    fun getBooleanFlow(key: String, default: Boolean): Flow<Boolean> {
        return settingsDao.getAllSettings().map { list ->
            list.find { it.key == key }?.value?.toBooleanStrictOrNull() ?: default
        }
    }

    fun getStringFlow(key: String, default: String = ""): Flow<String> {
        return settingsDao.getAllSettings().map { list ->
            list.find { it.key == key }?.value ?: default
        }
    }

    // Secure storage for API keys
    fun saveApiKey(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }

    fun getApiKey(key: String): String {
        return securePrefs.getString(key, "") ?: ""
    }

    suspend fun initDefaults() {
        val defaults = mapOf(
            Constants.KEY_AI_MODEL to Constants.OPENAI_MODEL
        )
        defaults.forEach { (key, value) ->
            if (settingsDao.getSetting(key) == null) {
                settingsDao.upsert(SettingEntity(key, value))
            }
        }
    }
}
