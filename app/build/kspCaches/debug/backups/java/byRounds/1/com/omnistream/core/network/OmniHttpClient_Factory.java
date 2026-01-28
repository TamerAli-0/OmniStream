package com.omnistream.core.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class OmniHttpClient_Factory implements Factory<OmniHttpClient> {
  @Override
  public OmniHttpClient get() {
    return newInstance();
  }

  public static OmniHttpClient_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static OmniHttpClient newInstance() {
    return new OmniHttpClient();
  }

  private static final class InstanceHolder {
    private static final OmniHttpClient_Factory INSTANCE = new OmniHttpClient_Factory();
  }
}
