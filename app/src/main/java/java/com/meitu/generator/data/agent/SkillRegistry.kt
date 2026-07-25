package com.meitu.generator.data.agent

import com.meitu.generator.data.model.SkillDefinition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 技能注册表 - v3.0 精简版
 * 只保留与 cloud_build 相关的技能
 */
@Singleton
class SkillRegistry @Inject constructor() {
    private val skills = mutableMapOf<String, SkillDefinition>()
    
    fun register(skill: SkillDefinition) {
        skills[skill.id] = skill
    }
    
    fun getAll(): List<SkillDefinition> = skills.values.toList()
    
    fun getEnabled(): List<SkillDefinition> = skills.values.filter { it.enabled }
    
    fun get(id: String): SkillDefinition? = skills[id]
    
    fun setEnabled(id: String, enabled: Boolean) {
        skills[id]?.let { existing ->
            skills[id] = existing.copy(enabled = enabled)
        }
    }
    
    fun getToolNamesForSkill(id: String): List<String> {
        return skills[id]?.tools ?: emptyList()
    }
    
    fun getEnabledToolNames(): Set<String> {
        return skills.values
            .filter { it.enabled }
            .flatMap { it.tools }
            .toSet()
    }
    
    fun loadDefaults() {
        register(SkillDefinition(
            id = "cloud_build",
            name = "云端编译",
            description = "推送代码到GitHub并通过Actions编译生成APK",
            version = "3.0.0",
            enabled = true,
            tools = listOf("cloud_build")
        ))
    }
}
