package com.meitu.generator.data.agent;

import android.content.SharedPreferences;
import com.meitu.generator.data.local.dao.MemoryDao;
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
public final class MemoryCompressor_Factory implements Factory<MemoryCompressor> {
  private final Provider<MemoryDao> memoryDaoProvider;

  private final Provider<OpenAIService> openAIServiceProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<SharedPreferences> securePrefsProvider;

  public MemoryCompressor_Factory(Provider<MemoryDao> memoryDaoProvider,
      Provider<OpenAIService> openAIServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    this.memoryDaoProvider = memoryDaoProvider;
    this.openAIServiceProvider = openAIServiceProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public MemoryCompressor get() {
    return newInstance(memoryDaoProvider.get(), openAIServiceProvider.get(), settingsRepositoryProvider.get(), securePrefsProvider.get());
  }

  public static MemoryCompressor_Factory create(Provider<MemoryDao> memoryDaoProvider,
      Provider<OpenAIService> openAIServiceProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    return new MemoryCompressor_Factory(memoryDaoProvider, openAIServiceProvider, settingsRepositoryProvider, securePrefsProvider);
  }

  public static MemoryCompressor newInstance(MemoryDao memoryDao, OpenAIService openAIService,
      SettingsRepository settingsRepository, SharedPreferences securePrefs) {
    return new MemoryCompressor(memoryDao, openAIService, settingsRepository, securePrefs);
  }
}
