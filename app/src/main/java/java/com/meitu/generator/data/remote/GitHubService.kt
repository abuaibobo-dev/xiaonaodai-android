package com.meitu.generator.data.remote

import com.meitu.generator.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * GitHub REST API 接口 - 用于云端编译模块
 * 
 * 文档: https://docs.github.com/en/rest
 * 所有接口需要 Authorization: Bearer {token} header (由 OkHttp Interceptor 注入)
 */
interface GitHubService {

    // ============ Repos Contents API ============

    /**
     * 创建或更新文件内容
     * PUT /repos/{owner}/{repo}/contents/{path}
     */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: GitHubFileRequest
    ): GitHubFileContent

    /**
     * 获取文件内容（用于获取 SHA 以更新已有文件）
     * GET /repos/{owner}/{repo}/contents/{path}
     */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String = "main"
    ): GitHubFileDetail

    // ============ Actions API ============

    /**
     * 触发 workflow dispatch 事件
     * POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches
     * 注意: 此接口返回 204 No Content，无响应体
     */
    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun triggerWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body request: WorkflowDispatchRequest
    ): Response<Unit>

    /**
     * 获取 workflow 运行列表
     * GET /repos/{owner}/{repo}/actions/runs
     */
    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): WorkflowRunList

    /**
     * 获取指定 workflow 的运行列表
     * GET /repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs
     */
    @GET("repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs")
    suspend fun getWorkflowRunsByWorkflowId(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1
    ): WorkflowRunList

    /**
     * 获取单次 workflow 运行详情
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}
     */
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): WorkflowRun

    // ============ Workflow Jobs API ============

    /**
     * 获取 workflow run 的 jobs 列表
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}/jobs
     */
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun getWorkflowRunJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): WorkflowJobsResponse

    // ============ Artifacts API ============

    /**
     * 获取运行产物的列表
     * GET /repos/{owner}/{repo}/actions/runs/{run_id}/artifacts
     */
    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): ArtifactList

    /**
     * 下载产物 ZIP 包
     * GET /repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip
     * 返回的 URL 需要跟随 302 重定向获取实际下载地址
     */
    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip")
    suspend fun downloadArtifact(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long
    ): ResponseBody

    // ============ Releases API ============

    /**
     * 获取指定 tag 的 Release
     * GET /repos/{owner}/{repo}/releases/tags/{tag}
     */
    @GET("repos/{owner}/{repo}/releases/tags/{tag}")
    suspend fun getReleaseByTag(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String
    ): GitHubRelease

    /**
     * 获取最新的 Release
     * GET /repos/{owner}/{repo}/releases/latest
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRelease
}
