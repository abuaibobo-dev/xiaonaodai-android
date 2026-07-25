package com.meitu.generator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AgnesImageRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String,
    @SerializedName("negative_prompt") val negativePrompt: String = ""
)

data class AgnesImageResponse(
    val data: List<AgnesImageData>? = null,
    val error: AgnesError? = null
)

data class AgnesImageData(
    val url: String? = null,
    @SerializedName("b64_json") val b64Json: String? = null
)

data class AgnesError(
    val message: String? = null,
    val type: String? = null
)
