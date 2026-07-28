package com.meitu.generator.data.remote

import com.meitu.generator.data.remote.dto.DeepSeekBalanceResponse
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * DeepSeek 专用 API Service
 * 支持余额查询等 DeepSeek 专属接口
 */
interface DeepSeekService {

    @GET("user/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String
    ): DeepSeekBalanceResponse
}
