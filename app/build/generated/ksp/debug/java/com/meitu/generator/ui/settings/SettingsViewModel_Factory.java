package com.meitu.generator.ui.settings;

import android.app.Application;
import android.content.SharedPreferences;
import com.meitu.generator.data.local.dao.LogDao;
import com.meitu.generator.data.local.dao.TaskDao;
import com.meitu.generator.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
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

  private final Provider<SharedPreferences> securePrefsProvider;

  private final Provider<TaskDao> taskDaoProvider;

  private final Provider<LogDao> logDaoProvider;

  public SettingsViewModel_Factory(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<SharedPreferences> securePrefsProvider, Provider<TaskDao> taskDaoProvider,
      Provider<LogDao> logDaoProvider) {
    this.applicationProvider = applicationProvider;
    this.settingsRepoProvider = settingsRepoProvider;
    this.securePrefsProvider = securePrefsProvider;
    this.taskDaoProvider = taskDaoProvider;
    this.logDaoProvider = logDaoProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(applicationProvider.get(), settingsRepoProvider.get(), securePrefsProvider.get(), taskDaoProvider.get(), logDaoProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<SharedPreferences> securePrefsProvider, Provider<TaskDao> taskDaoProvider,
      Provider<LogDao> logDaoProvider) {
    return new SettingsViewModel_Factory(applicationProvider, settingsRepoProvider, securePrefsProvider, taskDaoProvider, logDaoProvider);
  }

  public static SettingsViewModel newInstance(Application application,
      SettingsRepository settingsRepo, SharedPreferences securePrefs, TaskDao taskDao,
      LogDao logDao) {
    return new SettingsViewModel(application, settingsRepo, securePrefs, taskDao, logDao);
  }
}
