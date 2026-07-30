package com.meitu.generator.ui.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.remote.CozeApiClient
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // ============ Token 消耗统计（动态 Map） ============
    data class ChannelTokenStats(
        val total: Int = 0,
        val input: Int = 0,
        val output: Int = 0,
        val messages: Int = 0
    )

    private val _channelTokenStats = MutableStateFlow<Map<String, ChannelTokenStats>>(emptyMap())
    val channelTokenStats: StateFlow<Map<String, ChannelTokenStats>> = _channelTokenStats.asStateFlow()

    private val _totalTokens = MutableStateFlow(0)
    val totalTokens: StateFlow<Int> = _totalTokens.asStateFlow()

    /** 获取通道的显示名称 */
    fun getChannelDisplayName(channelKey: String): String {
        return when (channelKey) {
            "coze" -> "🧠 Coze"
            "deepseek" -> "🔍 DeepSeek"
            else -> {
                val id = channelKey.removePrefix("custom:")
                _customApiList.value.find { it.id == id }?.let { "${it.emoji} ${it.name}" }
                    ?: channelKey
            }
        }
    }

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

    // ============ 自定义 API 通道 ============
    private val _customApiList = MutableStateFlow<List<CustomApiConfig>>(emptyList())
    val customApiList: StateFlow<List<CustomApiConfig>> = _customApiList.asStateFlow()

    private fun loadCustomApiList() {
        val json = securePrefs.getString(Constants.KEY_CUSTOM_API_LIST, "[]") ?: "[]"
        val type = object : TypeToken<List<CustomApiConfig>>() {}.type
        _customApiList.value = try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
    }

    private fun saveCustomApiList(list: List<CustomApiConfig>) {
        securePrefs.edit().putString(Constants.KEY_CUSTOM_API_LIST, gson.toJson(list)).apply()
        _customApiList.value = list
    }

    fun addCustomApi(config: CustomApiConfig) {
        val list = _customApiList.value.toMutableList()
        list.add(config)
        saveCustomApiList(list)
        _toastMessage.value = "✅ 已添加自定义 API「${config.name}」"
    }

    fun updateCustomApi(config: CustomApiConfig) {
        val list = _customApiList.value.toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) {
            list[idx] = config
            saveCustomApiList(list)
            _toastMessage.value = "✅ 已更新「${config.name}」"
        }
    }

    fun deleteCustomApi(id: String) {
        val list = _customApiList.value.toMutableList()
        list.removeAll { it.id == id }
        saveCustomApiList(list)
        val channel = _currentChannel.value
        if (channel == "${Constants.CHANNEL_CUSTOM_PREFIX}$id") {
            switchChannel(Constants.CHANNEL_COZE)
        }
        _toastMessage.value = "✅ 已删除"
    }

    fun getCustomApiById(id: String): CustomApiConfig? {
        return _customApiList.value.find { it.id == id }
    }

    // ============ GitHub 关联 ============
    data class GitHubUser(
        val login: String,
        val avatarUrl: String,
        val name: String,
        val publicRepos: Int,
        val followers: Int
    )

    data class GitHubRepo(
        val name: String,
        val fullName: String,
        val description: String,
        val language: String?,
        val stars: Int,
        val forks: Int,
        val updatedAt: String
    )

    data class GitHubCommit(
        val sha: String,
        val message: String,
        val author: String,
        val date: String
    )

    data class GitHubNotification(
        val title: String,
        val repoName: String,
        val type: String,
        val url: String,
        val updatedAt: String
    )

    private val _githubToken = MutableStateFlow("")
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _githubUser = MutableStateFlow<GitHubUser?>(null)
    val githubUser: StateFlow<GitHubUser?> = _githubUser.asStateFlow()

    private val _githubRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val githubRepos: StateFlow<List<GitHubRepo>> = _githubRepos.asStateFlow()

    private val _githubNotifications = MutableStateFlow<List<GitHubNotification>>(emptyList())
    val githubNotifications: StateFlow<List<GitHubNotification>> = _githubNotifications.asStateFlow()

    private val _selectedRepoCommits = MutableStateFlow<List<GitHubCommit>>(emptyList())
    val selectedRepoCommits: StateFlow<List<GitHubCommit>> = _selectedRepoCommits.asStateFlow()

    private val _isLoadingGitHub = MutableStateFlow(false)
    val isLoadingGitHub: StateFlow<Boolean> = _isLoadingGitHub.asStateFlow()

    private val _githubError = MutableStateFlow<String?>(null)
    val githubError: StateFlow<String?> = _githubError.asStateFlow()

    private fun loadGitHubToken() {
        _githubToken.value = securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: ""
    }

    fun saveGitHubToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_GITHUB_TOKEN, token).apply()
        _githubToken.value = token
        if (token.isNotBlank()) {
            refreshGitHubData()
        } else {
            _githubUser.value = null
            _githubRepos.value = emptyList()
            _githubNotifications.value = emptyList()
        }
    }

    fun refreshGitHubData() {
        val token = _githubToken.value
        if (token.isBlank()) return
        _isLoadingGitHub.value = true
        _githubError.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 获取用户信息
                    val userResp = gitHubApiGet("/user", token)
                    val userJson = JsonParser.parseString(userResp).asJsonObject
                    _githubUser.value = GitHubUser(
                        login = userJson.get("login")?.asString ?: "",
                        avatarUrl = userJson.get("avatar_url")?.asString ?: "",
                        name = userJson.get("name")?.asString ?: userJson.get("login")?.asString ?: "",
                        publicRepos = userJson.get("public_repos")?.asInt ?: 0,
                        followers = userJson.get("followers")?.asInt ?: 0
                    )

                    // 获取仓库列表
                    val reposResp = gitHubApiGet("/user/repos?sort=updated&per_page=20", token)
                    val reposArray = JsonParser.parseString(reposResp).asJsonArray
                    _githubRepos.value = reposArray.map { repo ->
                        val obj = repo.asJsonObject
                        GitHubRepo(
                            name = obj.get("name")?.asString ?: "",
                            fullName = obj.get("full_name")?.asString ?: "",
                            description = obj.get("description")?.asString ?: "",
                            language = obj.get("language")?.asString,
                            stars = obj.get("stargazers_count")?.asInt ?: 0,
                            forks = obj.get("forks_count")?.asInt ?: 0,
                            updatedAt = obj.get("updated_at")?.asString?.take(10) ?: ""
                        )
                    }

                    // 获取通知
                    try {
                        val notifResp = gitHubApiGet("/notifications?per_page=10", token)
                        val notifArray = JsonParser.parseString(notifResp).asJsonArray
                        _githubNotifications.value = notifArray.map { n ->
                            val obj = n.asJsonObject
                            val subject = obj.getAsJsonObject("subject")
                            val repo = obj.getAsJsonObject("repository")
                            GitHubNotification(
                                title = subject?.get("title")?.asString ?: "",
                                repoName = repo?.get("full_name")?.asString ?: "",
                                type = subject?.get("type")?.asString ?: "",
                                url = subject?.get("url")?.asString ?: "",
                                updatedAt = obj.get("updated_at")?.asString?.take(10) ?: ""
                            )
                        }
                    } catch (_: Exception) {
                        _githubNotifications.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                _githubError.value = e.message ?: "加载失败"
            } finally {
                _isLoadingGitHub.value = false
            }
        }
    }

    fun loadRepoCommits(fullName: String) {
        val token = _githubToken.value
        if (token.isBlank()) return
        _isLoadingGitHub.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resp = gitHubApiGet("/repos/$fullName/commits?per_page=10", token)
                    val array = JsonParser.parseString(resp).asJsonArray
                    _selectedRepoCommits.value = array.map { c ->
                        val obj = c.asJsonObject
                        val commit = obj.getAsJsonObject("commit")
                        val author = commit?.getAsJsonObject("author")
                        GitHubCommit(
                            sha = obj.get("sha")?.asString?.take(7) ?: "",
                            message = commit?.get("message")?.asString?.split("\n")?.firstOrNull() ?: "",
                            author = author?.get("name")?.asString ?: "",
                            date = author?.get("date")?.asString?.take(10) ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                _githubError.value = "加载 commits 失败: ${e.message}"
            } finally {
                _isLoadingGitHub.value = false
            }
        }
    }

    private fun gitHubApiGet(path: String, token: String): String {
        val url = java.net.URL("${Constants.GITHUB_API_BASE}$path")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val code = conn.responseCode
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw Exception("GitHub API 错误 ($code): $err")
        }
        return conn.inputStream.bufferedReader().readText()
    }

    // ============ HuggingFace ============
    data class HfModel(
        val modelId: String,
        val author: String,
        val pipelineTag: String,
        val downloads: Long,
        val likes: Int,
        val lastModified: String
    )

    private val _hfToken = MutableStateFlow("")
    val hfToken: StateFlow<String> = _hfToken.asStateFlow()

    private val _hfModels = MutableStateFlow<List<HfModel>>(emptyList())
    val hfModels: StateFlow<List<HfModel>> = _hfModels.asStateFlow()

    private val _isLoadingHf = MutableStateFlow(false)
    val isLoadingHf: StateFlow<Boolean> = _isLoadingHf.asStateFlow()

    private val _hfError = MutableStateFlow<String?>(null)
    val hfError: StateFlow<String?> = _hfError.asStateFlow()

    private val _hfTrendingTag = MutableStateFlow("text-generation")
    val hfTrendingTag: StateFlow<String> = _hfTrendingTag.asStateFlow()

    fun loadHfToken() {
        _hfToken.value = securePrefs.getString(Constants.KEY_HF_TOKEN, "") ?: ""
    }

    fun saveHfToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_HF_TOKEN, token).apply()
        _hfToken.value = token
        _toastMessage.value = if (token.isNotBlank()) "✅ HuggingFace Token 已保存" else "✅ 已清除"
    }

    fun searchHfModels(query: String) {
        if (query.isBlank()) return
        _isLoadingHf.value = true
        _hfError.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    val resp = hfApiGet("/models?search=$encoded&sort=downloads&direction=-1&limit=20")
                    val array = JsonParser.parseString(resp).asJsonArray
                    _hfModels.value = array.map { m ->
                        val obj = m.asJsonObject
                        HfModel(
                            modelId = obj.get("modelId")?.asString ?: obj.get("id")?.asString ?: "",
                            author = obj.get("author")?.asString ?: obj.get("modelId")?.asString?.split("/")?.firstOrNull() ?: "",
                            pipelineTag = obj.get("pipeline_tag")?.asString ?: "其他",
                            downloads = obj.get("downloads")?.asLong ?: 0,
                            likes = obj.get("likes")?.asInt ?: 0,
                            lastModified = obj.get("lastModified")?.asString?.take(10) ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                _hfError.value = e.message ?: "搜索失败"
                _hfModels.value = emptyList()
            } finally {
                _isLoadingHf.value = false
            }
        }
    }

    fun loadHfTrending(tag: String? = null) {
        val pipelineTag = tag ?: _hfTrendingTag.value
        if (tag != null) _hfTrendingTag.value = tag
        _isLoadingHf.value = true
        _hfError.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val resp = hfApiGet("/models?pipeline_tag=$pipelineTag&sort=downloads&direction=-1&limit=20")
                    val array = JsonParser.parseString(resp).asJsonArray
                    _hfModels.value = array.map { m ->
                        val obj = m.asJsonObject
                        HfModel(
                            modelId = obj.get("modelId")?.asString ?: obj.get("id")?.asString ?: "",
                            author = obj.get("author")?.asString ?: obj.get("modelId")?.asString?.split("/")?.firstOrNull() ?: "",
                            pipelineTag = obj.get("pipeline_tag")?.asString ?: "其他",
                            downloads = obj.get("downloads")?.asLong ?: 0,
                            likes = obj.get("likes")?.asInt ?: 0,
                            lastModified = obj.get("lastModified")?.asString?.take(10) ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                _hfError.value = e.message ?: "加载失败"
                _hfModels.value = emptyList()
            } finally {
                _isLoadingHf.value = false
            }
        }
    }

    private fun hfApiGet(path: String): String {
        val token = _hfToken.value
        val url = java.net.URL("${Constants.HF_API_BASE}$path")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        if (token.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val code = conn.responseCode
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw Exception("HuggingFace API 错误 ($code): $err")
        }
        return conn.inputStream.bufferedReader().readText()
    }

    // ============ Server酱 / PushPlus ============
    private val _serverchanKey = MutableStateFlow("")
    val serverchanKey: StateFlow<String> = _serverchanKey.asStateFlow()

    private val _pushplusToken = MutableStateFlow("")
    val pushplusToken: StateFlow<String> = _pushplusToken.asStateFlow()

    private val _pushTestResult = MutableStateFlow<String?>(null)
    val pushTestResult: StateFlow<String?> = _pushTestResult.asStateFlow()

    private val _isTestingPush = MutableStateFlow(false)
    val isTestingPush: StateFlow<Boolean> = _isTestingPush.asStateFlow()

    fun loadPushConfig() {
        _serverchanKey.value = securePrefs.getString(Constants.KEY_SERVERCHAN_KEY, "") ?: ""
        _pushplusToken.value = securePrefs.getString(Constants.KEY_PUSHPLUS_TOKEN, "") ?: ""
    }

    fun saveServerchanKey(key: String) {
        securePrefs.edit().putString(Constants.KEY_SERVERCHAN_KEY, key).apply()
        _serverchanKey.value = key
        _toastMessage.value = if (key.isNotBlank()) "✅ Server酱 Key 已保存" else "✅ 已清除"
    }

    fun savePushplusToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_PUSHPLUS_TOKEN, token).apply()
        _pushplusToken.value = token
        _toastMessage.value = if (token.isNotBlank()) "✅ PushPlus Token 已保存" else "✅ 已清除"
    }

    fun testServerchan() {
        val key = _serverchanKey.value
        if (key.isBlank()) { _toastMessage.value = "请先配置 Server酱 SendKey"; return }
        _isTestingPush.value = true; _pushTestResult.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = java.net.URL("https://sctapi.ftqq.com/$key.send")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    val body = """{"title":"布老师App 推送测试","desp":"如果你看到这条消息，说明推送配置成功了 🎉"}"""
                    conn.outputStream.write(body.toByteArray(Charsets.UTF_8))
                    val resp = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val json = JsonParser.parseString(resp).asJsonObject
                    val code = json.get("code")?.asInt
                    _pushTestResult.value = if (code == 0) "✅ 测试成功！" else "❌ 推送失败: ${json.get("message")?.asString ?: resp}"
                }
            } catch (e: Exception) {
                _pushTestResult.value = "❌ 测试失败: ${e.message}"
            } finally {
                _isTestingPush.value = false
            }
        }
    }

    fun testPushplus() {
        val token = _pushplusToken.value
        if (token.isBlank()) { _toastMessage.value = "请先配置 PushPlus Token"; return }
        _isTestingPush.value = true; _pushTestResult.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = java.net.URL("https://www.pushplus.plus/send")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    val body = """{"token":"$token","title":"布老师App 推送测试","content":"如果你看到这条消息，说明推送配置成功了 🎉"}"""
                    conn.outputStream.write(body.toByteArray(Charsets.UTF_8))
                    val resp = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val json = JsonParser.parseString(resp).asJsonObject
                    val code = json.get("code")?.asInt
                    _pushTestResult.value = if (code == 200) "✅ 测试成功！" else "❌ 推送失败: ${json.get("msg")?.asString ?: resp}"
                }
            } catch (e: Exception) {
                _pushTestResult.value = "❌ 测试失败: ${e.message}"
            } finally {
                _isTestingPush.value = false
            }
        }
    }

    fun clearPushTestResult() { _pushTestResult.value = null }

    // ============ 通用余额查询 ============
    data class BalanceCheckService(
        val id: String,
        val name: String,
        val baseUrl: String,
        val apiKey: String
    )

    private val _balanceServices = MutableStateFlow<List<BalanceCheckService>>(emptyList())
    val balanceServices: StateFlow<List<BalanceCheckService>> = _balanceServices.asStateFlow()

    private val _balanceResults = MutableStateFlow<Map<String, String>>(emptyMap())
    val balanceResults: StateFlow<Map<String, String>> = _balanceResults.asStateFlow()

    private val _isLoadingBalanceCheck = MutableStateFlow(false)
    val isLoadingBalanceCheck: StateFlow<Boolean> = _isLoadingBalanceCheck.asStateFlow()

    fun loadBalanceServices() {
        val json = securePrefs.getString(Constants.KEY_BALANCE_CHECK_LIST, "[]") ?: "[]"
        val type = object : TypeToken<List<BalanceCheckService>>() {}.type
        _balanceServices.value = try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
    }

    private fun saveBalanceServices(list: List<BalanceCheckService>) {
        securePrefs.edit().putString(Constants.KEY_BALANCE_CHECK_LIST, gson.toJson(list)).apply()
        _balanceServices.value = list
    }

    fun addBalanceService(name: String, baseUrl: String, apiKey: String) {
        val service = BalanceCheckService(
            id = java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            baseUrl = baseUrl.trim().trimEnd('/'),
            apiKey = apiKey.trim()
        )
        val list = _balanceServices.value.toMutableList()
        list.add(service)
        saveBalanceServices(list)
        _toastMessage.value = "✅ 已添加余额查询「${service.name}」"
        // 添加后自动查询
        queryBalance(service.id)
    }

    fun deleteBalanceService(id: String) {
        val list = _balanceServices.value.toMutableList()
        list.removeAll { it.id == id }
        saveBalanceServices(list)
        val results = _balanceResults.value.toMutableMap()
        results.remove(id)
        _balanceResults.value = results
        _toastMessage.value = "✅ 已删除"
    }

    fun queryBalance(serviceId: String) {
        val service = _balanceServices.value.find { it.id == serviceId } ?: return
        _isLoadingBalanceCheck.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 尝试多个常见余额接口路径
                    val paths = listOf("/user/balance", "/v1/user/balance", "/dashboard/billing/credit_grants")
                    var lastError: String? = null
                    for (path in paths) {
                        try {
                            val url = java.net.URL("${service.baseUrl}$path")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "GET"
                            conn.setRequestProperty("Authorization", "Bearer ${service.apiKey}")
                            conn.setRequestProperty("Accept", "application/json")
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            val code = conn.responseCode
                            if (code == 200) {
                                val text = conn.inputStream.bufferedReader().readText()
                                conn.disconnect()
                                // 尝试解析常见格式
                                return@withContext try {
                                    val json = JsonParser.parseString(text).asJsonObject
                                    // 格式1: DeepSeek → balance_infos[0].total_balance
                                    val balanceInfos = json.getAsJsonArray("balance_infos")
                                    if (balanceInfos != null && balanceInfos.size() > 0) {
                                        "¥${balanceInfos[0].asJsonObject.get("total_balance")?.asString ?: "?"}"
                                    } else {
                                        val data = json.getAsJsonObject("data")
                                        when {
                                            data != null && data.get("balance") != null -> "¥${data.get("balance")?.asString ?: data.get("balance")?.asNumber ?: "?"}"
                                            json.get("balance") != null -> "¥${json.get("balance")?.asString ?: json.get("balance")?.asNumber ?: "?"}"
                                            json.get("total_balance") != null -> "¥${json.get("total_balance")?.asString ?: "?"}"
                                            else -> text.take(200)
                                        }
                                    }
                                } catch (_: Exception) {
                                    text.take(200)
                                }
                            }
                            conn.disconnect()
                            lastError = "HTTP $code"
                        } catch (_: Exception) {
                            lastError = "${_->message}"
                        }
                    }
                    "❌ $lastError"
                }
                val results = _balanceResults.value.toMutableMap()
                results[serviceId] = result
                _balanceResults.value = results
            } catch (e: Exception) {
                val results = _balanceResults.value.toMutableMap()
                results[serviceId] = "❌ ${e.message}"
                _balanceResults.value = results
            } finally {
                _isLoadingBalanceCheck.value = false
            }
        }
    }

    fun queryAllBalances() {
        _balanceServices.value.forEach { queryBalance(it.id) }
    }

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
        loadCustomApiList()
        loadGitHubToken()
        loadHfToken()
        loadPushConfig()
        loadBalanceServices()
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

        val isCustom = channel.startsWith(Constants.CHANNEL_CUSTOM_PREFIX)
        if (isCustom) {
            cozeClient.currentBotId = ""
            val customId = channel.removePrefix(Constants.CHANNEL_CUSTOM_PREFIX)
            val config = getCustomApiById(customId)
            _toastMessage.value = "✅ 已切换到「${config?.name ?: "自定义"}」"
        } else {
            cozeClient.currentBotId = if (channel == Constants.CHANNEL_DEEPSEEK) {
                ""
            } else {
                val savedBotId = securePrefs.getString("current_agent_id", "") ?: ""
                if (savedBotId.isNotBlank()) savedBotId
                else {
                    viewModelScope.launch {
                        val botId = settingsRepo.getString(Constants.KEY_COZE_BOT_ID, "")
                        cozeClient.currentBotId = botId.ifBlank { Constants.DEFAULT_COZE_BOT_ID }
                        _cozeBotId.value = cozeClient.currentBotId
                    }
                    _cozeBotId.value
                }
            }
            _toastMessage.value = when (channel) {
                Constants.CHANNEL_DEEPSEEK -> "✅ 已切换到 DeepSeek"
                else -> "✅ 已切换到 Coze"
            }
        }
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
                val result = withContext(Dispatchers.IO) {
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
                    val balanceInfos = json.getAsJsonArray("balance_infos")
                    val totalBalance = balanceInfos?.firstOrNull()?.asJsonObject?.get("total_balance")?.asString
                    if (totalBalance != null) "¥$totalBalance" else "查询失败"
                }
                _deepseekBalance.value = result
            } catch (e: Exception) {
                _deepseekBalance.value = "查询失败: ${e.message ?: "网络异常"}"
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
        val map = mutableMapOf<String, ChannelTokenStats>()
        var grandTotal = 0

        // 内置通道：Coze
        val cozeTotal = securePrefs.getInt("coze_total_tokens", 0)
        map["coze"] = ChannelTokenStats(
            total = cozeTotal,
            input = securePrefs.getInt("coze_total_input", 0),
            output = securePrefs.getInt("coze_total_output", 0),
            messages = securePrefs.getInt("coze_total_messages", 0)
        )
        grandTotal += cozeTotal

        // 内置通道：DeepSeek
        val dsTotal = securePrefs.getInt("ds_total_tokens", 0)
        map["deepseek"] = ChannelTokenStats(
            total = dsTotal,
            input = securePrefs.getInt("ds_total_input", 0),
            output = securePrefs.getInt("ds_total_output", 0),
            messages = securePrefs.getInt("ds_total_messages", 0)
        )
        grandTotal += dsTotal

        // 自定义 API 通道
        _customApiList.value.forEach { config ->
            val prefix = "custom:${config.id}"
            val total = securePrefs.getInt("${prefix}_total_tokens", 0)
            if (total > 0 || securePrefs.contains("${prefix}_total_tokens")) {
                map[prefix] = ChannelTokenStats(
                    total = total,
                    input = securePrefs.getInt("${prefix}_total_input", 0),
                    output = securePrefs.getInt("${prefix}_total_output", 0),
                    messages = securePrefs.getInt("${prefix}_total_messages", 0)
                )
                grandTotal += total
            }
        }

        _channelTokenStats.value = map
        _totalTokens.value = grandTotal
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
