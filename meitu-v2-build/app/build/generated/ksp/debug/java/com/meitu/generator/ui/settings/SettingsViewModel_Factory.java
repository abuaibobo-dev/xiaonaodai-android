package com.meitu.generator.ui.settings;

import android.app.Application;
import com.meitu.generator.data.local.dao.ImageDao;
import com.meitu.generator.data.local.dao.LogDao;
import com.meitu.generator.data.local.dao.PresetDao;
import com.meitu.generator.data.local.dao.TaskDao;
import com.meitu.generator.repository.GenerationRepository;
import com.meitu.generator.repository.ImageRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<ImageRepository> imageRepoProvider;

  private final Provider<GenerationRepository> genRepoProvider;

  private final Provider<PresetDao> presetDaoProvider;

  private final Provider<ImageDao> imageDaoProvider;

  private final Provider<TaskDao> taskDaoProvider;

  private final Provider<LogDao> logDaoProvider;

  public SettingsViewModel_Factory(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<ImageRepository> imageRepoProvider, Provider<GenerationRepository> genRepoProvider,
      Provider<PresetDao> presetDaoProvider, Provider<ImageDao> imageDaoProvider,
      Provider<TaskDao> taskDaoProvider, Provider<LogDao> logDaoProvider) {
    this.applicationProvider = applicationProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.imageRepoProvider = imageRepoProvider;
    this.genRepoProvider = genRepoProvider;
    this.presetDaoProvider = presetDaoProvider;
    this.imageDaoProvider = imageDaoProvider;
    this.taskDaoProvider = taskDaoProvider;
    this.logDaoProvider = logDaoProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(applicationProvider.get(), settingsRepoProvider.get(), imageRepoProvider.get(), genRepoProvider.get(), presetDaoProvider.get(), imageDaoProvider.get(), taskDaoProvider.get(), logDaoProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<ImageRepository> imageRepoProvider, Provider<GenerationRepository> genRepoProvider,
      Provider<PresetDao> presetDaoProvider, Provider<ImageDao> imageDaoProvider,
      Provider<TaskDao> taskDaoProvider, Provider<LogDao> logDaoProvider) {
    return new SettingsViewModel_Factory(applicationProvider, settingsRepoProvider, imageRepoProvider, genRepoProvider, presetDaoProvider, imageDaoProvider, taskDaoProvider, logDaoProvider);
  }

  public static SettingsViewModel newInstance(Application application,
      SettingsRepository settingsRepo, ImageRepository imageRepo, GenerationRepository genRepo,
      PresetDao presetDao, ImageDao imageDao, TaskDao taskDao, LogDao logDao) {
    return new SettingsViewModel(application, settingsRepo, imageRepo, genRepo, presetDao, imageDao, taskDao, logDao);
  }
}
