package com.omnistream.ui.detail;

import androidx.lifecycle.SavedStateHandle;
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
public final class MangaDetailViewModel_Factory implements Factory<MangaDetailViewModel> {
  private final Provider<SourceManager> sourceManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public MangaDetailViewModel_Factory(Provider<SourceManager> sourceManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.sourceManagerProvider = sourceManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public MangaDetailViewModel get() {
    return newInstance(sourceManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static MangaDetailViewModel_Factory create(Provider<SourceManager> sourceManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new MangaDetailViewModel_Factory(sourceManagerProvider, savedStateHandleProvider);
  }

  public static MangaDetailViewModel newInstance(SourceManager sourceManager,
      SavedStateHandle savedStateHandle) {
    return new MangaDetailViewModel(sourceManager, savedStateHandle);
  }
}
