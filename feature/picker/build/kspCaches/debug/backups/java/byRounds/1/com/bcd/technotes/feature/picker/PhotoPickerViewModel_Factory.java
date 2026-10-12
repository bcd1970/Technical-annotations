package com.bcd.technotes.feature.picker;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class PhotoPickerViewModel_Factory implements Factory<PhotoPickerViewModel> {
  private final Provider<MediaRepository> mediaRepositoryProvider;

  public PhotoPickerViewModel_Factory(Provider<MediaRepository> mediaRepositoryProvider) {
    this.mediaRepositoryProvider = mediaRepositoryProvider;
  }

  @Override
  public PhotoPickerViewModel get() {
    return newInstance(mediaRepositoryProvider.get());
  }

  public static PhotoPickerViewModel_Factory create(
      Provider<MediaRepository> mediaRepositoryProvider) {
    return new PhotoPickerViewModel_Factory(mediaRepositoryProvider);
  }

  public static PhotoPickerViewModel newInstance(MediaRepository mediaRepository) {
    return new PhotoPickerViewModel(mediaRepository);
  }
}
