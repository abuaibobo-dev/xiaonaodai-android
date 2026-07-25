package com.meitu.generator.repository

import android.util.Base64
import com.meitu.generator.data.local.dao.LogDao
import com.meitu.generator.data.local.dao.TaskDao
import com.meitu.generator.data.local.entity.ImageEntity
import com.meitu.generator.data.local.entity.LogEntity
import com.meitu.generator.data.local.entity.TaskEntity
import com.meitu.generator.data.remote.AgnesService
import com.meitu.generator.data.remote.GeminiService
import com.meitu.generator.data.remote.ImgBBService
import com.meitu.generator.data.remote.dto.*
import com.meitu.generator.util.Constants
import com.meitu.generator.util.ResolutionHelper
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerationRepository @Inject constructor(
    private val agnesService: AgnesService,
    private val geminiService: GeminiService,
    private val imgBBService: ImgBBService,
    private val imageRepository: ImageRepository,
    private val settingsRepository: SettingsRepository,
    private val logDao: LogDao,
    private val taskDao: TaskDao
) {
    // ============ Gemini Reverse Prompt ============
    suspend fun reversePrompt(imageBytes: ByteArray, mimeType: String = "image/jpeg"): Result<String> {
        return try {
            val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val request = GeminiRequest(
                contents = listOf(GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "Analyze this reference photo in detail and generate a comprehensive English prompt for AI image generation. Focus on: subject description, pose, clothing, environment, lighting, camera angle, style, mood. Return ONLY the prompt text, no explanations."),
                        GeminiPart(inlineData = GeminiInlineData(mime_type = mimeType, data = b64))
                    )
                ))
            )
            val response = geminiService.generateContent(Constants.GEMINI_API_KEY, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrEmpty()) {
                Result.failure(Exception("反推返回空结果，请换一张更清晰的图片"))
            } else {
                Result.success(text.trim())
            }
        } catch (e: Exception) {
            Result.failure(Exception("反推失败: ${e.message}"))
        }
    }

    // ============ English Tag to Chinese Mapping ============
    fun mapEnglishTagsToChinese(englishTags: List<String>): List<String> {
        val mapping = mapOf(
            "stockings" to "丝袜美腿", "pantyhose" to "丝袜美腿",
            "office" to "职业OL", "business" to "职业OL", "ol" to "职业OL",
            "school_uniform" to "JK制服", "japanese_school" to "JK制服",
            "swimsuit" to "泳装比基尼", "bikini" to "泳装比基尼", "beach" to "泳装比基尼",
            "yoga" to "健身瑜伽", "fitness" to "健身瑜伽", "sportswear" to "健身瑜伽",
            "evening_gown" to "晚礼服", "formal_dress" to "晚礼服",
            "casual" to "日常休闲", "everyday" to "日常休闲",
            "hanfu" to "古风汉服", "chinese_traditional" to "古风汉服",
            "sexy" to "性感时尚", "fashionable" to "性感时尚", "glamorous" to "性感时尚",
            "cute" to "清纯甜美", "sweet" to "清纯甜美", "innocent" to "清纯甜美",
            "cool" to "酷飒冷艳", "elegant" to "温柔知性", "gentle" to "温柔知性",
            "cheerful" to "活泼可爱", "lively" to "活泼可爱",
            "mysterious" to "神秘暗黑", "dark" to "神秘暗黑", "gothic" to "神秘暗黑",
            "natural_light" to "自然光", "sunlight" to "自然光",
            "warm" to "暖色调", "golden_hour" to "暖色调",
            "cold" to "冷色调", "cool_tone" to "冷色调",
            "high_contrast" to "高对比", "dramatic" to "高对比",
            "soft_light" to "柔光", "diffused" to "柔光",
            "cinematic" to "电影质感", "movie" to "电影质感",
            "indoor" to "室内", "studio" to "studio棚拍",
            "outdoor" to "户外自然", "nature" to "户外自然",
            "street" to "城市街道", "urban" to "城市街道",
            "beach" to "海边沙滩", "seaside" to "海边沙滩",
            "full_body" to "全身", "half_body" to "半身",
            "close_up" to "特写", "portrait" to "半身",
            "top_down" to "俯拍", "aerial" to "俯拍",
            "low_angle" to "仰拍", "worm_eye" to "仰拍",
            "solo" to "单人写真", "pair" to "双人互动", "group" to "多人场景"
        )
        return englishTags.mapNotNull { tag ->
            val key = tag.lowercase().trim()
            mapping[key]
        }.distinct()
    }

    // ============ Image Generation ============
    suspend fun generateImage(
        prompt: String,
        negativePrompt: String,
        ratio: String,
        quality: String,
        model: String
    ): Result<String> {
        return try {
            val size = ResolutionHelper.toSizeString(ratio, quality)
            val modelStr = when (model) {
                "真实写实" -> "realistic"
                "艺术风格" -> "artistic"
                "动漫二次元" -> "anime"
                "电影质感" -> "cinematic"
                "性感时尚" -> "glamorous"
                else -> "realistic"
            }
            val fullPrompt = "$prompt, $modelStr style, high quality, detailed"
            val request = AgnesImageRequest(
                model = Constants.AGNES_MODEL,
                prompt = fullPrompt,
                n = 1,
                size = size,
                negativePrompt = negativePrompt
            )
            val response = agnesService.generateImage("Bearer ${Constants.AGNES_API_KEY}", request)
            if (response.error != null) {
                Result.failure(Exception(response.error.message ?: "Agnes服务暂时不可用，请稍后重试"))
            } else {
                val imageData = response.data?.firstOrNull()
                if (imageData?.url != null) {
                    Result.success(imageData.url)
                } else if (imageData?.b64Json != null) {
                    Result.success("data:image/png;base64,${imageData.b64Json}")
                } else {
                    Result.failure(Exception("生成失败，返回数据为空"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("生成失败: ${e.message}"))
        }
    }

    // ============ ImgBB Upload ============
    suspend fun uploadToImgBB(imageBytes: ByteArray, name: String, apiKey: String): Result<Pair<String, String>> {
        return try {
            val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val imagePart = MultipartBody.Part.createFormData(
                "image", b64, b64.toRequestBody("text/plain".toMediaTypeOrNull())
            )
            val namePart = MultipartBody.Part.createFormData("name", name)
            val response = imgBBService.uploadImage(apiKey, imagePart, namePart)
            if (response.success && response.data != null) {
                Result.success(Pair(response.data.url ?: "", response.data.deleteUrl ?: ""))
            } else {
                Result.failure(Exception(response.error?.message ?: "上传失败"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("上传失败: ${e.message}"))
        }
    }

    // ============ Logging ============
    suspend fun addLog(level: String, message: String, relatedImageId: Long = 0) {
        logDao.insert(LogEntity(level = level, message = message, relatedImageId = relatedImageId))
        logDao.trimLogs()
    }

    fun getRecentLogs(): Flow<List<LogEntity>> = logDao.getRecentLogs()

    // ============ Tasks ============
    suspend fun createTask(presetId: Long, presetName: String, targetCount: Int): Long {
        return taskDao.insert(TaskEntity(
            presetId = presetId,
            presetName = presetName,
            targetCount = targetCount,
            status = 0
        ))
    }

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)
    suspend fun getRunningTask(): TaskEntity? = taskDao.getRunningTask()
    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getById(id)
    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
}
