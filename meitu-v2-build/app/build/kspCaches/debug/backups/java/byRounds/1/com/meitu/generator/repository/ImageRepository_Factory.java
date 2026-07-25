package com.meitu.generator.repository;

import android.content.Context;
import com.meitu.generator.data.local.dao.ImageDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ImageRepository_Factory implements Factory<ImageRepository> {
  private final Provider<ImageDao> imageDaoProvider;

  private final Provider<Context> contextProvider;

  public ImageRepository_Factory(Provider<ImageDao> imageDaoProvider,
      Provider<Context> contextProvider) {
    this.imageDaoProvider = imageDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ImageRepository get() {
    return newInstance(imageDaoProvider.get(), contextProvider.get());
  }

  public static ImageRepository_Factory create(Provider<ImageDao> imageDaoProvider,
      Provider<Context> contextProvider) {
    return new ImageRepository_Factory(imageDaoProvider, contextProvider);
  }

  public static ImageRepository newInstance(ImageDao imageDao, Context context) {
    return new ImageRepository(imageDao, context);
  }
}
