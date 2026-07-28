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
            appendLine("4. 不要写任何 import 语句（系统会自动注入完整的 import 列表）")
            appendLine("5. 不要使用任何项目中不存在的第三方库（禁止 Hilt、禁止 Room、禁止 Retrofit、禁止 Coil）")
            appendLine("6. 不要使用 android.R 的资源")
            appendLine("7. 只使用 Jetpack Compose 标准组件（Material3、Foundation、Animation 等）")
            appendLine("8. 如需使用协程相关功能（delay、launch 等），直接调用即可，import 由系统处理")
            appendLine()
            appendLine("=== 文件结构模板 ===")
            appendLine("package com.example.app")
            appendLine()
            appendLine("// 不需要写 import，系统自动注入")
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
            appendLine("只输出完整 Kotlin 代码（包含 package + 完整实现，不含 import），不要任何解释。")
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
        
        val lines = fixed.lines().toMutableList()
        
        // Find package line
        val packageIdx = lines.indexOfFirst { it.trimStart().startsWith("package ") }
        val packageLine = if (packageIdx >= 0) lines[packageIdx] else "package com.example.app"
        
        // Remove all existing import lines (AI-generated imports are often wrong)
        val codeLines = lines.filter { !it.trimStart().startsWith("import ") }
        
        // Rebuild: package + blank line + comprehensive imports + blank line + code body
        val header = """import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Badge
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Popup
import androidx.compose.material3.ListItem
import androidx.compose.material3.Chip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.random.Random"""
        
        // Find where code body starts (after package line)
        val bodyStartIdx = if (packageIdx >= 0) packageIdx + 1 else 0
        val bodyLines = codeLines.drop(bodyStartIdx).dropWhile { it.isBlank() }
        
        fixed = buildString {
            appendLine(packageLine)
            appendLine()
            appendLine(header)
            appendLine()
            bodyLines.forEachIndexed { index, line ->
                append(line)
                if (index < bodyLines.size - 1) appendLine()
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
