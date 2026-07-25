package com.meitu.generator.data.agent;

import com.meitu.generator.data.local.dao.MemoryDao;
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
public final class AgentMemory_Factory implements Factory<AgentMemory> {
  private final Provider<MemoryDao> memoryDaoProvider;

  public AgentMemory_Factory(Provider<MemoryDao> memoryDaoProvider) {
    this.memoryDaoProvider = memoryDaoProvider;
  }

  @Override
  public AgentMemory get() {
    return newInstance(memoryDaoProvider.get());
  }

  public static AgentMemory_Factory create(Provider<MemoryDao> memoryDaoProvider) {
    return new AgentMemory_Factory(memoryDaoProvider);
  }

  public static AgentMemory newInstance(MemoryDao memoryDao) {
    return new AgentMemory(memoryDao);
  }
}
