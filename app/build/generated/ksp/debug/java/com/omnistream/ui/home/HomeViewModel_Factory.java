package com.omnistream.ui.home;

import com.omnistream.source.SourceManager;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<SourceManager> sourceManagerProvider;

  public HomeViewModel_Factory(Provider<SourceManager> sourceManagerProvider) {
    this.sourceManagerProvider = sourceManagerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(sourceManagerProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<SourceManager> sourceManagerProvider) {
    return new HomeViewModel_Factory(sourceManagerProvider);
  }

  public static HomeViewModel newInstance(SourceManager sourceManager) {
    return new HomeViewModel(sourceManager);
  }
}
