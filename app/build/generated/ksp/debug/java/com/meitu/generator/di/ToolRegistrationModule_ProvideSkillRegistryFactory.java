package com.meitu.generator.di;

import com.meitu.generator.data.agent.SkillRegistry;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ToolRegistrationModule_ProvideSkillRegistryFactory implements Factory<SkillRegistry> {
  @Override
  public SkillRegistry get() {
    return provideSkillRegistry();
  }

  public static ToolRegistrationModule_ProvideSkillRegistryFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SkillRegistry provideSkillRegistry() {
    return Preconditions.checkNotNullFromProvides(ToolRegistrationModule.INSTANCE.provideSkillRegistry());
  }

  private static final class InstanceHolder {
    private static final ToolRegistrationModule_ProvideSkillRegistryFactory INSTANCE = new ToolRegistrationModule_ProvideSkillRegistryFactory();
  }
}
