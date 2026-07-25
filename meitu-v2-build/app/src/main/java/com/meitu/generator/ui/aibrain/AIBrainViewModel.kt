package com.meitu.generator.ui.aibrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meitu.generator.data.local.entity.PresetEntity
import com.meitu.generator.repository.GenerationRepository
import com.meitu.generator.repository.ImageRepository
import com.meitu.generator.repository.PresetRepository
import com.meitu.generator.repository.SettingsRepository
import com.meitu.generator.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AIBrainViewModel @Inject constructor(
    private val presetRepo: PresetRepository,
    private val imageRepo: ImageRepository,
    private val settingsRepo: SettingsRepository,
    private val genRepo: GenerationRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "你好！我是AI大脑，你可以用自然语言告诉我你想做什么。",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    private val _triggerGeneration = MutableStateFlow<Int?>(null)
    val triggerGeneration: StateFlow<Int?> = _triggerGeneration.asStateFlow()

    val quickCommands = listOf(
        "帮我生成40张图",
        "今天生成了多少张",
        "显示所有预设",
        "切换到高清画质",
        "成功率是多少",
        "教我怎么用"
    )

    val activePreset: StateFlow<PresetEntity?> = presetRepo.getActivePreset()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setInput(text: String) { _inputText.value = text }

    fun sendQuickCommand(cmd: String) {
        _inputText.value = cmd
        processInput(cmd)
    }

    fun sendInput() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        processInput(text)
    }

    private fun processInput(input: String) {
        val userMsg = ChatMessage(text = input, isUser = true)
        _messages.value = (_messages.value + userMsg).takeLast(20)
        _inputText.value = ""
        viewModelScope.launch {
            val response = parseAndExecute(input)
            val aiMsg = ChatMessage(text = response, isUser = false)
            _messages.value = (_messages.value + aiMsg).takeLast(20)
        }
    }

    private suspend fun parseAndExecute(input: String): String {
        val lower = input.lowercase()

        if (matchesAny(lower, listOf("生成.*张", "批量生成", "开始生成", "全自动生成"))) {
            val count = extractNumber(lower) ?: 40
            val preset = presetRepo.getActivePresetSync()
            if (preset == null) {
                return "当前没有激活的预设，请先去AI助手配置并保存预设。"
            }
            _triggerGeneration.value = count
            return "已启动全自动生成，当前预设「${preset.name}」，目标${count}张。"
        }

        if (matchesAny(lower, listOf("切换.*预设", "切换到", "换.*预设"))) {
            val name = extractQuotedText(input) ?: extractAfterKeyword(input, listOf("切换到", "换成", "使用"))
            if (name != null) {
                val presets = presetRepo.getAllPresets().first()
                val match = presets.find { it.name.contains(name) }
                if (match != null) {
                    presetRepo.activatePreset(match.id)
                    return "已切换到预设「${match.name}」"
                }
                return "未找到名为「${name}」的预设"
            }
            val presets = presetRepo.getAllPresets().first()
            if (presets.isEmpty()) return "当前没有任何预设。"
            val names = presets.joinToString("、") { it.name + if (it.isActive) "(当前)" else "" }
            return "请指定预设名称，当前可用：$names"
        }

        if (matchesAny(lower, listOf("换.*模型", "模型.*换", "切换模型"))) {
            val models = listOf("真实写实", "艺术风格", "动漫二次元", "电影质感", "性感时尚")
            val match = models.find { it in input }
            if (match != null) {
                settingsRepo.setString(Constants.KEY_DEFAULT_MODEL, match)
                return "已切换模型为「${match}」"
            }
            return "请选择模型：真实写实、艺术风格、动漫二次元、电影质感、性感时尚"
        }

        if (matchesAny(lower, listOf("比例.*改", "改.*比例", "比例换成"))) {
            listOf("1:1", "3:4", "9:16", "16:9").forEach { r ->
                if (r in input) return "比例已切换为${r}（下次生成生效）"
            }
            return "请选择比例：1:1、3:4、9:16、16:9"
        }

        if (matchesAny(lower, listOf("画质.*换", "换.*画质", "切换画质", "画质调"))) {
            if ("高清" in input || "HD" in input.uppercase()) {
                settingsRepo.setString(Constants.KEY_DEFAULT_QUALITY, "HD")
                return "画质已切换为高清HD"
            }
            if ("标清" in input || "SD" in input.uppercase()) {
                settingsRepo.setString(Constants.KEY_DEFAULT_QUALITY, "SD")
                return "画质已切换为标清SD"
            }
            return "请选择画质：标清SD 或 高清HD"
        }

        if (matchesAny(lower, listOf("今天.*多少", "今日.*生成", "今天生成"))) {
            val todayCount = imageRepo.getTodayCount(getStartOfDay()).first()
            return "今天共生成了${todayCount}张图片。"
        }

        if (matchesAny(lower, listOf("本月.*多少", "这个月"))) {
            val monthCount = imageRepo.getMonthCount(getStartOfMonth()).first()
            return "本月共生成了${monthCount}张图片。"
        }

        if (matchesAny(lower, listOf("成功率"))) {
            val total = imageRepo.getTotalCount().first()
            val success = imageRepo.getSuccessCount().first()
            val rate = if (total > 0) (success * 100) / total else 0
            return "总成功率：${rate}%（${success}/${total}张）"
        }

        if (matchesAny(lower, listOf("进度", "到哪了", "还剩多少"))) {
            return "当前没有正在进行的任务。点击首页全自动生成按钮开始。"
        }

        if (matchesAny(lower, listOf("预设列表", "所有预设", "显示.*预设"))) {
            val presets = presetRepo.getAllPresets().first()
            if (presets.isEmpty()) return "当前没有任何预设。请去AI助手创建第一个预设。"
            val names = presets.joinToString("、") { it.name + if (it.isActive) "[激活]" else "" }
            return "预设列表：$names"
        }

        if (matchesAny(lower, listOf("删除.*预设"))) {
            val name = extractQuotedText(input) ?: extractAfterKeyword(input, listOf("删除"))
            if (name != null) {
                val presets = presetRepo.getAllPresets().first()
                val match = presets.find { it.name.contains(name) }
                if (match != null) {
                    if (presets.size <= 1) return "只剩最后一个预设，无法删除。"
                    presetRepo.deletePreset(match)
                    return "已删除预设「${match.name}」"
                }
            }
            return "请指定要删除的预设名称。"
        }

        if (matchesAny(lower, listOf("收藏.*图", "显示.*收藏"))) {
            val count = imageRepo.getFavoriteCount().first()
            return "当前共收藏了${count}张图片，可在图库-收藏标签中查看。"
        }

        if (matchesAny(lower, listOf("教.*怎么用", "怎么用", "帮助", "help"))) {
            return "使用指南：生成类说「帮我生成40张图」；配置类说「切换到XX预设」「换动漫模型」；查询类说「今天生多少张」「成功率多少」；管理类说「显示预设列表」。先去AI助手上传参考图并保存预设，然后告诉我开始生成即可！"
        }

        if (matchesAny(lower, listOf("推荐.*参数", "推荐"))) {
            return "推荐参数：比例9:16、画质高清HD、模型真实写实、间隔4秒。"
        }

        return "我没有理解你的意思。试试说「教我怎么用」查看使用指南。"
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { pattern ->
            try { Regex(pattern).containsMatchIn(text) }
            catch (e: Exception) { text.contains(pattern) }
        }
    }

    private fun extractNumber(text: String): Int? {
        val match = Regex("(\\d+)\\s*张").find(text)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractQuotedText(text: String): String? {
        val match = Regex("[\u300c\u201c](.+?)[\u300d\u201d]").find(text)
        return match?.groupValues?.get(1)
    }

    private fun extractAfterKeyword(text: String, keywords: List<String>): String? {
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx >= 0) {
                val after = text.substring(idx + kw.length).trim()
                if (after.isNotEmpty()) return after.take(20)
            }
        }
        return null
    }

    private fun getStartOfDay(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun clearNavigation() { _navigateTo.value = null }
    fun clearTrigger() { _triggerGeneration.value = null }
}
