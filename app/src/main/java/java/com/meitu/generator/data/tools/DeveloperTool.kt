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
            appendLine("你是一个资深 Android 开发专家。请根据需求生成一个完整、可直接编译的 Kotlin 代码文件。")
            appendLine()
            appendLine("文件路径: $filePath")
            appendLine("需求: $requirement")
            if (codeContext.isNotBlank()) {
                appendLine("上下文: $codeContext")
            }
            appendLine()
            appendLine("=== 严格要求（违反将导致编译失败） ===")
            appendLine("1. 必须以 package com.example.app 开头")
            appendLine("2. 禁止使用 @AndroidEntryPoint 或任何 Hilt/Dagger 注解")
            appendLine("3. 类必须继承 androidx.activity.ComponentActivity，不要任何注解")
            appendLine("4. 必须在 package 之后、class 之前写出所有 import，每个 import 独占一行")
            appendLine("5. 常用 import（必须按需用到的都写上）：")
            appendLine("   import android.os.Bundle")
            appendLine("   import androidx.activity.ComponentActivity")
            appendLine("   import androidx.activity.compose.setContent")
            appendLine("   import androidx.compose.runtime.Composable")
            appendLine("   import androidx.compose.runtime.getValue")
            appendLine("   import androidx.compose.runtime.setValue")
            appendLine("   import androidx.compose.runtime.mutableStateOf")
            appendLine("   import androidx.compose.runtime.remember")
            appendLine("   import androidx.compose.runtime.LaunchedEffect")
            appendLine("   import androidx.compose.ui.Modifier")
            appendLine("   import androidx.compose.ui.unit.dp")
            appendLine("   import androidx.compose.ui.unit.sp")
            appendLine("   import androidx.compose.ui.graphics.Color")
            appendLine("   import androidx.compose.ui.Alignment")
            appendLine("   import androidx.compose.material3.MaterialTheme")
            appendLine("   import androidx.compose.material3.Text")
            appendLine("   import androidx.compose.foundation.layout.*")
            appendLine("   import kotlinx.coroutines.delay")
            appendLine("6. 只要使用了 Text、Box、Column、Row、Button、Image、Spacer、fillMaxSize、padding、size、offset、background、clickable 等，必须 import 对应的包")
            appendLine("7. 不要使用任何项目中不存在的第三方库（禁止 Hilt、禁止 Room、禁止 Retrofit）")
            appendLine("8. 不要使用 android.R 的资源")
            appendLine()
            appendLine("=== 文件结构模板 ===")
            appendLine("package com.example.app")
            appendLine()
            appendLine("import android.os.Bundle")
            appendLine("import androidx.activity.ComponentActivity")
            appendLine("import androidx.activity.compose.setContent")
            appendLine("// ... 其他所有需要的 import ...")
            appendLine()
            appendLine("class MainActivity : ComponentActivity() {")
            appendLine("    override fun onCreate(savedInstanceState: Bundle?) {")
            appendLine("        super.onCreate(savedInstanceState)")
            appendLine("        setContent {")
            appendLine("            MaterialTheme {")
            appendLine("                // 你的 Compose 内容")
            appendLine("            }")
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("只输出完整 Kotlin 代码（包含 package + 所有 import + 完整实现），不要任何解释。")
            appendLine("```kotlin")
            appendLine("// 完整代码")
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

            val rawCode = extractKotlinCode(content)
            val code = fixGeneratedCode(rawCode)

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

    /**
     * Post-process AI-generated code to fix common issues:
     * - Remove @AndroidEntryPoint and other Hilt annotations
     * - Add missing imports if none are present
     * - Ensure package declaration exists
     */
    private fun fixGeneratedCode(code: String): String {
        var fixed = code
        // Remove Hilt annotations
        fixed = fixed.replace("""@AndroidEntryPoint\s*\n""".toRegex(), "")
        fixed = fixed.replace("""@Inject\s*\n""".toRegex(), "")
        fixed = fixed.replace("""@HiltAndroidApp\s*\n""".toRegex(), "")
        
        // Check if imports exist
        val hasImports = fixed.lines().any { it.trimStart().startsWith("import ") }
        val hasPackage = fixed.lines().any { it.trimStart().startsWith("package ") }
        
        if (!hasImports) {
            // Inject comprehensive common imports after package declaration
            val fallbackImports = """
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
""".trimIndent()
            
            if (hasPackage) {
                val lines = fixed.lines().toMutableList()
                val packageIdx = lines.indexOfFirst { it.trimStart().startsWith("package ") }
                if (packageIdx >= 0) {
                    lines.add(packageIdx + 1, "")
                    lines.add(packageIdx + 2, fallbackImports)
                    fixed = lines.joinToString("\n")
                }
            } else {
                fixed = "package com.example.app\n\n$fallbackImports\n\n$fixed"
            }
        }
        
        return fixed
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
