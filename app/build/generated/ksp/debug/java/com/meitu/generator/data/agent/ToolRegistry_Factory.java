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
public final class ToolRegistry_Factory implements Factory<ToolRegistry> {
  @Override
  public ToolRegistry get() {
    return newInstance();
  }

  public static ToolRegistry_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ToolRegistry newInstance() {
    return new ToolRegistry();
  }

  private static final class InstanceHolder {
    private static final ToolRegistry_Factory INSTANCE = new ToolRegistry_Factory();
  }
}
