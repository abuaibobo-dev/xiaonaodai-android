package com.meitu.generator.ui.aibrain;

import com.meitu.generator.repository.GenerationRepository;
import com.meitu.generator.repository.ImageRepository;
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
public final class AIBrainViewModel_Factory implements Factory<AIBrainViewModel> {
  private final Provider<PresetRepository> presetRepoProvider;

  private final Provider<ImageRepository> imageRepoProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<GenerationRepository> genRepoProvider;

  public AIBrainViewModel_Factory(Provider<PresetRepository> presetRepoProvider,
      Provider<ImageRepository> imageRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<GenerationRepository> genRepoProvider) {
    this.presetRepoProvider = presetRepoProvider;
    this.imageRepoProvider = imageRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.genRepoProvider = genRepoProvider;
  }

  @Override
  public AIBrainViewModel get() {
    return newInstance(presetRepoProvider.get(), imageRepoProvider.get(), settingsRepoProvider.get(), genRepoProvider.get());
  }

  public static AIBrainViewModel_Factory create(Provider<PresetRepository> presetRepoProvider,
      Provider<ImageRepository> imageRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<GenerationRepository> genRepoProvider) {
    return new AIBrainViewModel_Factory(presetRepoProvider, imageRepoProvider, settingsRepoProvider, genRepoProvider);
  }

  public static AIBrainViewModel newInstance(PresetRepository presetRepo, ImageRepository imageRepo,
      SettingsRepository settingsRepo, GenerationRepository genRepo) {
    return new AIBrainViewModel(presetRepo, imageRepo, settingsRepo, genRepo);
  }
}
