package com.meitu.generator.ui.assistant;

import android.app.Application;
import com.meitu.generator.data.agent.AgentEngine;
import com.meitu.generator.data.remote.OpenAIService;
import com.meitu.generator.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class AssistantViewModel_Factory implements Factory<AssistantViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<OpenAIService> openAIServiceProvider;

  private final Provider<AgentEngine> agentEngineProvider;

  public AssistantViewModel_Factory(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<OpenAIService> openAIServiceProvider, Provider<AgentEngine> agentEngineProvider) {
    this.applicationProvider = applicationProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.openAIServiceProvider = openAIServiceProvider;
    this.agentEngineProvider = agentEngineProvider;
  }

  @Override
  public AssistantViewModel get() {
    return newInstance(applicationProvider.get(), settingsRepoProvider.get(), openAIServiceProvider.get(), agentEngineProvider.get());
  }

  public static AssistantViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<OpenAIService> openAIServiceProvider, Provider<AgentEngine> agentEngineProvider) {
    return new AssistantViewModel_Factory(applicationProvider, settingsRepoProvider, openAIServiceProvider, agentEngineProvider);
  }

  public static AssistantViewModel newInstance(Application application,
      SettingsRepository settingsRepo, OpenAIService openAIService, AgentEngine agentEngine) {
    return new AssistantViewModel(application, settingsRepo, openAIService, agentEngine);
  }
}
