package com.meitu.generator.data.remote.dto

import com.google.gson.*
import java.lang.reflect.Type

/**
 * OpenAI-compatible API models
 * 支持 DeepSeek V4 / R1 / VL 系列
 * 支持多模态内容（文本 + 图片）和深度思考（reasoning_content）
 */

// ============ 多模态内容支持 ============

data class ContentPart(
    val type: String,    // "text" or "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String      // "data:image/jpeg;base64,{base64_data}"
)

// ============ Request ============

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double? = 0.7,
    val max_tokens: Int? = null,
    val stream: Boolean = false,
    val thinking: ThinkingConfig? = null
)

data class ThinkingConfig(
    val type: String,           // "enabled" or "disabled"
    val reasoning_effort: String? = null  // "high", "max" (仅 R1 系列)
)

// ============ Message ============

data class OpenAIMessage(
    val role: String,
    val content: String? = null,
    val contentParts: List<ContentPart>? = null,
    val reasoning_content: String? = null
)

// ============ Response ============

data class OpenAIResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenAIChoice>? = null,
    val usage: OpenAIUsage? = null,
    val error: OpenAIError? = null
)

data class OpenAIChoice(
    val index: Int = 0,
    val message: OpenAIMessage? = null,
    val finish_reason: String? = null
)

data class OpenAIUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0
)

data class OpenAIError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

// ============ 自定义序列化器 ============

/**
 * OpenAIMessage 序列化器
 * 处理多模态内容：纯文本 → content: "string"，含图片 → content: [{type:"text",...},{type:"image_url",...}]
 */
class OpenAIMessageSerializer : JsonSerializer<OpenAIMessage> {
    override fun serialize(
        src: OpenAIMessage,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val obj = JsonObject()
        obj.addProperty("role", src.role)

        if (src.contentParts != null && src.contentParts.isNotEmpty()) {
            val arr = JsonArray()
            for (part in src.contentParts) {
                val partObj = JsonObject()
                partObj.addProperty("type", part.type)
                if (part.type == "text" && part.text != null) {
                    partObj.addProperty("text", part.text)
                } else if (part.type == "image_url" && part.image_url != null) {
                    val imgObj = JsonObject()
                    imgObj.addProperty("url", part.image_url.url)
                    partObj.add("image_url", imgObj)
                }
                arr.add(partObj)
            }
            obj.add("content", arr)
        } else {
            obj.addProperty("content", src.content ?: "")
        }

        return obj
    }
}

/**
 * OpenAIMessage 反序列化器
 * 解析响应中的 content（可能是 string 或 array）和 reasoning_content
 */
class OpenAIMessageDeserializer : JsonDeserializer<OpenAIMessage> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): OpenAIMessage {
        val obj = json.asJsonObject
        val role = obj.get("role")?.asString ?: ""

        var content: String? = null
        val contentElement = obj.get("content")

        if (contentElement != null) {
            if (contentElement.isJsonPrimitive) {
                content = contentElement.asString
            } else if (contentElement.isJsonArray) {
                // content 是数组时，提取所有 text 部分
                val sb = StringBuilder()
                for (part in contentElement.asJsonArray) {
                    if (part.isJsonObject) {
                        val partObj = part.asJsonObject
                        if (partObj.get("type")?.asString == "text") {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(partObj.get("text")?.asString ?: "")
                        }
                    }
                }
                content = sb.toString()
            }
        }

        val reasoning = obj.get("reasoning_content")?.asString

        return OpenAIMessage(
            role = role,
            content = content,
            reasoning_content = reasoning
        )
    }
}

// ============ Models List Response ============

data class OpenAIModelsResponse(
    val `object`: String? = null,
    val data: List<ModelData>? = null
)

data class ModelData(
    val id: String,
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null
)
