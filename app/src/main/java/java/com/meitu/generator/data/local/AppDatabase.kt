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
        PlanEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun logDao(): LogDao
    abstract fun taskDao(): TaskDao
    abstract fun memoryDao(): MemoryDao
    abstract fun planDao(): PlanDao
}
