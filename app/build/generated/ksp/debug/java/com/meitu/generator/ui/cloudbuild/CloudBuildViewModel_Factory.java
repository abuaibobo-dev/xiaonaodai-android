package com.meitu.generator.ui.cloudbuild;

import android.content.SharedPreferences;
import com.meitu.generator.repository.CloudBuildRepository;
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
public final class CloudBuildViewModel_Factory implements Factory<CloudBuildViewModel> {
  private final Provider<CloudBuildRepository> cloudBuildRepositoryProvider;

  private final Provider<SharedPreferences> securePrefsProvider;

  public CloudBuildViewModel_Factory(Provider<CloudBuildRepository> cloudBuildRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    this.cloudBuildRepositoryProvider = cloudBuildRepositoryProvider;
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public CloudBuildViewModel get() {
    return newInstance(cloudBuildRepositoryProvider.get(), securePrefsProvider.get());
  }

  public static CloudBuildViewModel_Factory create(
      Provider<CloudBuildRepository> cloudBuildRepositoryProvider,
      Provider<SharedPreferences> securePrefsProvider) {
    return new CloudBuildViewModel_Factory(cloudBuildRepositoryProvider, securePrefsProvider);
  }

  public static CloudBuildViewModel newInstance(CloudBuildRepository cloudBuildRepository,
      SharedPreferences securePrefs) {
    return new CloudBuildViewModel(cloudBuildRepository, securePrefs);
  }
}
