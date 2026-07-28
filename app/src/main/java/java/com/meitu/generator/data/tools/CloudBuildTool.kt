package com.meitu.generator.data.tools

import android.content.SharedPreferences
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.model.ToolContext
import com.meitu.generator.repository.CloudBuildRepository
import com.meitu.generator.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 编译进度回调
 */
data class BuildProgress(
    val status: String,
    val message: String,
    val progress: Float = 0f,
    val apkUri: String? = null,
    val apkSizeBytes: Long = 0L,
    val errorLog: String? = null
)

/**
 * 编译进度回调接口
 */
fun interface BuildProgressCallback {
    fun onProgress(progress: BuildProgress)
}

/**
 * 云端编译工具 - 推送代码后由 push 事件自动触发编译，不再手动 triggerBuild
 */
@Singleton
class CloudBuildTool @Inject constructor(
    private val cloudBuildRepository: CloudBuildRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : Tool {

    companion object {
        val progressCallback = ThreadLocal<BuildProgressCallback?>()
    }

    override val name = "cloud_build"
    override val description = "将Android项目代码推送到GitHub并通过Actions编译生成APK，自动轮询编译状态并下载APK"
    override val parametersSchema = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply {
            add("projectCode", JsonObject().apply {
                addProperty("type", "object")
                addProperty("description", "项目文件映射，key为文件路径，value为文件内容")
            })
            add("commitMessage", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "Git 提交信息")
            })
        })
        add("required", JsonArray().apply { add("projectCode") })
    }

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val token = (securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: "").ifBlank { Constants.DEFAULT_GITHUB_TOKEN }
        val callback = progressCallback.get()

        @Suppress("UNCHECKED_CAST")
        val projectCodeRaw = arguments["projectCode"]
        val projectCode: Map<String, String> = when (projectCodeRaw) {
            is Map<*, *> -> projectCodeRaw.entries.mapNotNull { (k, v) ->
                if (k is String && v is String) k to v else null
            }.toMap()
            else -> return "错误: projectCode 参数格式不正确"
        }

        if (projectCode.isEmpty()) return "错误: projectCode 为空"

        val commitMessage = (arguments["commitMessage"] as? String) ?: "Auto build from ${Constants.APP_NAME} App"

        return try {
            // Step 1: 推送代码（push 事件会自动触发 workflow 编译）
            callback?.onProgress(BuildProgress("pushing", "📤 正在推送代码到 GitHub...", 0.1f))

            val pushResult = cloudBuildRepository.pushProjectToGithub(projectCode, token)
            if (pushResult.isFailure) {
                callback?.onProgress(BuildProgress("failed", "❌ 推送失败", errorLog = pushResult.exceptionOrNull()?.message))
                return "推送失败: ${pushResult.exceptionOrNull()?.message?.take(200)}"
            }

            val pushSummary = pushResult.getOrThrow()
            if (!pushSummary.isAllSuccess) {
                callback?.onProgress(BuildProgress("failed", "❌ 推送部分失败", errorLog = pushSummary.errors.joinToString("; ")))
                return "推送部分失败: $pushSummary"
            }

            // Step 2: 等待 push 事件触发 workflow run（GitHub 通常需要几秒）
            callback?.onProgress(BuildProgress("building", "⏳ 等待 GitHub Actions 启动编译...", 0.15f))

            var workflowRunId: Long? = null
            for (attempt in 1..8) {
                delay(5000)
                val latestRun = cloudBuildRepository.getLatestRun(token).getOrNull()
                if (latestRun != null && latestRun.status != "completed") {
                    workflowRunId = latestRun.id
                    callback?.onProgress(BuildProgress("building", "🚀 检测到编译任务 (Run #$workflowRunId)", 0.2f))
                    break
                }
            }

            if (workflowRunId == null) {
                val latestRun = cloudBuildRepository.getLatestRun(token).getOrNull()
                if (latestRun == null) {
                    callback?.onProgress(BuildProgress("failed", "❌ 无法获取编译状态"))
                    return "无法获取编译运行状态"
                }
                workflowRunId = latestRun.id
            }

            // Step 3: 轮询编译状态
            val runId = workflowRunId!!
            var buildSuccess = false

            cloudBuildRepository.pollBuildStatus(token, runId).collect { run ->
                val progress = when (run.status) {
                    "queued" -> 0.3f
                    "in_progress" -> 0.5f
                    "completed" -> 0.8f
                    else -> 0.4f
                }
                val statusText = when (run.status) {
                    "queued" -> "⏳ 排队等待中..."
                    "in_progress" -> "🔨 编译进行中..."
                    "completed" -> if (run.conclusion == "success") "✅ 编译成功!" else "❌ 编译失败"
                    else -> "🔄 ${run.status}"
                }
                callback?.onProgress(BuildProgress(
                    if (run.isCompleted) if (run.conclusion == "success") "downloading" else "failed" else "building",
                    statusText, progress
                ))
                if (run.isCompleted) buildSuccess = run.conclusion == "success"
            }

            if (!buildSuccess) {
                val errorLog = cloudBuildRepository.getBuildErrorLog(token, runId)
                callback?.onProgress(BuildProgress("failed", "❌ 编译失败", errorLog = errorLog?.takeLast(500)))
                return buildString {
                    appendLine("❌ 编译失败"); appendLine(); appendLine("错误日志:"); appendLine("```"); appendLine(errorLog?.takeLast(500) ?: "未知错误"); appendLine("```")
                }
            }

            // Step 4: 下载 APK
            callback?.onProgress(BuildProgress("downloading", "📥 正在下载 APK...", 0.85f))

            val artifact = cloudBuildRepository.findApkArtifact(token, runId).getOrNull()
            if (artifact == null) {
                callback?.onProgress(BuildProgress("failed", "❌ 未找到 APK 产物"))
                return "编译成功但未找到 APK 产物"
            }

            val downloadResult = cloudBuildRepository.downloadApkToLocal(context.applicationContext, token, artifact.id)
            if (downloadResult.isFailure) {
                callback?.onProgress(BuildProgress("failed", "❌ APK 下载失败", errorLog = downloadResult.exceptionOrNull()?.message))
                return "APK 下载失败: ${downloadResult.exceptionOrNull()?.message?.take(200)}"
            }

            val (apkUri, apkSize) = downloadResult.getOrThrow()
            val sizeMb = "%.1f".format(apkSize / 1024.0 / 1024.0)

            callback?.onProgress(BuildProgress("completed", "✅ 编译完成！APK 已就绪 ($sizeMb MB)", 1.0f, apkUri, apkSize))

            buildString {
                appendLine("BUILD_SUCCESS")
                appendLine("apk_uri=$apkUri")
                appendLine("apk_size=$sizeMb MB")
                appendLine("推送: ${pushSummary.successCount}/${pushSummary.totalFiles} 文件")
                appendLine("提交: $commitMessage")
            }
        } catch (e: Exception) {
            callback?.onProgress(BuildProgress("failed", "❌ 编译异常: ${e.message?.take(100)}"))
            "云端编译异常: ${e.message?.take(200)}"
        }
    }
}
