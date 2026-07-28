package com.meitu.generator.data.tools

import android.content.SharedPreferences
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.meitu.generator.data.agent.Tool
import com.meitu.generator.data.model.ToolContext
import com.meitu.generator.repository.CloudBuildRepository
import com.meitu.generator.util.Constants
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 编译进度回调
 */
data class BuildProgress(
    val status: String,     // "pushing", "building", "downloading", "completed", "failed"
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
 * 云端编译工具 - 完整流程：推送 → 触发编译 → 轮询状态 → 下载APK
 */
@Singleton
class CloudBuildTool @Inject constructor(
    private val cloudBuildRepository: CloudBuildRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : Tool {

    companion object {
        /** ThreadLocal 进度回调，供外部在调用前设置 */
        val progressCallback = AtomicReference<BuildProgressCallback?>(null)
    }

    override val name = "cloud_build"
    override val description = "将Android项目代码推送到GitHub并通过Actions编译生成APK，自动轮询编译状态并下载APK"
    override val parametersSchema = JsonObject().apply {
        addProperty("type", "object")
        add("properties", JsonObject().apply {
            add("projectCode", JsonObject().apply {
                addProperty("type", "object")
                addProperty("description", "项目文件映射，key为文件路径，value为文件内容。例如: {\"app/src/main/java/.../MainActivity.kt\": \"package ...\"}")
            })
            add("commitMessage", JsonObject().apply {
                addProperty("type", "string")
                addProperty("description", "Git 提交信息，默认为 'Auto build from 布老师 App'")
            })
        })
        add("required", JsonArray().apply { add("projectCode") })
    }

    override suspend fun execute(arguments: Map<String, Any>, context: ToolContext): String {
        val token = (securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: "").ifBlank { Constants.DEFAULT_GITHUB_TOKEN }
        val callback = progressCallback.get()

        // 解析 projectCode 参数
        @Suppress("UNCHECKED_CAST")
        val projectCodeRaw = arguments["projectCode"]
        val projectCode: Map<String, String> = when (projectCodeRaw) {
            is Map<*, *> -> {
                projectCodeRaw.entries.mapNotNull { (k, v) ->
                    if (k is String && v is String) k to v else null
                }.toMap()
            }
            else -> return "错误: projectCode 参数格式不正确，应为 Map<文件路径, 文件内容>"
        }

        if (projectCode.isEmpty()) {
            return "错误: projectCode 为空，至少需要一个文件"
        }

        val commitMessage = (arguments["commitMessage"] as? String)
            ?: "Auto build from ${Constants.APP_NAME} App"

        return try {
            // ========== Step 1: 推送代码 ==========
            callback?.onProgress(BuildProgress("pushing", "📤 正在推送代码到 GitHub...", 0.1f))

            val pushResult = cloudBuildRepository.pushProjectToGithub(projectCode, token)
            if (pushResult.isFailure) {
                callback?.onProgress(BuildProgress("failed", "❌ 推送失败", errorLog = pushResult.exceptionOrNull()?.message))
                return "推送失败: ${pushResult.exceptionOrNull()?.message?.take(200)}"
            }

            val pushSummary = pushResult.getOrThrow()
            if (!pushSummary.isAllSuccess) {
                val errMsg = "推送部分失败: $pushSummary"
                callback?.onProgress(BuildProgress("failed", "❌ $errMsg", errorLog = pushSummary.errors.joinToString("; ")))
                return errMsg
            }

            // ========== Step 2: 检查是否有正在进行的编译 ==========
            val latestRun = cloudBuildRepository.getLatestRun(token).getOrNull()
            if (latestRun != null && !latestRun.isCompleted) {
                callback?.onProgress(BuildProgress("building", "⏳ 已有编译进行中，等待完成...", 0.15f))
                // 等待已有编译完成
                cloudBuildRepository.pollBuildStatus(token, latestRun.id).collect { run ->
                    val elapsed = when (run.status) {
                        "queued" -> 0.2f
                        "in_progress" -> 0.4f
                        else -> 0.5f
                    }
                    callback?.onProgress(BuildProgress("building", "⏳ 等待编译中... (${run.status})", elapsed))
                }
            }

            // ========== Step 3: 触发新编译 ==========
            callback?.onProgress(BuildProgress("building", "🚀 触发 GitHub Actions 编译...", 0.2f))

            val triggerResult = cloudBuildRepository.triggerBuild(token)
            if (triggerResult.isFailure) {
                callback?.onProgress(BuildProgress("failed", "❌ 触发编译失败", errorLog = triggerResult.exceptionOrNull()?.message))
                return "触发编译失败: ${triggerResult.exceptionOrNull()?.message?.take(200)}"
            }

            // 等待 GitHub 创建 run（通常需要几秒）
            delay(5000)

            // ========== Step 4: 获取最新 run 并轮询 ==========
            val newRun = cloudBuildRepository.getLatestRun(token).getOrNull()
            if (newRun == null) {
                callback?.onProgress(BuildProgress("failed", "❌ 无法获取编译状态"))
                return "无法获取编译运行状态"
            }

            val runId = newRun.id
            callback?.onProgress(BuildProgress("building", "🔨 编译中... (Run #$runId)", 0.25f))

            var buildSuccess = false
            cloudBuildRepository.pollBuildStatus(token, runId).collect { run ->
                val progress = when (run.status) {
                    "queued" -> 0.3f
                    "in_progress" -> 0.5f
                    "completed" -> if (run.conclusion == "success") 0.8f else 0.8f
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
                    statusText,
                    progress
                ))
                if (run.isCompleted) {
                    buildSuccess = run.conclusion == "success"
                }
            }

            // ========== Step 5: 编译失败 → 获取错误日志 ==========
            if (!buildSuccess) {
                val errorLog = cloudBuildRepository.getBuildErrorLog(token, runId)
                val errorMsg = errorLog?.takeLast(500) ?: "编译失败，请前往 GitHub Actions 查看日志"
                callback?.onProgress(BuildProgress("failed", "❌ 编译失败", errorLog = errorLog?.takeLast(500)))
                return buildString {
                    appendLine("❌ 编译失败")
                    appendLine()
                    appendLine("错误日志:")
                    appendLine("```")
                    appendLine(errorMsg)
                    appendLine("```")
                }
            }

            // ========== Step 6: 下载 APK ==========
            callback?.onProgress(BuildProgress("downloading", "📥 正在下载 APK...", 0.85f))

            val artifactResult = cloudBuildRepository.findApkArtifact(token, runId)
            val artifact = artifactResult.getOrNull()
            if (artifact == null) {
                callback?.onProgress(BuildProgress("failed", "❌ 未找到 APK 产物"))
                return "编译成功但未找到 APK 产物"
            }

            val downloadResult = cloudBuildRepository.downloadApkToLocal(
                context.applicationContext, token, artifact.id
            )

            if (downloadResult.isFailure) {
                callback?.onProgress(BuildProgress("failed", "❌ APK 下载失败", errorLog = downloadResult.exceptionOrNull()?.message))
                return "APK 下载失败: ${downloadResult.exceptionOrNull()?.message?.take(200)}"
            }

            val (apkUri, apkSize) = downloadResult.getOrThrow()
            val sizeMb = "%.1f".format(apkSize / 1024.0 / 1024.0)

            // ========== Step 7: 完成 ==========
            callback?.onProgress(BuildProgress(
                "completed",
                "✅ 编译完成！APK 已就绪 ($sizeMb MB)",
                1.0f,
                apkUri = apkUri,
                apkSizeBytes = apkSize
            ))

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
