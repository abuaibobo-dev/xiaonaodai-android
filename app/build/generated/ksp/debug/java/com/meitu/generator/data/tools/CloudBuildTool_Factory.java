package com.meitu.generator.data.tools;

import android.content.SharedPreferences;
import com.meitu.generator.repository.CloudBuildRepository;
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
public final class CloudBuildTool_Factory implements Factory<CloudBuildTool> {
  private final Provider<CloudBuildRepository> cloudBuildRepositoryProvider;

  private final Provider<SharedPreferences> securePrefsProvider;

  public CloudBuildTool_Factory(Provider<CloudBuildRepository> cloudBuildRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    this.cloudBuildRepositoryProvider = cloudBuildRepositoryProvider;
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public CloudBuildTool get() {
    return newInstance(cloudBuildRepositoryProvider.get(), securePrefsProvider.get());
  }

  public static CloudBuildTool_Factory create(
      Provider<CloudBuildRepository> cloudBuildRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    return new CloudBuildTool_Factory(cloudBuildRepositoryProvider, securePrefsProvider);
  }

  public static CloudBuildTool newInstance(CloudBuildRepository cloudBuildRepository,
      SharedPreferences securePrefs) {
    return new CloudBuildTool(cloudBuildRepository, securePrefs);
  }
}
