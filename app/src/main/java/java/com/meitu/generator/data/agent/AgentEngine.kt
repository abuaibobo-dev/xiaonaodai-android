package com.meitu.generator.data.agent

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.meitu.generator.data.local.dao.PlanDao
import com.meitu.generator.data.local.entity.PlanEntity
import com.meitu.generator.data.model.AgentMessage
import com.meitu.generator.data.model.ToolContext
import com.meitu.generator.data.remote.OpenAIService
import com.meitu.generator.data.remote.GeminiService
import com.meitu.generator.data.remote.dto.GeminiRequest
import com.meitu.generator.data.remote.dto.GeminiContent
import com.meitu.generator.data.remote.dto.GeminiPart
import com.meitu.generator.data.remote.dto.GeminiInlineData
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.data.tools.BuildProgressCallback
import com.meitu.generator.data.tools.CloudBuildTool
import com.meitu.generator.data.tools.DeveloperTool
import com.meitu.generator.data.remote.dto.*
import com.meitu.generator.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * ReAct 循环引擎 - v4.8.0
 * 仅使用已验证可用的免费模型：OpenRouter（主力） + SambaNova（备用）
 */
@Singleton
class AgentEngine @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val toolRegistry: ToolRegistry,
    private val skillRegistry: SkillRegistry,
    private val agentMemory: AgentMemory,
    private val openAIService: OpenAIService,
    @Named("sambanovaService") private val sambanovaService: OpenAIService,
    @Named("deepseekService") private val deepseekService: OpenAIService,
    private val geminiService: GeminiService,
    @Named("openrouterService") private val openrouterService: OpenAIService,
    private val planDao: PlanDao,
    private val semanticCache: SemanticCache,
    private val memoryCompressor: MemoryCompressor,
    private val circuitBreaker: CircuitBreaker,
    private val preferenceLearner: PreferenceLearner,
    private val settingsRepository: SettingsRepository,
    private val developerTool: DeveloperTool,
    private val cloudBuildTool: CloudBuildTool,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) {
    companion object {
        const val MAX_REACT_CYCLES = 5

        /** 模型名 → 所属平台映射 */
        private val SAMBANOVA_MODELS = setOf(
            "Meta-Llama-3.3-70B-Instruct", "gpt-oss-120b",
            "DeepSeek-V3.1", "gemma-4-31B-it"
        )
        private val DEEPSEEK_MODELS = setOf("deepseek-chat")
        private val GEMINI_MODELS = setOf("gemini-2.0-flash")

        fun getModelPlatform(model: String): String = when {
            DEEPSEEK_MODELS.contains(model) -> "deepseek"
            GEMINI_MODELS.contains(model) -> "gemini"
            SAMBANOVA_MODELS.contains(model) -> "sambanova"
            else -> "openrouter" // 默认走 OpenRouter
        }
    }

    private val gson = Gson()

    private var statusCallback: ((String) -> Unit)? = null
    private var thinkingCallback: ((String) -> Unit)? = null

    private fun reportStatus(status: String) {
        statusCallback?.invoke(status)
    }

    private fun reportThinking(content: String) {
        thinkingCallback?.invoke(content)
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
     * 记录各平台 token 使用量到 SharedPreferences
     */
    private fun recordTokenUsage(platform: String, inputChars: Int, outputChars: Int) {
        val prefs = appContext.getSharedPreferences("token_usage", Context.MODE_PRIVATE)
        val inputKey = "${platform}_input_chars"
        val outputKey = "${platform}_output_chars"
        val totalInput = prefs.getInt(inputKey, 0) + inputChars
        val totalOutput = prefs.getInt(outputKey, 0) + outputChars
        prefs.edit()
            .putInt(inputKey, totalInput)
            .putInt(outputKey, totalOutput)
            .putLong("last_used_${platform}", System.currentTimeMillis())
            .apply()
    }

    fun classifyIntent(query: String): IntentRouter.IntentResult {
        return IntentRouter.classify(query)
    }

    suspend fun run(
        query: String,
        imageBase64: String? = null,
        imageMimeType: String? = null,
        buildProgressCallback: BuildProgressCallback? = null,
        statusCallback: ((String) -> Unit)? = null,
        deepThinkingEnabled: Boolean = false,
        webSearchEnabled: Boolean = false,
        thinkingCallback: ((String) -> Unit)? = null
    ): String {
        CloudBuildTool.progressCallback.set(buildProgressCallback)
        this.statusCallback = statusCallback
        this.thinkingCallback = thinkingCallback
        TokenEstimator.reset()
        TokenEstimator.account(query, "")

        if (imageBase64 == null) {
            val cached = semanticCache.lookup(query)
            if (cached != null) return cached
        }

        agentMemory.recordAction("用户: $query")

        val intent = IntentRouter.classify(query)
        val hasImage = imageBase64 != null
        val effectiveModel = resolveModel(query, imageBase64, deepThinkingEnabled)
        reportStatus(if (hasImage) "🖼️ 正在分析图片..." else "🧠 正在思考...")

        val result = when (intent.type) {
            IntentRouter.IntentType.TASK_GENERATE -> {
                generateAndBuild(query)
            }
            else -> {
                val q = if (intent.type == IntentRouter.IntentType.TASK_BUILD) "[路由:cloud_build] $query" else query
                val plan = if (intent.type == IntentRouter.IntentType.TASK_MODIFY) plan(q) else null
                if (plan != null) {
                    executeWithPlan(plan, q, imageBase64, imageMimeType)
                } else {
                    reactLoop(q, imageBase64, imageMimeType, effectiveModel, webSearchEnabled, hasImage)
                }
            }
        }

        if (imageBase64 == null) {
            semanticCache.store(query, result)
        }

        TokenEstimator.account("", result)
        if (TokenEstimator.shouldCompress()) {
            memoryCompressor.compressIfNeeded()
        }

        agentMemory.recordAction("AI: ${result.take(100)}")
        agentMemory.cleanup()
        preferenceLearner.recordAction(
            intentType = intent.type.name.lowercase(),
            modelUsed = effectiveModel,
            deepThinkingOn = deepThinkingEnabled,
            webSearchOn = webSearchEnabled,
            hasImage = hasImage,
            responseLength = result.length
        )
        preferenceLearner.analyzeAndLearn()

        CloudBuildTool.progressCallback.remove()
        this.statusCallback = null
        this.thinkingCallback = null

        return result
    }

    private suspend fun resolveModel(query: String, imageBase64: String?, deepThinkingEnabled: Boolean): String {
        val userSelectedModel = settingsRepository.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        if (userSelectedModel != Constants.OPENAI_MODEL && userSelectedModel != "auto") {
            return userSelectedModel
        }
        return ModelRouter.selectModel(query, imageBase64 != null, deepThinkingEnabled)
    }

    /**
     * ReAct 循环核心
     */
    private suspend fun reactLoop(
        query: String,
        imageBase64: String? = null,
        imageMimeType: String? = null,
        model: String = Constants.OPENAI_MODEL,
        webSearchEnabled: Boolean = false,
        hasImage: Boolean = false
    ): String {
        val messages = mutableListOf<OpenAIMessage>()
        val memoryPrompt = agentMemory.buildMemoryPrompt()
        val systemText = buildSystemPrompt(memoryPrompt, model, webSearchEnabled, hasImage)

        messages.add(OpenAIMessage(role = "system", content = systemText))

        if (imageBase64 != null) {
            val mime = imageMimeType ?: "image/jpeg"
            val textContent = if (query.isBlank()) "请分析这张图片的内容并给出详细描述" else query
            messages.add(OpenAIMessage(
                role = "user",
                contentParts = listOf(
                    ContentPart(type = "text", text = textContent),
                    ContentPart(type = "image_url", image_url = ImageUrl(url = "data:$mime;base64,$imageBase64"))
                )
            ))
        } else {
            messages.add(OpenAIMessage(role = "user", content = query))
        }

        val temperature = 0.7
        var cycleCount = 0
        var lastAssistantText = ""

        while (cycleCount < MAX_REACT_CYCLES) {
            cycleCount++

            try {
                reportStatus(if (hasImage) "🖼️ 正在分析图片..." else "🧠 正在思考...")

                val request = OpenAIRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    max_tokens = 4096
                )

                // 根据模型名路由到正确的 service
                val platform = getModelPlatform(model)
                if (platform == "gemini") {
                    // Gemini 使用独立 API 格式
                    val geminiMessages = mutableListOf<GeminiPart>()
                    val systemParts = mutableListOf<GeminiPart>()
                    systemParts.add(GeminiPart(text = systemText))
                    
                    for (msg in messages) {
                        if (msg.role == "system") {
                            systemParts.add(GeminiPart(text = msg.content ?: ""))
                        }
                    }
                    
                    // 构建 Gemini contents（跳过 system messages）
                    val geminiContents = mutableListOf<GeminiContent>()
                    for (msg in messages) {
                        if (msg.role == "system") continue
                        val role = if (msg.role == "assistant") "model" else "user"
                        val parts = mutableListOf<GeminiPart>()
                        if (msg.contentParts != null) {
                            for (cp in msg.contentParts) {
                                if (cp.type == "text" && cp.text != null) {
                                    parts.add(GeminiPart(text = cp.text))
                                } else if (cp.type == "image_url" && cp.image_url != null) {
                                    val base64Data = cp.image_url.url.substringAfter("base64,")
                                    parts.add(GeminiPart(inlineData = GeminiInlineData(
                                        mime_type = imageMimeType ?: "image/jpeg",
                                        data = base64Data
                                    )))
                                }
                            }
                        } else if (msg.content != null) {
                            parts.add(GeminiPart(text = msg.content))
                        }
                        if (parts.isNotEmpty()) {
                            geminiContents.add(GeminiContent(parts = parts))
                        }
                    }
                    
                    val geminiRequest = GeminiRequest(
                        contents = geminiContents,
                        systemInstruction = if (systemParts.isNotEmpty()) GeminiContent(parts = systemParts) else null
                    )
                    
                    val geminiResponse = geminiService.generateContent(
                        model = model,
                        apiKey = getGeminiApiKey(),
                        request = geminiRequest
                    )
                    
                    // 记录 token 使用量
                    recordTokenUsage("gemini", request.toString().length, geminiResponse.toString().length)
                    
                    if (geminiResponse.error != null) {
                        return "[Gemini 错误] ${geminiResponse.error.message ?: "未知错误"}"
                    }
                    
                    val geminiContent = geminiResponse.candidates?.firstOrNull()?.content
                    val geminiText = geminiContent?.parts?.firstOrNull { it.text != null }?.text
                    if (geminiText == null) break
                    lastAssistantText = geminiText
                    
                    // 检查工具调用（同 OpenAI 格式处理）
                    val toolCallResult = tryExtractToolCall(geminiText)
                    if (toolCallResult != null) {
                        val (toolName, args) = toolCallResult
                        val permError = PermissionInterceptor.check(appContext, toolName)
                        if (permError != null) {
                            messages.add(OpenAIMessage(role = "assistant", content = geminiText))
                            messages.add(OpenAIMessage(role = "user", content = "工具调用结果: $permError"))
                            continue
                        }
                        reportStatus("🔧 正在处理...")
                        val tool = toolRegistry.get(toolName)
                        val toolResult = if (tool != null) {
                            try {
                                val provider = circuitBreaker.getAvailableProvider(toolName)
                                val ctx = ToolContext(applicationContext = appContext)
                                val output = tool.execute(args, ctx)
                                circuitBreaker.reportSuccess(provider)
                                val reflection = Reflection.validate(toolName, output)
                                if (reflection.needsRetry) {
                                    "[工具${toolName}结果异常: ${reflection.reason}]，原始: ${output.take(200)}"
                                } else output
                            } catch (e: Exception) {
                                val provider = circuitBreaker.getAvailableProvider(toolName)
                                circuitBreaker.reportFailure(provider)
                                Reflection.handleToolError(toolName, e)
                            }
                        } else "工具 $toolName 未注册"
                        messages.add(OpenAIMessage(role = "assistant", content = geminiText))
                        messages.add(OpenAIMessage(role = "user", content = "工具 [$toolName] 执行结果：\n$toolResult\n\n请根据结果继续处理，或给出最终回复。"))
                        continue
                    }
                    
                    return geminiText
                }
                
                // OpenAI 兼容格式（DeepSeek / OpenRouter / SambaNova）
                val response = when (platform) {
                    "deepseek" -> deepseekService.chatCompletions(request, "Bearer ${getDeepSeekApiKey()}")
                    "sambanova" -> sambanovaService.chatCompletions(request, "Bearer ${getSambaNovaApiKey()}")
                    else -> openrouterService.chatCompletions(request, "Bearer ${getOpenRouterApiKey()}")
                }

                // 记录 token 使用量
                recordTokenUsage(platform, request.toString().length, response.toString().length)
                TokenEstimator.account(request.toString(), response.toString())

                if (response.error != null) {
                    return "[引擎错误] ${response.error.message ?: "未知错误"}"
                }

                val choice = response.choices?.firstOrNull() ?: break
                val content = choice.message?.content ?: break
                lastAssistantText = content

                // 检查工具调用
                val toolCallResult = tryExtractToolCall(content)
                if (toolCallResult != null) {
                    val (toolName, args) = toolCallResult

                    val permError = PermissionInterceptor.check(appContext, toolName)
                    if (permError != null) {
                        messages.add(OpenAIMessage(role = "assistant", content = content))
                        messages.add(OpenAIMessage(role = "user", content = "工具调用结果: $permError"))
                        continue
                    }

                    reportStatus("🔧 正在处理...")
                    val tool = toolRegistry.get(toolName)
                    val toolResult = if (tool != null) {
                        try {
                            val provider = circuitBreaker.getAvailableProvider(toolName)
                            val ctx = ToolContext(applicationContext = appContext)
                            val output = tool.execute(args, ctx)
                            circuitBreaker.reportSuccess(provider)
                            val reflection = Reflection.validate(toolName, output)
                            if (reflection.needsRetry) {
                                "[工具${toolName}结果异常: ${reflection.reason}]，原始: ${output.take(200)}"
                            } else output
                        } catch (e: Exception) {
                            val provider = circuitBreaker.getAvailableProvider(toolName)
                            circuitBreaker.reportFailure(provider)
                            Reflection.handleToolError(toolName, e)
                        }
                    } else "工具 $toolName 未注册"

                    messages.add(OpenAIMessage(role = "assistant", content = content))
                    messages.add(OpenAIMessage(
                        role = "user",
                        content = "工具 [$toolName] 执行结果：\n$toolResult\n\n请根据结果继续处理，或给出最终回复。"
                    ))
                    continue
                }

                return content

            } catch (e: Exception) {
                val errMsg = e.message ?: ""
                // 降级：OpenRouter → SambaNova
                if (errMsg.contains("402") || errMsg.contains("429") || errMsg.contains("quota") ||
                    errMsg.contains("insufficient") || errMsg.contains("balance") || errMsg.contains("rate")) {
                    reportStatus("⚠️ 服务暂时不可用，正在切换备用通道...")
                    // 尝试 SambaNova
                    val snResult = callSambaNova(query, imageBase64, imageMimeType)
                    if (!snResult.startsWith("[SambaNova 错误]") && !snResult.startsWith("[SambaNova 调用失败]")) {
                        return snResult
                    }
                    return "[ERROR] 所有备用模型均不可用，请稍后重试"
                }
                if (errMsg.contains("429")) {
                    delay(3000L)
                    if (cycleCount >= MAX_REACT_CYCLES) {
                        return "[请求限流，请稍后重试]"
                    }
                    continue
                }
                return "[引擎异常] ${errMsg.take(100).ifBlank { "未知错误" }}"
            }
        }

        return lastAssistantText.ifBlank { "[已达到最大推理轮次(${MAX_REACT_CYCLES})，结果可能不完整]" }
    }

    /** 调用 SambaNova 备用模型 */
    private suspend fun callSambaNova(query: String, imageBase64: String?, imageMimeType: String?): String {
        val apiKey = getSambaNovaApiKey()
        if (apiKey.isBlank()) return "[SambaNova 调用失败] 未配置 API Key"
        try {
            val messages = mutableListOf<OpenAIMessage>()
            val memoryPrompt = agentMemory.buildMemoryPrompt()
            messages.add(OpenAIMessage(role = "system", content = "你是「布老师」AI 智能体助手，请用中文回复用户。当前记忆：$memoryPrompt"))

            if (imageBase64 != null) {
                val mime = imageMimeType ?: "image/jpeg"
                val textContent = if (query.isBlank()) "请分析这张图片" else query
                messages.add(OpenAIMessage(
                    role = "user",
                    contentParts = listOf(
                        ContentPart(type = "text", text = textContent),
                        ContentPart(type = "image_url", image_url = ImageUrl(url = "data:$mime;base64,$imageBase64"))
                    )
                ))
            } else {
                messages.add(OpenAIMessage(role = "user", content = query))
            }

            val request = OpenAIRequest(
                model = Constants.SAMBANOVA_MODEL,
                messages = messages,
                temperature = 0.7,
                max_tokens = 4096
            )

            val response = sambanovaService.chatCompletions(request, "Bearer $apiKey")

            if (response.error != null) {
                return "[SambaNova 错误] ${response.error.message ?: "未知错误"}"
            }

            val choice = response.choices?.firstOrNull()
            val content = choice?.message?.content
            return content ?: "[SambaNova 返回为空]"
        } catch (e: Exception) {
            return "[SambaNova 调用失败] ${e.message?.take(100) ?: "未知错误"}"
        }
    }

    private fun tryExtractToolCall(content: String): Pair<String, Map<String, Any>>? {
        val trimmed = content.trim()
        val jsonStr = if (trimmed.startsWith("```json")) {
            trimmed.removePrefix("```json").removeSuffix("```").trim()
        } else if (trimmed.startsWith("{") && (trimmed.contains("\"tool\"") || trimmed.contains("\"tool_call\""))) {
            trimmed
        } else return null

        return try {
            val obj = Gson().fromJson(jsonStr, JsonObject::class.java)
            val toolName = obj.get("tool")?.asString ?: obj.get("tool_call")?.asString ?: return null
            val argsObj = obj.getAsJsonObject("args") ?: obj.getAsJsonObject("parameters")
            val args: Map<String, Any> = if (argsObj != null) {
                gson.fromJson(argsObj, object : TypeToken<Map<String, Any>>() {}.type)
            } else emptyMap()
            Pair(toolName, args)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun plan(query: String): PlanEntity? {
        if (query.length < 10 || isSimpleQuery(query)) return null
        try {
            val planPrompt = """你是一个任务规划器。分析用户请求，判断是否需要多步执行。
如果需要，输出JSON计划；如果一步即可完成，返回null。

用户请求：$query

可用工具：${toolRegistry.getAll().joinToString(", ") { it.name }}

输出格式（需要多步时）：
{"steps":[{"step":1,"tool":"工具名","reason":"原因"},{"step":2,"tool":"工具名","reason":"原因"}]}

输出null如果一步即可完成。只输出JSON，不要解释。"""

            val request = OpenAIRequest(
                model = Constants.OPENAI_MODEL,
                messages = listOf(OpenAIMessage(role = "user", content = planPrompt)),
                temperature = 0.3,
                max_tokens = 500
            )
            val response = openrouterService.chatCompletions(
                request = request,
                authorization = "Bearer ${getOpenRouterApiKey()}"
            )
            val text = response.choices?.firstOrNull()?.message?.content?.trim() ?: return null

            if (text == "null" || text.contains("\"null\"")) return null

            val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val planObj = gson.fromJson(cleanJson, JsonObject::class.java)
            val steps = planObj.getAsJsonArray("steps") ?: return null

            if (steps.size() <= 1) return null

            val planEntity = PlanEntity(
                sessionId = System.currentTimeMillis().toString(),
                steps = steps.toString(),
                currentStep = 0,
                status = "pending",
                originalQuery = query
            )
            val planId = planDao.insert(planEntity)
            return planEntity.copy(id = planId, status = "running")
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun executeWithPlan(
        plan: PlanEntity,
        query: String,
        imageBase64: String?,
        imageMimeType: String?
    ): String {
        val stepList: List<Map<String, Any>> = try {
            gson.fromJson(plan.steps, object : TypeToken<List<Map<String, Any>>>() {}.type)
        } catch (e: Exception) {
            return reactLoop(query, imageBase64, imageMimeType)
        }

        val results = mutableListOf<String>()
        var currentStep = plan.currentStep

        for (i in currentStep until stepList.size) {
            val step = stepList[i]
            val toolName = step["tool"] as? String ?: continue
            val tool = toolRegistry.get(toolName) ?: continue

            try {
                val ctx = ToolContext(applicationContext = appContext)
                val result = tool.execute(emptyMap(), ctx)
                val reflection = Reflection.validate(toolName, result)

                results.add("步骤${i + 1}(${toolName}): ${if (reflection.needsRetry) "[异常: ${reflection.reason}]" else result.take(200)}")
                planDao.update(plan.copy(currentStep = i + 1, status = "running", updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                results.add("步骤${i + 1}(${toolName}): 失败 - ${e.message?.take(100)}")
            }
        }

        planDao.update(plan.copy(status = "completed", updatedAt = System.currentTimeMillis()))

        val summaryPrompt = """用户请求：$query
执行计划已完成，各步骤结果：
${results.joinToString("\n")}

请基于以上结果，用简洁友好的中文回复用户。"""

        return reactLoop(summaryPrompt, null, null)
    }

    private suspend fun generateAndBuild(query: String): String {
        reportStatus("🧠 正在生成代码...")
        val devArgs = mapOf(
            "filePath" to "app/src/main/java/com/example/app/MainActivity.kt",
            "requirement" to query,
            "context" to "这是一个 Android 项目，使用 Jetpack Compose，Hilt 依赖注入"
        )
        val ctx = ToolContext(applicationContext = appContext)
        val devResult = developerTool.execute(devArgs, ctx)

        if (devResult.startsWith("错误") || devResult.startsWith("代码生成失败")) {
            return "代码生成失败: $devResult"
        }

        val code = extractKotlinCode(devResult)
        if (code.isBlank()) {
            return "无法从生成结果中提取代码"
        }

        reportStatus("📤 正在推送代码到 GitHub...")
        val buildArgs = mapOf(
            "projectCode" to mapOf(
                "app/src/main/java/com/example/app/MainActivity.kt" to code
            ),
            "commitMessage" to "Auto generate: ${query.take(50)}"
        )
        val buildResult = cloudBuildTool.execute(buildArgs, ctx)

        return buildString {
            appendLine("✅ 代码已生成并推送编译")
            appendLine()
            appendLine("生成的代码:")
            appendLine("```kotlin")
            appendLine(code.take(500))
            if (code.length > 500) appendLine("... (代码过长，已截断)")
            appendLine("```")
            appendLine()
            appendLine("编译结果:")
            appendLine(buildResult)
        }
    }

    private fun extractKotlinCode(result: String): String {
        val codeBlockRegex = Regex("```kotlin\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(result)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val genericRegex = Regex("```\\s*\\n(.*?)\\n```", RegexOption.DOT_MATCHES_ALL)
        val genericMatch = genericRegex.find(result)
        if (genericMatch != null) {
            return genericMatch.groupValues[1].trim()
        }
        return result.substringAfter("```kotlin").substringBefore("```").trim()
    }

    private fun isSimpleQuery(query: String): Boolean {
        val simplePatterns = listOf("帮助", "怎么用", "hi", "hello", "你好", "谢谢")
        return simplePatterns.any { query.contains(it, ignoreCase = true) }
    }

    private suspend fun buildSystemPrompt(memoryPrompt: String, currentModel: String, webSearchEnabled: Boolean, hasImage: Boolean): String {
        val enabledTools = toolRegistry.getAll().map { "${it.name}: ${it.description}" }
        val sb = StringBuilder()

        sb.appendLine("你是「布老师」v4.8.0 AI 智能体引擎，一个纯AI智能体助手。")
        sb.appendLine("拥有以下工具能力：")
        sb.appendLine(enabledTools.joinToString("\n"))
        sb.appendLine()

        // 注入学习到的用户偏好
        val prefs = preferenceLearner.getLearnedPreferences()
        if (prefs.isNotEmpty()) {
            sb.appendLine("[学习到的用户偏好 - 请据此调整回复风格]")
            prefs.forEach { (key, value) ->
                val desc = when (key) {
                    "preferred_intent" -> "常用操作类型: $value"
                    "preferred_model" -> "偏好模型: $value"
                    "prefer_deep_thinking" -> "深度思考偏好: ${if (value == "true") "喜欢开启" else "不太使用"}"
                    "prefer_web_search" -> "联网搜索偏好: ${if (value == "true") "经常使用" else "不太使用"}"
                    "preferred_response_style" -> when (value) {
                        "concise" -> "回复风格: 偏好简洁"
                        "detailed" -> "回复风格: 偏好详细"
                        else -> "回复风格: 平衡"
                    }
                    "active_period" -> "活跃时段: $value"
                    else -> "$key: $value"
                }
                sb.appendLine("- $desc")
            }
            sb.appendLine()
        }

        if (hasImage) {
            sb.appendLine("【当前模式：图片分析】")
            sb.appendLine("用户发送了图片，请仔细分析图片内容并给出详细、有用的回复。")
            sb.appendLine()
        }

        if (webSearchEnabled) {
            sb.appendLine("【当前模式：联网搜索】")
            sb.appendLine("当用户询问实时信息、最新数据、新闻、价格、天气等需要时效性内容的问题时，")
            sb.appendLine("请优先使用 web_search 工具搜索，再基于搜索结果回答。")
            sb.appendLine()
        }

        sb.appendLine("用户记忆：")
        sb.appendLine(memoryPrompt)
        sb.appendLine()
        sb.appendLine("规则：")
        sb.appendLine("1. 需要使用工具时，返回 JSON：{\"tool\":\"工具名\",\"args\":{参数}}")
        sb.appendLine("2. 可以连续调用多个工具")
        sb.appendLine("3. 用中文回复用户")
        sb.appendLine("4. 如果不需要工具，直接回答即可")

        return sb.toString()
    }
}
