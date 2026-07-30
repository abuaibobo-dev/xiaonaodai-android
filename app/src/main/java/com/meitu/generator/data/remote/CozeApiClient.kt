package com.meitu.generator.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Coze API 客户端 - 使用 OkHttp 直接处理 SSE 流式响应
 */
class CozeApiClient(
    private val baseUrl: String,
    private val pat: String,
    botId: String,
    private val httpClient: OkHttpClient
) {
    var currentBotId: String = botId
    companion object {
        private const val TAG = "CozeApiClient"
        private const val API_BASE = "https://api.coze.cn"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val gson = Gson()

    fun streamChat(
        message: String,
        conversationId: String? = null,
        userId: String = "default_user"
    ): Flow<StreamEvent> = flow {
        val requestBody = JsonObject().apply {
            addProperty("bot_id", currentBotId)
            addProperty("user_id", userId)
            addProperty("stream", true)
            addProperty("auto_save_history", true)
            conversationId?.let { addProperty("conversation_id", it) }

            val additionalMessages = com.google.gson.JsonArray()
            val msgObj = JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", message)
                addProperty("content_type", "text")
            }
            additionalMessages.add(msgObj)
            add("additional_messages", additionalMessages)
        }

        val url = "${API_BASE}/v3/chat"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $pat")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA))
            .build()

        Log.d(TAG, "Creating chat with bot: $currentBotId")

        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            Log.e(TAG, "Coze API error: ${response.code} - $errorBody")
            emit(StreamEvent.Error("API 请求失败 (${response.code}): $errorBody"))
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var fullText = StringBuilder()
        var chatId = ""
        var convId = conversationId ?: ""
        var hasEmittedDone = false

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data:")) continue
                val dataStr = l.removePrefix("data:").trim()
                if (dataStr.isEmpty() || dataStr == "[DONE]") continue

                try {
                    val json = JsonParser.parseString(dataStr).asJsonObject
                    val event = json.get("event")?.asString ?: continue

                    when (event) {
                        "conversation.chat.created" -> {
                            chatId = json.getAsJsonObject("chat")?.get("id")?.asString ?: ""
                            convId = json.getAsJsonObject("chat")?.get("conversation_id")?.asString ?: convId
                            Log.d(TAG, "Chat created: $chatId, conversation: $convId")
                            emit(StreamEvent.Status("思考中..."))
                        }
                        "conversation.chat.in_progress" -> {}
                        "conversation.message.delta" -> {
                            val content = json.get("content")?.asString ?: ""
                            val reasoning = json.get("reasoning_content")?.asString ?: ""
                            if (content.isNotEmpty()) {
                                fullText.append(content)
                                emit(StreamEvent.Delta(content))
                            } else if (reasoning.isNotEmpty() && fullText.isEmpty()) {
                                emit(StreamEvent.Status("深度思考中..."))
                            }
                        }
                        "conversation.message.completed" -> {
                            val type = json.get("type")?.asString ?: ""
                            val content = json.get("content")?.asString ?: ""
                            if (type == "answer" && content.isNotEmpty() && fullText.isEmpty()) {
                                fullText.append(content)
                                Log.d(TAG, "Recovered full text from completed event")
                            }
                        }
                        "conversation.chat.completed" -> {
                            Log.d(TAG, "Chat completed. fullText length: ${fullText.length}")
                            // 解析 token 消耗
                            val usage = json.getAsJsonObject("chat")?.getAsJsonObject("usage")
                            if (usage != null) {
                                val tokenCount = usage.get("token_count")?.asInt ?: 0
                                val inputCount = usage.get("input_count")?.asInt ?: 0
                                val outputCount = usage.get("output_count")?.asInt ?: 0
                                Log.d(TAG, "Usage: total=$tokenCount, in=$inputCount, out=$outputCount")
                                emit(StreamEvent.TokenUsage(tokenCount, inputCount, outputCount))
                            }
                            if (!hasEmittedDone) {
                                if (fullText.isNotEmpty()) {
                                    emit(StreamEvent.Done(fullText.toString(), chatId, convId))
                                } else {
                                    emit(StreamEvent.Error("AI 回复为空，请重试"))
                                }
                                hasEmittedDone = true
                            }
                        }
                        "conversation.chat.failed" -> {
                            val errCode = json.getAsJsonObject("chat")?.getAsJsonObject("last_error")?.get("code")?.asInt ?: 0
                            val errMsg = json.getAsJsonObject("chat")?.getAsJsonObject("last_error")?.get("msg")?.asString ?: ""
                            emit(StreamEvent.Error("AI 回复失败 (code=$errCode): $errMsg"))
                        }
                        "done" -> {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parse SSE error: ${e.message}")
                }
            }
        } finally {
            reader.close()
            response.close()
        }

        if (!hasEmittedDone) {
            if (fullText.isNotEmpty()) {
                emit(StreamEvent.Done(fullText.toString(), chatId, convId))
            } else {
                emit(StreamEvent.Error("AI 未返回内容，请重试"))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendChat(
        message: String,
        conversationId: String? = null,
        userId: String = "default_user"
    ): CozeChatResult = withContext(Dispatchers.IO) {
        val requestBody = JsonObject().apply {
            addProperty("bot_id", currentBotId)
            addProperty("user_id", userId)
            addProperty("stream", false)
            addProperty("auto_save_history", true)
            conversationId?.let { addProperty("conversation_id", it) }

            val additionalMessages = com.google.gson.JsonArray()
            val msgObj = JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", message)
                addProperty("content_type", "text")
            }
            additionalMessages.add(msgObj)
            add("additional_messages", additionalMessages)
        }

        val createUrl = "${API_BASE}/v3/chat"
        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $pat")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA))
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        if (!createResponse.isSuccessful) {
            return@withContext CozeChatResult.Error("API 请求失败: ${createResponse.code}")
        }

        val createBody = createResponse.body?.string() ?: ""
        val createJson = JsonParser.parseString(createBody).asJsonObject
        val chatData = createJson.getAsJsonObject("data")
            ?: return@withContext CozeChatResult.Error("响应格式异常")

        val chatId = chatData.get("id")?.asString ?: ""
        val convId = chatData.get("conversation_id")?.asString ?: ""
        val status = chatData.get("status")?.asString ?: ""

        var maxRetries = 120
        var currentStatus = status
        while (currentStatus != "completed" && currentStatus != "failed" && maxRetries > 0) {
            delay(1000)
            maxRetries--

            val retrieveBody = JsonObject().apply {
                addProperty("conversation_id", convId)
                addProperty("chat_id", chatId)
            }
            val retrieveRequest = Request.Builder()
                .url("${API_BASE}/v3/chat/retrieve")
                .addHeader("Authorization", "Bearer $pat")
                .addHeader("Content-Type", "application/json")
                .post(retrieveBody.toString().toRequestBody(JSON_MEDIA))
                .build()

            val retrieveResponse = httpClient.newCall(retrieveRequest).execute()
            val retrieveJson = JsonParser.parseString(retrieveResponse.body?.string() ?: "{}").asJsonObject
            currentStatus = retrieveJson.getAsJsonObject("data")?.get("status")?.asString ?: "unknown"
        }

        if (currentStatus == "failed") {
            return@withContext CozeChatResult.Error("AI 回复超时或失败")
        }

        val messagesRequest = Request.Builder()
            .url("${API_BASE}/v3/chat/message/list?conversation_id=$convId&chat_id=$chatId")
            .addHeader("Authorization", "Bearer $pat")
            .get()
            .build()

        val messagesResponse = httpClient.newCall(messagesRequest).execute()
        val messagesBody = messagesResponse.body?.string() ?: "[]"
        val messagesJson = JsonParser.parseString(messagesBody).asJsonObject
        val messagesArray = messagesJson.getAsJsonArray("data")

        val assistantMessage = messagesArray?.find { elem ->
            val obj = elem.asJsonObject
            obj.get("role")?.asString == "assistant" && obj.get("type")?.asString == "answer"
        }?.asJsonObject

        val answer = assistantMessage?.get("content")?.asString ?: "未获取到回复"
        CozeChatResult.Success(answer, chatId, convId)
    }

    // ============ Bot 创建与发布 ============

    suspend fun createBot(name: String, systemPrompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JsonObject().apply {
                addProperty("space_id", "")  // 使用默认个人空间
                addProperty("name", name)
                addProperty("description", "由布老师App创建的AI助手")
                addProperty("prompt_info", systemPrompt)
            }
            val url = "${API_BASE}/v1/bot/create"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $pat")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("创建Bot失败: ${response.code} - $body"))
            }
            val json = JsonParser.parseString(body).asJsonObject
            val botId = json.getAsJsonObject("data")?.get("bot_id")?.asString
            if (botId != null) Result.success(botId)
            else Result.failure(Exception("创建Bot失败: 未获取到bot_id"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishBot(botId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JsonObject().apply {
                addProperty("bot_id", botId)
                val connectorIds = com.google.gson.JsonArray()
                connectorIds.add("1024") // API 渠道
                add("connector_ids", connectorIds)
            }
            val url = "${API_BASE}/v1/bot/publish"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $pat")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("发布Bot失败: ${response.code} - $body"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============ DeepSeek 直连流式对话 ============

    /**
     * DeepSeek 直连流式对话（OpenAI 兼容格式），消耗 DeepSeek 账户余额
     */
    fun streamDeepSeekChat(
        apiKey: String,
        message: String,
        model: String = "deepseek-v4-flash",
        systemPrompt: String? = null,
        history: List<Pair<String, String>> = emptyList()
    ): Flow<StreamEvent> = flow {
        val messages = com.google.gson.JsonArray()

        // 系统提示词
        if (!systemPrompt.isNullOrBlank()) {
            messages.add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
        }

        // 历史对话（最近5轮）
        val recentHistory = if (history.size > 10) history.takeLast(10) else history
        for ((role, content) in recentHistory) {
            messages.add(JsonObject().apply {
                addProperty("role", role)
                addProperty("content", content)
            })
        }

        // 当前用户消息
        messages.add(JsonObject().apply {
            addProperty("role", "user")
            addProperty("content", message)
        })

        val requestBody = JsonObject().apply {
            addProperty("model", model)
            add("messages", messages)
            addProperty("stream", true)
        }

        val url = "https://api.deepseek.com/v1/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA))
            .build()

        Log.d(TAG, "DeepSeek streaming chat request")

        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            Log.e(TAG, "DeepSeek API error: ${response.code} - $errorBody")
            emit(StreamEvent.Error("DeepSeek 请求失败 (${response.code}): $errorBody"))
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var fullText = StringBuilder()
        var totalTokens = 0
        var hasEmittedDone = false

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data:")) continue
                val dataStr = l.removePrefix("data:").trim()
                if (dataStr.isEmpty() || dataStr == "[DONE]") {
                    if (dataStr == "[DONE]") {
                        if (!hasEmittedDone && fullText.isNotEmpty()) {
                            emit(StreamEvent.Done(fullText.toString(), "", ""))
                            hasEmittedDone = true
                        }
                    }
                    continue
                }

                try {
                    val json = JsonParser.parseString(dataStr).asJsonObject
                    val choices = json.getAsJsonArray("choices")
                    if (choices != null && choices.size() > 0) {
                        val choice = choices[0].asJsonObject
                        val delta = choice.getAsJsonObject("delta")
                        val content = delta?.get("content")?.asString ?: ""
                        if (content.isNotEmpty()) {
                            fullText.append(content)
                            emit(StreamEvent.Delta(content))
                        }
                    }

                    // 尝试从 usage 字段提取 token 消耗
                    val usage = json.getAsJsonObject("usage")
                    if (usage != null) {
                        totalTokens = usage.get("total_tokens")?.asInt ?: 0
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parse DeepSeek SSE error: ${e.message}")
                }
            }
        } finally {
            reader.close()
            response.close()
        }

        if (!hasEmittedDone) {
            if (fullText.isNotEmpty()) {
                emit(StreamEvent.Done(fullText.toString(), "", ""))
            } else {
                emit(StreamEvent.Error("DeepSeek 未返回内容，请检查 API Key 或余额"))
            }
        }

        // 发送 token 消耗（DeepSeek 流式可能不返回 usage，此时估算）
        if (totalTokens == 0 && fullText.isNotEmpty()) {
            totalTokens = fullText.length // 粗略估算
        }
        if (totalTokens > 0) {
            emit(StreamEvent.TokenUsage(totalTokens, totalTokens / 2, totalTokens / 2))
        }
    }.flowOn(Dispatchers.IO)
}

sealed class StreamEvent {
    data class Status(val message: String) : StreamEvent()
    data class Delta(val text: String) : StreamEvent()
    data class Done(val fullText: String, val chatId: String, val conversationId: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    data class TokenUsage(val total: Int, val input: Int, val output: Int) : StreamEvent()
}

sealed class CozeChatResult {
    data class Success(val answer: String, val chatId: String, val conversationId: String) : CozeChatResult()
    data class Error(val message: String) : CozeChatResult()
}
