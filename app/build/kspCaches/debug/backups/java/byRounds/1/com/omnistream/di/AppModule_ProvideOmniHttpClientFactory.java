package com.omnistream.di;

import com.omnistream.core.network.OmniHttpClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideOmniHttpClientFactory implements Factory<OmniHttpClient> {
  @Override
  public OmniHttpClient get() {
    return provideOmniHttpClient();
  }

  public static AppModule_ProvideOmniHttpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OmniHttpClient provideOmniHttpClient() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideOmniHttpClient());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideOmniHttpClientFactory INSTANCE = new AppModule_ProvideOmniHttpClientFactory();
  }
}
