package com.meitu.generator.data.tools

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.local.AppDatabase
import com.meitu.generator.data.model.ToolContext
import com.meitu.generator.data.remote.DeepSeekBalanceService
import com.meitu.generator.data.remote.GitHubService
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.data.remote.dto.OpenAIMessage
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 系统自检/诊断工具
 * 检查 API Key 连通性、数据库状态、GitHub Token 等
 */
@Singleton
class DiagnoseTool @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Named("deepseekService") private val deepseekService: OpenAIService,
    @Named("googleService") private val googleService: OpenAIService,
    private val deepSeekBalanceService: DeepSeekBalanceService,
    private val gitHubService: GitHubService,
    private val database: AppDatabase,
    private val settingsRepo: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : Tool {
    override val name = "system_diagnose"
    override val description = "系统自检诊断，检查API Key连通性、数据库状态、GitHub Token权限等，生成完整诊断报告"

    override val parametersSchema = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply {
            add("scope", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "检查范围：all=全部检查, api=仅API, db=仅数据库, github=仅GitHub")
            })
        })
    }

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val scope = arguments["scope"] as? String ?: "all"
        val results = mutableListOf<String>()

        results.add("🔧 布老师 v${Constants.APP_VERSION} 系统诊断报告")
        results.add("=".repeat(30))

        if (scope == "all" || scope == "api") {
            results.add("")
            results.add(checkDeepSeek())
            results.add("")
            results.add(checkGoogle())
        }

        if (scope == "all" || scope == "db") {
            results.add("")
            results.add(checkDatabase())
        }

        if (scope == "all" || scope == "github") {
            results.add("")
            results.add(checkGitHub())
        }

        return results.joinToString("\n")
    }

    private suspend fun checkDeepSeek(): String {
        val sb = StringBuilder()
        sb.appendLine("📡 DeepSeek API")

        val apiKey = (securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: "")
            .ifBlank { Constants.OPENAI_API_KEY }

        if (apiKey.isBlank()) {
            sb.appendLine("   ❌ 未配置 API Key")
            return sb.toString()
        }

        val chatResult = withTimeoutOrNull(10000L) {
            try {
                val request = OpenAIRequest(
                    model = "deepseek-chat",
                    messages = listOf(OpenAIMessage(role = "user", content = "hi")),
                    max_tokens = 5
                )
                val response = deepseekService.chatCompletions(request, "Bearer $apiKey")
                if (response.error != null) {
                    "FAIL:${response.error.message ?: "未知错误"}"
                } else {
                    "OK"
                }
            } catch (e: Exception) {
                "FAIL:${e.message?.take(80) ?: "连接失败"}"
            }
        } ?: "FAIL:超时(10s)"

        if (chatResult == "OK") {
            sb.appendLine("   ✅ 连通正常")
        } else {
            sb.appendLine("   ❌ 连接失败: ${chatResult.removePrefix("FAIL:")}")
        }

        val balanceResult = withTimeoutOrNull(10000L) {
            try {
                val balance = deepSeekBalanceService.getBalance("Bearer $apiKey")
                val isAvailable = balance.isAvailable
                val cnyInfo = balance.balanceInfos.firstOrNull { it.currency == "CNY" }
                val totalBalance = cnyInfo?.totalBalance ?: balance.balanceInfos.firstOrNull()?.totalBalance ?: "未知"
                if (!isAvailable) "EXHAUSTED:$totalBalance" else "OK:$totalBalance"
            } catch (e: Exception) {
                "FAIL:${e.message?.take(80) ?: "查询失败"}"
            }
        } ?: "FAIL:超时"

        if (balanceResult.startsWith("OK:")) {
            sb.appendLine("   💰 余额: ¥${balanceResult.removePrefix("OK:")}")
        } else if (balanceResult.startsWith("EXHAUSTED:")) {
            sb.appendLine("   ⚠️ 余额已耗尽 (¥${balanceResult.removePrefix("EXHAUSTED:")})")
        } else {
            sb.appendLine("   ⚠️ 余额查询失败: ${balanceResult.removePrefix("FAIL:")}")
        }

        return sb.toString().trimEnd()
    }

    private suspend fun checkGoogle(): String {
        val sb = StringBuilder()
        sb.appendLine("📡 Google AI API")

        val apiKey = (securePrefs.getString(Constants.KEY_GOOGLE_API_KEY, "") ?: "")
            .ifBlank { Constants.GOOGLE_API_KEY }

        if (apiKey.isBlank()) {
            sb.appendLine("   ❌ 未配置 API Key")
            return sb.toString()
        }

        val result = withTimeoutOrNull(10000L) {
            try {
                val request = OpenAIRequest(
                    model = "gemini-2.0-flash",
                    messages = listOf(OpenAIMessage(role = "user", content = "hi")),
                    max_tokens = 5
                )
                val response = googleService.chatCompletions(request, "Bearer $apiKey")
                if (response.error != null) {
                    "FAIL:${response.error.message ?: "未知错误"}"
                } else {
                    "OK"
                }
            } catch (e: Exception) {
                "FAIL:${e.message?.take(80) ?: "连接失败"}"
            }
        } ?: "FAIL:超时(10s)"

        if (result == "OK") {
            sb.appendLine("   ✅ 连通正常")
        } else {
            sb.appendLine("   ❌ 连接失败: ${result.removePrefix("FAIL:")}")
        }

        return sb.toString().trimEnd()
    }

    private suspend fun checkDatabase(): String {
        val sb = StringBuilder()
        sb.appendLine("💾 本地数据库")

        return withContext(Dispatchers.IO) {
            try {
                val testKey = "_diag_${System.currentTimeMillis()}"
                database.settingsDao().upsert(
                    com.meitu.generator.data.local.entity.SettingEntity(key = testKey, value = "ok")
                )
                val read = database.settingsDao().getSetting(testKey)
                database.settingsDao().delete(testKey)

                if (read?.value == "ok") {
                    sb.appendLine("   ✅ 读写正常")
                } else {
                    sb.appendLine("   ⚠️ 写入成功但读取不匹配")
                }

                val settings = database.settingsDao().getAllSettings().first()
                val logs = database.logDao().getRecentLogs().first()
                val chats = database.chatMessageDao().getAllMessages()
                sb.appendLine("   📊 设置: ${settings.size}条 | 日志: ${logs.size}条 | 对话: ${chats.size}条")
            } catch (e: Exception) {
                sb.appendLine("   ❌ 数据库异常: ${e.message?.take(100)}")
            }
            sb.toString().trimEnd()
        }
    }

    private suspend fun checkGitHub(): String {
        val sb = StringBuilder()
        sb.appendLine("🔗 GitHub")

        val token = (securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: "")
            .ifBlank { Constants.DEFAULT_GITHUB_TOKEN }

        if (token.isBlank()) {
            sb.appendLine("   ❌ 未配置 Token")
            return sb.toString()
        }

        val result = withTimeoutOrNull(10000L) {
            try {
                val runs = gitHubService.getWorkflowRuns(
                    Constants.GITHUB_REPO_OWNER,
                    Constants.GITHUB_REPO_NAME,
                    perPage = 1
                )
                "OK:${runs.totalCount}"
            } catch (e: Exception) {
                val msg = e.message ?: ""
                when {
                    msg.contains("401") -> "FAIL:Token无效或已过期"
                    msg.contains("403") -> "FAIL:权限不足(需要repo+workflow)"
                    msg.contains("404") -> "FAIL:仓库不存在或无权访问"
                    else -> "FAIL:${msg.take(80)}"
                }
            }
        } ?: "FAIL:超时(10s)"

        if (result.startsWith("OK:")) {
            sb.appendLine("   ✅ Token有效")
            sb.appendLine("   📋 编译记录: ${result.removePrefix("OK:")}条")
        } else {
            sb.appendLine("   ❌ ${result.removePrefix("FAIL:")}")
        }

        return sb.toString().trimEnd()
    }
}
