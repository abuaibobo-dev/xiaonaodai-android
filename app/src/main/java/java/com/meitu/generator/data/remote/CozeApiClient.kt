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
 * 支持 Deepseek 思考模式：reasoning_content 用于思考展示，
 * content 用于最终回答。completed 事件兜底确保消息不丢失。
 */
class CozeApiClient(
    private val baseUrl: String,
    private val pat: String,
    private val botId: String,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "CozeApiClient"
        private const val API_BASE = "https://api.coze.cn"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val gson = Gson()

    /**
     * 发送消息并流式接收回复
     * @return Flow<StreamEvent> 每次 emit 一段增量文本或状态
     */
    fun streamChat(
        message: String,
        conversationId: String? = null,
        userId: String = "default_user"
    ): Flow<StreamEvent> = flow {
        // Step 1: 创建对话
        val requestBody = JsonObject().apply {
            addProperty("bot_id", botId)
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

        Log.d(TAG, "Creating chat with bot: $botId, thinking mode supported")

        val response = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            Log.e(TAG, "Coze API error: ${response.code} - $errorBody")
            emit(StreamEvent.Error("API 请求失败 (${response.code}): $errorBody"))
            return@flow
        }

        // Step 2: 读取 SSE 流
        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        var fullText = StringBuilder()
        var reasoningText = StringBuilder()
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
                        "conversation.chat.in_progress" -> {
                            // 持续显示思考中状态
                        }
                        "conversation.message.delta" -> {
                            // 优先读取 content（正式回答）
                            val content = json.get("content")?.asString ?: ""
                            // 读取 reasoning_content（思考过程，Deepseek 思考模式）
                            val reasoning = json.get("reasoning_content")?.asString ?: ""

                            if (content.isNotEmpty()) {
                                fullText.append(content)
                                emit(StreamEvent.Delta(content))
                            } else if (reasoning.isNotEmpty()) {
                                // 思考阶段也收集，但不单独 emit delta（避免显示内部推理）
                                reasoningText.append(reasoning)
                                // 如果还没有收到任何正式内容，保持"思考中..."
                                if (fullText.isEmpty()) {
                                    emit(StreamEvent.Status("深度思考中..."))
                                }
                            }
                        }
                        "conversation.message.completed" -> {
                            val type = json.get("type")?.asString ?: ""
                            val content = json.get("content")?.asString ?: ""
                            Log.d(TAG, "Message completed: type=$type, contentLen=${content.length}")
                            // 如果是 answer 类型且 content 有内容，但 delta 还没收到，兜底使用
                            if (type == "answer" && content.isNotEmpty() && fullText.isEmpty()) {
                                fullText.append(content)
                                Log.d(TAG, "Recovered full text from completed event")
                            }
                        }
                        "conversation.chat.completed" -> {
                            Log.d(TAG, "Chat completed. fullText length: ${fullText.length}")
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
                        "done" -> {
                            // 流结束
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parse SSE error: ${e.message}")
                }
            }
        } finally {
            reader.close()
            response.close()
        }

        // 如果流结束了但没有收到 completed 事件，兜底
        if (!hasEmittedDone) {
            if (fullText.isNotEmpty()) {
                emit(StreamEvent.Done(fullText.toString(), chatId, convId))
            } else {
                emit(StreamEvent.Error("AI 未返回内容，请重试"))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 非流式发送消息（备用）
     */
    suspend fun sendChat(
        message: String,
        conversationId: String? = null,
        userId: String = "default_user"
    ): CozeChatResult = withContext(Dispatchers.IO) {
        val requestBody = JsonObject().apply {
            addProperty("bot_id", botId)
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

        // 创建对话
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

        // 轮询等待完成
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

        // 获取消息列表
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
}

sealed class StreamEvent {
    data class Status(val message: String) : StreamEvent()
    data class Delta(val text: String) : StreamEvent()
    data class Done(val fullText: String, val chatId: String, val conversationId: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

sealed class CozeChatResult {
    data class Success(val answer: String, val chatId: String, val conversationId: String) : CozeChatResult()
    data class Error(val message: String) : CozeChatResult()
}
