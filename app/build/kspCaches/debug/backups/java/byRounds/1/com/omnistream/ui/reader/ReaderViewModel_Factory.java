package com.omnistream.ui.reader;

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
public final class ReaderViewModel_Factory implements Factory<ReaderViewModel> {
  private final Provider<SourceManager> sourceManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ReaderViewModel_Factory(Provider<SourceManager> sourceManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.sourceManagerProvider = sourceManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ReaderViewModel get() {
    return newInstance(sourceManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static ReaderViewModel_Factory create(Provider<SourceManager> sourceManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ReaderViewModel_Factory(sourceManagerProvider, savedStateHandleProvider);
  }

  public static ReaderViewModel newInstance(SourceManager sourceManager,
      SavedStateHandle savedStateHandle) {
    return new ReaderViewModel(sourceManager, savedStateHandle);
  }
}
