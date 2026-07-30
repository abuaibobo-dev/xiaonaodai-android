package com.meitu.generator.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.remote.CozeApiClient
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class AgentConfig(
    val name: String,
    val botId: String,
    val prompt: String,
    val emoji: String = "🤖"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val cozeClient: CozeApiClient,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : ViewModel() {

    private val gson = Gson()

    // ============ Coze 配置 ============
    private val _cozePat = MutableStateFlow("")
    val cozePat: StateFlow<String> = _cozePat.asStateFlow()

    private val _cozeBotId = MutableStateFlow("")
    val cozeBotId: StateFlow<String> = _cozeBotId.asStateFlow()

    private val _isUsingDefaultPat = MutableStateFlow(true)
    val isUsingDefaultPat: StateFlow<Boolean> = _isUsingDefaultPat.asStateFlow()

    private val _isUsingDefaultBotId = MutableStateFlow(true)
    val isUsingDefaultBotId: StateFlow<Boolean> = _isUsingDefaultBotId.asStateFlow()

    // ============ Token 消耗统计（分通道） ============
    // Coze 通道
    private val _cozeTokens = MutableStateFlow(0)
    val cozeTokens: StateFlow<Int> = _cozeTokens.asStateFlow()
    private val _cozeInputTokens = MutableStateFlow(0)
    val cozeInputTokens: StateFlow<Int> = _cozeInputTokens.asStateFlow()
    private val _cozeOutputTokens = MutableStateFlow(0)
    val cozeOutputTokens: StateFlow<Int> = _cozeOutputTokens.asStateFlow()
    private val _cozeMessages = MutableStateFlow(0)
    val cozeMessages: StateFlow<Int> = _cozeMessages.asStateFlow()

    // DeepSeek 通道
    private val _dsTokens = MutableStateFlow(0)
    val dsTokens: StateFlow<Int> = _dsTokens.asStateFlow()
    private val _dsInputTokens = MutableStateFlow(0)
    val dsInputTokens: StateFlow<Int> = _dsInputTokens.asStateFlow()
    private val _dsOutputTokens = MutableStateFlow(0)
    val dsOutputTokens: StateFlow<Int> = _dsOutputTokens.asStateFlow()
    private val _dsMessages = MutableStateFlow(0)
    val dsMessages: StateFlow<Int> = _dsMessages.asStateFlow()

    // 合计
    private val _totalTokens = MutableStateFlow(0)
    val totalTokens: StateFlow<Int> = _totalTokens.asStateFlow()

    // ============ AI 通道 ============
    private val _currentChannel = MutableStateFlow(Constants.CHANNEL_COZE)
    val currentChannel: StateFlow<String> = _currentChannel.asStateFlow()

    private val _deepseekApiKey = MutableStateFlow("")
    val deepseekApiKey: StateFlow<String> = _deepseekApiKey.asStateFlow()

    // ============ DeepSeek 模型选择 ============
    private val _deepseekModel = MutableStateFlow("deepseek-v4-flash")
    val deepseekModel: StateFlow<String> = _deepseekModel.asStateFlow()

    // ============ DeepSeek 余额 ============
    private val _deepseekBalance = MutableStateFlow<String?>(null)
    val deepseekBalance: StateFlow<String?> = _deepseekBalance.asStateFlow()
    private val _isLoadingBalance = MutableStateFlow(false)
    val isLoadingBalance: StateFlow<Boolean> = _isLoadingBalance.asStateFlow()

    // ============ Agent 管理 ============
    private val _agentList = MutableStateFlow<List<AgentConfig>>(emptyList())
    val agentList: StateFlow<List<AgentConfig>> = _agentList.asStateFlow()

    private val _currentAgentId = MutableStateFlow("")
    val currentAgentId: StateFlow<String> = _currentAgentId.asStateFlow()

    private val _isCreatingAgent = MutableStateFlow(false)
    val isCreatingAgent: StateFlow<Boolean> = _isCreatingAgent.asStateFlow()

    // ============ Toast ============
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    companion object {
        private const val KEY_AGENTS = "agent_list"
        private const val KEY_CURRENT_AGENT = "current_agent_id"
    }

    init {
        loadConfig()
        loadTokenUsage()
        loadAgents()
        loadChannelAndDeepseek()
    }

    private fun loadChannelAndDeepseek() {
        val channel = securePrefs.getString(Constants.KEY_AI_CHANNEL, Constants.CHANNEL_COZE) ?: Constants.CHANNEL_COZE
        _currentChannel.value = channel
        val dsKey = securePrefs.getString(Constants.KEY_DEEPSEEK_API_KEY, "") ?: ""
        _deepseekApiKey.value = dsKey
        val dsModel = securePrefs.getString(Constants.KEY_DEEPSEEK_MODEL, "deepseek-v4-flash") ?: "deepseek-v4-flash"
        _deepseekModel.value = dsModel
    }

    fun switchChannel(channel: String) {
        securePrefs.edit().putString(Constants.KEY_AI_CHANNEL, channel).apply()
        _currentChannel.value = channel
        cozeClient.currentBotId = if (channel == Constants.CHANNEL_DEEPSEEK) {
            "" // DeepSeek 模式不需要 botId
        } else {
            // 恢复 Coze botId
            val savedBotId = securePrefs.getString("current_agent_id", "") ?: ""
            if (savedBotId.isNotBlank()) savedBotId
            else {
                // 从 settingsRepo 异步读取
                viewModelScope.launch {
                    val botId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
                    cozeClient.currentBotId = botId.ifBlank { Constants.DEFAULT_COZE_BOT_ID }
                    _cozeBotId.value = cozeClient.currentBotId
                }
                _cozeBotId.value
            }
        }
        _toastMessage.value = if (channel == Constants.CHANNEL_DEEPSEEK) "✅ 已切换到 DeepSeek" else "✅ 已切换到 Coze"
    }

    fun saveDeepseekApiKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_DEEPSEEK_API_KEY, key).apply()
        _deepseekApiKey.value = key
        _toastMessage.value = if (key.isNotBlank()) "✅ DeepSeek API Key 已保存" else "✅ 已清除 DeepSeek API Key"
    }

    fun saveDeepseekModel(model: String) {
        securePrefs.edit().putString(Constants.KEY_DEEPSEEK_MODEL, model).apply()
        _deepseekModel.value = model
        _toastMessage.value = "✅ 已切换模型: $model"
    }

    fun queryDeepseekBalance() {
        val apiKey = _deepseekApiKey.value
        if (apiKey.isBlank()) {
            _toastMessage.value = "请先配置 DeepSeek API Key"
            return
        }
        _isLoadingBalance.value = true
        viewModelScope.launch {
            try {
                val url = java.net.URL("https://api.deepseek.com/user/balance")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = com.google.gson.JsonParser.parseString(response).asJsonObject
                val balance = json.get("balance")?.asString
                val toppedUp = json.get("topped_up_balance")?.asString
                _deepseekBalance.value = if (balance != null) "¥$balance" else "查询失败"
            } catch (e: Exception) {
                _deepseekBalance.value = "查询失败: ${e.message}"
            } finally {
                _isLoadingBalance.value = false
            }
        }
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
        // Coze 通道
        val cozeTotal = securePrefs.getInt("coze_total_tokens", 0)
        val cozeInput = securePrefs.getInt("coze_total_input", 0)
        val cozeOutput = securePrefs.getInt("coze_total_output", 0)
        val cozeMsgs = securePrefs.getInt("coze_total_messages", 0)
        _cozeTokens.value = cozeTotal
        _cozeInputTokens.value = cozeInput
        _cozeOutputTokens.value = cozeOutput
        _cozeMessages.value = cozeMsgs

        // DeepSeek 通道
        val dsTotal = securePrefs.getInt("ds_total_tokens", 0)
        val dsInput = securePrefs.getInt("ds_total_input", 0)
        val dsOutput = securePrefs.getInt("ds_total_output", 0)
        val dsMsgs = securePrefs.getInt("ds_total_messages", 0)
        _dsTokens.value = dsTotal
        _dsInputTokens.value = dsInput
        _dsOutputTokens.value = dsOutput
        _dsMessages.value = dsMsgs

        // 合计
        _totalTokens.value = cozeTotal + dsTotal
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

    // ============ Agent 管理 ============

    private fun loadAgents() {
        val json = securePrefs.getString(KEY_AGENTS, "[]") ?: "[]"
        val type = object : TypeToken<List<AgentConfig>>() {}.type
        _agentList.value = try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
        _currentAgentId.value = securePrefs.getString(KEY_CURRENT_AGENT, "") ?: ""
    }

    private fun saveAgents(agents: List<AgentConfig>) {
        securePrefs.edit().putString(KEY_AGENTS, gson.toJson(agents)).apply()
        _agentList.value = agents
    }

    fun createAgent(name: String, prompt: String, emoji: String) {
        if (name.isBlank() || prompt.isBlank()) {
            _toastMessage.value = "⚠️ 名称和提示词不能为空"
            return
        }
        viewModelScope.launch {
            _isCreatingAgent.value = true
            _toastMessage.value = "⏳ 正在创建 Agent..."

            // 1. 通过 Coze API 创建 Bot
            val createResult = cozeClient.createBot(name, prompt)
            if (createResult.isFailure) {
                _toastMessage.value = "❌ 创建失败: ${createResult.exceptionOrNull()?.message}"
                _isCreatingAgent.value = false
                return@launch
            }
            val botId = createResult.getOrNull()!!

            // 2. 发布 Bot 到 API 渠道
            val publishResult = cozeClient.publishBot(botId)
            if (publishResult.isFailure) {
                _toastMessage.value = "❌ 发布失败: ${publishResult.exceptionOrNull()?.message}"
                _isCreatingAgent.value = false
                return@launch
            }

            // 3. 保存到本地
            val agent = AgentConfig(name = name, botId = botId, prompt = prompt, emoji = emoji)
            val currentList = _agentList.value.toMutableList()
            currentList.add(agent)
            saveAgents(currentList)

            // 4. 设为当前 Agent
            securePrefs.edit().putString(KEY_CURRENT_AGENT, botId).apply()
            _currentAgentId.value = botId
            cozeClient.currentBotId = botId
            settingsRepo.setString(Constants.KEY_COZE_BOT_ID, botId)
            _cozeBotId.value = botId
            _isUsingDefaultBotId.value = false

            _toastMessage.value = "✅ Agent「$name」创建成功"
            _isCreatingAgent.value = false
        }
    }

    fun switchAgent(agent: AgentConfig) {
        cozeClient.currentBotId = agent.botId
        viewModelScope.launch {
            settingsRepo.setString(Constants.KEY_COZE_BOT_ID, agent.botId)
        }
        securePrefs.edit().putString(KEY_CURRENT_AGENT, agent.botId).apply()
        _currentAgentId.value = agent.botId
        _cozeBotId.value = agent.botId
        _isUsingDefaultBotId.value = false
        _toastMessage.value = "✅ 已切换到「${agent.name}」"
    }

    fun deleteAgent(agent: AgentConfig) {
        val currentList = _agentList.value.toMutableList()
        currentList.removeAll { it.botId == agent.botId }
        saveAgents(currentList)

        // 如果删除的是当前Agent，切回默认
        if (_currentAgentId.value == agent.botId) {
            val defaultBotId = Constants.DEFAULT_COZE_BOT_ID
            cozeClient.currentBotId = defaultBotId
            viewModelScope.launch {
                settingsRepo.setString(Constants.KEY_COZE_BOT_ID, defaultBotId)
            }
            securePrefs.edit().putString(KEY_CURRENT_AGENT, "").apply()
            _currentAgentId.value = ""
            _cozeBotId.value = defaultBotId
        }
        _toastMessage.value = "✅ 已删除「${agent.name}」"
    }

    fun clearToast() { _toastMessage.value = null }
}
