package com.meitu.generator.data.agent;

import android.content.Context;
import android.content.SharedPreferences;
import com.meitu.generator.data.local.dao.PlanDao;
import com.meitu.generator.data.remote.OpenAIService;
import com.meitu.generator.data.tools.CloudBuildTool;
import com.meitu.generator.data.tools.DeveloperTool;
import com.meitu.generator.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "javax.inject.Named"
})
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AgentEngine_Factory implements Factory<AgentEngine> {
  private final Provider<Context> appContextProvider;

  private final Provider<ToolRegistry> toolRegistryProvider;

  private final Provider<SkillRegistry> skillRegistryProvider;

  private final Provider<AgentMemory> agentMemoryProvider;

  private final Provider<OpenAIService> openAIServiceProvider;

  private final Provider<PlanDao> planDaoProvider;

  private final Provider<SemanticCache> semanticCacheProvider;

  private final Provider<MemoryCompressor> memoryCompressorProvider;

  private final Provider<CircuitBreaker> circuitBreakerProvider;

  private final Provider<PreferenceLearner> preferenceLearnerProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<DeveloperTool> developerToolProvider;

  private final Provider<CloudBuildTool> cloudBuildToolProvider;

  private final Provider<SharedPreferences> securePrefsProvider;

  public AgentEngine_Factory(Provider<Context> appContextProvider,
      Provider<ToolRegistry> toolRegistryProvider, Provider<SkillRegistry> skillRegistryProvider,
      Provider<AgentMemory> agentMemoryProvider, Provider<OpenAIService> openAIServiceProvider,
      Provider<PlanDao> planDaoProvider, Provider<SemanticCache> semanticCacheProvider,
      Provider<MemoryCompressor> memoryCompressorProvider,
      Provider<CircuitBreaker> circuitBreakerProvider,
      Provider<PreferenceLearner> preferenceLearnerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<DeveloperTool> developerToolProvider,
      Provider<CloudBuildTool> cloudBuildToolProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    this.appContextProvider = appContextProvider;
    this.toolRegistryProvider = toolRegistryProvider;
    this.skillRegistryProvider = skillRegistryProvider;
    this.agentMemoryProvider = agentMemoryProvider;
    this.openAIServiceProvider = openAIServiceProvider;
    this.planDaoProvider = planDaoProvider;
    this.semanticCacheProvider = semanticCacheProvider;
    this.memoryCompressorProvider = memoryCompressorProvider;
    this.circuitBreakerProvider = circuitBreakerProvider;
    this.preferenceLearnerProvider = preferenceLearnerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.developerToolProvider = developerToolProvider;
    this.cloudBuildToolProvider = cloudBuildToolProvider;
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public AgentEngine get() {
    return newInstance(appContextProvider.get(), toolRegistryProvider.get(), skillRegistryProvider.get(), agentMemoryProvider.get(), openAIServiceProvider.get(), planDaoProvider.get(), semanticCacheProvider.get(), memoryCompressorProvider.get(), circuitBreakerProvider.get(), preferenceLearnerProvider.get(), settingsRepositoryProvider.get(), developerToolProvider.get(), cloudBuildToolProvider.get(), securePrefsProvider.get());
  }

  public static AgentEngine_Factory create(Provider<Context> appContextProvider,
      Provider<ToolRegistry> toolRegistryProvider, Provider<SkillRegistry> skillRegistryProvider,
      Provider<AgentMemory> agentMemoryProvider, Provider<OpenAIService> openAIServiceProvider,
      Provider<PlanDao> planDaoProvider, Provider<SemanticCache> semanticCacheProvider,
      Provider<MemoryCompressor> memoryCompressorProvider,
      Provider<CircuitBreaker> circuitBreakerProvider,
      Provider<PreferenceLearner> preferenceLearnerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<DeveloperTool> developerToolProvider,
      Provider<CloudBuildTool> cloudBuildToolProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    return new AgentEngine_Factory(appContextProvider, toolRegistryProvider, skillRegistryProvider, agentMemoryProvider, openAIServiceProvider, planDaoProvider, semanticCacheProvider, memoryCompressorProvider, circuitBreakerProvider, preferenceLearnerProvider, settingsRepositoryProvider, developerToolProvider, cloudBuildToolProvider, securePrefsProvider);
  }

  public static AgentEngine newInstance(Context appContext, ToolRegistry toolRegistry,
      SkillRegistry skillRegistry, AgentMemory agentMemory, OpenAIService openAIService,
      PlanDao planDao, SemanticCache semanticCache, MemoryCompressor memoryCompressor,
      CircuitBreaker circuitBreaker, PreferenceLearner preferenceLearner,
      SettingsRepository settingsRepository, DeveloperTool developerTool,
      CloudBuildTool cloudBuildTool, SharedPreferences securePrefs) {
    return new AgentEngine(appContext, toolRegistry, skillRegistry, agentMemory, openAIService, planDao, semanticCache, memoryCompressor, circuitBreaker, preferenceLearner, settingsRepository, developerTool, cloudBuildTool, securePrefs);
  }
}
