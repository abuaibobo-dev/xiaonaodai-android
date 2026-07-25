package com.meitu.generator.di;

import com.meitu.generator.data.remote.GitHubService;
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
public final class NetworkModule_ProvideGitHubServiceFactory implements Factory<GitHubService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideGitHubServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public GitHubService get() {
    return provideGitHubService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideGitHubServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideGitHubServiceFactory(retrofitProvider);
  }

  public static GitHubService provideGitHubService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGitHubService(retrofit));
  }
}
