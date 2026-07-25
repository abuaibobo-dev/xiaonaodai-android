package com.meitu.generator.ui.home;

import android.app.Application;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<PresetRepository> presetRepoProvider;

  private final Provider<ImageRepository> imageRepoProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<GenerationRepository> genRepoProvider;

  public HomeViewModel_Factory(Provider<Application> applicationProvider,
      Provider<PresetRepository> presetRepoProvider, Provider<ImageRepository> imageRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<GenerationRepository> genRepoProvider) {
    this.applicationProvider = applicationProvider;
    this.presetRepoProvider = presetRepoProvider;
    this.imageRepoProvider = imageRepoProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.genRepoProvider = genRepoProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(applicationProvider.get(), presetRepoProvider.get(), imageRepoProvider.get(), settingsRepoProvider.get(), genRepoProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<PresetRepository> presetRepoProvider, Provider<ImageRepository> imageRepoProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<GenerationRepository> genRepoProvider) {
    return new HomeViewModel_Factory(applicationProvider, presetRepoProvider, imageRepoProvider, settingsRepoProvider, genRepoProvider);
  }

  public static HomeViewModel newInstance(Application application, PresetRepository presetRepo,
      ImageRepository imageRepo, SettingsRepository settingsRepo, GenerationRepository genRepo) {
    return new HomeViewModel(application, presetRepo, imageRepo, settingsRepo, genRepo);
  }
}
