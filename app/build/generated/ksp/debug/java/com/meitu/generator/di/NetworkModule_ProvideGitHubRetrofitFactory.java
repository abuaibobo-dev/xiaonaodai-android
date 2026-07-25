package com.meitu.generator.di;

import android.content.SharedPreferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideGitHubRetrofitFactory implements Factory<Retrofit> {
  private final Provider<SharedPreferences> securePrefsProvider;

  public NetworkModule_ProvideGitHubRetrofitFactory(
      Provider<SharedPreferences> securePrefsProvider) {
    this.securePrefsProvider = securePrefsProvider;
  }

  @Override
  public Retrofit get() {
    return provideGitHubRetrofit(securePrefsProvider.get());
  }

  public static NetworkModule_ProvideGitHubRetrofitFactory create(
      Provider<SharedPreferences> securePrefsProvider) {
    return new NetworkModule_ProvideGitHubRetrofitFactory(securePrefsProvider);
  }

  public static Retrofit provideGitHubRetrofit(SharedPreferences securePrefs) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGitHubRetrofit(securePrefs));
  }
}
