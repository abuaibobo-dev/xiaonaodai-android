package com.meitu.generator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ImgBBResponse(
    val success: Boolean = false,
    val data: ImgBBData? = null,
    val error: ImgBBError? = null
)

data class ImgBBData(
    val id: String? = null,
    val url: String? = null,
    @SerializedName("delete_url") val deleteUrl: String? = null,
    val size: Long? = null,
    val title: String? = null
)

data class ImgBBError(
    val message: String? = null,
    val code: Int? = null
)
