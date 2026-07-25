package com.meitu.generator.di;

import com.meitu.generator.data.local.AppDatabase;
import com.meitu.generator.data.local.dao.ImageDao;
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
public final class DatabaseModule_ProvideImageDaoFactory implements Factory<ImageDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideImageDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ImageDao get() {
    return provideImageDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideImageDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideImageDaoFactory(dbProvider);
  }

  public static ImageDao provideImageDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideImageDao(db));
  }
}
