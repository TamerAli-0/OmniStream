package com.omnistream.source;

import android.content.Context;
import com.omnistream.core.network.OmniHttpClient;
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
public final class SourceManager_Factory implements Factory<SourceManager> {
  private final Provider<Context> contextProvider;

  private final Provider<OmniHttpClient> httpClientProvider;

  public SourceManager_Factory(Provider<Context> contextProvider,
      Provider<OmniHttpClient> httpClientProvider) {
    this.contextProvider = contextProvider;
    this.httpClientProvider = httpClientProvider;
  }

  @Override
  public SourceManager get() {
    return newInstance(contextProvider.get(), httpClientProvider.get());
  }

  public static SourceManager_Factory create(Provider<Context> contextProvider,
      Provider<OmniHttpClient> httpClientProvider) {
    return new SourceManager_Factory(contextProvider, httpClientProvider);
  }

  public static SourceManager newInstance(Context context, OmniHttpClient httpClient) {
    return new SourceManager(context, httpClient);
  }
}
