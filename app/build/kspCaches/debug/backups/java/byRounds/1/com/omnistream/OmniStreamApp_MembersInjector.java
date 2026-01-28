package com.omnistream;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class OmniStreamApp_MembersInjector implements MembersInjector<OmniStreamApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public OmniStreamApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<OmniStreamApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new OmniStreamApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(OmniStreamApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.omnistream.OmniStreamApp.workerFactory")
  public static void injectWorkerFactory(OmniStreamApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
