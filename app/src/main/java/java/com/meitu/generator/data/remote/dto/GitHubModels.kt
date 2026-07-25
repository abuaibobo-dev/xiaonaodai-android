package com.meitu.generator.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * GitHub API 响应数据模型 - 用于云端编译模块
 */

// ============ 文件内容 API ============

/** 创建/更新文件请求体 */
data class GitHubFileRequest(
    val message: String,
    val content: String,    // Base64 编码的文件内容
    val sha: String? = null, // 更新文件时需要提供原文件的 SHA
    val branch: String = "main"
)

/** 创建/更新文件响应 */
data class GitHubFileContent(
    val content: GitHubFileDetail? = null,
    val commit: GitHubCommit? = null
)

data class GitHubFileDetail(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    @SerializedName("download_url")
    val downloadUrl: String?
)

data class GitHubCommit(
    val sha: String,
    val message: String
)

// ============ Actions API ============

/** 触发 workflow 请求体 */
data class WorkflowDispatchRequest(
    @SerializedName("ref")
    val ref: String = "main",
    val inputs: Map<String, String> = emptyMap()
)

/** Workflow 运行记录 */
data class WorkflowRun(
    val id: Long,
    val name: String,
    val status: String,          // queued, in_progress, completed, waiting, requested
    val conclusion: String? = null, // success, failure, cancelled, null(进行中)
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("head_branch")
    val headBranch: String,
    @SerializedName("head_sha")
    val headSha: String,
    @SerializedName("run_number")
    val runNumber: Int,
    @SerializedName("run_attempt")
    val runAttempt: Int = 1,
    val url: String
) {
    /** 是否已完成（无论成功或失败） */
    val isCompleted: Boolean get() = status == "completed"

    /** 是否成功完成 */
    val isSuccess: Boolean get() = status == "completed" && conclusion == "success"

    /** 是否失败 */
    val isFailed: Boolean get() = status == "completed" && conclusion != "success"
}

/** Workflow 运行列表响应 */
data class WorkflowRunList(
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("workflow_runs")
    val workflowRuns: List<WorkflowRun>
)

// ============ Artifacts API ============

/** 构建产物 */
data class Artifact(
    val id: Long,
    @SerializedName("node_id")
    val nodeId: String,
    val name: String,
    @SerializedName("size_in_bytes")
    val sizeInBytes: Long,
    val url: String,
    @SerializedName("archive_download_url")
    val archiveDownloadUrl: String,
    val expired: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("expires_at")
    val expiresAt: String
)

/** 产物列表响应 */
data class ArtifactList(
    @SerializedName("total_count")
    val totalCount: Int,
    val artifacts: List<Artifact>
)

// ============ 通用错误响应 ============

data class GitHubError(
    val message: String,
    @SerializedName("documentation_url")
    val documentationUrl: String? = null
)
