package com.meitu.generator.ui.gallery;

import android.app.Application;
import com.meitu.generator.repository.ImageRepository;
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
public final class GalleryViewModel_Factory implements Factory<GalleryViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<ImageRepository> imageRepoProvider;

  public GalleryViewModel_Factory(Provider<Application> applicationProvider,
      Provider<ImageRepository> imageRepoProvider) {
    this.applicationProvider = applicationProvider;
    this.imageRepoProvider = imageRepoProvider;
  }

  @Override
  public GalleryViewModel get() {
    return newInstance(applicationProvider.get(), imageRepoProvider.get());
  }

  public static GalleryViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<ImageRepository> imageRepoProvider) {
    return new GalleryViewModel_Factory(applicationProvider, imageRepoProvider);
  }

  public static GalleryViewModel newInstance(Application application, ImageRepository imageRepo) {
    return new GalleryViewModel(application, imageRepo);
  }
}
