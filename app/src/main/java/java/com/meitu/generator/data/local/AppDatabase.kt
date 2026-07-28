package com.meitu.generator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meitu.generator.data.local.dao.*
import com.meitu.generator.data.local.entity.*

@Database(
    entities = [
        SettingEntity::class,
        LogEntity::class,
        TaskEntity::class,
        MemoryEntity::class,
        PlanEntity::class,
        // EverOS 语义记忆系统
        EverMemoryEntity::class,
        EverMemorySceneEntity::class,
        UserProfileEntity::class,
        // 聊天消息持久化
        ChatMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun logDao(): LogDao
    abstract fun taskDao(): TaskDao
    abstract fun memoryDao(): MemoryDao
    abstract fun planDao(): PlanDao
    abstract fun everMemoryDao(): EverMemoryDao
    abstract fun chatMessageDao(): ChatMessageDao
}
