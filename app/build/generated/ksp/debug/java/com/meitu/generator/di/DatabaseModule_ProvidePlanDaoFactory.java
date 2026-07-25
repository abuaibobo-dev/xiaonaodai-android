package com.meitu.generator.di;

import com.meitu.generator.data.local.AppDatabase;
import com.meitu.generator.data.local.dao.PlanDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidePlanDaoFactory implements Factory<PlanDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvidePlanDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PlanDao get() {
    return providePlanDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePlanDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvidePlanDaoFactory(dbProvider);
  }

  public static PlanDao providePlanDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePlanDao(db));
  }
}
