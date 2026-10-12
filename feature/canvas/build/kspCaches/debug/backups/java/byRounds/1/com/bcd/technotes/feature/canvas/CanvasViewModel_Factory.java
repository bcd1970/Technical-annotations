package com.bcd.technotes.feature.canvas;

import android.app.Application;
import com.bcd.technotes.core.image.BitmapService;
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
public final class CanvasViewModel_Factory implements Factory<CanvasViewModel> {
  private final Provider<BitmapService> bitmapServiceProvider;

  private final Provider<Application> applicationProvider;

  public CanvasViewModel_Factory(Provider<BitmapService> bitmapServiceProvider,
      Provider<Application> applicationProvider) {
    this.bitmapServiceProvider = bitmapServiceProvider;
    this.applicationProvider = applicationProvider;
  }

  @Override
  public CanvasViewModel get() {
    return newInstance(bitmapServiceProvider.get(), applicationProvider.get());
  }

  public static CanvasViewModel_Factory create(Provider<BitmapService> bitmapServiceProvider,
      Provider<Application> applicationProvider) {
    return new CanvasViewModel_Factory(bitmapServiceProvider, applicationProvider);
  }

  public static CanvasViewModel newInstance(BitmapService bitmapService, Application application) {
    return new CanvasViewModel(bitmapService, application);
  }
}
