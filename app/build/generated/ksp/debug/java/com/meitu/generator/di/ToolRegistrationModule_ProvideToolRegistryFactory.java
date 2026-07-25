package com.meitu.generator.di;

import com.meitu.generator.data.agent.ToolRegistry;
import com.meitu.generator.data.tools.CloudBuildTool;
import com.meitu.generator.data.tools.DeveloperTool;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class ToolRegistrationModule_ProvideToolRegistryFactory implements Factory<ToolRegistry> {
  private final Provider<CloudBuildTool> cloudBuildToolProvider;

  private final Provider<DeveloperTool> developerToolProvider;

  public ToolRegistrationModule_ProvideToolRegistryFactory(
      Provider<CloudBuildTool> cloudBuildToolProvider,
      Provider<DeveloperTool> developerToolProvider) {
    this.cloudBuildToolProvider = cloudBuildToolProvider;
    this.developerToolProvider = developerToolProvider;
  }

  @Override
  public ToolRegistry get() {
    return provideToolRegistry(cloudBuildToolProvider.get(), developerToolProvider.get());
  }

  public static ToolRegistrationModule_ProvideToolRegistryFactory create(
      Provider<CloudBuildTool> cloudBuildToolProvider,
      Provider<DeveloperTool> developerToolProvider) {
    return new ToolRegistrationModule_ProvideToolRegistryFactory(cloudBuildToolProvider, developerToolProvider);
  }

  public static ToolRegistry provideToolRegistry(CloudBuildTool cloudBuildTool,
      DeveloperTool developerTool) {
    return Preconditions.checkNotNullFromProvides(ToolRegistrationModule.INSTANCE.provideToolRegistry(cloudBuildTool, developerTool));
  }
}
