package com.meitu.generator.ui.cloudbuild

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.remote.dto.WorkflowJob
import com.meitu.generator.data.remote.dto.WorkflowRun
import com.meitu.generator.repository.BuildStep
import com.meitu.generator.repository.DownloadProgress
import com.meitu.generator.repository.StepStatus
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
    object Pushing : BuildState()
    object Triggering : BuildState()
    data class Building(val runId: Long, val status: String = "in_progress") : BuildState()
    data class Downloading(val runId: Long, val progress: Float = 0f, val progressText: String = "") : BuildState()
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

    private val _buildSteps = MutableStateFlow<List<BuildStep>>(emptyList())
    val buildSteps: StateFlow<List<BuildStep>> = _buildSteps.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _ciJobs = MutableStateFlow<List<WorkflowJob>>(emptyList())
    val ciJobs: StateFlow<List<WorkflowJob>> = _ciJobs.asStateFlow()

    private val _buildLogs = MutableStateFlow<List<BuildLog>>(emptyList())
    val buildLogs: StateFlow<List<BuildLog>> = _buildLogs.asStateFlow()

    private val _githubToken = MutableStateFlow("")
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    private val _currentRunId = MutableStateFlow<Long?>(null)
    val currentRunId: StateFlow<Long?> = _currentRunId.asStateFlow()

    // ============ 初始化 ============

    init {
        _githubToken.value = (securePrefs.getString(Constants.KEY_GITHUB_TOKEN, "") ?: "").ifBlank { Constants.DEFAULT_GITHUB_TOKEN }
    }

    // ============ Token 管理 ============

    fun saveToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_GITHUB_TOKEN, token).apply()
        _githubToken.value = token
        addLog("info", "GitHub Token 已保存")
    }

    fun clearToken() {
        securePrefs.edit().remove(Constants.KEY_GITHUB_TOKEN).apply()
        _githubToken.value = ""
        addLog("info", "GitHub Token 已清除")
    }

    // ============ 步骤管理 ============

    private fun initBuildSteps() {
        _buildSteps.value = listOf(
            BuildStep("push", "推送代码到 GitHub"),
            BuildStep("trigger", "触发云端编译"),
            BuildStep("compile", "云端编译中"),
            BuildStep("download", "下载 APK")
        )
    }

    private fun updateStep(stepId: String, status: StepStatus, detail: String = "") {
        _buildSteps.value = _buildSteps.value.map { step ->
            if (step.id == stepId) step.copy(status = status, detail = detail) else step
        }
    }

    // ============ 编译流程 ============

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

        // 初始化步骤和日志
        initBuildSteps()
        _downloadProgress.value = null
        _ciJobs.value = emptyList()

        viewModelScope.launch {
            try {
                // Step 1: 推送代码
                _buildState.value = BuildState.Pushing
                updateStep("push", StepStatus.RUNNING, "正在连接 GitHub...")
                addLog("info", "开始推送 ${projectFiles.size} 个文件到 GitHub...")

                val pushResult = cloudBuildRepository.pushProjectToGithub(
                    projectFiles, token,
                    onPushProgress = { progress ->
                        val detail = "(${progress.current}/${progress.total}) ${progress.currentFile}"
                        updateStep("push", StepStatus.RUNNING, detail)
                        when (progress.success) {
                            true -> addLog("info", "✅ ${progress.currentFile}")
                            false -> addLog("error", "❌ ${progress.currentFile}")
                            null -> {} // 进行中
                        }
                    }
                )

                if (pushResult.isFailure) {
                    val error = pushResult.exceptionOrNull()?.message?.take(200) ?: "未知错误"
                    updateStep("push", StepStatus.FAILED, error)
                    addLog("error", "推送失败: $error")
                    _buildState.value = BuildState.Failed(error = "推送失败: $error")
                    return@launch
                }

                val summary = pushResult.getOrThrow()
                if (summary.isAllSuccess) {
                    updateStep("push", StepStatus.SUCCESS, "${summary.totalFiles} 个文件全部推送成功")
                } else {
                    updateStep("push", StepStatus.FAILED, "${summary.failCount} 个文件推送失败")
                }
                addLog("info", "推送完成: ${summary.successCount}/${summary.totalFiles} 成功")

                if (summary.failCount > 0 && !summary.isAllSuccess) {
                    _buildState.value = BuildState.Failed(error = "推送部分失败: ${summary.errors.take(2).joinToString("; ")}")
                    return@launch
                }

                // Step 2: 触发编译
                _buildState.value = BuildState.Triggering
                updateStep("trigger", StepStatus.RUNNING, "正在触发...")
                addLog("info", "触发 GitHub Actions 编译...")

                // 先检查已有编译
                val latestRun = cloudBuildRepository.getLatestRun(token).getOrNull()
                if (latestRun != null && !latestRun.isCompleted) {
                    addLog("info", "已有编译进行中 (run #${latestRun.id})，切换到监控模式")
                    _currentRunId.value = latestRun.id
                    updateStep("trigger", StepStatus.SUCCESS, "已有编译进行中")
                    updateStep("compile", StepStatus.RUNNING, "排队中...")
                    _buildState.value = BuildState.Building(latestRun.id, latestRun.status)
                    pollStatusWithJobs(latestRun.id, token)
                    return@launch
                }

                val triggerResult = cloudBuildRepository.triggerBuild(token)
                if (triggerResult.isFailure) {
                    val error = triggerResult.exceptionOrNull()?.message?.take(200) ?: "触发失败"
                    updateStep("trigger", StepStatus.FAILED, error)
                    addLog("error", "触发编译失败: $error")
                    _buildState.value = BuildState.Failed(error = "触发编译失败: $error")
                    return@launch
                }

                updateStep("trigger", StepStatus.SUCCESS, "编译任务已创建")
                addLog("info", "编译已触发，等待启动...")

                kotlinx.coroutines.delay(3000)

                // Step 3: 轮询编译
                val newRun = cloudBuildRepository.getLatestRun(token).getOrNull()
                if (newRun != null) {
                    _currentRunId.value = newRun.id
                    addLog("info", "编译运行 ID: ${newRun.id}")
                    updateStep("compile", StepStatus.RUNNING, when (newRun.status) {
                        "queued" -> "排队等待中..."
                        "in_progress" -> "编译进行中..."
                        else -> "${newRun.status}"
                    })
                    _buildState.value = BuildState.Building(newRun.id, newRun.status)
                    pollStatusWithJobs(newRun.id, token)
                } else {
                    updateStep("compile", StepStatus.FAILED, "无法获取编译状态")
                    addLog("warning", "无法获取编译运行 ID")
                    _buildState.value = BuildState.Idle
                }
            } catch (e: Exception) {
                addLog("error", "编译流程异常: ${e.message?.take(200)}")
                _buildState.value = BuildState.Failed(error = e.message?.take(200) ?: "未知异常")
            }
        }
    }

    /**
     * 轮询编译状态 + CI jobs 详情
     */
    private fun pollStatusWithJobs(runId: Long, token: String) {
        viewModelScope.launch {
            cloudBuildRepository.pollBuildStatusWithJobs(token, runId).collect { (run, jobs) ->
                _ciJobs.value = jobs

                // 更新 compile 步骤详情
                val ciDetail = if (jobs.isNotEmpty()) {
                    val currentStep = jobs.flatMap { it.steps }.firstOrNull { it.status == "in_progress" }
                    val completedSteps = jobs.flatMap { it.steps }.count { it.conclusion == "success" }
                    val totalSteps = jobs.flatMap { it.steps }.size
                    if (currentStep != null) {
                        "正在执行: ${currentStep.name} ($completedSteps/$totalSteps 步骤完成)"
                    } else if (run.status == "queued") {
                        "排队等待中..."
                    } else {
                        "编译中... ($completedSteps/$totalSteps 步骤完成)"
                    }
                } else {
                    when (run.status) {
                        "queued" -> "排队等待中..."
                        "in_progress" -> "编译进行中..."
                        else -> run.status
                    }
                }

                val statusText = when {
                    run.isCompleted && run.isSuccess -> "✅ 编译成功"
                    run.isCompleted && run.isFailed -> "❌ 编译失败"
                    run.status == "error" -> "⚠️ 查询出错"
                    else -> "🔄 $ciDetail"
                }
                addLog("info", "$statusText (run #${run.runNumber})")

                when {
                    run.isCompleted && run.isSuccess -> {
                        updateStep("compile", StepStatus.SUCCESS, "编译完成")
                        _buildState.value = BuildState.Success(runId)
                        addLog("info", "编译成功，准备下载 APK...")
                        // 自动开始下载
                        downloadApk()
                    }
                    run.isCompleted && run.isFailed -> {
                        val failedSteps = jobs.flatMap { it.steps }.filter { it.conclusion == "failure" }
                        val errorDetail = if (failedSteps.isNotEmpty()) {
                            "失败步骤: ${failedSteps.joinToString(", ") { it.name }}"
                        } else {
                            "编译失败: ${run.conclusion}"
                        }
                        updateStep("compile", StepStatus.FAILED, errorDetail)
                        _buildState.value = BuildState.Failed(runId = runId, error = errorDetail)
                        // 获取错误日志
                        viewModelScope.launch {
                            val errorLog = cloudBuildRepository.getBuildErrorLog(token, runId)
                            if (errorLog != null) {
                                addLog("error", "错误详情:\n$errorLog")
                            }
                        }
                    }
                    run.status == "error" -> {
                        updateStep("compile", StepStatus.FAILED, "状态查询失败")
                        _buildState.value = BuildState.Failed(runId = runId, error = "状态查询失败")
                    }
                    else -> {
                        updateStep("compile", StepStatus.RUNNING, ciDetail)
                        _buildState.value = BuildState.Building(runId, run.status)
                    }
                }
            }
        }
    }

    fun checkStatus() {
        val token = _githubToken.value
        val runId = _currentRunId.value
        if (token.isBlank() || runId == null) return

        viewModelScope.launch {
            val result = cloudBuildRepository.getBuildStatus(token, runId)
            if (result.isSuccess) {
                val run = result.getOrThrow()
                // 获取 jobs
                val jobs = cloudBuildRepository.getBuildJobs(token, runId).getOrNull()
                if (jobs != null) _ciJobs.value = jobs.jobs

                when {
                    run.isCompleted && run.isSuccess -> {
                        _buildState.value = BuildState.Success(runId)
                        updateStep("compile", StepStatus.SUCCESS, "编译完成")
                        addLog("info", "✅ 编译成功")
                    }
                    run.isCompleted && run.isFailed -> {
                        _buildState.value = BuildState.Failed(runId, "编译失败: ${run.conclusion}")
                        updateStep("compile", StepStatus.FAILED, "编译失败")
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
            updateStep("download", StepStatus.RUNNING, "查找 APK 产物...")
            _buildState.value = BuildState.Downloading(runId, 0f, "查找产物中...")
            addLog("info", "查找 APK 产物...")

            val artifactResult = cloudBuildRepository.findApkArtifact(token, runId)
            if (artifactResult.isFailure) {
                addLog("error", "查找产物失败: ${artifactResult.exceptionOrNull()?.message?.take(100)}")
                updateStep("download", StepStatus.FAILED, "查找产物失败")
                return@launch
            }

            val artifact = artifactResult.getOrNull()
            if (artifact == null) {
                addLog("warning", "未找到 APK 产物")
                updateStep("download", StepStatus.FAILED, "未找到产物")
                return@launch
            }

            val totalMb = "%.1f".format(artifact.sizeInBytes / 1024.0 / 1024.0)
            addLog("info", "找到产物: ${artifact.name} (${totalMb} MB)，开始下载...")
            updateStep("download", StepStatus.RUNNING, "正在下载... 0.0 MB / ${totalMb} MB")

            val context = getApplication<Application>().applicationContext
            val downloadResult = cloudBuildRepository.downloadApkToLocal(
                context, token, artifact.id, runId,
                onDownloadProgress = { progress ->
                    _downloadProgress.value = progress
                    val percent = (progress.percent * 100).toInt()
                    val text = "${progress.displayBytes} (${percent}%)"
                    updateStep("download", StepStatus.RUNNING, "下载中 $text")
                    _buildState.value = BuildState.Downloading(runId, progress.percent, text)
                }
            )

            if (downloadResult.isSuccess) {
                val (localUri, fileSize) = downloadResult.getOrThrow()
                val sizeStr = when {
                    fileSize >= 1024 * 1024 -> "%.1f MB".format(fileSize / (1024.0 * 1024.0))
                    fileSize >= 1024 -> "%.1f KB".format(fileSize / 1024.0)
                    else -> "$fileSize B"
                }
                _downloadProgress.value = null
                updateStep("download", StepStatus.SUCCESS, "已下载 $sizeStr")
                addLog("info", "✅ APK 下载完成 ($sizeStr)")
                _buildState.value = BuildState.Success(runId, localUri)
            } else {
                val error = downloadResult.exceptionOrNull()?.message?.take(200) ?: "下载失败"
                updateStep("download", StepStatus.FAILED, error)
                addLog("error", "下载 APK 失败: $error")
                _buildState.value = BuildState.Failed(runId = runId, error = "下载失败: $error")
            }
        }
    }

    fun reset() {
        _buildState.value = BuildState.Idle
        _buildSteps.value = emptyList()
        _downloadProgress.value = null
        _ciJobs.value = emptyList()
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
}

data class BuildLog(
    val timestamp: Long,
    val level: String,
    val message: String
)
