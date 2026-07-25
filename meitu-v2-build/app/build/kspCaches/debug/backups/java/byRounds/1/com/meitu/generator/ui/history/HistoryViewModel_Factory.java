package com.meitu.generator.ui.history;

import com.meitu.generator.repository.GenerationRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<GenerationRepository> genRepoProvider;

  public HistoryViewModel_Factory(Provider<GenerationRepository> genRepoProvider) {
    this.genRepoProvider = genRepoProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(genRepoProvider.get());
  }

  public static HistoryViewModel_Factory create(Provider<GenerationRepository> genRepoProvider) {
    return new HistoryViewModel_Factory(genRepoProvider);
  }

  public static HistoryViewModel newInstance(GenerationRepository genRepo) {
    return new HistoryViewModel(genRepo);
  }
}
