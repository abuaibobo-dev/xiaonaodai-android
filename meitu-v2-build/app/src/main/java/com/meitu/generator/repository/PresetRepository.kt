package com.meitu.generator.repository

import com.meitu.generator.data.local.dao.PresetDao
import com.meitu.generator.data.local.entity.PresetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val presetDao: PresetDao
) {
    fun getAllPresets(): Flow<List<PresetEntity>> = presetDao.getAllPresets()
    fun getActivePreset(): Flow<PresetEntity?> = presetDao.getActivePreset()
    suspend fun getActivePresetSync(): PresetEntity? = presetDao.getActivePresetSync()
    suspend fun getPresetCount(): Int = presetDao.getPresetCount()
    suspend fun getById(id: Long): PresetEntity? = presetDao.getById(id)

    suspend fun savePreset(preset: PresetEntity): Long {
        presetDao.deactivateAll()
        val id = presetDao.insert(preset.copy(isActive = true, updatedAt = System.currentTimeMillis()))
        return id
    }

    suspend fun activatePreset(id: Long) {
        presetDao.deactivateAll()
        presetDao.activate(id)
    }

    suspend fun deletePreset(preset: PresetEntity) {
        presetDao.delete(preset)
    }

    suspend fun updatePreset(preset: PresetEntity) {
        presetDao.update(preset.copy(updatedAt = System.currentTimeMillis()))
    }
}
