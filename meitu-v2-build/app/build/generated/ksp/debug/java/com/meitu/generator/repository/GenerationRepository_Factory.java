package com.meitu.generator.repository;

import com.meitu.generator.data.local.dao.LogDao;
import com.meitu.generator.data.local.dao.TaskDao;
import com.meitu.generator.data.remote.AgnesService;
import com.meitu.generator.data.remote.GeminiService;
import com.meitu.generator.data.remote.ImgBBService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class GenerationRepository_Factory implements Factory<GenerationRepository> {
  private final Provider<AgnesService> agnesServiceProvider;

  private final Provider<GeminiService> geminiServiceProvider;

  private final Provider<ImgBBService> imgBBServiceProvider;

  private final Provider<ImageRepository> imageRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<LogDao> logDaoProvider;

  private final Provider<TaskDao> taskDaoProvider;

  public GenerationRepository_Factory(Provider<AgnesService> agnesServiceProvider,
      Provider<GeminiService> geminiServiceProvider, Provider<ImgBBService> imgBBServiceProvider,
      Provider<ImageRepository> imageRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<LogDao> logDaoProvider,
      Provider<TaskDao> taskDaoProvider) {
    this.agnesServiceProvider = agnesServiceProvider;
    this.geminiServiceProvider = geminiServiceProvider;
    this.imgBBServiceProvider = imgBBServiceProvider;
    this.imageRepositoryProvider = imageRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.logDaoProvider = logDaoProvider;
    this.taskDaoProvider = taskDaoProvider;
  }

  @Override
  public GenerationRepository get() {
    return newInstance(agnesServiceProvider.get(), geminiServiceProvider.get(), imgBBServiceProvider.get(), imageRepositoryProvider.get(), settingsRepositoryProvider.get(), logDaoProvider.get(), taskDaoProvider.get());
  }

  public static GenerationRepository_Factory create(Provider<AgnesService> agnesServiceProvider,
      Provider<GeminiService> geminiServiceProvider, Provider<ImgBBService> imgBBServiceProvider,
      Provider<ImageRepository> imageRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider, Provider<LogDao> logDaoProvider,
      Provider<TaskDao> taskDaoProvider) {
    return new GenerationRepository_Factory(agnesServiceProvider, geminiServiceProvider, imgBBServiceProvider, imageRepositoryProvider, settingsRepositoryProvider, logDaoProvider, taskDaoProvider);
  }

  public static GenerationRepository newInstance(AgnesService agnesService,
      GeminiService geminiService, ImgBBService imgBBService, ImageRepository imageRepository,
      SettingsRepository settingsRepository, LogDao logDao, TaskDao taskDao) {
    return new GenerationRepository(agnesService, geminiService, imgBBService, imageRepository, settingsRepository, logDao, taskDao);
  }
}
