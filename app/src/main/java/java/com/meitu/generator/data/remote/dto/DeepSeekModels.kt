package com.meitu.generator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DeepSeekBalanceResponse(
    @SerializedName("is_available") val isAvailable: Boolean,
    @SerializedName("balance_infos") val balanceInfos: List<BalanceInfo>
)

data class BalanceInfo(
    @SerializedName("currency") val currency: String,
    @SerializedName("total_balance") val totalBalance: String,
    @SerializedName("granted_balance") val grantedBalance: String,
    @SerializedName("topped_up_balance") val toppedUpBalance: String
)
