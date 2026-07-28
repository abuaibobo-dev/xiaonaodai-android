package com.meitu.generator.di

import com.meitu.generator.data.agent.SkillRegistry
import com.meitu.generator.data.agent.ToolRegistry
import com.meitu.generator.data.tools.CloudBuildTool
import com.meitu.generator.data.tools.DeveloperTool
import com.meitu.generator.data.tools.WebSearchTool
import com.meitu.generator.data.tools.DiagnoseTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 工具注册模块 - v5.0 注册全部工具
 */
@Module
@InstallIn(SingletonComponent::class)
object ToolRegistrationModule {

    @Provides
    @Singleton
    fun provideToolRegistry(
        cloudBuildTool: CloudBuildTool,
        developerTool: DeveloperTool,
        webSearchTool: WebSearchTool,
        diagnoseTool: DiagnoseTool
    ): ToolRegistry {
        val registry = ToolRegistry()
        registry.registerAll(listOf(cloudBuildTool, developerTool, webSearchTool, diagnoseTool))
        return registry
    }

    @Provides
    @Singleton
    fun provideSkillRegistry(): SkillRegistry {
        val registry = SkillRegistry()
        registry.loadDefaults()
        return registry
    }
}
