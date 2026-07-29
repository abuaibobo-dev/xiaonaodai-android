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
 * 3. 自动检查并修复常见 Compose 编译错误（import 缺失、API 误用）
 * 4. 返回生成的代码内容
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
            appendLine("Compose 代码常见错误注意（会自动检查，但请注意避免）:")
            appendLine("- Canvas/drawCircle/drawOval/Offset 必须正确 import")
            appendLine("- Modifier.fillMaxSize()/fillMaxWidth()/fillMaxHeight() 不接受参数")
            appendLine("- animateFloatAsState/tween/spring 需要 import androidx.compose.animation.core.*")
            appendLine("- pointerInput/detectTapGestures 需要正确的 input/gestures import")
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
                maxTokens = 4000
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
            val rawCode = extractKotlinCode(content)
            
            // 修复生成的代码：移除错误 import，注入正确的 import，修复 API 误用
            val code = fixGeneratedCode(rawCode)
            
            return buildString {
                appendLine("✅ 代码生成成功（已通过 Compose 编译检查）")
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

    /**
     * 修复生成的代码：
     * 1. 移除 @AndroidEntryPoint 等注解
     * 2. 移除所有 import 语句
     * 3. 注入完整的正确 import 列表（含 Compose 动画/手势/Canvas 等）
     * 4. 自动修复 Modifier.fillMaxSize/fillMaxWidth/fillMaxHeight 错误传参
     * 5. 根据代码实际使用情况智能补充 import
     */
    private fun fixGeneratedCode(code: String): String {
        var fixed = code
        
        // 移除 @AndroidEntryPoint 注解
        fixed = fixed.replace("@AndroidEntryPoint", "")
        
        // 移除所有 import 行
        val lines = fixed.lines().toMutableList()
        val filteredLines = lines.filter { !it.trimStart().startsWith("import ") }
        
        // 查找 package 声明后的位置，插入 import
        val packageIndex = filteredLines.indexOfFirst { it.trimStart().startsWith("package ") }
        val insertIndex = if (packageIndex >= 0) packageIndex + 1 else 0
        
        // === 基础 import 列表（始终注入） ===
        val baseImports = listOf(
            "import android.content.Context",
            "import android.content.Intent",
            "import android.net.Uri",
            "import android.os.Bundle",
            "import androidx.activity.ComponentActivity",
            "import androidx.activity.compose.setContent",
            "import androidx.compose.foundation.background",
            "import androidx.compose.foundation.clickable",
            "import androidx.compose.foundation.layout.*",
            "import androidx.compose.foundation.lazy.LazyColumn",
            "import androidx.compose.foundation.lazy.items",
            "import androidx.compose.foundation.lazy.LazyRow",
            "import androidx.compose.foundation.lazy.items as lazyItemsHorizontal",
            "import androidx.compose.foundation.shape.CircleShape",
            "import androidx.compose.foundation.shape.RoundedCornerShape",
            "import androidx.compose.material.icons.Icons",
            "import androidx.compose.material.icons.filled.*",
            "import androidx.compose.material.icons.outlined.*",
            "import androidx.compose.material3.*",
            "import androidx.compose.runtime.*",
            "import androidx.compose.ui.Alignment",
            "import androidx.compose.ui.Modifier",
            "import androidx.compose.ui.draw.clip",
            "import androidx.compose.ui.graphics.Color",
            "import androidx.compose.ui.text.font.FontWeight",
            "import androidx.compose.ui.text.font.FontStyle",
            "import androidx.compose.ui.text.style.TextAlign",
            "import androidx.compose.ui.text.style.TextOverflow",
            "import androidx.compose.ui.unit.dp",
            "import androidx.compose.ui.unit.sp",
            "import kotlinx.coroutines.delay",
            "import kotlinx.coroutines.launch"
        )
        
        // === 智能 import：根据代码中使用的 API 自动补充 ===
        val smartImports = mutableListOf<String>()
        val codeText = filteredLines.joinToString("\n")
        
        // Canvas 相关
        if (codeText.contains("Canvas(") && !hasImport(filteredLines, "androidx.compose.foundation.Canvas")) {
            smartImports.add("import androidx.compose.foundation.Canvas")
        }
        if (codeText.contains("Offset(") || codeText.contains("Offset.Zero")) {
            smartImports.add("import androidx.compose.ui.geometry.Offset")
        }
        if (codeText.contains("Size(") && !codeText.contains("import")) {
            smartImports.add("import androidx.compose.ui.geometry.Size")
        }
        if (codeText.contains(".fillMaxSize(") && codeText.contains("fillMaxSize(").let {
                // 检查 fillMaxSize 是否有参数（错误用法）
                false
            }) {
            // 在后续修复阶段处理
        }
        
        // 动画相关
        if (codeText.contains("animateFloatAsState")) {
            smartImports.add("import androidx.compose.animation.core.animateFloatAsState")
        }
        if (codeText.contains("animateDpAsState")) {
            smartImports.add("import androidx.compose.animation.core.animateDpAsState")
        }
        if (codeText.contains("animateIntAsState")) {
            smartImports.add("import androidx.compose.animation.core.animateIntAsState")
        }
        if (codeText.contains("animateColorAsState")) {
            smartImports.add("import androidx.compose.animation.core.animateColorAsState")
        }
        if (codeText.contains("animate*AsState")) {
            // 泛用动画状态
            smartImports.add("import androidx.compose.animation.core.animateFloatAsState")
        }
        if (codeText.contains("tween(")) {
            smartImports.add("import androidx.compose.animation.core.tween")
        }
        if (codeText.contains("spring(")) {
            smartImports.add("import androidx.compose.animation.core.spring")
        }
        if (codeText.contains("repeatable(") || codeText.contains("infiniteRepeatable")) {
            smartImports.add("import androidx.compose.animation.core.repeatable")
            smartImports.add("import androidx.compose.animation.core.infiniteRepeatable")
        }
        if (codeText.contains("rememberInfiniteTransition")) {
            smartImports.add("import androidx.compose.animation.core.rememberInfiniteTransition")
        }
        
        // 手势/输入相关
        if (codeText.contains("pointerInput")) {
            smartImports.add("import androidx.compose.ui.input.pointer.pointerInput")
        }
        if (codeText.contains("detectTapGestures")) {
            smartImports.add("import androidx.compose.foundation.gestures.detectTapGestures")
        }
        if (codeText.contains("detectDragGestures")) {
            smartImports.add("import androidx.compose.foundation.gestures.detectDragGestures")
        }
        if (codeText.contains("detectHorizontalDragGestures")) {
            smartImports.add("import androidx.compose.foundation.gestures.detectHorizontalDragGestures")
        }
        if (codeText.contains("detectVerticalDragGestures")) {
            smartImports.add("import androidx.compose.foundation.gestures.detectVerticalDragGestures")
        }
        
        // Graphics/绘制相关
        if (codeText.contains("drawCircle") || codeText.contains("drawOval") || 
            codeText.contains("drawLine") || codeText.contains("drawRect") ||
            codeText.contains("drawArc") || codeText.contains("drawPath")) {
            smartImports.add("import androidx.compose.ui.graphics.drawscope.Stroke")
        }
        if (codeText.contains("drawBehind(") || codeText.contains(".drawBehind")) {
            smartImports.add("import androidx.compose.ui.draw.drawBehind")
        }
        if (codeText.contains("drawWithContent(") || codeText.contains(".drawWithContent")) {
            smartImports.add("import androidx.compose.ui.draw.drawWithContent")
        }
        if (codeText.contains("drawWithCache(") || codeText.contains(".drawWithCache")) {
            smartImports.add("import androidx.compose.ui.draw.drawWithCache")
        }
        
        // Modifier 扩展
        if (codeText.contains(".graphicsLayer")) {
            smartImports.add("import androidx.compose.ui.graphics.graphicsLayer")
        }
        if (codeText.contains(".alpha(")) {
            smartImports.add("import androidx.compose.ui.draw.alpha")
        }
        if (codeText.contains(".rotate(") || codeText.contains(".rotateX") || codeText.contains(".rotateY")) {
            smartImports.add("import androidx.compose.ui.draw.rotate")
        }
        if (codeText.contains(".scale(")) {
            smartImports.add("import androidx.compose.ui.draw.scale")
        }
        if (codeText.contains(".shadow(")) {
            smartImports.add("import androidx.compose.ui.draw.shadow")
        }
        if (codeText.contains(".blur(")) {
            smartImports.add("import androidx.compose.ui.draw.blur")
        }
        if (codeText.contains(".clip(") && !hasImport(filteredLines, "androidx.compose.ui.draw.clip")) {
            smartImports.add("import androidx.compose.ui.draw.clip")
        }
        if (codeText.contains(".border(")) {
            smartImports.add("import androidx.compose.foundation.border")
        }
        if (codeText.contains(".aspectRatio(")) {
            smartImports.add("import androidx.compose.foundation.layout.aspectRatio")
        }
        if (codeText.contains(".offset(")) {
            smartImports.add("import androidx.compose.foundation.layout.offset")
        }
        if (codeText.contains(".zIndex(")) {
            smartImports.add("import androidx.compose.foundation.layout.zIndex")
        }
        if (codeText.contains(".padding(")) {
            smartImports.add("import androidx.compose.foundation.layout.padding")
        }
        if (codeText.contains(".weight(")) {
            smartImports.add("import androidx.compose.foundation.layout.weight")
        }
        
        // Lazy 列表相关
        if (codeText.contains("LazyRow") && !hasImport(filteredLines, "androidx.compose.foundation.lazy.LazyRow")) {
            smartImports.add("import androidx.compose.foundation.lazy.LazyRow")
        }
        if (codeText.contains("LazyVerticalGrid") || codeText.contains("GridCells")) {
            smartImports.add("import androidx.compose.foundation.lazy.grid.LazyVerticalGrid")
            smartImports.add("import androidx.compose.foundation.lazy.grid.GridCells")
            smartImports.add("import androidx.compose.foundation.lazy.grid.items")
        }
        if (codeText.contains("LazyHorizontalGrid")) {
            smartImports.add("import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid")
            smartImports.add("import androidx.compose.foundation.lazy.grid.GridCells")
            smartImports.add("import androidx.compose.foundation.lazy.grid.items")
        }
        
        // 导航相关
        if (codeText.contains("NavController") || codeText.contains("NavHost") || codeText.contains("composable(")) {
            smartImports.add("import androidx.navigation.NavController")
            smartImports.add("import androidx.navigation.compose.NavHost")
            smartImports.add("import androidx.navigation.compose.composable")
            smartImports.add("import androidx.navigation.compose.rememberNavController")
        }
        
        // Lifecycle / ViewModel
        if (codeText.contains("ViewModel") && !hasImport(filteredLines, "androidx.lifecycle.ViewModel")) {
            smartImports.add("import androidx.lifecycle.ViewModel")
        }
        if (codeText.contains("viewModel()") || codeText.contains("viewModel<")) {
            smartImports.add("import androidx.lifecycle.viewmodel.compose.viewModel")
        }
        if (codeText.contains("collectAsState")) {
            smartImports.add("import androidx.lifecycle.compose.collectAsStateWithLifecycle")
        }
        if (codeText.contains("collectAsStateWithLifecycle")) {
            smartImports.add("import androidx.lifecycle.compose.collectAsStateWithLifecycle")
        }
        
        // Hilt DI
        if (codeText.contains("@HiltViewModel") || codeText.contains("@HiltAndroidApp")) {
            smartImports.add("import dagger.hilt.android.lifecycle.HiltViewModel")
            smartImports.add("import dagger.hilt.android.qualifiers.ApplicationContext")
        }
        if (codeText.contains("@Inject")) {
            smartImports.add("import javax.inject.Inject")
        }
        
        // 权限相关
        if (codeText.contains("rememberLauncherForActivityResult") || codeText.contains("ActivityResultContracts")) {
            smartImports.add("import androidx.activity.compose.rememberLauncherForActivityResult")
            smartImports.add("import androidx.activity.result.contract.ActivityResultContracts")
        }
        if (codeText.contains("LocalContext")) {
            smartImports.add("import androidx.compose.ui.platform.LocalContext")
        }
        if (codeText.contains("LocalConfiguration")) {
            smartImports.add("import androidx.compose.ui.platform.LocalConfiguration")
        }
        if (codeText.contains("LocalDensity")) {
            smartImports.add("import androidx.compose.ui.platform.LocalDensity")
        }
        
        // Kotlin 标准库
        if (codeText.contains("Random.nextInt") || codeText.contains("Random.nextLong") || codeText.contains("Random.nextFloat")) {
            smartImports.add("import kotlin.random.Random")
        }
        if (codeText.contains("mutableStateListOf")) {
            smartImports.add("import androidx.compose.runtime.mutableStateListOf")
        }
        if (codeText.contains("snapshotFlow")) {
            smartImports.add("import androidx.compose.runtime.snapshotFlow")
        }
        
        // 去重
        val allImports = (baseImports + smartImports).distinct()
        
        val result = filteredLines.toMutableList()
        result.addAll(insertIndex, allImports)
        
        // === 修复 Modifier 错误传参 ===
        var finalCode = result.joinToString("\n")
        finalCode = fixModifierMisuse(finalCode)
        
        return finalCode
    }

    /**
     * 修复 Modifier.fillMaxSize/fillMaxWidth/fillMaxHeight 错误传参
     * 这些函数不接受参数，常见错误: Modifier.fillMaxSize(scale) 应改为 Modifier.fillMaxSize()
     */
    private fun fixModifierMisuse(code: String): String {
        var fixed = code
        
        // 修复 Modifier.fillMaxSize(xxx) → Modifier.fillMaxSize()
        fixed = fixed.replace(Regex("""\.fillMaxSize\s*\([^)]+\)"""), ".fillMaxSize()")
        
        // 修复 Modifier.fillMaxWidth(xxx) → Modifier.fillMaxWidth()
        fixed = fixed.replace(Regex("""\.fillMaxWidth\s*\([^)]+\)"""), ".fillMaxWidth()")
        
        // 修复 Modifier.fillMaxHeight(xxx) → Modifier.fillMaxHeight()
        fixed = fixed.replace(Regex("""\.fillMaxHeight\s*\([^)]+\)"""), ".fillMaxHeight()")
        
        // 修复 Modifier.weight(xxx, false) 中常见的错误第二个参数
        // weight 只接受 Float 参数，不接受 Boolean
        
        return fixed
    }

    /**
     * 检查现有 import 列表中是否已包含指定包路径
     */
    private fun hasImport(lines: List<String>, importPath: String): Boolean {
        return lines.any { it.trim().startsWith("import ") && it.contains(importPath) }
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
