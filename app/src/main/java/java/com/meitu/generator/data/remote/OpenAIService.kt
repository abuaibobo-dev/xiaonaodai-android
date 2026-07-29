package com.meitu.generator.data.remote

import com.meitu.generator.data.remote.dto.OpenAIModelsResponse
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.data.remote.dto.OpenAIResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * OpenAI-compatible API service
 * 支持 GitHub 聚合 API、DeepSeek、OpenAI、Google Gemini 等
 */
interface OpenAIService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String,
        @Query("key") apiKey: String? = null
    ): OpenAIResponse

    @GET("models")
    suspend fun getModels(
        @Header("Authorization") authorization: String,
        @Query("key") apiKey: String? = null
    ): OpenAIModelsResponse
}
