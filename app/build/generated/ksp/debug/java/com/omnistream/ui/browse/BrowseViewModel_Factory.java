package com.omnistream.ui.browse;

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
public final class BrowseViewModel_Factory implements Factory<BrowseViewModel> {
  private final Provider<SourceManager> sourceManagerProvider;

  public BrowseViewModel_Factory(Provider<SourceManager> sourceManagerProvider) {
    this.sourceManagerProvider = sourceManagerProvider;
  }

  @Override
  public BrowseViewModel get() {
    return newInstance(sourceManagerProvider.get());
  }

  public static BrowseViewModel_Factory create(Provider<SourceManager> sourceManagerProvider) {
    return new BrowseViewModel_Factory(sourceManagerProvider);
  }

  public static BrowseViewModel newInstance(SourceManager sourceManager) {
    return new BrowseViewModel(sourceManager);
  }
}
