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
public final class CircuitBreaker_Factory implements Factory<CircuitBreaker> {
  private final Provider<AgentMemory> agentMemoryProvider;

  public CircuitBreaker_Factory(Provider<AgentMemory> agentMemoryProvider) {
    this.agentMemoryProvider = agentMemoryProvider;
  }

  @Override
  public CircuitBreaker get() {
    return newInstance(agentMemoryProvider.get());
  }

  public static CircuitBreaker_Factory create(Provider<AgentMemory> agentMemoryProvider) {
    return new CircuitBreaker_Factory(agentMemoryProvider);
  }

  public static CircuitBreaker newInstance(AgentMemory agentMemory) {
    return new CircuitBreaker(agentMemory);
  }
}
