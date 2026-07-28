package com.meitu.generator.data.tools

import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.AgentEngine
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.model.ToolContext
import com.meitu.generator.data.remote.GeminiService
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.dto.*
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 开发者工具 - 生成单个 Kotlin 文件
 * 
 * 功能: 根据用户需求生成完整的 Kotlin 代码文件
 * 支持多模型路由：OpenRouter / SambaNova / DeepSeek / Gemini
 */
@Singleton
class DeveloperTool @Inject constructor(
    private val openAIService: OpenAIService,
    @Named("sambanovaService") private val sambanovaService: OpenAIService,
    @Named("deepseekService") private val deepseekService: OpenAIService,
    private val geminiService: GeminiService,
    private val settingsRepository: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : Tool {
    override val name = "developer"
    override val description = "根据需求生成单个 Kotlin 文件代码"
    override val parametersSchema = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply {
            add("filePath", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "文件路径，例如: app/src/main/java/com/example/MainActivity.kt")
            })
            add("requirement", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "用户需求描述，例如: 创建一个带有登录界面的 Activity")
            })
            add("context", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "可选的上下文信息，例如项目结构、依赖等")
            })
        })
        add("required", com.google.gson.JsonArray().apply {
            add("filePath")
            add("requirement")
        })
    }

    companion object {
        private val SAMBANOVA_MODELS = setOf(
            "Meta-Llama-3.3-70B-Instruct", "gpt-oss-120b",
            "DeepSeek-V3.1", "gemma-4-31B-it"
        )
    }

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val filePath = arguments["filePath"] as? String
            ?: return "错误: 缺少 filePath 参数"
        val requirement = arguments["requirement"] as? String
            ?: return "错误: 缺少 requirement 参数"
        val codeContext = arguments["context"] as? String ?: ""

        val model = settingsRepository.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        val effectiveModel = if (model == "auto" || model.isBlank()) "nvidia/nemotron-3-super-120b-a12b:free" else model

        val prompt = buildString {
            appendLine("你是一个 Android 开发专家。请根据需求生成完整的 Kotlin 代码文件。")
            appendLine()
            appendLine("文件路径: $filePath")
            appendLine("需求: $requirement")
            if (codeContext.isNotBlank()) {
                appendLine("上下文: $codeContext")
            }
            appendLine()
            appendLine("要求:")
            appendLine("1. 生成完整、可编译的代码，包含所有必要的 import")
            appendLine("2. 遵循 Android 最佳实践和 Kotlin 编码规范")
            appendLine("3. 使用 Jetpack Compose 构建 UI（如果是 Activity/Fragment）")
            appendLine("4. 添加必要的注释说明关键逻辑")
            appendLine()
            appendLine("只输出代码，不要解释。代码格式:")
            appendLine("```kotlin")
            appendLine("// 完整的 Kotlin 代码")
            appendLine("```")
        }

        try {
            val platform = AgentEngine.getModelPlatform(effectiveModel)
            val content = when (platform) {
                "gemini" -> callGemini(effectiveModel, prompt)
                "deepseek" -> callOpenAICompatible(deepseekService, getDeepSeekApiKey(), effectiveModel, prompt)
                "sambanova" -> callOpenAICompatible(sambanovaService, getSambaNovaApiKey(), effectiveModel, prompt)
                else -> callOpenAICompatible(openAIService, getOpenRouterApiKey(), effectiveModel, prompt)
            }

            if (content.startsWith("[ERROR]") || content.startsWith("[调用失败]")) {
                return "代码生成失败: $content"
            }

            val code = extractKotlinCode(content)

            return buildString {
                appendLine("✅ 代码生成成功")
                appendLine("文件: $filePath")
                appendLine("代码长度: ${code.length} 字符")
                appendLine()
                appendLine("```kotlin")
                appendLine(code)
                appendLine("```")
                appendLine()
                appendLine("文件内容:")
                appendLine(code)
            }
        } catch (e: Exception) {
            return "代码生成异常: ${e.message?.take(200)}"
        }
    }

    private suspend fun callOpenAICompatible(
        service: OpenAIService,
        apiKey: String,
        model: String,
        prompt: String
    ): String {
        val request = OpenAIRequest(
            model = model,
            messages = listOf(OpenAIMessage(role = "user", content = prompt)),
            temperature = 0.3,
            max_tokens = 4000
        )
        val response = service.chatCompletions(
            request = request,
            authorization = "Bearer $apiKey"
        )
        if (response.error != null) {
            return "[ERROR] ${response.error.message ?: "未知错误"}"
        }
        return response.choices?.firstOrNull()?.message?.content?.trim()
            ?: "[ERROR] 无响应内容"
    }

    private suspend fun callGemini(model: String, prompt: String): String {
        val geminiRequest = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )
        val response = geminiService.generateContent(
            model = model,
            apiKey = getGeminiApiKey(),
            request = geminiRequest
        )
        if (response.error != null) {
            return "[ERROR] ${response.error.message ?: "未知错误"}"
        }
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            ?: "[ERROR] 无响应内容"
    }

    private fun getOpenRouterApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_OPENROUTER_API_KEY, "") ?: ""
        return if (savedKey.isNotBlank()) savedKey else Constants.OPENROUTER_API_KEY
    }

    private fun getSambaNovaApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_SAMBANOVA_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.SAMBANOVA_API_KEY else savedKey
    }

    private fun getDeepSeekApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_DEEPSEEK_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.DEEPSEEK_API_KEY else savedKey
    }

    private fun getGeminiApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_GEMINI_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.GEMINI_API_KEY else savedKey
    }

    private fun extractKotlinCode(content: String): String {
        val codeBlockRegex = Regex("```kotlin\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        val genericBlockRegex = Regex("```\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val genericMatch = genericBlockRegex.find(content)
        if (genericMatch != null) {
            return genericMatch.groupValues[1].trim()
        }
        
        return content.trim()
    }
}
