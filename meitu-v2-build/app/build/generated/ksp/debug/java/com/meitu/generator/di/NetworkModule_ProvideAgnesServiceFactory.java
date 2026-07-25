package com.meitu.generator.di;

import com.meitu.generator.data.remote.AgnesService;
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
public final class NetworkModule_ProvideAgnesServiceFactory implements Factory<AgnesService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideAgnesServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public AgnesService get() {
    return provideAgnesService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideAgnesServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideAgnesServiceFactory(retrofitProvider);
  }

  public static AgnesService provideAgnesService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAgnesService(retrofit));
  }
}
