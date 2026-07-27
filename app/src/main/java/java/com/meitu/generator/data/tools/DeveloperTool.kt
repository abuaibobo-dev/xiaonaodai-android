package com.meitu.generator.data.tools

import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.model.ToolContext
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.dto.OpenAIMessage
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 开发者工具 - 生成单个 Kotlin 文件
 * 
 * 功能: 根据用户需求生成完整的 Kotlin 代码文件
 * 
 * 调用流程:
 * 1. 接收用户需求描述和文件名
 * 2. 调用 LLM 生成完整代码
 * 3. 返回生成的代码内容
 */
@Singleton
class DeveloperTool @Inject constructor(
    private val openAIService: OpenAIService,
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

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val filePath = arguments["filePath"] as? String
            ?: return "错误: 缺少 filePath 参数"
        val requirement = arguments["requirement"] as? String
            ?: return "错误: 缺少 requirement 参数"
        val codeContext = arguments["context"] as? String ?: ""

        val model = settingsRepository.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        val effectiveModel = if (model == "auto" || model.isBlank()) "agnes-2.5-flash" else model

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
            val request = OpenAIRequest(
                model = effectiveModel,
                messages = listOf(OpenAIMessage(role = "user", content = prompt)),
                temperature = 0.3,
                max_tokens = 4000
            )

            val apiKey = (securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: "").ifBlank { Constants.OPENAI_API_KEY }
            val response = openAIService.chatCompletions(
                request = request,
                authorization = "Bearer $apiKey"
            )

            if (response.error != null) {
                return "代码生成失败: ${response.error.message}"
            }

            val content = response.choices?.firstOrNull()?.message?.content?.trim()
                ?: return "代码生成失败: 无响应内容"

            // 提取代码块
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

    private fun extractKotlinCode(content: String): String {
        // 尝试提取 ```kotlin ... ``` 代码块
        val codeBlockRegex = Regex("```kotlin\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // 如果没有代码块标记，尝试提取 ``` ... ```
        val genericBlockRegex = Regex("```\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val genericMatch = genericBlockRegex.find(content)
        if (genericMatch != null) {
            return genericMatch.groupValues[1].trim()
        }
        
        // 都没有，返回原始内容
        return content.trim()
    }
}
