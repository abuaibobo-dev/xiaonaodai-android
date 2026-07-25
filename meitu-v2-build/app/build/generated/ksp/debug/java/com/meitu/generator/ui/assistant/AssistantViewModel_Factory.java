package com.meitu.generator.ui.assistant;

import android.app.Application;
import com.meitu.generator.repository.GenerationRepository;
import com.meitu.generator.repository.PresetRepository;
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

  private final Provider<PresetRepository> presetRepoProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<GenerationRepository> genRepoProvider;

  public AssistantViewModel_Factory(Provider<Application> applicationProvider,
      Provider<PresetRepository> presetRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<GenerationRepository> genRepoProvider) {
    this.applicationProvider = applicationProvider;
    this.presetRepoProvider = presetRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.genRepoProvider = genRepoProvider;
  }

  @Override
  public AssistantViewModel get() {
    return newInstance(applicationProvider.get(), presetRepoProvider.get(), settingsRepoProvider.get(), genRepoProvider.get());
  }

  public static AssistantViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<PresetRepository> presetRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<GenerationRepository> genRepoProvider) {
    return new AssistantViewModel_Factory(applicationProvider, presetRepoProvider, settingsRepoProvider, genRepoProvider);
  }

  public static AssistantViewModel newInstance(Application application, PresetRepository presetRepo,
      SettingsRepository settingsRepo, GenerationRepository genRepo) {
    return new AssistantViewModel(application, presetRepo, settingsRepo, genRepo);
  }
}
