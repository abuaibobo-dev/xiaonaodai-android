package com.meitu.generator.data.remote

import com.meitu.generator.data.remote.dto.OpenAIModelsResponse
import com.meitu.generator.data.remote.dto.OpenAIRequest
import com.meitu.generator.data.remote.dto.OpenAIResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI-compatible API service
 * 支持 GitHub 聚合 API (api.gptapi.us)、DeepSeek、OpenAI 等
 */
interface OpenAIService {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Body request: OpenAIRequest,
        @Header("Authorization") authorization: String
    ): OpenAIResponse

    @GET("models")
    suspend fun getModels(
        @Header("Authorization") authorization: String
    ): OpenAIModelsResponse
}
