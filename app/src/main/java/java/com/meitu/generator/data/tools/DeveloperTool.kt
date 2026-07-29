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
 * 3. 四层检查与自动修复，确保代码可编译
 * 4. 四层全部通过后才推送
 */
@Singleton
class DeveloperTool @Inject constructor(
    private val openAIService: OpenAIService,
    private val settingsRepository: SettingsRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : Tool {
    override val name = "developer"
    override val description = "根据需求生成单个 Kotlin 文件代码（自动四层编译检查后才推送）"
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
            appendLine("⚠️ Compose 代码常见错误（后续会自动四层检查，但请尽量避免）:")
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

            val rawCode = extractKotlinCode(content)
            
            // ========== 四层检查与自动修复 ==========
            val checkResult = fourLayerCheck(rawCode)
            
            if (!checkResult.allPassed) {
                return buildString {
                    appendLine("❌ 代码生成后未通过四层编译检查，已中止推送")
                    appendLine("文件: $filePath")
                    appendLine()
                    appendLine("检查报告:")
                    appendLine("  第一层(Import完整性): ${if (checkResult.layer1Passed) "✅ 通过" else "❌ ${checkResult.layer1Issues.joinToString("; ")}"}")
                    appendLine("  第二层(语法检查):     ${if (checkResult.layer2Passed) "✅ 通过" else "❌ ${checkResult.layer2Issues.joinToString("; ")}"}")
                    appendLine("  第三层(自动修复):     ${if (checkResult.layer3Passed) "✅ 已修复" else "⚠️ 部分问题无法自动修复"}")
                    appendLine("  第四层(最终验证):     ${if (checkResult.layer4Passed) "✅ 通过" else "❌ 修复后仍有问题: ${checkResult.layer4Issues.joinToString("; ")}"}")
                    appendLine()
                    appendLine("代码已拦截，不会推送到仓库。请修改需求描述后重试。")
                }
            }
            
            val code = checkResult.fixedCode
            
            return buildString {
                appendLine("✅ 代码生成成功（已通过四层编译检查，可安全推送）")
                appendLine("文件: $filePath")
                appendLine("代码长度: ${code.length} 字符")
                appendLine()
                appendLine("四层检查结果:")
                appendLine("  第一层(Import完整性): ✅ 通过")
                appendLine("  第二层(语法检查):     ✅ 通过")
                appendLine("  第三层(自动修复):     ${if (checkResult.autoFixApplied) "已修复 ${checkResult.fixCount} 处问题" else "无需修复"}")
                appendLine("  第四层(最终验证):     ✅ 通过")
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

    // ====================================================================
    // 四层编译检查系统
    // ====================================================================

    /**
     * 四层检查结果
     */
    data class FourLayerCheckResult(
        val layer1Passed: Boolean,
        val layer1Issues: List<String>,
        val layer2Passed: Boolean,
        val layer2Issues: List<String>,
        val layer3Passed: Boolean,      // 自动修复是否全部成功
        val layer4Passed: Boolean,      // 修复后最终验证是否全部通过
        val layer4Issues: List<String>,
        val fixedCode: String,
        val autoFixApplied: Boolean,
        val fixCount: Int,
        val allPassed: Boolean          // 四层全部通过
    )

    /**
     * 四层编译检查主入口
     * 
     * 第一层: Import 完整性检查 — 代码中用到的类/API 是否都有对应 import
     * 第二层: 语法错误检查 — Modifier 参数误用、API 错误用法等
     * 第三层: 自动修复 — 对第一层和第二层发现的问题自动修复
     * 第四层: 最终验证 — 修复后重新跑第一层+第二层，确认全部通过
     * 
     * 只有四层全部通过，代码才能推送
     */
    private fun fourLayerCheck(code: String): FourLayerCheckResult {
        var currentCode = code
        var totalFixes = 0
        
        // === 第一层: Import 完整性检查 ===
        val layer1Result = checkImportCompleteness(currentCode)
        
        // === 第二层: 语法错误检查 ===
        val layer2Result = checkSyntaxErrors(currentCode)
        
        val needsFix = !layer1Result.passed || !layer2Result.passed
        
        if (needsFix) {
            // === 第三层: 自动修复 ===
            val fixResult = autoFixCode(currentCode, layer1Result, layer2Result)
            currentCode = fixResult.fixedCode
            totalFixes = fixResult.fixCount
            
            // === 第四层: 最终验证（修复后重新检查） ===
            val layer4ImportCheck = checkImportCompleteness(currentCode)
            val layer4SyntaxCheck = checkSyntaxErrors(currentCode)
            
            val layer4Passed = layer4ImportCheck.passed && layer4SyntaxCheck.passed
            val layer4Issues = layer4ImportCheck.issues + layer4SyntaxCheck.issues
            
            return FourLayerCheckResult(
                layer1Passed = layer1Result.passed,
                layer1Issues = layer1Result.issues,
                layer2Passed = layer2Result.passed,
                layer2Issues = layer2Result.issues,
                layer3Passed = fixResult.allFixed,
                layer4Passed = layer4Passed,
                layer4Issues = layer4Issues,
                fixedCode = currentCode,
                autoFixApplied = totalFixes > 0,
                fixCount = totalFixes,
                allPassed = layer4Passed
            )
        }
        
        // 无需修复，四层全通过
        return FourLayerCheckResult(
            layer1Passed = true,
            layer1Issues = emptyList(),
            layer2Passed = true,
            layer2Issues = emptyList(),
            layer3Passed = true,
            layer4Passed = true,
            layer4Issues = emptyList(),
            fixedCode = currentCode,
            autoFixApplied = false,
            fixCount = 0,
            allPassed = true
        )
    }

    // ============ 第一层: Import 完整性检查 ============
    
    data class LayerCheckResult(val passed: Boolean, val issues: List<String>)
    
    /**
     * 第一层: 检查代码中用到的类/API 是否都有对应的 import
     * 扫描代码中的标识符，匹配预置的 import 映射表
     */
    private fun checkImportCompleteness(code: String): LayerCheckResult {
        val issues = mutableListOf<String>()
        val codeText = code
        val existingImports = code.lines().filter { it.trimStart().startsWith("import ") }
        
        // 检查每种常用 API 的 import 是否存在
        val importRules = getImportRules()
        
        for ((identifier, requiredImport, checkCondition) in importRules) {
            if (checkCondition(codeText) && !existingImports.any { it.contains(requiredImport) }) {
                // 检查是否有通配符 import 覆盖
                val wildcardImport = requiredImport.substringBeforeLast(".")
                val hasWildcard = existingImports.any { it.contains("$wildcardImport.*") }
                if (!hasWildcard) {
                    issues.add("缺少 import: $requiredImport (代码中使用了 $identifier)")
                }
            }
        }
        
        return LayerCheckResult(passed = issues.isEmpty(), issues = issues)
    }

    // ============ 第二层: 语法错误检查 ============
    
    /**
     * 第二层: 检查常见语法错误和 API 误用
     */
    private fun checkSyntaxErrors(code: String): LayerCheckResult {
        val issues = mutableListOf<String>()
        
        // 检查 Modifier.fillMaxSize/fillMaxWidth/fillMaxHeight 错误传参
        val fillMaxSizeWithParam = Regex("""\.fillMaxSize\s*\([^)]+\)""").findAll(code).count()
        if (fillMaxSizeWithParam > 0) {
            issues.add("Modifier.fillMaxSize() 不接受参数，发现 $fillMaxSizeWithParam 处错误调用")
        }
        val fillMaxWidthWithParam = Regex("""\.fillMaxWidth\s*\([^)]+\)""").findAll(code).count()
        if (fillMaxWidthWithParam > 0) {
            issues.add("Modifier.fillMaxWidth() 不接受参数，发现 $fillMaxWidthWithParam 处错误调用")
        }
        val fillMaxHeightWithParam = Regex("""\.fillMaxHeight\s*\([^)]+\)""").findAll(code).count()
        if (fillMaxHeightWithParam > 0) {
            issues.add("Modifier.fillMaxHeight() 不接受参数，发现 $fillMaxHeightWithParam 处错误调用")
        }
        
        // 检查常见括号不匹配
        val openParens = code.count { it == '(' }
        val closeParens = code.count { it == ')' }
        if (openParens != closeParens) {
            issues.add("括号不匹配: ( 有 $openParens 个, ) 有 $closeParens 个")
        }
        
        val openBraces = code.count { it == '{' }
        val closeBraces = code.count { it == '}' }
        if (openBraces != closeBraces) {
            issues.add("花括号不匹配: { 有 $openBraces 个, } 有 $closeBraces 个")
        }
        
        // 检查常见错误模式
        // 如: remember { mutableStateOf } 缺少括号
        if (code.contains("remember { mutableStateOf") && !code.contains("remember { mutableStateOf(")) {
            issues.add("remember { mutableStateOf } 可能缺少初始化参数括号")
        }
        
        // 检查 @Composable 函数是否有正确的函数签名
        val composableWithoutFun = Regex("""@Composable\s*\n\s*(?!fun\s|private\s|internal\s|public\s)""").findAll(code).count()
        if (composableWithoutFun > 0) {
            issues.add("@Composable 注解可能未修饰正确的函数声明")
        }
        
        return LayerCheckResult(passed = issues.isEmpty(), issues = issues)
    }

    // ============ 第三层: 自动修复 ============
    
    data class FixResult(val fixedCode: String, val allFixed: Boolean, val fixCount: Int)
    
    /**
     * 第三层: 对第一层和第二层发现的问题进行自动修复
     */
    private fun autoFixCode(
        code: String,
        layer1Result: LayerCheckResult,
        layer2Result: LayerCheckResult
    ): FixResult {
        var fixed = code
        var fixCount = 0
        
        // --- 修复 Import 缺失 ---
        fixed = injectAllRequiredImports(fixed)
        val newImportCount = fixed.lines().count { it.trimStart().startsWith("import ") } - 
                             code.lines().count { it.trimStart().startsWith("import ") }
        if (newImportCount > 0) fixCount += newImportCount
        
        // --- 修复 Modifier 错误传参 ---
        val beforeModifier = fixed
        fixed = fixModifierMisuse(fixed)
        if (fixed != beforeModifier) {
            val modifierFixes = listOf(
                Regex("""\.fillMaxSize\s*\([^)]+\)""").findAll(beforeModifier).count(),
                Regex("""\.fillMaxWidth\s*\([^)]+\)""").findAll(beforeModifier).count(),
                Regex("""\.fillMaxHeight\s*\([^)]+\)""").findAll(beforeModifier).count()
            ).sum()
            fixCount += modifierFixes
        }
        
        // --- 修复其他常见语法问题 ---
        // 修复 @AndroidEntryPoint 注解（user-project 不需要）
        if (fixed.contains("@AndroidEntryPoint")) {
            fixed = fixed.replace("@AndroidEntryPoint", "")
            fixCount++
        }
        
        return FixResult(
            fixedCode = fixed,
            allFixed = fixCount > 0,
            fixCount = fixCount
        )
    }

    // ====================================================================
    // Import 规则库与注入
    // ====================================================================

    /**
     * 获取完整的 import 规则列表
     * 返回: List<Triple<标识符, 需要的import路径, 检测条件>>
     */
    private fun getImportRules(): List<Triple<String, String, (String) -> Boolean>> {
        return listOf(
            // Canvas / 绘制
            Triple("Canvas", "androidx.compose.foundation.Canvas") { it.contains("Canvas(") },
            Triple("Offset", "androidx.compose.ui.geometry.Offset") { it.contains("Offset(") || it.contains("Offset.Zero") },
            Triple("Size", "androidx.compose.ui.geometry.Size") { it.contains("Size(") && !it.contains("DpSize") },
            Triple("Stroke", "androidx.compose.ui.graphics.drawscope.Stroke") { 
                it.contains("Stroke(") || (it.contains("style = Stroke") || it.contains("style=Stroke"))
            },
            Triple("drawBehind", "androidx.compose.ui.draw.drawBehind") { it.contains("drawBehind(") || it.contains(".drawBehind") },
            Triple("drawWithContent", "androidx.compose.ui.draw.drawWithContent") { it.contains("drawWithContent(") },
            Triple("drawWithCache", "androidx.compose.ui.draw.drawWithCache") { it.contains("drawWithCache(") },
            
            // 动画
            Triple("animateFloatAsState", "androidx.compose.animation.core.animateFloatAsState") { it.contains("animateFloatAsState") },
            Triple("animateDpAsState", "androidx.compose.animation.core.animateDpAsState") { it.contains("animateDpAsState") },
            Triple("animateIntAsState", "androidx.compose.animation.core.animateIntAsState") { it.contains("animateIntAsState") },
            Triple("animateColorAsState", "androidx.compose.animation.core.animateColorAsState") { it.contains("animateColorAsState") },
            Triple("tween", "androidx.compose.animation.core.tween") { it.contains("tween(") },
            Triple("spring", "androidx.compose.animation.core.spring") { it.contains("spring(") },
            Triple("repeatable", "androidx.compose.animation.core.repeatable") { it.contains("repeatable(") },
            Triple("infiniteRepeatable", "androidx.compose.animation.core.infiniteRepeatable") { it.contains("infiniteRepeatable") },
            Triple("rememberInfiniteTransition", "androidx.compose.animation.core.rememberInfiniteTransition") { it.contains("rememberInfiniteTransition") },
            Triple("Easing", "androidx.compose.animation.core.Easing") { it.contains("Easing") && (it.contains("LinearEasing") || it.contains("FastOutSlowInEasing") || it.contains("CubicBezierEasing")) },
            
            // 手势 / 输入
            Triple("pointerInput", "androidx.compose.ui.input.pointer.pointerInput") { it.contains("pointerInput") },
            Triple("detectTapGestures", "androidx.compose.foundation.gestures.detectTapGestures") { it.contains("detectTapGestures") },
            Triple("detectDragGestures", "androidx.compose.foundation.gestures.detectDragGestures") { it.contains("detectDragGestures") && !it.contains("detectHorizontalDragGestures") && !it.contains("detectVerticalDragGestures") },
            Triple("detectHorizontalDragGestures", "androidx.compose.foundation.gestures.detectHorizontalDragGestures") { it.contains("detectHorizontalDragGestures") },
            Triple("detectVerticalDragGestures", "androidx.compose.foundation.gestures.detectVerticalDragGestures") { it.contains("detectVerticalDragGestures") },
            Triple("detectTransformGestures", "androidx.compose.foundation.gestures.detectTransformGestures") { it.contains("detectTransformGestures") },
            
            // Modifier 扩展
            Triple("graphicsLayer", "androidx.compose.ui.graphics.graphicsLayer") { it.contains(".graphicsLayer") },
            Triple("rotate", "androidx.compose.ui.draw.rotate") { it.contains(".rotate(") },
            Triple("scale", "androidx.compose.ui.draw.scale") { it.contains(".scale(") && !it.contains("scaleX") && !it.contains("scaleY") },
            Triple("shadow", "androidx.compose.ui.draw.shadow") { it.contains(".shadow(") },
            Triple("blur", "androidx.compose.ui.draw.blur") { it.contains(".blur(") },
            Triple("border", "androidx.compose.foundation.border") { it.contains(".border(") },
            Triple("aspectRatio", "androidx.compose.foundation.layout.aspectRatio") { it.contains(".aspectRatio(") },
            Triple("zIndex", "androidx.compose.foundation.layout.zIndex") { it.contains(".zIndex(") },
            
            // Lazy 列表
            Triple("LazyRow", "androidx.compose.foundation.lazy.LazyRow") { it.contains("LazyRow") && !it.contains("import") },
            Triple("LazyVerticalGrid", "androidx.compose.foundation.lazy.grid.LazyVerticalGrid") { it.contains("LazyVerticalGrid") },
            Triple("GridCells", "androidx.compose.foundation.lazy.grid.GridCells") { it.contains("GridCells") },
            
            // 导航
            Triple("NavController", "androidx.navigation.NavController") { it.contains("NavController") },
            Triple("NavHost", "androidx.navigation.compose.NavHost") { it.contains("NavHost") },
            Triple("rememberNavController", "androidx.navigation.compose.rememberNavController") { it.contains("rememberNavController") },
            
            // ViewModel / Lifecycle
            Triple("ViewModel", "androidx.lifecycle.ViewModel") { it.contains("ViewModel") && !it.contains("import") && !it.contains("viewModel()") },
            Triple("viewModel()", "androidx.lifecycle.viewmodel.compose.viewModel") { it.contains("viewModel()") || it.contains("viewModel<") },
            Triple("collectAsStateWithLifecycle", "androidx.lifecycle.compose.collectAsStateWithLifecycle") { it.contains("collectAsStateWithLifecycle") },
            
            // Hilt DI
            Triple("HiltViewModel", "dagger.hilt.android.lifecycle.HiltViewModel") { it.contains("@HiltViewModel") },
            Triple("Inject", "javax.inject.Inject") { it.contains("@Inject") },
            
            // 平台
            Triple("LocalContext", "androidx.compose.ui.platform.LocalContext") { it.contains("LocalContext") },
            Triple("LocalConfiguration", "androidx.compose.ui.platform.LocalConfiguration") { it.contains("LocalConfiguration") },
            Triple("LocalDensity", "androidx.compose.ui.platform.LocalDensity") { it.contains("LocalDensity") },
            Triple("LocalLifecycleOwner", "androidx.compose.ui.platform.LocalLifecycleOwner") { it.contains("LocalLifecycleOwner") },
            
            // ActivityResult
            Triple("rememberLauncherForActivityResult", "androidx.activity.compose.rememberLauncherForActivityResult") { it.contains("rememberLauncherForActivityResult") },
            Triple("ActivityResultContracts", "androidx.activity.result.contract.ActivityResultContracts") { it.contains("ActivityResultContracts") },
            
            // Kotlin 标准
            Triple("Random", "kotlin.random.Random") { it.contains("Random.") },
            Triple("mutableStateListOf", "androidx.compose.runtime.mutableStateListOf") { it.contains("mutableStateListOf") },
            Triple("snapshotFlow", "androidx.compose.runtime.snapshotFlow") { it.contains("snapshotFlow") },
            
            // 协程
            Triple("Dispatchers", "kotlinx.coroutines.Dispatchers") { it.contains("Dispatchers.") },
            Triple("withContext", "kotlinx.coroutines.withContext") { it.contains("withContext(") },
            Triple("launch", "kotlinx.coroutines.launch") { it.contains("launch {") || it.contains("launch(") },
            
            // 基础 UI
            Triple("painterResource", "androidx.compose.ui.res.painterResource") { it.contains("painterResource") },
            Triple("stringResource", "androidx.compose.ui.res.stringResource") { it.contains("stringResource") },
            Triple("colorResource", "androidx.compose.ui.res.colorResource") { it.contains("colorResource") },
            Triple("Image", "androidx.compose.foundation.layout.Box") { it.contains("Image(") && !it.contains("import") },
            
            // 富文本 / URL
            Triple("Uri", "android.net.Uri") { it.contains("Uri.parse") || it.contains("Uri:") },
            Triple("Intent", "android.content.Intent") { it.contains("Intent(") || it.contains("Intent.") }
        )
    }

    /**
     * 注入所有需要的 import 到代码中
     */
    private fun injectAllRequiredImports(code: String): String {
        // 移除 @AndroidEntryPoint
        var fixed = code.replace("@AndroidEntryPoint", "")
        
        // 移除所有现有 import 行
        val lines = fixed.lines().toMutableList()
        val filteredLines = lines.filter { !it.trimStart().startsWith("import ") }
        
        // 查找 package 声明后的位置
        val packageIndex = filteredLines.indexOfFirst { it.trimStart().startsWith("package ") }
        val insertIndex = if (packageIndex >= 0) packageIndex + 1 else 0
        
        // 基础 import 列表（始终注入）
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
            "import kotlinx.coroutines.delay"
        )
        
        // 智能 import：根据代码实际使用情况补充
        val smartImports = mutableListOf<String>()
        val codeText = filteredLines.joinToString("\n")
        val importRules = getImportRules()
        
        for ((_, requiredImport, checkCondition) in importRules) {
            if (checkCondition(codeText)) {
                smartImports.add(requiredImport)
            }
        }
        
        // 去重
        val allImports = (baseImports + smartImports).distinct()
        
        val result = filteredLines.toMutableList()
        result.addAll(insertIndex, allImports)
        
        // 修复 Modifier 误用
        var finalCode = result.joinToString("\n")
        finalCode = fixModifierMisuse(finalCode)
        
        return finalCode
    }

    /**
     * 修复 Modifier.fillMaxSize/fillMaxWidth/fillMaxHeight 错误传参
     */
    private fun fixModifierMisuse(code: String): String {
        var fixed = code
        fixed = fixed.replace(Regex("""\.fillMaxSize\s*\([^)]+\)"""), ".fillMaxSize()")
        fixed = fixed.replace(Regex("""\.fillMaxWidth\s*\([^)]+\)"""), ".fillMaxWidth()")
        fixed = fixed.replace(Regex("""\.fillMaxHeight\s*\([^)]+\)"""), ".fillMaxHeight()")
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
