package com.meitu.generator.repository;

import com.meitu.generator.data.local.dao.PresetDao;
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
public final class PresetRepository_Factory implements Factory<PresetRepository> {
  private final Provider<PresetDao> presetDaoProvider;

  public PresetRepository_Factory(Provider<PresetDao> presetDaoProvider) {
    this.presetDaoProvider = presetDaoProvider;
  }

  @Override
  public PresetRepository get() {
    return newInstance(presetDaoProvider.get());
  }

  public static PresetRepository_Factory create(Provider<PresetDao> presetDaoProvider) {
    return new PresetRepository_Factory(presetDaoProvider);
  }

  public static PresetRepository newInstance(PresetDao presetDao) {
    return new PresetRepository(presetDao);
  }
}
