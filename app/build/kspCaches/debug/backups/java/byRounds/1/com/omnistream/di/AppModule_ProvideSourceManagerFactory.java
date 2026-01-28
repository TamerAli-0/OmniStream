package com.omnistream.di;

import android.content.Context;
import com.omnistream.core.network.OmniHttpClient;
import com.omnistream.source.SourceManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideSourceManagerFactory implements Factory<SourceManager> {
  private final Provider<Context> contextProvider;

  private final Provider<OmniHttpClient> httpClientProvider;

  public AppModule_ProvideSourceManagerFactory(Provider<Context> contextProvider,
      Provider<OmniHttpClient> httpClientProvider) {
    this.contextProvider = contextProvider;
    this.httpClientProvider = httpClientProvider;
  }

  @Override
  public SourceManager get() {
    return provideSourceManager(contextProvider.get(), httpClientProvider.get());
  }

  public static AppModule_ProvideSourceManagerFactory create(Provider<Context> contextProvider,
      Provider<OmniHttpClient> httpClientProvider) {
    return new AppModule_ProvideSourceManagerFactory(contextProvider, httpClientProvider);
  }

  public static SourceManager provideSourceManager(Context context, OmniHttpClient httpClient) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSourceManager(context, httpClient));
  }
}
