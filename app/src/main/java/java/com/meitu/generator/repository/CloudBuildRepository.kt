package com.meitu.generator.repository

import android.util.Base64
import com.meitu.generator.data.remote.GitHubService
import com.meitu.generator.data.remote.dto.*
import com.meitu.generator.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端编译仓库 - 管理代码推送、触发编译、轮询状态、下载 APK
 *
 * 核心流程:
 * 1. pushProjectToGithub: 批量推送项目文件到 GitHub 仓库
 * 2. triggerBuild: 触发 GitHub Actions workflow
 * 3. pollBuildStatus: 轮询编译状态直到完成
 * 4. downloadApk: 下载构建产物中的 APK
 */
@Singleton
class CloudBuildRepository @Inject constructor(
    private val gitHubService: GitHubService
) {
    private val owner = Constants.GITHUB_REPO_OWNER
    private val repo = Constants.GITHUB_REPO_NAME

    // ============ 文件推送 ============

    /**
     * 推送单个文件到 GitHub
     * @param path 文件在仓库中的路径 (如 "app/src/main/java/...")
     * @param content 文件内容
     * @param commitMsg 提交信息
     * @param token GitHub Personal Access Token
     * @return 文件的 SHA (用于后续更新)
     */
    suspend fun pushFile(
        path: String,
        content: String,
        commitMsg: String = "Update $path",
        token: String
    ): Result<String> = runCatching {
        val base64Content = Base64.encodeToString(
            content.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        // 尝试获取已有文件的 SHA (如果文件已存在则需要 SHA 才能更新)
        val existingSha = try {
            gitHubService.getFileContent(owner, repo, path).sha
        } catch (_: Exception) {
            null // 文件不存在，创建新文件
        }

        val request = GitHubFileRequest(
            message = commitMsg,
            content = base64Content,
            sha = existingSha,
            branch = "main"
        )

        val response = gitHubService.createOrUpdateFile(owner, repo, path, request)
        response.content?.sha ?: throw IllegalStateException("推送文件失败: 无返回 SHA")
    }

    /**
     * 批量推送项目文件到 GitHub
     * @param projectFiles Map<文件路径, 文件内容>
     * @param token GitHub Token
     * @return 推送结果摘要
     */
    suspend fun pushProjectToGithub(
        projectFiles: Map<String, String>,
        token: String
    ): Result<PushSummary> = runCatching {
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()

        projectFiles.entries.forEach { (path, content) ->
            val result = pushFile(path, content, token = token)
            if (result.isSuccess) {
                successCount++
            } else {
                failCount++
                errors.add("$path: ${result.exceptionOrNull()?.message?.take(80)}")
            }
        }

        PushSummary(
            totalFiles = projectFiles.size,
            successCount = successCount,
            failCount = failCount,
            errors = errors
        )
    }

    // ============ 触发编译 ============

    /**
     * 触发 GitHub Actions workflow 编译
     * @param token GitHub Token
     * @param workflowId workflow 文件名或 ID (如 "build.yml")
     * @return 触发结果
     */
    suspend fun triggerBuild(
        token: String,
        workflowId: String = Constants.GITHUB_WORKFLOW_ID
    ): Result<Unit> = runCatching {
        val request = WorkflowDispatchRequest(ref = "main")
        gitHubService.triggerWorkflow(owner, repo, workflowId, request)
    }

    // ============ 编译状态 ============

    /**
     * 获取最新的编译运行
     * @param token GitHub Token
     * @return 最新的 WorkflowRun 或 null
     */
    suspend fun getLatestRun(token: String): Result<WorkflowRun?> = runCatching {
        val runs = gitHubService.getWorkflowRuns(owner, repo, perPage = 1)
        runs.workflowRuns.firstOrNull()
    }

    /**
     * 获取指定 runId 的编译状态
     * @param token GitHub Token
     * @param runId 运行 ID
     */
    suspend fun getBuildStatus(token: String, runId: Long): Result<WorkflowRun> = runCatching {
        gitHubService.getWorkflowRun(owner, repo, runId)
    }

    /**
     * 轮询编译状态直到完成
     * @param token GitHub Token
     * @param runId 运行 ID
     * @param intervalMs 轮询间隔 (毫秒)
     * @return Flow<WorkflowRun> 每次轮询返回最新状态
     */
    fun pollBuildStatus(
        token: String,
        runId: Long,
        intervalMs: Long = Constants.GITHUB_POLL_INTERVAL_MS
    ): Flow<WorkflowRun> = flow {
        while (true) {
            val result = getBuildStatus(token, runId)
            if (result.isSuccess) {
                val run = result.getOrThrow()
                emit(run)
                if (run.isCompleted) break
            } else {
                // 查询失败，等待后重试
                val errorRun = WorkflowRun(
                    id = runId, name = "Unknown", status = "error",
                    conclusion = "error", createdAt = "", updatedAt = "",
                    headBranch = "", headSha = "", runNumber = 0,
                    url = ""
                )
                emit(errorRun)
                break
            }
            delay(intervalMs)
        }
    }

    // ============ 产物下载 ============

    /**
     * 获取编译产物的列表
     * @param token GitHub Token
     * @param runId 运行 ID
     */
    suspend fun getArtifacts(token: String, runId: Long): Result<ArtifactList> = runCatching {
        gitHubService.getArtifacts(owner, repo, runId)
    }

    /**
     * 查找 APK 产物
     * @param token GitHub Token
     * @param runId 运行 ID
     * @return APK Artifact 或 null
     */
    suspend fun findApkArtifact(token: String, runId: Long): Result<Artifact?> = runCatching {
        val artifacts = gitHubService.getArtifacts(owner, repo, runId)
        artifacts.artifacts.firstOrNull { !it.expired }
    }

    /**
     * 下载 APK 产物并保存到本地
     * @param context Android Context
     * @param token GitHub Token
     * @param artifactId 产物 ID
     * @return 本地 APK 文件的 Uri 字符串 (content://...) 和文件大小
     */
    suspend fun downloadApkToLocal(
        context: android.content.Context,
        token: String,
        artifactId: Long
    ): Result<Pair<String, Long>> = runCatching {
        // 使用 OkHttp 直接下载带认证的 ZIP
        val client = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val url = "https://api.github.com/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("下载失败: HTTP ${response.code}")
        }

        val body = response.body ?: throw IllegalStateException("下载响应为空")

        // 保存到临时文件
        val tempFile = java.io.File(context.cacheDir, "artifact_$artifactId.zip")
        body.byteStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 解压 ZIP 获取 APK
        val apkDir = java.io.File(context.getExternalFilesDir(null), "Download")
        if (!apkDir.exists()) apkDir.mkdirs()

        var apkSize = 0L
        var apkFileName = "app-debug.apk"

        java.util.zip.ZipFile(tempFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.endsWith(".apk") && !entry.isDirectory) {
                    apkFileName = entry.name.substringAfterLast("/")
                    val apkFile = java.io.File(apkDir, apkFileName)
                    zip.getInputStream(entry).use { input ->
                        apkFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    apkSize = apkFile.length()
                }
            }
        }

        // 清理临时 ZIP
        tempFile.delete()

        if (apkSize == 0L) {
            throw IllegalStateException("ZIP 中未找到 APK 文件")
        }

        // 生成 FileProvider URI
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(apkDir, apkFileName)
        )

        Pair(uri.toString(), apkSize)
    }

    /**
     * 获取构建失败时的错误日志
     * @param token GitHub Token
     * @param runId 运行 ID
     * @return 错误日志文本
     */
    suspend fun getBuildErrorLog(token: String, runId: Long): String? {
        return try {
            val artifacts = gitHubService.getArtifacts(owner, repo, runId)
            val errorLog = artifacts.artifacts.firstOrNull { it.name == "build-error-log" }
            if (errorLog != null) {
                val client = okhttp3.OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(errorLog.archiveDownloadUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body ?: return null
                    val tempFile = java.io.File.createTempFile("error_log", ".zip")
                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    var logContent = ""
                    java.util.zip.ZipFile(tempFile).use { zip ->
                        zip.entries().asSequence().firstOrNull()?.let { entry ->
                            zip.getInputStream(entry).use { input ->
                                logContent = input.bufferedReader().readText()
                            }
                        }
                    }
                    tempFile.delete()
                    // 只返回最后 30 行
                    val lines = logContent.lines()
                    if (lines.size > 30) lines.takeLast(30).joinToString("\n") else logContent
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 推送结果摘要
 */
data class PushSummary(
    val totalFiles: Int,
    val successCount: Int,
    val failCount: Int,
    val errors: List<String>
) {
    val isAllSuccess: Boolean get() = failCount == 0
    override fun toString(): String = buildString {
        append("推送完成: $successCount/$totalFiles 成功")
        if (failCount > 0) append(", $failCount 失败")
    }
}
