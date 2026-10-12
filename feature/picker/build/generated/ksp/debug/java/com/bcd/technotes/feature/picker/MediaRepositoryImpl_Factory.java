package com.bcd.technotes.feature.picker;

import android.app.Application;
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
public final class MediaRepositoryImpl_Factory implements Factory<MediaRepositoryImpl> {
  private final Provider<Application> applicationProvider;

  public MediaRepositoryImpl_Factory(Provider<Application> applicationProvider) {
    this.applicationProvider = applicationProvider;
  }

  @Override
  public MediaRepositoryImpl get() {
    return newInstance(applicationProvider.get());
  }

  public static MediaRepositoryImpl_Factory create(Provider<Application> applicationProvider) {
    return new MediaRepositoryImpl_Factory(applicationProvider);
  }

  public static MediaRepositoryImpl newInstance(Application application) {
    return new MediaRepositoryImpl(application);
  }
}
