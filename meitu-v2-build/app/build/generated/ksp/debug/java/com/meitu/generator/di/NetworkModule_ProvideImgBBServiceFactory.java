package com.meitu.generator.di;

import com.meitu.generator.data.remote.ImgBBService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvideImgBBServiceFactory implements Factory<ImgBBService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideImgBBServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ImgBBService get() {
    return provideImgBBService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideImgBBServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideImgBBServiceFactory(retrofitProvider);
  }

  public static ImgBBService provideImgBBService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideImgBBService(retrofit));
  }
}
