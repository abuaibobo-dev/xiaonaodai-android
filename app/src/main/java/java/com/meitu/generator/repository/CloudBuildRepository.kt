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
 * 构建步骤状态
 */
data class BuildStep(
    val id: String,
    val name: String,
    val status: StepStatus = StepStatus.PENDING,
    val detail: String = ""
)

enum class StepStatus {
    PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
}

/**
 * 推送进度
 */
data class PushProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
    val success: Boolean? = null // null=进行中, true=成功, false=失败
)

/**
 * 下载进度
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Float
) {
    val displayBytes: String
        get() {
            val downloaded = when {
                bytesDownloaded >= 1024 * 1024 -> "%.1fMB".format(bytesDownloaded / (1024.0 * 1024.0))
                bytesDownloaded >= 1024 -> "%.0fKB".format(bytesDownloaded / 1024.0)
                else -> "${bytesDownloaded}B"
            }
            val total = when {
                totalBytes >= 1024 * 1024 -> "%.1fMB".format(totalBytes / (1024.0 * 1024.0))
                totalBytes >= 1024 -> "%.0fKB".format(totalBytes / 1024.0)
                else -> "${totalBytes}B"
            }
            return "$downloaded / $total"
        }
}

/**
 * 云端编译仓库 - 管理代码推送、触发编译、轮询状态、下载 APK
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

        val existingSha = try {
            gitHubService.getFileContent(owner, repo, path).sha
        } catch (_: Exception) {
            null
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
     * 批量推送项目文件到 GitHub，带逐文件进度回调
     * @param onPushProgress 每个文件推送时的进度回调
     */
    suspend fun pushProjectToGithub(
        projectFiles: Map<String, String>,
        token: String,
        onPushProgress: ((PushProgress) -> Unit)? = null
    ): Result<PushSummary> = runCatching {
        var successCount = 0
        var failCount = 0
        val errors = mutableListOf<String>()
        val total = projectFiles.size

        projectFiles.entries.forEachIndexed { index, (originalPath, content) ->
            val path = if (originalPath.startsWith("app/")) {
                "user-project/$originalPath"
            } else {
                originalPath
            }
            val shortName = path.substringAfterLast("/")

            onPushProgress?.invoke(PushProgress(index + 1, total, shortName, null))

            val result = pushFile(path, content, token = token)
            if (result.isSuccess) {
                successCount++
                onPushProgress?.invoke(PushProgress(index + 1, total, shortName, true))
            } else {
                failCount++
                val errMsg = result.exceptionOrNull()?.message?.take(80) ?: ""
                errors.add("$path: $errMsg")
                onPushProgress?.invoke(PushProgress(index + 1, total, shortName, false))
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

    suspend fun triggerBuild(
        token: String,
        workflowId: String = Constants.GITHUB_WORKFLOW_ID
    ): Result<Unit> = runCatching {
        val request = WorkflowDispatchRequest(ref = "main")
        val response = gitHubService.triggerWorkflow(owner, repo, workflowId, request)
        if (!response.isSuccessful) {
            throw IllegalStateException("触发编译失败: HTTP ${response.code()}")
        }
    }

    // ============ 编译状态 ============

    private val userProjectWorkflowId = "user-project-build.yml"

    suspend fun getLatestRun(token: String): Result<WorkflowRun?> = runCatching {
        try {
            val runs = gitHubService.getWorkflowRunsByWorkflowId(owner, repo, userProjectWorkflowId, perPage = 1)
            val userRun = runs.workflowRuns.firstOrNull()
            if (userRun != null) return@runCatching userRun
        } catch (_: Exception) {}
        val runs = gitHubService.getWorkflowRuns(owner, repo, perPage = 1)
        runs.workflowRuns.firstOrNull()
    }

    suspend fun getBuildStatus(token: String, runId: Long): Result<WorkflowRun> = runCatching {
        gitHubService.getWorkflowRun(owner, repo, runId)
    }

    /**
     * 获取编译任务的 jobs 和 steps 详情
     */
    suspend fun getBuildJobs(token: String, runId: Long): Result<WorkflowJobsResponse> = runCatching {
        gitHubService.getWorkflowRunJobs(owner, repo, runId)
    }

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
                val errorRun = WorkflowRun(
                    id = runId, name = "Unknown", status = "error",
                    conclusion = "error", createdAt = "", updatedAt = "",
                    headBranch = "", headSha = "", runNumber = 0, url = ""
                )
                emit(errorRun)
                break
            }
            delay(intervalMs)
        }
    }

    /**
     * 轮询编译状态 + CI jobs 详情
     */
    fun pollBuildStatusWithJobs(
        token: String,
        runId: Long,
        intervalMs: Long = Constants.GITHUB_POLL_INTERVAL_MS
    ): Flow<Pair<WorkflowRun, List<WorkflowJob>>> = flow {
        while (true) {
            val result = getBuildStatus(token, runId)
            if (result.isSuccess) {
                val run = result.getOrThrow()
                // 获取 jobs 详情
                val jobs = try {
                    gitHubService.getWorkflowRunJobs(owner, repo, runId).jobs
                } catch (_: Exception) {
                    emptyList()
                }
                emit(Pair(run, jobs))
                if (run.isCompleted) break
            } else {
                val errorRun = WorkflowRun(
                    id = runId, name = "Unknown", status = "error",
                    conclusion = "error", createdAt = "", updatedAt = "",
                    headBranch = "", headSha = "", runNumber = 0, url = ""
                )
                emit(Pair(errorRun, emptyList()))
                break
            }
            delay(intervalMs)
        }
    }

    // ============ 产物下载 ============

    suspend fun getArtifacts(token: String, runId: Long): Result<ArtifactList> = runCatching {
        gitHubService.getArtifacts(owner, repo, runId)
    }

    suspend fun findApkArtifact(token: String, runId: Long): Result<Artifact?> = runCatching {
        val artifacts = gitHubService.getArtifacts(owner, repo, runId)
        // 优先匹配 user-project-apk，其次匹配任意非过期产物
        artifacts.artifacts.firstOrNull { it.name == "user-project-apk" && !it.expired }
            ?: artifacts.artifacts.firstOrNull { it.name.endsWith("-apk") && !it.expired }
            ?: artifacts.artifacts.firstOrNull { !it.expired }
    }

    /**
     * 从 GitHub Release 下载 APK（速度更快）
     * 优先尝试 Release 下载，失败则回退到 Artifact 下载
     * @param onDownloadProgress 下载进度回调
     */
    suspend fun downloadApkToLocal(
        context: android.content.Context,
        token: String,
        artifactId: Long,
        runId: Long? = null,
        releaseTag: String = "latest-apk",
        onDownloadProgress: ((DownloadProgress) -> Unit)? = null
    ): Result<Pair<String, Long>> = runCatching {
        // 优先尝试从 Release 下载
        val releaseResult = tryDownloadFromRelease(context, token, releaseTag, onDownloadProgress)
        if (releaseResult != null) {
            return@runCatching releaseResult
        }

        // 回退到 Artifact 下载
        downloadFromArtifact(context, token, artifactId, onDownloadProgress)
    }

    /**
     * 尝试从 Release 下载 APK
     */
    private suspend fun tryDownloadFromRelease(
        context: android.content.Context,
        token: String,
        releaseTag: String = "latest-apk",
        onDownloadProgress: ((DownloadProgress) -> Unit)?
    ): Pair<String, Long>? {
        return try {
            val release = gitHubService.getReleaseByTag(owner, repo, releaseTag)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return null

            val totalBytes = apkAsset.size
            onDownloadProgress?.invoke(DownloadProgress(0, totalBytes, 0f))

            val client = okhttp3.OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = okhttp3.Request.Builder()
                .url(apkAsset.browserDownloadUrl)
                .addHeader("Accept", "application/octet-stream")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body ?: return null
            val contentLength = body.contentLength().let { if (it > 0) it else totalBytes }

            val apkDir = java.io.File(context.getExternalFilesDir(null), "Download")
            if (!apkDir.exists()) apkDir.mkdirs()
            val apkFile = java.io.File(apkDir, "app-debug.apk")

            var downloaded = 0L
            val buffer = ByteArray(8192)
            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onDownloadProgress?.invoke(
                            DownloadProgress(downloaded, contentLength, downloaded.toFloat() / contentLength)
                        )
                    }
                }
            }

            val apkSize = apkFile.length()
            if (apkSize == 0L) return null

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            Pair(uri.toString(), apkSize)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 从 Artifact 下载 APK（回退方案）
     */
    private suspend fun downloadFromArtifact(
        context: android.content.Context,
        token: String,
        artifactId: Long,
        onDownloadProgress: ((DownloadProgress) -> Unit)?
    ): Pair<String, Long> {
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
        val totalBytes = body.contentLength()

        // 保存到临时 ZIP
        val tempFile = java.io.File(context.cacheDir, "artifact_$artifactId.zip")
        var downloaded = 0L
        val buffer = ByteArray(8192)
        body.byteStream().use { input ->
            tempFile.outputStream().use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0) {
                        onDownloadProgress?.invoke(
                            DownloadProgress(downloaded, totalBytes, downloaded.toFloat() / totalBytes)
                        )
                    }
                }
            }
        }

        // 解压 ZIP
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

        tempFile.delete()

        if (apkSize == 0L) {
            throw IllegalStateException("ZIP 中未找到 APK 文件")
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            java.io.File(apkDir, apkFileName)
        )
        return Pair(uri.toString(), apkSize)
    }

    /**
     * 获取构建失败时的错误日志
     */
    suspend fun getBuildErrorLog(token: String, runId: Long): String? {
        return try {
            val artifacts = gitHubService.getArtifacts(owner, repo, runId)
            val errorLog = artifacts.artifacts.firstOrNull { it.name == "user-build-error-log" || it.name == "build-error-log" }
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
                    val lines = logContent.lines()
                    if (lines.size > 30) lines.takeLast(30).joinToString("\n") else logContent
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

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

