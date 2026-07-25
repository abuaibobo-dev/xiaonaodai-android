package com.meitu.generator.ui.assistant

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.entity.PresetEntity
import com.meitu.generator.repository.GenerationRepository
import com.meitu.generator.repository.PresetRepository
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TagCategory(
    val name: String,
    val options: List<String>,
    val maxSelect: Int,
    val selected: List<String> = emptyList()
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    application: Application,
    private val presetRepo: PresetRepository,
    private val settingsRepo: SettingsRepository,
    private val genRepo: GenerationRepository
) : AndroidViewModel(application) {

    private val _referenceImageUri = MutableStateFlow<Uri?>(null)
    val referenceImageUri: StateFlow<Uri?> = _referenceImageUri.asStateFlow()

    private val _referenceFileName = MutableStateFlow("")
    val referenceFileName: StateFlow<String> = _referenceFileName.asStateFlow()

    private val _referenceFileSize = MutableStateFlow("")
    val referenceFileSize: StateFlow<String> = _referenceFileSize.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _isPromptEditing = MutableStateFlow(false)
    val isPromptEditing: StateFlow<Boolean> = _isPromptEditing.asStateFlow()

    private val _isReversing = MutableStateFlow(false)
    val isReversing: StateFlow<Boolean> = _isReversing.asStateFlow()

    private val _reverseError = MutableStateFlow<String?>(null)
    val reverseError: StateFlow<String?> = _reverseError.asStateFlow()

    private val _ratio = MutableStateFlow("1:1")
    val ratio: StateFlow<String> = _ratio.asStateFlow()

    private val _quality = MutableStateFlow("SD")
    val quality: StateFlow<String> = _quality.asStateFlow()

    private val _model = MutableStateFlow("真实写实")
    val model: StateFlow<String> = _model.asStateFlow()

    private val _suggestedTags = MutableStateFlow<List<String>>(emptyList())
    val suggestedTags: StateFlow<List<String>> = _suggestedTags.asStateFlow()

    val allPresets: StateFlow<List<PresetEntity>> = presetRepo.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _tagCategories = MutableStateFlow(listOf(
        TagCategory("主体类型", listOf("单人写真", "双人互动", "多人场景"), 1),
        TagCategory("场景环境", listOf("室内", "户外自然", "城市街道", "海边沙滩", "studio棚拍"), 2),
        TagCategory("着装风格", listOf("丝袜美腿", "职业OL", "JK制服", "泳装比基尼", "健身瑜伽", "晚礼服", "日常休闲", "古风汉服"), 2),
        TagCategory("画面氛围", listOf("性感时尚", "清纯甜美", "酷飒冷艳", "温柔知性", "活泼可爱", "神秘暗黑"), 2),
        TagCategory("光影色调", listOf("自然光", "暖色调", "冷色调", "高对比", "柔光", "电影质感"), 1),
        TagCategory("构图视角", listOf("全身", "半身", "特写", "俯拍", "仰拍"), 1)
    ))
    val tagCategories: StateFlow<List<TagCategory>> = _tagCategories.asStateFlow()

    private val _showPresetDialog = MutableStateFlow(false)
    val showPresetDialog: StateFlow<Boolean> = _showPresetDialog.asStateFlow()

    private val _presetName = MutableStateFlow("")
    val presetName: StateFlow<String> = _presetName.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultModel = settingsRepo.getString(Constants.KEY_DEFAULT_MODEL, "真实写实")
            val defaultQuality = settingsRepo.getString(Constants.KEY_DEFAULT_QUALITY, "SD")
            _model.value = defaultModel
            _quality.value = defaultQuality
        }
    }

    fun onReferenceImageSelected(uri: Uri) {
        val context = getApplication<Application>()
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: return
        if (bytes.size > 10 * 1024 * 1024) {
            _reverseError.value = "图片超过10MB，请压缩后重试"
            return
        }

        // Save to local
        val dir = context.getExternalFilesDir("reference")
        if (dir != null && !dir.exists()) dir.mkdirs()
        val fileName = "ref_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)
        file.writeBytes(bytes)

        _referenceImageUri.value = uri
        _referenceFileName.value = fileName
        _referenceFileSize.value = "${bytes.size / 1024}KB"
        _reverseError.value = null
        _prompt.value = ""
        _suggestedTags.value = emptyList()
    }

    fun startReversePrompt() {
        val uri = _referenceImageUri.value ?: return
        val context = getApplication<Application>()
        _isReversing.value = true
        _reverseError.value = null

        viewModelScope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            if (bytes == null) {
                _reverseError.value = "无法读取图片"
                _isReversing.value = false
                return@launch
            }

            val result = genRepo.reversePrompt(bytes)
            result.fold(
                onSuccess = { text ->
                    _prompt.value = text
                    // Try to extract and map tags
                    val extractedTags = extractTags(text)
                    val chineseTags = genRepo.mapEnglishTagsToChinese(extractedTags)
                    _suggestedTags.value = chineseTags
                    autoSelectTags(chineseTags)
                    genRepo.addLog("success", "反推提示词成功")
                },
                onFailure = { error ->
                    _reverseError.value = error.message
                    genRepo.addLog("error", "反推失败: ${error.message}")
                }
            )
            _isReversing.value = false
        }
    }

    private fun extractTags(text: String): List<String> {
        val words = text.lowercase().split(Regex("[,;.\\s]+"))
        return words.filter { it.length > 2 }.take(20)
    }

    private fun autoSelectTags(chineseTags: List<String>) {
        val categories = _tagCategories.value.toMutableList()
        for (i in categories.indices) {
            val matched = categories[i].options.filter { it in chineseTags }
            if (matched.isNotEmpty()) {
                val limited = matched.take(categories[i].maxSelect)
                categories[i] = categories[i].copy(selected = limited)
            }
        }
        _tagCategories.value = categories
        appendTagsToPrompt()
    }

    fun toggleTag(categoryIndex: Int, tag: String) {
        val categories = _tagCategories.value.toMutableList()
        val cat = categories[categoryIndex]
        val newSelected = if (tag in cat.selected) {
            cat.selected - tag
        } else {
            if (cat.selected.size >= cat.maxSelect) {
                cat.selected.dropLast(1) + tag
            } else {
                cat.selected + tag
            }
        }
        categories[categoryIndex] = cat.copy(selected = newSelected)
        _tagCategories.value = categories
        appendTagsToPrompt()
    }

    private fun appendTagsToPrompt() {
        val basePrompt = _prompt.value.split(", style:").first().trim()
        val allTags = _tagCategories.value.flatMap { it.selected }
        if (allTags.isNotEmpty()) {
            _prompt.value = "$basePrompt, style: ${allTags.joinToString(", ")}"
        } else {
            _prompt.value = basePrompt
        }
    }

    fun setRatio(r: String) { _ratio.value = r }
    fun setQuality(q: String) { _quality.value = q }
    fun setModel(m: String) { _model.value = m }
    fun setPromptEditing(editing: Boolean) { _isPromptEditing.value = editing }
    fun updatePrompt(text: String) { _prompt.value = text }

    fun showSavePresetDialog() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _presetName.value = "参考图_${_tagCategories.value[1].selected.firstOrNull() ?: "默认"}_${date}"
        _showPresetDialog.value = true
    }

    fun dismissPresetDialog() { _showPresetDialog.value = false }
    fun setPresetName(name: String) { _presetName.value = name }

    fun savePreset() {
        viewModelScope.launch {
            val tags = _tagCategories.value.flatMap { it.selected }
            val preset = PresetEntity(
                name = _presetName.value,
                prompt = _prompt.value,
                tags = com.google.gson.Gson().toJson(tags),
                ratio = _ratio.value,
                model = _model.value,
                quality = _quality.value,
                referenceImagePath = _referenceImageUri.value?.toString() ?: ""
            )
            presetRepo.savePreset(preset)
            genRepo.addLog("success", "预设已保存: ${preset.name}")
            _showPresetDialog.value = false
        }
    }

    fun activatePreset(id: Long) {
        viewModelScope.launch {
            presetRepo.activatePreset(id)
            val preset = presetRepo.getById(id)
            if (preset != null) {
                _prompt.value = preset.prompt
                _ratio.value = preset.ratio
                _model.value = preset.model
                _quality.value = preset.quality
                val tags = try {
                    com.google.gson.Gson().fromJson(preset.tags, Array<String>::class.java)?.toList() ?: emptyList()
                } catch (e: Exception) { emptyList() }
                _suggestedTags.value = tags
            }
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch {
            presetRepo.deletePreset(preset)
        }
    }

    fun clearReference() {
        _referenceImageUri.value = null
        _referenceFileName.value = ""
        _referenceFileSize.value = ""
        _prompt.value = ""
        _suggestedTags.value = emptyList()
        _reverseError.value = null
    }
}
