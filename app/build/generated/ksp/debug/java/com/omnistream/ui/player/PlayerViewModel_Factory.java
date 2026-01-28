package com.omnistream.ui.player;

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
public final class PlayerViewModel_Factory implements Factory<PlayerViewModel> {
  private final Provider<SourceManager> sourceManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public PlayerViewModel_Factory(Provider<SourceManager> sourceManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.sourceManagerProvider = sourceManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public PlayerViewModel get() {
    return newInstance(sourceManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static PlayerViewModel_Factory create(Provider<SourceManager> sourceManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new PlayerViewModel_Factory(sourceManagerProvider, savedStateHandleProvider);
  }

  public static PlayerViewModel newInstance(SourceManager sourceManager,
      SavedStateHandle savedStateHandle) {
    return new PlayerViewModel(sourceManager, savedStateHandle);
  }
}
