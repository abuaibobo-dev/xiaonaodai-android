package com.meitu.generator.data.agent

import com.meitu.generator.data.local.dao.MemoryDao
import com.meitu.generator.data.local.entity.MemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent 记忆系统 v3 - 增强版
 * 
 * 功能：
 * 1. 基础记忆存储/检索（用户偏好、历史操作、收藏的提示词）
 * 2. 纠错学习 - 记录用户纠正的错误，避免重复犯错
 * 3. 对话摘要压缩 - 自动压缩长对话为摘要，节省上下文
 * 4. 点赞/踩反馈 - 记录用户对回复的满意度，优化回复质量
 * 5. 每次对话前注入相关记忆到 system prompt
 */
@Singleton
class AgentMemory @Inject constructor(
    private val memoryDao: MemoryDao
) {
    /** 保存一条记忆 */
    suspend fun save(key: String, value: String, category: String = "context") {
        val existing = memoryDao.getByKey(key)
        if (existing != null) {
            memoryDao.update(existing.copy(value = value, updatedAt = System.currentTimeMillis()))
        } else {
            memoryDao.insert(MemoryEntity(key = key, value = value, category = category))
        }
    }
    
    /** 获取一条记忆 */
    suspend fun get(key: String): String? {
        return memoryDao.getByKey(key)?.value
    }
    
    /** 获取某个分类下所有记忆 */
    suspend fun getByCategory(category: String): List<MemoryEntity> {
        return memoryDao.getByCategory(category)
    }
    
    /** 构建注入到 system prompt 的记忆摘要 */
    suspend fun buildMemoryPrompt(): String {
        val prefs = memoryDao.getByCategory("preference")
        val favs = memoryDao.getByCategory("favorite_prompt")
        val recent = memoryDao.getByCategory("history").take(5)
        val corrections = memoryDao.getByCategory("correction").take(5)
        val feedbackSummary = getFeedbackSummary()
        
        val sb = StringBuilder()
        if (prefs.isNotEmpty()) {
            sb.appendLine("[用户偏好]")
            prefs.forEach { sb.appendLine("- ${it.key}: ${it.value}") }
        }
        if (favs.isNotEmpty()) {
            sb.appendLine("[收藏提示词]")
            favs.take(3).forEach { sb.appendLine("- ${it.value}") }
        }
        if (recent.isNotEmpty()) {
            sb.appendLine("[近期操作]")
            recent.forEach { sb.appendLine("- ${it.value}") }
        }
        if (corrections.isNotEmpty()) {
            sb.appendLine("[纠错记录 - 避免重复犯错]")
            corrections.forEach { sb.appendLine("- ${it.value}") }
        }
        if (feedbackSummary.isNotBlank()) {
            sb.appendLine("[反馈统计]")
            sb.appendLine(feedbackSummary)
        }
        return sb.toString().ifBlank { "暂无用户记忆" }
    }
    
    /** 记录一次操作到历史 */
    suspend fun recordAction(action: String) {
        val key = "action_${System.currentTimeMillis()}"
        memoryDao.insert(MemoryEntity(
            key = key,
            value = action,
            category = "history",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
    }
    
    /** 清理旧历史（保留最近100条） */
    suspend fun cleanup() {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L // 7天前
        memoryDao.deleteOldByCategory("history", cutoff)
        // 也清理旧的反馈记录
        val feedbackCutoff = System.currentTimeMillis() - 30 * 24 * 3600 * 1000L // 30天前
        memoryDao.deleteOldByCategory("feedback", feedbackCutoff)
    }

    // ============ v3 新增功能 ============

    /**
     * 纠错学习 - 记录用户纠正的错误
     * @param originalResponse 原始错误回复
     * @param correction 用户纠正的内容
     * @param context 纠正发生的上下文
     */
    suspend fun recordCorrection(originalResponse: String, correction: String, context: String = "") {
        val key = "correction_${System.currentTimeMillis()}"
        val value = buildString {
            appendLine("上下文: ${context.take(100)}")
            appendLine("错误: ${originalResponse.take(200)}")
            appendLine("正确: ${correction.take(200)}")
        }
        memoryDao.insert(MemoryEntity(
            key = key,
            value = value,
            category = "correction",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))

        // 同时更新纠错摘要（用于 system prompt 注入）
        updateCorrectionSummary()
    }

    /**
     * 更新纠错摘要 - 保持摘要简洁
     */
    private suspend fun updateCorrectionSummary() {
        val corrections = memoryDao.getByCategory("correction").takeLast(10)
        if (corrections.isEmpty()) return

        val summary = buildString {
            appendLine("最近纠正的问题：")
            corrections.takeLast(5).forEachIndexed { index, c ->
                val correctLine = c.value.lines().find { it.startsWith("正确:") }
                appendLine("${index + 1}. ${correctLine?.substringAfter("正确:")?.take(80) ?: c.value.take(80)}")
            }
        }
        save("correction_summary", summary, "correction")
    }

    /**
     * 对话摘要压缩 - 将长对话压缩为摘要
     * @param conversationHistory 完整对话历史
     * @param sessionId 会话ID
     */
    suspend fun compressConversation(conversationHistory: String, sessionId: String) {
        // 简单的摘要压缩：提取关键信息点
        val lines = conversationHistory.lines()
        val summary = buildString {
            appendLine("会话 $sessionId 摘要:")
            appendLine("- 总轮数: ${lines.count { it.isNotBlank() } / 2}")
            
            // 提取关键动作
            val actionKeywords = listOf("完成", "创建", "修改", "删除", "修复", "生成", "编译", "部署")
            val keyActions = lines.filter { line -> actionKeywords.any { line.contains(it) } }
            if (keyActions.isNotEmpty()) {
                appendLine("- 关键操作:")
                keyActions.take(10).forEach { appendLine("  - ${it.take(80)}") }
            }
            
            // 提取决策
            val decisionKeywords = listOf("决定", "选择", "确认", "同意", "采用")
            val decisions = lines.filter { line -> decisionKeywords.any { line.contains(it) } }
            if (decisions.isNotEmpty()) {
                appendLine("- 关键决策:")
                decisions.take(5).forEach { appendLine("  - ${it.take(80)}") }
            }
        }

        val key = "session_summary_$sessionId"
        save(key, summary, "conversation_summary")
    }

    /**
     * 记录用户反馈（点赞/踩）
     * @param messageId 消息ID
     * @param feedback "positive" 或 "negative"
     * @param messageContent 被反馈的消息内容（用于分析）
     */
    suspend fun recordFeedback(messageId: String, feedback: String, messageContent: String = "") {
        val key = "feedback_${System.currentTimeMillis()}_$messageId"
        val value = buildString {
            appendLine("feedback=$feedback")
            appendLine("message_id=$messageId")
            if (messageContent.isNotBlank()) {
                appendLine("content_preview=${messageContent.take(200)}")
            }
        }
        memoryDao.insert(MemoryEntity(
            key = key,
            value = value,
            category = "feedback",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))

        // 更新反馈统计
        updateFeedbackSummary()
    }

    /**
     * 更新反馈统计摘要
     */
    private suspend fun updateFeedbackSummary() {
        val feedbacks = memoryDao.getByCategory("feedback")
        val total = feedbacks.size
        if (total == 0) return

        val positive = feedbacks.count { it.value.contains("feedback=positive") }
        val negative = feedbacks.count { it.value.contains("feedback=negative") }
        val satisfactionRate = if (total > 0) (positive.toFloat() / total * 100).toInt() else 0

        val summary = buildString {
            appendLine("总反馈: $total")
            appendLine("正面: $positive (${satisfactionRate}%)")
            appendLine("负面: $negative (${100 - satisfactionRate}%)")
        }
        save("feedback_summary", summary, "feedback")
    }

    /**
     * 获取反馈统计摘要（用于 system prompt）
     */
    private suspend fun getFeedbackSummary(): String {
        val summary = get("feedback_summary")
        return summary ?: ""
    }

    /**
     * 获取最近的对话摘要
     */
    suspend fun getRecentConversationSummaries(maxCount: Int = 3): List<String> {
        return memoryDao.getByCategory("conversation_summary")
            .takeLast(maxCount)
            .map { it.value }
    }

    /**
     * 根据纠错记录获取应避免的错误模式
     */
    suspend fun getCorrectionPatterns(): List<String> {
        val corrections = memoryDao.getByCategory("correction").takeLast(10)
        return corrections.map { correction ->
            val errorLine = correction.value.lines().find { it.startsWith("错误:") }
            val correctLine = correction.value.lines().find { it.startsWith("正确:") }
            "避免: ${errorLine?.substringAfter("错误:")?.take(60) ?: ""} → 应该: ${correctLine?.substringAfter("正确:")?.take(60) ?: ""}"
        }
    }
}
