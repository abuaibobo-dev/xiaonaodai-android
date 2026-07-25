package com.meitu.generator.data.tools;

import android.content.SharedPreferences;
import com.meitu.generator.data.remote.OpenAIService;
import com.meitu.generator.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DeveloperTool_Factory implements Factory<DeveloperTool> {
  private final Provider<OpenAIService> openAIServiceProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<SharedPreferences> securePrefsProvider;

  public DeveloperTool_Factory(Provider<OpenAIService> openAIServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    this.openAIServiceProvider = openAIServiceProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public DeveloperTool get() {
    return newInstance(openAIServiceProvider.get(), settingsRepositoryProvider.get(), securePrefsProvider.get());
  }

  public static DeveloperTool_Factory create(Provider<OpenAIService> openAIServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    return new DeveloperTool_Factory(openAIServiceProvider, settingsRepositoryProvider, securePrefsProvider);
  }

  public static DeveloperTool newInstance(OpenAIService openAIService,
      SettingsRepository settingsRepository, SharedPreferences securePrefs) {
    return new DeveloperTool(openAIService, settingsRepository, securePrefs);
  }
}
