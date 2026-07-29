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
 * 主力：DeepSeek Chat/Reasoner，备选：Google Gemini
 */
@Singleton
class AgentEngine @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val toolRegistry: ToolRegistry,
    private val skillRegistry: SkillRegistry,
    private val agentMemory: AgentMemory,
    private val openAIService: OpenAIService,
    @Named("deepseekService") private val deepseekService: OpenAIService,
    @Named("googleService") private val googleService: OpenAIService,
    @Named("openaiService") private val openaiRealService: OpenAIService,
    @Named("groqService") private val groqService: OpenAIService,
    @Named("siliconflowService") private val siliconflowService: OpenAIService,
    @Named("moonshotService") private val moonshotService: OpenAIService,
    @Named("zhipuService") private val zhipuService: OpenAIService,
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

        /** DeepSeek 模型集合 */
        private val DEEPSEEK_MODELS = setOf(
            "deepseek-chat", "deepseek-reasoner"
        )

        /** Google Gemini 模型集合 */
        val GOOGLE_MODELS = setOf(
            "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-1.5-flash"
        )

        /** OpenAI 模型集合 */
        val OPENAI_MODELS = setOf(
            "gpt-4o", "gpt-4o-mini"
        )

        /** Groq 模型集合 */
        val GROQ_MODELS = setOf(
            "llama-3.3-70b-versatile", "llama-3.1-8b-instant"
        )

        /** SiliconFlow (硅基流动) 模型集合 */
        val SILICONFLOW_MODELS = setOf(
            "deepseek-ai/DeepSeek-V3", "Qwen/Qwen2.5-72B-Instruct"
        )

        /** Moonshot (Kimi) 模型集合 */
        val MOONSHOT_MODELS = setOf(
            "moonshot-v1-8k", "moonshot-v1-32k"
        )

        /** Zhipu AI (智谱) 模型集合 */
        val ZHIPU_MODELS = setOf(
            "glm-4-flash", "glm-4"
        )

        /** 模型名 → 所属平台映射 */
        fun getModelPlatform(model: String): String = when {
            DEEPSEEK_MODELS.contains(model) -> "deepseek"
            GOOGLE_MODELS.contains(model) -> "google"
            OPENAI_MODELS.contains(model) -> "openai"
            GROQ_MODELS.contains(model) -> "groq"
            SILICONFLOW_MODELS.contains(model) -> "siliconflow"
            MOONSHOT_MODELS.contains(model) -> "moonshot"
            ZHIPU_MODELS.contains(model) -> "zhipu"
            else -> "deepseek" // 默认走 DeepSeek
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


    private fun getGoogleApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_GOOGLE_API_KEY, "") ?: ""
        return if (savedKey.isNotBlank()) savedKey else Constants.GOOGLE_API_KEY
    }

    private fun getDeepSeekApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: ""
        return if (savedKey.isNotBlank()) savedKey else Constants.OPENAI_API_KEY
    }

    private fun getOpenAIApiKey(): String {
        return securePrefs.getString(Constants.KEY_OPENAI_API_KEY, "") ?: ""
    }

    private fun getGroqApiKey(): String {
        return securePrefs.getString(Constants.KEY_GROQ_API_KEY, "") ?: ""
    }

    private fun getSiliconFlowApiKey(): String {
        return securePrefs.getString(Constants.KEY_SILICONFLOW_API_KEY, "") ?: ""
    }

    private fun getMoonshotApiKey(): String {
        return securePrefs.getString(Constants.KEY_MOONSHOT_API_KEY, "") ?: ""
    }

    private fun getZhipuApiKey(): String {
        return securePrefs.getString(Constants.KEY_ZHIPU_API_KEY, "") ?: ""
    }

    /**
     * 根据平台获取 API Key，如果未配置则返回 null（调用方回退到 DeepSeek）
     */
    private fun getApiKeyForPlatform(platform: String): String? {
        val key = when (platform) {
            "deepseek" -> getDeepSeekApiKey()
            "google" -> getGoogleApiKey()
            "openai" -> getOpenAIApiKey()
            "groq" -> getGroqApiKey()
            "siliconflow" -> getSiliconFlowApiKey()
            "moonshot" -> getMoonshotApiKey()
            "zhipu" -> getZhipuApiKey()
            else -> getDeepSeekApiKey()
        }
        return key.ifBlank { null }
    }

    /**
     * 根据 platform 获取对应 Service，如果 Key 未配置则回退到 DeepSeek
     */
    private fun getServiceForPlatform(platform: String): Pair<OpenAIService, String> {
        val apiKey = getApiKeyForPlatform(platform)
        if (apiKey != null) {
            val service = when (platform) {
                "deepseek" -> deepseekService
                "google" -> googleService
                "openai" -> openaiRealService
                "groq" -> groqService
                "siliconflow" -> siliconflowService
                "moonshot" -> moonshotService
                "zhipu" -> zhipuService
                else -> deepseekService
            }
            return Pair(service, "Bearer $apiKey")
        }
        // Key 未配置，回退到 DeepSeek
        return Pair(deepseekService, "Bearer ${getDeepSeekApiKey()}")
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
        thinkingCallback: ((String) -> Unit)? = null,
        conversationHistory: List<OpenAIMessage> = emptyList()
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
                    reactLoop(q, imageBase64, imageMimeType, effectiveModel, webSearchEnabled, hasImage, conversationHistory)
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

        CloudBuildTool.progressCallback.set(null)
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
        hasImage: Boolean = false,
        conversationHistory: List<OpenAIMessage> = emptyList()
    ): String {
        val messages = mutableListOf<OpenAIMessage>()
        val memoryPrompt = agentMemory.buildMemoryPrompt()
        val systemText = buildSystemPrompt(memoryPrompt, model, webSearchEnabled, hasImage)

        messages.add(OpenAIMessage(role = "system", content = systemText))

        // Insert conversation history for multi-turn context
        for (msg in conversationHistory) {
            messages.add(msg)
        }

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
                val (service, authHeader) = getServiceForPlatform(platform)
                val response = service.chatCompletions(request, authHeader)

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
                // 限流：等待后重试
                if (errMsg.contains("429")) {
                    delay(3000L)
                    if (cycleCount >= MAX_REACT_CYCLES) {
                        return "[请求限流，请稍后重试]"
                    }
                    continue
                }
                // 服务不可用或配额耗尽
                if (errMsg.contains("402") || errMsg.contains("quota") ||
                    errMsg.contains("insufficient") || errMsg.contains("balance")) {
                    reportStatus("⚠️ 服务暂时不可用")
                    return "[ERROR] 模型服务暂不可用，请稍后重试或切换模型"
                }
                return "[引擎异常] ${errMsg.take(100).ifBlank { "未知错误" }}"
            }
        }

        return lastAssistantText.ifBlank { "[已达到最大推理轮次(${MAX_REACT_CYCLES})，结果可能不完整]" }
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
            val model = Constants.OPENAI_MODEL
            val planPlatform = getModelPlatform(model)
            val (planService, planAuth) = getServiceForPlatform(planPlatform)
            val response = planService.chatCompletions(request, planAuth)
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

        sb.appendLine("你是布老师，用户的专属数字员工。用户的每一句话都可能是任务，你必须主动思考和回应。")
        sb.appendLine("核心原则：你是一个员工，不是在等指令的机器人。用户说什么，你都要想着怎么帮他把事办成。")
        sb.appendLine()
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
        sb.appendLine("【行为模式 - 根据用户意图主动行动】")
        sb.appendLine("1. 明确需求（包含具体功能列表、技术细节）→ 直接执行，不反复确认")
        sb.appendLine("2. 创建/开发类需求（\"做一个XX\"\"帮我做XX\"\"写个XX\"\"实现一个XX\"等）且没有具体功能列表 → 必须先问3个关键问题再动手：①功能范围：核心功能有哪些？②风格偏好：UI风格/交互方式？③数据存储：数据怎么存？问完给出建议方案，用户确认后才执行")
        sb.appendLine("3. 查看/分析类（\"帮我看看XX\"）→ 分析问题，给出具体建议，不要只说\"可以的\"")
        sb.appendLine("4. 问候（\"你好\"）→ 简单问候，准备好随时进入工作状态")
        sb.appendLine("5. 知识问答（\"XX是什么\"）→ 知识性回答，结束后问\"需要我帮你实现吗\"")
        sb.appendLine("6. 修改迭代（\"给那个XX加个YY\"\"把XX改成YY\"）→ 从记忆检索上下文 → 修改 → 验证 → 交付")
        sb.appendLine("7. 诊断查询（\"检查状态\"\"看看哪里有问题\"）→ 运行诊断 → 生成报告 → 标注问题 → 自动修复可修复项")
        sb.appendLine()
        sb.appendLine("【核心规则】")
        sb.appendLine("1. 不要等用户说\"写代码\"才行动。用户说的任何需求型语句（\"我想\"\"我要\"\"帮我\"\"能不能\"）都要主动进入工作模式")
        sb.appendLine("2. ⚠️ 绝对规则——需求澄清优先：用户的任何需求，只要没有明确列出具体的功能清单（不是笼统描述，而是逐条列出要做什么），就必须先提问再执行。没有例外。")
        sb.appendLine("   - \"简单的\"\"基本的\"\"随便做个\"\"快速搞一个\"这类词不是功能清单，不能跳过提问。简单不等于需求明确。")
        sb.appendLine("   - \"帮我做一个计算器\" → 必须问。\"帮我做一个带加减乘除和历史记录的计算器\" → 可以直接执行。")
        sb.appendLine("   - 提问后直接给建议方案，不让用户做选择题。")
        sb.appendLine("3. 明确需求（有具体功能列表）直接干，不反复确认")
        sb.appendLine("4. 记住用户历史偏好，下次自动应用")
        sb.appendLine("5. 错误提示说人话，不说技术术语")
        sb.appendLine("6. 用中文回复，简洁直接，不啰嗦")
        sb.appendLine("7. 每次回答前，必须先在 ```thinking``` 标签里写出你的推理过程，再输出最终答案")
        sb.appendLine("8. 明确需求执行时，先输出执行计划再动手。格式：")
        sb.appendLine("   ```plan")
        sb.appendLine("   目标：[一句话概括]")
        sb.appendLine("   步骤：1. [步骤1] 2. [步骤2]")
        sb.appendLine("   ```")
        sb.appendLine("9. developer 工具生成的 .plan 文件是规划文档（功能清单+技术方案），不是完整可运行的代码。生成 plan 后必须主动问用户：\"规划已生成，要我开始生成完整项目代码吗？\"，得到确认后才进入编码阶段。严禁把 plan 文件当作最终交付物。")
        sb.appendLine()
        sb.appendLine("【工具调用规则】")
        sb.appendLine("1. 需要工具时，返回 JSON：{\"tool\":\"工具名\",\"args\":{参数}}")
        sb.appendLine("2. 可以连续调用多个工具")
        sb.appendLine("3. 简洁直接，不啰嗦，不堆砌废话")

        return sb.toString()
    }
}
