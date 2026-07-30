package com.meitu.generator.data.remote

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Coze API 服务接口
 */
interface CozeService {
    
    @POST("v3/chat")
    suspend fun createChat(
        @Body body: JsonObject
    ): Response<JsonObject>
    
    @GET("v3/chat/message/list")
    suspend fun listMessages(
        @Query("conversation_id") conversationId: String,
        @Query("chat_id") chatId: String
    ): Response<JsonObject>
    
    @GET("v3/chat/retrieve")
    suspend fun retrieveChat(
        @Body body: JsonObject
    ): Response<JsonObject>
}
