package com.meitu.generator.data.agent;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SkillRegistry_Factory implements Factory<SkillRegistry> {
  @Override
  public SkillRegistry get() {
    return newInstance();
  }

  public static SkillRegistry_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SkillRegistry newInstance() {
    return new SkillRegistry();
  }

  private static final class InstanceHolder {
    private static final SkillRegistry_Factory INSTANCE = new SkillRegistry_Factory();
  }
}
