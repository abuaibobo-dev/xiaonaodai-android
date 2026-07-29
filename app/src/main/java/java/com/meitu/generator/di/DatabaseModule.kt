package com.meitu.generator.di

import android.content.Context
import androidx.room.Room
import com.meitu.generator.data.local.AppDatabase
import com.meitu.generator.data.local.dao.ChatMessageDao
import com.meitu.generator.data.local.dao.SettingsDao
import com.meitu.generator.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DB_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideSettingsDao(db: AppDatabase): SettingsDao = db.settingsDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
}
