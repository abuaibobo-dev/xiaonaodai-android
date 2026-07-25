package com.meitu.generator.di

import com.meitu.generator.data.agent.SkillRegistry
import com.meitu.generator.data.agent.ToolRegistry
import com.meitu.generator.data.tools.CloudBuildTool
import com.meitu.generator.data.tools.DeveloperTool
import com.meitu.generator.data.tools.WebSearchTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 工具注册模块 - v4.4 注册 CloudBuildTool + DeveloperTool + WebSearchTool
 */
@Module
@InstallIn(SingletonComponent::class)
object ToolRegistrationModule {

    @Provides
    @Singleton
    fun provideToolRegistry(
        cloudBuildTool: CloudBuildTool,
        developerTool: DeveloperTool,
        webSearchTool: WebSearchTool
    ): ToolRegistry {
        val registry = ToolRegistry()
        registry.registerAll(listOf(cloudBuildTool, developerTool, webSearchTool))
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
