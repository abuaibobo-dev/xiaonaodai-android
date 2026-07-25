package com.meitu.generator.repository;

import com.meitu.generator.data.remote.GitHubService;
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
public final class CloudBuildRepository_Factory implements Factory<CloudBuildRepository> {
  private final Provider<GitHubService> gitHubServiceProvider;

  public CloudBuildRepository_Factory(Provider<GitHubService> gitHubServiceProvider) {
    this.gitHubServiceProvider = gitHubServiceProvider;
  }

  @Override
  public CloudBuildRepository get() {
    return newInstance(gitHubServiceProvider.get());
  }

  public static CloudBuildRepository_Factory create(Provider<GitHubService> gitHubServiceProvider) {
    return new CloudBuildRepository_Factory(gitHubServiceProvider);
  }

  public static CloudBuildRepository newInstance(GitHubService gitHubService) {
    return new CloudBuildRepository(gitHubService);
  }
}
