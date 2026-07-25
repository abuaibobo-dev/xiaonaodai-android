package com.meitu.generator.ui.cloudbuild

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.remote.dto.Artifact
import com.meitu.generator.data.remote.dto.WorkflowRun
import com.meitu.generator.repository.CloudBuildRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * 云端编译状态
 */
sealed class BuildState {
    object Idle : BuildState()
    object Pushing : BuildState()           // 推送代码中
    object Triggering : BuildState()        // 触发编译中
    data class Building(val runId: Long, val status: String = "in_progress") : BuildState()
    data class Success(val runId: Long, val apkUrl: String? = null) : BuildState()
    data class Failed(val runId: Long = 0, val error: String) : BuildState()
}

/**
 * 云端编译 ViewModel - 管理编译流程、状态和 GitHub Token
 */
@HiltViewModel
class CloudBuildViewModel @Inject constructor(
    application: Application,
    private val cloudBuildRepository: CloudBuildRepository,
    @Named("securePrefs") private val securePrefs: SharedPreferences
) : AndroidViewModel(application) {

    // ============ 状态流 ============

    private val _buildState = MutableStateFlow<BuildState>(BuildState.Idle)
    val buildState: StateFlow<BuildState> = _buildState.asStateFlow()

    private val _buildLogs = MutableStateFlow<List<BuildLog>>(emptyList())
    val buildLogs: StateFlow<List<BuildLog>> = _buildLogs.asStateFlow()

    private val _githubToken = MutableStateFlow("")
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _currentRunId = MutableStateFlow<Long?>(null)
    val currentRunId: StateFlow<Long?> = _currentRunId.asStateFlow()

    // ============ 初始化 ============

    init {
        // 从 EncryptedSharedPreferences 读取已保存的 Token，无则用默认
        _githubToken.value = (securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: "").ifBlank { Constants.DEFAULT_GITHUB_TOKEN }
    }

    // ============ Token 管理 ============

    /**
     * 保存 GitHub Token 到安全存储
     */
    fun saveToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_GITHUB_TOKEN, token).apply()
        _githubToken.value = token
        addLog("info", "GitHub Token 已保存")
    }

    /**
     * 清除 GitHub Token
     */
    fun clearToken() {
        securePrefs.edit().remove(Constants.KEY_GITHUB_TOKEN).apply()
        _githubToken.value = ""
        addLog("info", "GitHub Token 已清除")
    }

    // ============ 编译流程 ============

    /**
     * 开始编译流程 (推送代码 + 触发 Actions)
     * @param projectFiles 项目文件 Map<路径, 内容>
     */
    fun startBuild(projectFiles: Map<String, String>) {
        val token = _githubToken.value
        if (token.isBlank()) {
            addLog("error", "未配置 GitHub Token，请先设置 Token")
            _buildState.value = BuildState.Failed(error = "未配置 GitHub Token")
            return
        }

        if (projectFiles.isEmpty()) {
            addLog("error", "项目文件为空")
            _buildState.value = BuildState.Failed(error = "项目文件为空")
            return
        }

        viewModelScope.launch {
            try {
                // Step 1: 推送代码
                _buildState.value = BuildState.Pushing
                addLog("info", "开始推送 ${projectFiles.size} 个文件到 GitHub...")

                val pushResult = cloudBuildRepository.pushProjectToGithub(projectFiles, token)
                if (pushResult.isFailure) {
                    val error = pushResult.exceptionOrNull()?.message?.take(200) ?: "未知错误"
                    addLog("error", "推送失败: $error")
                    _buildState.value = BuildState.Failed(error = "推送失败: $error")
                    return@launch
                }

                val summary = pushResult.getOrThrow()
                addLog("info", "推送完成: ${summary.successCount}/${summary.totalFiles} 成功")
                if (summary.failCount > 0) {
                    addLog("warning", "${summary.failCount} 个文件推送失败: ${summary.errors.take(3).joinToString("; ")}")
                }

                // Step 2: 检查是否有正在进行的编译
                val latestRun = cloudBuildRepository.getLatestRun(token).getOrNull()
                if (latestRun != null && !latestRun.isCompleted) {
                    addLog("warning", "已有编译正在进行 (runId=${latestRun.id})，切换到监控模式")
                    _currentRunId.value = latestRun.id
                    _buildState.value = BuildState.Building(latestRun.id, latestRun.status)
                    pollStatus(latestRun.id, token)
                    return@launch
                }

                // Step 3: 触发编译
                _buildState.value = BuildState.Triggering
                addLog("info", "触发 GitHub Actions 编译...")

                val triggerResult = cloudBuildRepository.triggerBuild(token)
                if (triggerResult.isFailure) {
                    val error = triggerResult.exceptionOrNull()?.message?.take(200) ?: "触发失败"
                    addLog("error", "触发编译失败: $error")
                    _buildState.value = BuildState.Failed(error = "触发编译失败: $error")
                    return@launch
                }

                addLog("info", "编译已触发，等待编译启动...")

                // 等待一下让 GitHub 创建新的 run
                kotlinx.coroutines.delay(3000)

                // Step 4: 获取最新的 run ID 并开始轮询
                val newRun = cloudBuildRepository.getLatestRun(token).getOrNull()
                if (newRun != null) {
                    _currentRunId.value = newRun.id
                    addLog("info", "编译运行 ID: ${newRun.id}")
                    _buildState.value = BuildState.Building(newRun.id, newRun.status)
                    pollStatus(newRun.id, token)
                } else {
                    addLog("warning", "无法获取编译运行 ID，请手动查看 GitHub Actions")
                    _buildState.value = BuildState.Idle
                }
            } catch (e: Exception) {
                addLog("error", "编译流程异常: ${e.message?.take(200)}")
                _buildState.value = BuildState.Failed(error = e.message?.take(200) ?: "未知异常")
            }
        }
    }

    /**
     * 轮询编译状态
     */
    private fun pollStatus(runId: Long, token: String) {
        viewModelScope.launch {
            cloudBuildRepository.pollBuildStatus(token, runId).collect { run ->
                val statusText = when {
                    run.isCompleted && run.isSuccess -> "✅ 编译成功"
                    run.isCompleted && run.isFailed -> "❌ 编译失败 (${run.conclusion})"
                    run.status == "error" -> "⚠️ 查询出错"
                    else -> "🔄 编译中 [${run.status}]"
                }
                addLog("info", "$statusText (run #$${run.runNumber})")

                when {
                    run.isCompleted && run.isSuccess -> {
                        _buildState.value = BuildState.Success(runId)
                        addLog("info", "可以下载 APK 了")
                    }
                    run.isCompleted && run.isFailed -> {
                        _buildState.value = BuildState.Failed(
                            runId = runId,
                            error = "编译失败: ${run.conclusion}"
                        )
                    }
                    run.status == "error" -> {
                        _buildState.value = BuildState.Failed(
                            runId = runId,
                            error = "状态查询失败"
                        )
                    }
                    else -> {
                        _buildState.value = BuildState.Building(runId, run.status)
                    }
                }
            }
        }
    }

    /**
     * 检查编译状态（手动刷新）
     */
    fun checkStatus() {
        val token = _githubToken.value
        val runId = _currentRunId.value
        if (token.isBlank() || runId == null) return

        viewModelScope.launch {
            val result = cloudBuildRepository.getBuildStatus(token, runId)
            if (result.isSuccess) {
                val run = result.getOrThrow()
                when {
                    run.isCompleted && run.isSuccess -> {
                        _buildState.value = BuildState.Success(runId)
                        addLog("info", "✅ 编译成功")
                    }
                    run.isCompleted && run.isFailed -> {
                        _buildState.value = BuildState.Failed(runId, "编译失败: ${run.conclusion}")
                        addLog("error", "❌ 编译失败: ${run.conclusion}")
                    }
                    else -> {
                        _buildState.value = BuildState.Building(runId, run.status)
                        addLog("info", "编译状态: ${run.status}")
                    }
                }
            } else {
                addLog("error", "查询状态失败: ${result.exceptionOrNull()?.message?.take(100)}")
            }
        }
    }

    /**
     * 下载 APK
     */
    fun downloadApk() {
        val token = _githubToken.value
        val runId = _currentRunId.value
        if (token.isBlank() || runId == null) return

        viewModelScope.launch {
            addLog("info", "查找 APK 产物...")
            val artifactResult = cloudBuildRepository.findApkArtifact(token, runId)
            if (artifactResult.isFailure) {
                addLog("error", "查找产物失败: ${artifactResult.exceptionOrNull()?.message?.take(100)}")
                return@launch
            }

            val artifact = artifactResult.getOrNull()
            if (artifact == null) {
                addLog("warning", "未找到 APK 产物")
                return@launch
            }

            addLog("info", "找到产物: ${artifact.name} (${formatBytes(artifact.sizeInBytes)})")
            val context = getApplication<Application>().applicationContext
            val downloadResult = cloudBuildRepository.downloadApkToLocal(context, token, artifact.id)
            if (downloadResult.isSuccess) {
                val (localUri, fileSize) = downloadResult.getOrThrow()
                addLog("info", "APK 已下载: $localUri (${formatBytes(fileSize)})")
                _buildState.value = BuildState.Success(runId, localUri)
            } else {
                addLog("error", "下载 APK 失败: ${downloadResult.exceptionOrNull()?.message?.take(100)}")
            }
        }
    }

    /**
     * 重置编译状态
     */
    fun reset() {
        _buildState.value = BuildState.Idle
        _buildLogs.value = emptyList()
        _currentRunId.value = null
    }

    // ============ 日志管理 ============

    private fun addLog(level: String, message: String) {
        val newLog = BuildLog(
            timestamp = System.currentTimeMillis(),
            level = level,
            message = message
        )
        _buildLogs.value = _buildLogs.value + newLog
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

/**
 * 编译日志条目
 */
data class BuildLog(
    val timestamp: Long,
    val level: String,   // info, warning, error
    val message: String
)
