package com.meitu.generator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meitu.generator.data.local.dao.*
import com.meitu.generator.data.local.entity.*

@Database(
    entities = [
        PresetEntity::class,
        ImageEntity::class,
        SettingEntity::class,
        LogEntity::class,
        TaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun imageDao(): ImageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun logDao(): LogDao
    abstract fun taskDao(): TaskDao
}
