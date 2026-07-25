package com.meitu.generator.repository;

import android.content.Context;
import com.meitu.generator.data.local.dao.SettingsDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsRepository_Factory implements Factory<SettingsRepository> {
  private final Provider<SettingsDao> settingsDaoProvider;

  private final Provider<Context> contextProvider;

  public SettingsRepository_Factory(Provider<SettingsDao> settingsDaoProvider,
      Provider<Context> contextProvider) {
    this.settingsDaoProvider = settingsDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsRepository get() {
    return newInstance(settingsDaoProvider.get(), contextProvider.get());
  }

  public static SettingsRepository_Factory create(Provider<SettingsDao> settingsDaoProvider,
      Provider<Context> contextProvider) {
    return new SettingsRepository_Factory(settingsDaoProvider, contextProvider);
  }

  public static SettingsRepository newInstance(SettingsDao settingsDao, Context context) {
    return new SettingsRepository(settingsDao, context);
  }
}
