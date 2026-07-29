package com.meitu.generator.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meitu.generator.data.local.dao.ChatMessageDao
import com.meitu.generator.data.local.dao.SettingsDao
import com.meitu.generator.data.local.entity.ChatMessageEntity
import com.meitu.generator.data.local.entity.SettingEntity

@Database(
    entities = [
        SettingEntity::class,
        ChatMessageEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun chatMessageDao(): ChatMessageDao
}
