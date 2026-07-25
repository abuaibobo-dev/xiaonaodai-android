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
import com.meitu.generator.data.remote.GeminiService
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
 * ReAct 循环引擎 - v4.4
 * 支持：多模态（图片分析）、深度思考（R1推理）、智能搜索（web_search）
 */
@Singleton
class AgentEngine @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val toolRegistry: ToolRegistry,
    private val skillRegistry: SkillRegistry,
    private val agentMemory: AgentMemory,
    private val openAIService: OpenAIService,
    private val geminiService: GeminiService,
    @Named("groqService") private val groqService: OpenAIService,
    @Named("sambanovaService") private val sambanovaService: OpenAIService,
    @Named("hfService") private val hfService: OpenAIService,
    @Named("openrouterService") private val openrouterService: OpenAIService,
    @Named("cerebrasService") private val cerebrasService: OpenAIService,
    @Named("nvidiaService") private val nvidiaService: OpenAIService,
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

        /** 模型名 → 所属平台映射（仅保留当前可用模型） */
        private val GROQ_MODELS = setOf(
            "openai/gpt-oss-120b", "openai/gpt-oss-20b",
            "llama-3.3-70b-versatile", "llama-3.1-8b-instant",
            "deepseek-r1-distill-70b", "moonshotai/kimi-k2-instruct"
        )
        private val SAMBANOVA_MODELS = setOf(
            "Meta-Llama-3.3-70B-Instruct", "gpt-oss-120b",
            "DeepSeek-V3.1", "gemma-4-31B-it"
        )
        private val GEMINI_MODELS = setOf(
            "gemini-2.0-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite"
        )
        private val HF_MODELS = setOf("meta-llama/Llama-3.3-70B-Instruct")

        fun isGeminiModel(model: String): Boolean = GEMINI_MODELS.contains(model)

        fun getModelPlatform(model: String): String = when {
            GEMINI_MODELS.contains(model) -> "gemini"
            GROQ_MODELS.contains(model) -> "groq"
            SAMBANOVA_MODELS.contains(model) -> "sambanova"
            HF_MODELS.contains(model) -> "hf"
            model.startsWith("deepseek-") -> "deepseek"
            else -> "deepseek" // 默认走 DeepSeek
        }
    }

    private val gson = Gson()

    /** 实时状态回调 */
    private var statusCallback: ((String) -> Unit)? = null
    /** 深度思考内容回调 */
    private var thinkingCallback: ((String) -> Unit)? = null

    private fun reportStatus(status: String) {
        statusCallback?.invoke(status)
    }

    private fun reportThinking(content: String) {
        thinkingCallback?.invoke(content)
    }

    private fun getApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_AI_API_KEY, "") ?: ""
        return if (savedKey.isNotBlank()) savedKey else Constants.OPENAI_API_KEY
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

        // 有图片时不走语义缓存
        if (imageBase64 == null) {
            val cached = semanticCache.lookup(query)
            if (cached != null) return cached
        }

        agentMemory.recordAction("用户: $query")

        val intent = IntentRouter.classify(query)

        // 确定使用的模型 - 智能路由
        val hasImage = imageBase64 != null
        val effectiveModel = resolveModel(query, imageBase64, deepThinkingEnabled)
        // 显示智能路由选择原因
        val selectionReason = ModelRouter.getSelectionReason(query, hasImage, deepThinkingEnabled)
        reportStatus("🤖 ${ModelRouter.getModelDisplayName(effectiveModel)} | $selectionReason")

        val result = when (intent.type) {
            IntentRouter.IntentType.TASK_GENERATE -> {
                generateAndBuild(query)
            }
            else -> {
                // Gemini 模型使用独立路径（非 OpenAI 兼容格式）
                if (isGeminiModel(effectiveModel)) {
                    val q = if (intent.type == IntentRouter.IntentType.TASK_BUILD) "[路由:cloud_build] $query" else query
                    val plan = if (intent.type == IntentRouter.IntentType.TASK_MODIFY) plan(q) else null
                    if (plan != null) {
                        executeWithPlan(plan, q, imageBase64, imageMimeType)
                    } else {
                        callGemini(q, imageBase64, imageMimeType, effectiveModel)
                    }
                } else {
                    val q = if (intent.type == IntentRouter.IntentType.TASK_BUILD) "[路由:cloud_build] $query" else query
                    val plan = if (intent.type == IntentRouter.IntentType.TASK_MODIFY) plan(q) else null
                    if (plan != null) {
                        executeWithPlan(plan, q, imageBase64, imageMimeType)
                    } else {
                        reactLoop(q, imageBase64, imageMimeType, effectiveModel, webSearchEnabled, hasImage)
                    }
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

    /**
     * 智能模型路由 - 根据任务类型自动选择最合适的免费模型
     */
    private suspend fun resolveModel(query: String, imageBase64: String?, deepThinkingEnabled: Boolean): String {
        // 用户手动指定了模型 → 尊重用户选择
        val userSelectedModel = settingsRepository.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL)
        if (userSelectedModel != Constants.OPENAI_MODEL) {
            return userSelectedModel
        }
        // 使用智能路由器自动选择
        return ModelRouter.selectModel(query, imageBase64 != null, deepThinkingEnabled)
    }

    /**
     * ReAct 循环核心 - 支持多模态 + 深度思考 + 联网搜索
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

        // 构建用户消息（多模态 or 纯文本）
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

        // 是否为推理模型（仅 DeepSeek 平台 R1 系列不支持 temperature + 支持 thinking）
        val isReasoningModel = model.startsWith("deepseek-r1") && getModelPlatform(model) == "deepseek"
        val temperature = if (isReasoningModel) null else 0.7

        // 深度思考配置
        val thinkingConfig = if (isReasoningModel) {
            ThinkingConfig(type = "enabled")  // R1 默认开启思考
        } else null

        var cycleCount = 0
        var lastAssistantText = ""

        while (cycleCount < MAX_REACT_CYCLES) {
            cycleCount++

            try {
                reportStatus(if (hasImage) "🖼️ 正在分析图片..." else if (isReasoningModel) "🧠 深度思考中..." else "🧠 正在思考...")

                val request = OpenAIRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    max_tokens = 4096,
                    thinking = thinkingConfig
                )

                // 根据模型名路由到正确的 service
                val response = when (getModelPlatform(model)) {
                    "groq" -> groqService.chatCompletions(request, "Bearer ${getGroqApiKey()}")
                    "sambanova" -> sambanovaService.chatCompletions(request, "Bearer ${getSambaNovaApiKey()}")
                    "hf" -> hfService.chatCompletions(request, "Bearer ${getHfApiKey()}")
                    "openrouter" -> openrouterService.chatCompletions(request, "Bearer ${getOpenRouterApiKey()}")
                    "cerebras" -> cerebrasService.chatCompletions(request, "Bearer ${getCerebrasApiKey()}")
                    "nvidia" -> nvidiaService.chatCompletions(request, "Bearer ${getNvidiaApiKey()}")
                    else -> openAIService.chatCompletions(request, "Bearer ${getApiKey()}") // deepseek 默认
                }

                TokenEstimator.account(request.toString(), response.toString())

                if (response.error != null) {
                    return "[引擎错误] ${response.error.message ?: "未知错误"}"
                }

                val choice = response.choices?.firstOrNull() ?: break
                val content = choice.message?.content ?: break
                val reasoning = choice.message?.reasoning_content
                lastAssistantText = content

                // 如果有思维链内容，通过回调传递
                if (!reasoning.isNullOrBlank()) {
                    reportThinking(reasoning)
                }

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

                    reportStatus("🔧 正在执行: $toolName")
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
                // 余额不足(402)或限流(429)时，依次尝试免费备用模型
                if (errMsg.contains("402") || errMsg.contains("429") || errMsg.contains("quota") || errMsg.contains("insufficient") || errMsg.contains("balance")) {
                    reportStatus("⚠️ DeepSeek 余额不足，切换到 Gemini 免费模型...")
                    val geminiResult = callGemini(query, imageBase64, imageMimeType)
                    if (!(geminiResult.startsWith("[Gemini 错误]") || geminiResult.startsWith("[Gemini 调用失败]"))) {
                        return geminiResult
                    }
                    reportStatus("⚠️ Gemini 也不可用，切换到 Groq 免费模型...")
                    val groqResult = callGroq(query, imageBase64, imageMimeType)
                    if (!(groqResult.startsWith("[Groq 错误]") || groqResult.startsWith("[Groq 调用失败]"))) {
                        return groqResult
                    }
                    reportStatus("⚠️ Groq 也不可用，切换到 SambaNova 免费模型...")
                    val sambanovaResult = callSambaNova(query, imageBase64, imageMimeType)
                    if (!(sambanovaResult.startsWith("[SambaNova 错误]") || sambanovaResult.startsWith("[SambaNova 调用失败]"))) {
                        return sambanovaResult
                    }
                    reportStatus("⚠️ SambaNova 也不可用，切换到 HuggingFace 免费模型...")
                    val hfResult = callHuggingFace(query, imageBase64, imageMimeType)
                    if (!(hfResult.startsWith("[HuggingFace 错误]") || hfResult.startsWith("[HuggingFace 调用失败]"))) {
                        return hfResult
                    }
                    // 以下平台未配置 Key 时会自动跳过
                    if (getOpenRouterApiKey().isNotBlank()) {
                        reportStatus("⚠️ HuggingFace 也不可用，切换到 OpenRouter 免费模型...")
                        val orResult = callOpenRouter(query, imageBase64, imageMimeType)
                        if (!(orResult.startsWith("[OpenRouter 错误]") || orResult.startsWith("[OpenRouter 调用失败]"))) {
                            return orResult
                        }
                    }
                    if (getCerebrasApiKey().isNotBlank()) {
                        reportStatus("⚠️ OpenRouter 也不可用，切换到 Cerebras 免费模型...")
                        val cbResult = callCerebras(query, imageBase64, imageMimeType)
                        if (!(cbResult.startsWith("[Cerebras 错误]") || cbResult.startsWith("[Cerebras 调用失败]"))) {
                            return cbResult
                        }
                    }
                    if (getNvidiaApiKey().isNotBlank()) {
                        reportStatus("⚠️ Cerebras 也不可用，切换到 NVIDIA 免费模型...")
                        val nvResult = callNvidia(query, imageBase64, imageMimeType)
                        if (!(nvResult.startsWith("[NVIDIA 错误]") || nvResult.startsWith("[NVIDIA 调用失败]"))) {
                            return nvResult
                        }
                    }
                    return "[引擎异常] 所有备用模型均不可用，请检查 API Key 配置"
                }
                if (errMsg.contains("429")) {
                    delay(3000L)
                    if (cycleCount >= MAX_REACT_CYCLES) {
                        return "[请求限流，请稍后重试]"
                    }
                    continue
                }
                return "[引擎异常] ${errMsg.take(100) ?: "未知错误"}"
            }
        }

        return lastAssistantText.ifBlank { "[已达到最大推理轮次(${MAX_REACT_CYCLES})，结果可能不完整]" }
    }

    /**
     * 获取 Gemini API Key
     */
    private fun getGeminiApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_GEMINI_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.GEMINI_API_KEY else savedKey
    }

    /**
     * 调用 Gemini 模型（支持指定模型名）
     */
    private suspend fun callGemini(query: String, imageBase64: String?, imageMimeType: String?, model: String = Constants.GEMINI_MODEL): String {
        val apiKey = getGeminiApiKey()

        try {
            val parts = mutableListOf<GeminiPart>()

            // 添加文本
            val textContent = if (query.isBlank()) "请分析这张图片的内容并给出详细描述" else query
            parts.add(GeminiPart(text = textContent))

            // 添加图片（如果有）
            if (imageBase64 != null) {
                val mime = imageMimeType ?: "image/jpeg"
                parts.add(GeminiPart(inlineData = GeminiInlineData(mime_type = mime, data = imageBase64)))
            }

            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = parts))
            )

            val response = geminiService.generateContent(model, apiKey, request)

            if (response.error != null) {
                return "[Gemini 错误] ${response.error.message ?: "未知错误"}"
            }

            val candidate = response.candidates?.firstOrNull()
            val content = candidate?.content?.parts?.firstOrNull { it.text != null }?.text
            return content ?: "[Gemini 返回为空]"
        } catch (e: Exception) {
            return "[Gemini 调用失败] ${e.message?.take(100) ?: "未知错误"}"
        }
    }

    /**
     * 获取 Groq API Key
     */
    private fun getGroqApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_GROQ_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.GROQ_API_KEY else savedKey
    }

    /**
     * 调用 Groq 免费模型作为第二备用
     */
    private suspend fun callGroq(query: String, imageBase64: String?, imageMimeType: String?): String {
        val apiKey = getGroqApiKey()

        try {
            val messages = mutableListOf<OpenAIMessage>()
            val memoryPrompt = agentMemory.buildMemoryPrompt()
            messages.add(OpenAIMessage(role = "system", content = "你是「布老师」AI 智能体助手，请用中文回复用户。当前记忆：$memoryPrompt"))

            // 构建用户消息（多模态 or 纯文本）
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

            val request = OpenAIRequest(
                model = Constants.GROQ_MODEL,
                messages = messages,
                temperature = 0.7,
                max_tokens = 4096
            )

            val response = groqService.chatCompletions(
                request = request,
                authorization = "Bearer $apiKey"
            )

            if (response.error != null) {
                return "[Groq 错误] ${response.error.message ?: "未知错误"}"
            }

            val choice = response.choices?.firstOrNull()
            val content = choice?.message?.content
            return content ?: "[Groq 返回为空]"
        } catch (e: Exception) {
            return "[Groq 调用失败] ${e.message?.take(100) ?: "未知错误"}"
        }
    }

    /**
     * 通用 OpenAI 兼容 API 调用（适用于 Groq/SambaNova/HuggingFace/OpenRouter/Cerebras/NVIDIA）
     */
    private suspend fun callOpenAICompatible(
        service: OpenAIService,
        apiKey: String,
        model: String,
        query: String,
        imageBase64: String?,
        imageMimeType: String?,
        platformName: String
    ): String {
        if (apiKey.isBlank()) return "[$platformName 调用失败] 未配置 API Key"
        try {
            val messages = mutableListOf<OpenAIMessage>()
            val memoryPrompt = agentMemory.buildMemoryPrompt()
            messages.add(OpenAIMessage(role = "system", content = "你是「布老师」AI 智能体助手，请用中文回复用户。当前记忆：$memoryPrompt"))

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

            val request = OpenAIRequest(
                model = model,
                messages = messages,
                temperature = 0.7,
                max_tokens = 4096
            )

            val response = service.chatCompletions(
                request = request,
                authorization = "Bearer $apiKey"
            )

            if (response.error != null) {
                return "[$platformName 错误] ${response.error.message ?: "未知错误"}"
            }

            val choice = response.choices?.firstOrNull()
            val content = choice?.message?.content
            return content ?: "[$platformName 返回为空]"
        } catch (e: Exception) {
            return "[$platformName 调用失败] ${e.message?.take(100) ?: "未知错误"}"
        }
    }

    /** 获取 SambaNova API Key */
    private fun getSambaNovaApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_SAMBANOVA_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.SAMBANOVA_API_KEY else savedKey
    }

    /** 调用 SambaNova 免费模型 */
    private suspend fun callSambaNova(query: String, imageBase64: String?, imageMimeType: String?): String {
        return callOpenAICompatible(sambanovaService, getSambaNovaApiKey(), Constants.SAMBANOVA_MODEL, query, imageBase64, imageMimeType, "SambaNova")
    }

    /** 获取 HuggingFace API Key */
    private fun getHfApiKey(): String {
        val savedKey = securePrefs.getString(Constants.KEY_HF_API_KEY, "")
        return if (savedKey.isNullOrBlank()) Constants.HF_API_KEY else savedKey
    }

    /** 调用 HuggingFace 免费模型 */
    private suspend fun callHuggingFace(query: String, imageBase64: String?, imageMimeType: String?): String {
        return callOpenAICompatible(hfService, getHfApiKey(), Constants.HF_MODEL, query, imageBase64, imageMimeType, "HuggingFace")
    }

    /** 获取 OpenRouter API Key */
    private fun getOpenRouterApiKey(): String {
        return securePrefs.getString(Constants.KEY_OPENROUTER_API_KEY, "") ?: ""
    }

    /** 调用 OpenRouter 免费模型 */
    private suspend fun callOpenRouter(query: String, imageBase64: String?, imageMimeType: String?): String {
        return callOpenAICompatible(openrouterService, getOpenRouterApiKey(), Constants.OPENROUTER_MODEL, query, imageBase64, imageMimeType, "OpenRouter")
    }

    /** 获取 Cerebras API Key */
    private fun getCerebrasApiKey(): String {
        return securePrefs.getString(Constants.KEY_CEREBRAS_API_KEY, "") ?: ""
    }

    /** 调用 Cerebras 免费模型 */
    private suspend fun callCerebras(query: String, imageBase64: String?, imageMimeType: String?): String {
        return callOpenAICompatible(cerebrasService, getCerebrasApiKey(), Constants.CEREBRAS_MODEL, query, imageBase64, imageMimeType, "Cerebras")
    }

    /** 获取 NVIDIA API Key */
    private fun getNvidiaApiKey(): String {
        return securePrefs.getString(Constants.KEY_NVIDIA_API_KEY, "") ?: ""
    }

    /** 调用 NVIDIA NIM 免费模型 */
    private suspend fun callNvidia(query: String, imageBase64: String?, imageMimeType: String?): String {
        return callOpenAICompatible(nvidiaService, getNvidiaApiKey(), Constants.NVIDIA_MODEL, query, imageBase64, imageMimeType, "NVIDIA")
    }

    /**
     * 尝试从 AI 回复中提取工具调用指令
     */
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

    /**
     * 规划器
     */
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
                model = settingsRepository.getString(Constants.KEY_AI_MODEL, Constants.OPENAI_MODEL),
                messages = listOf(OpenAIMessage(role = "user", content = planPrompt)),
                temperature = 0.3,
                max_tokens = 500
            )
            val response = openAIService.chatCompletions(
                request = request,
                authorization = "Bearer ${getApiKey()}"
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

    /**
     * 按计划执行
     */
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

    /**
     * 代码生成 + 编译一体化流程
     */
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

    // ============ 辅助方法 ============

    private fun isSimpleQuery(query: String): Boolean {
        val simplePatterns = listOf("帮助", "怎么用", "hi", "hello", "你好", "谢谢")
        return simplePatterns.any { query.contains(it, ignoreCase = true) }
    }

    private suspend fun buildSystemPrompt(memoryPrompt: String, currentModel: String, webSearchEnabled: Boolean, hasImage: Boolean): String {
        val enabledTools = toolRegistry.getAll().map { "${it.name}: ${it.description}" }
        val sb = StringBuilder()

        sb.appendLine("你是「布老师」v4.5.2 AI 智能体引擎，一个纯AI智能体助手。")
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
            sb.appendLine("可以描述图片内容、回答关于图片的问题、提取图片中的信息等。")
            sb.appendLine()
        }

        if (webSearchEnabled) {
            sb.appendLine("【当前模式：联网搜索】")
            sb.appendLine("当用户询问实时信息、最新数据、新闻、价格、天气等需要时效性内容的问题时，")
            sb.appendLine("请优先使用 web_search 工具搜索，再基于搜索结果回答。")
            sb.appendLine()
        }

        if (currentModel == "deepseek-r1") {
            sb.appendLine("【当前模式：深度思考】")
            sb.appendLine("你正在使用深度推理模式，请充分运用你的推理能力，进行深度分析。")
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
        sb.appendLine("5. 当前使用模型: $currentModel")

        return sb.toString()
    }
}
