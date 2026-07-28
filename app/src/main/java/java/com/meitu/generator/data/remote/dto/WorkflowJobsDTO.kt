package com.meitu.generator.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WorkflowJobsResponse(
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("jobs") val jobs: List<WorkflowJob> = emptyList()
)

data class WorkflowJob(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("conclusion") val conclusion: String? = null,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("steps") val steps: List<WorkflowStep> = emptyList()
)

data class WorkflowStep(
    @SerializedName("name") val name: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("conclusion") val conclusion: String? = null,
    @SerializedName("number") val number: Int = 0,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null
)
