package com.meitu.generator.ui.home

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.entity.LogEntity
import com.meitu.generator.data.local.entity.PresetEntity
import com.meitu.generator.data.local.entity.TaskEntity
import com.meitu.generator.data.local.entity.ImageEntity
import com.meitu.generator.repository.GenerationRepository
import com.meitu.generator.repository.ImageRepository
import com.meitu.generator.repository.PresetRepository
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HomeStats(
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val monthCount: Int = 0,
    val successRate: Int = 0,
    val todayFailed: Int = 0,
    val avgTime: String = "0s",
    val cloudCount: Int = 0,
    val favoriteCount: Int = 0,
    val taskProgress: String = "",
    val lastTaskTime: String = "-"
)

data class DailyCount(val date: String, val count: Int)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val presetRepo: PresetRepository,
    private val imageRepo: ImageRepository,
    private val settingsRepo: SettingsRepository,
    private val genRepo: GenerationRepository
) : AndroidViewModel(application) {

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    val activePreset: StateFlow<PresetEntity?> = presetRepo.getActivePreset()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val logs: StateFlow<List<LogEntity>> = genRepo.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = genRepo.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _taskProgress = MutableStateFlow(Pair(0, 0))
    val taskProgress: StateFlow<Pair<Int, Int>> = _taskProgress.asStateFlow()

    private val _completionMessage = MutableStateFlow<String?>(null)
    val completionMessage: StateFlow<String?> = _completionMessage.asStateFlow()

    private val _dailyCounts = MutableStateFlow<List<DailyCount>>(emptyList())
    val dailyCounts: StateFlow<List<DailyCount>> = _dailyCounts.asStateFlow()

    private var generationJob: Job? = null
    private var currentTaskId: Long = 0

    init {
        viewModelScope.launch {
            settingsRepo.initDefaults()
            loadStats()
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = cal.timeInMillis

            // Use separate flows collected into stats
            launch {
                imageRepo.getTotalCount().collect { total ->
                    _stats.value = _stats.value.copy(totalCount = total)
                }
            }
            launch {
                imageRepo.getTodayCount(startOfDay).collect { today ->
                    _stats.value = _stats.value.copy(todayCount = today)
                }
            }
            launch {
                imageRepo.getMonthCount(startOfMonth).collect { month ->
                    _stats.value = _stats.value.copy(monthCount = month)
                }
            }
            launch {
                imageRepo.getSuccessCount().collect { success ->
                    val total = _stats.value.totalCount
                    val rate = if (total > 0) (success * 100) / total else 0
                    _stats.value = _stats.value.copy(successRate = rate)
                }
            }
            launch {
                imageRepo.getTodayFailedCount(startOfDay).collect { failed ->
                    _stats.value = _stats.value.copy(todayFailed = failed)
                }
            }
            launch {
                imageRepo.getCloudBackupCount().collect { cloud ->
                    _stats.value = _stats.value.copy(cloudCount = cloud)
                }
            }
            launch {
                imageRepo.getFavoriteCount().collect { fav ->
                    _stats.value = _stats.value.copy(favoriteCount = fav)
                }
            }
        }

        // Load daily counts
        viewModelScope.launch {
            val counts = mutableListOf<DailyCount>()
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dateStr = sdf.format(Date(cal.timeInMillis))
                counts.add(DailyCount(dateStr, 0))
            }
            _dailyCounts.value = counts
        }
    }

    fun startGeneration() {
        if (_isRunning.value) return
        viewModelScope.launch {
            val preset = presetRepo.getActivePresetSync() ?: return@launch
            _isRunning.value = true

            val interval = settingsRepo.getInt(Constants.KEY_SUBMIT_INTERVAL, 4)
            val quality = settingsRepo.getString(Constants.KEY_DEFAULT_QUALITY, "SD")
            val autoUpload = settingsRepo.getBoolean(Constants.KEY_IMGBB_AUTO_UPLOAD, true)
            val imgbbKey = settingsRepo.getImgBBKey()

            val targetCount = 40
            currentTaskId = genRepo.createTask(preset.id, preset.name, targetCount)
            _taskProgress.value = Pair(0, targetCount)

            genRepo.addLog("info", "开始全自动生成任务: ${preset.name}, 目标${targetCount}张")

            var successCount = 0
            var failedCount = 0

            generationJob = viewModelScope.launch {
                for (i in 1..targetCount) {
                    val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val batterySafe = settingsRepo.getBoolean(Constants.KEY_BATTERY_SAFE_MODE, true)
                    if (batterySafe && level < 15) {
                        genRepo.addLog("warning", "电量低于15%，自动暂停任务")
                        break
                    }

                    genRepo.addLog("info", "提交第${i}/${targetCount}张生成请求...")
                    _taskProgress.value = Pair(i - 1, targetCount)

                    val result = genRepo.generateImage(
                        prompt = preset.prompt,
                        negativePrompt = preset.negativePrompt,
                        ratio = preset.ratio,
                        quality = quality,
                        model = preset.model
                    )

                    result.fold(
                        onSuccess = { imageUrl ->
                            val path = imageRepo.generateImagePath()
                            val saved = downloadImage(imageUrl, path)
                            val imageId = imageRepo.insert(ImageEntity(
                                presetId = preset.id,
                                prompt = preset.prompt,
                                model = preset.model,
                                ratio = preset.ratio,
                                quality = quality,
                                localPath = if (saved) path else "",
                                status = if (saved) 1 else 2,
                                generatedAt = System.currentTimeMillis()
                            ))

                            if (saved) {
                                successCount++
                                genRepo.addLog("success", "第${i}张生成成功", imageId)

                                if (autoUpload && imgbbKey.isNotEmpty()) {
                                    try {
                                        val bytes = File(path).readBytes()
                                        genRepo.uploadToImgBB(bytes, "meitu_${imageId}.jpg", imgbbKey).fold(
                                            onSuccess = { (url, deleteUrl) ->
                                                val img = imageRepo.getById(imageId)
                                                if (img != null) {
                                                    imageRepo.update(img.copy(
                                                        imgbbUrl = url,
                                                        imgbbDeleteUrl = deleteUrl,
                                                        uploadedAt = System.currentTimeMillis()
                                                    ))
                                                }
                                                genRepo.addLog("upload", "第${i}张已上传云端", imageId)
                                            },
                                            onFailure = {
                                                genRepo.addLog("warning", "第${i}张上传云端失败", imageId)
                                            }
                                        )
                                    } catch (e: Exception) {
                                        genRepo.addLog("warning", "上传异常: ${e.message}")
                                    }
                                }
                            } else {
                                failedCount++
                                genRepo.addLog("error", "第${i}张保存失败", imageId)
                            }
                        },
                        onFailure = { error ->
                            failedCount++
                            genRepo.addLog("error", "第${i}张生成失败: ${error.message}")
                            genRepo.addLog("warning", "等待2秒后重试...")
                            delay(2000)
                            val retryResult = genRepo.generateImage(
                                preset.prompt, preset.negativePrompt, preset.ratio, quality, preset.model
                            )
                            retryResult.fold(
                                onSuccess = { imageUrl ->
                                    val path = imageRepo.generateImagePath()
                                    if (downloadImage(imageUrl, path)) {
                                        imageRepo.insert(ImageEntity(
                                            presetId = preset.id, prompt = preset.prompt, model = preset.model,
                                            ratio = preset.ratio, quality = quality, localPath = path,
                                            status = 1, generatedAt = System.currentTimeMillis()
                                        ))
                                        successCount++
                                        failedCount--
                                        genRepo.addLog("success", "第${i}张重试成功")
                                    }
                                },
                                onFailure = { genRepo.addLog("error", "第${i}张重试仍失败") }
                            )
                        }
                    )

                    _taskProgress.value = Pair(i, targetCount)
                    if (i < targetCount) delay(interval * 1000L)
                }

                val task = genRepo.getTaskById(currentTaskId)
                if (task != null) {
                    genRepo.updateTask(task.copy(
                        successCount = successCount,
                        failedCount = failedCount,
                        status = 1,
                        finishedAt = System.currentTimeMillis(),
                        durationSeconds = (System.currentTimeMillis() - task.startedAt) / 1000
                    ))
                }

                genRepo.addLog("success", "全部完成！共${targetCount}张，成功${successCount}张，失败${failedCount}张")
                _completionMessage.value = "全部完成！共生成${targetCount}张，成功${successCount}张，失败${failedCount}张"
                _isRunning.value = false

                delay(5000)
                _completionMessage.value = null
            }
        }
    }

    private suspend fun downloadImage(url: String, path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (url.startsWith("data:image")) {
                val b64 = url.substringAfter("base64,")
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                FileOutputStream(path).use { it.write(bytes) }
                true
            } else {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.bytes()?.let { bytes ->
                            FileOutputStream(path).use { it.write(bytes) }
                            true
                        } ?: false
                    } else false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isRunning.value = false
        viewModelScope.launch {
            val task = genRepo.getTaskById(currentTaskId)
            if (task != null) {
                genRepo.updateTask(task.copy(status = 2, finishedAt = System.currentTimeMillis()))
            }
            genRepo.addLog("warning", "任务已手动停止")
        }
    }

    fun clearCompletionMessage() { _completionMessage.value = null }
}
