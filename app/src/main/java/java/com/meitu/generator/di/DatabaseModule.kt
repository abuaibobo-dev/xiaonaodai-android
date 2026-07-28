package com.meitu.generator.di

import android.content.Context
import androidx.room.Room
import com.meitu.generator.data.local.AppDatabase
import com.meitu.generator.data.local.dao.*
import com.meitu.generator.data.evermemory.BM25Retriever
import com.meitu.generator.data.evermemory.SimpleEmbedder
import com.meitu.generator.data.evermemory.SemanticClusterer
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
    @Provides fun provideLogDao(db: AppDatabase): LogDao = db.logDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideMemoryDao(db: AppDatabase): MemoryDao = db.memoryDao()
    @Provides fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()
    @Provides fun provideEverMemoryDao(db: AppDatabase): EverMemoryDao = db.everMemoryDao()

    // ============ EverOS 语义记忆系统依赖 ============

    @Provides
    @Singleton
    fun provideBM25Retriever(): BM25Retriever = BM25Retriever()

    @Provides
    @Singleton
    fun provideSimpleEmbedder(): SimpleEmbedder = SimpleEmbedder()

    @Provides
    @Singleton
    fun provideSemanticClusterer(embedder: SimpleEmbedder): SemanticClusterer =
        SemanticClusterer(embedder)
}
