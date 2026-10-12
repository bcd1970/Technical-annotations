package com.bcd.technotes.core.image;

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
public final class BitmapService_Factory implements Factory<BitmapService> {
  private final Provider<Application> applicationProvider;

  public BitmapService_Factory(Provider<Application> applicationProvider) {
    this.applicationProvider = applicationProvider;
  }

  @Override
  public BitmapService get() {
    return newInstance(applicationProvider.get());
  }

  public static BitmapService_Factory create(Provider<Application> applicationProvider) {
    return new BitmapService_Factory(applicationProvider);
  }

  public static BitmapService newInstance(Application application) {
    return new BitmapService(application);
  }
}
