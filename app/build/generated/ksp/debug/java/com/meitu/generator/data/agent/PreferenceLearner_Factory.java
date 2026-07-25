package com.meitu.generator.data.agent;

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
public final class PreferenceLearner_Factory implements Factory<PreferenceLearner> {
  private final Provider<AgentMemory> agentMemoryProvider;

  public PreferenceLearner_Factory(Provider<AgentMemory> agentMemoryProvider) {
    this.agentMemoryProvider = agentMemoryProvider;
  }

  @Override
  public PreferenceLearner get() {
    return newInstance(agentMemoryProvider.get());
  }

  public static PreferenceLearner_Factory create(Provider<AgentMemory> agentMemoryProvider) {
    return new PreferenceLearner_Factory(agentMemoryProvider);
  }

  public static PreferenceLearner newInstance(AgentMemory agentMemory) {
    return new PreferenceLearner(agentMemory);
  }
}
