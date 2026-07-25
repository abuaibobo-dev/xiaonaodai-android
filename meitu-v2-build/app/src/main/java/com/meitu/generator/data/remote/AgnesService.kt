package com.meitu.generator.data.remote

import com.meitu.generator.data.remote.dto.AgnesImageRequest
import com.meitu.generator.data.remote.dto.AgnesImageResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AgnesService {
    @POST("v1/images/generations")
    suspend fun generateImage(
        @Header("Authorization") auth: String,
        @Body request: AgnesImageRequest
    ): AgnesImageResponse
}
