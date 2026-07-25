package com.meitu.generator.data.agent

import com.meitu.generator.data.local.dao.MemoryDao
import com.meitu.generator.data.local.entity.MemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent 记忆系统 - 用 Room 存储：用户偏好、历史操作、收藏的提示词
 * 每次对话前注入相关记忆到 system prompt
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
    }
}
