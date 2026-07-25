package com.meitu.generator.data.remote

import com.meitu.generator.data.remote.dto.DeepSeekBalanceResponse
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * DeepSeek API - 余额查询
 */
interface DeepSeekBalanceService {
    @GET("user/balance")
    suspend fun getBalance(@Header("Authorization") authorization: String): DeepSeekBalanceResponse
}
