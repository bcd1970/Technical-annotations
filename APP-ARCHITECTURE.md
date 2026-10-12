# Technical Annotations — Code Architecture

## Module Structure

```
TechnicalAnnotations/
├── :app          Android application (Compose UI, ViewModels, services)
├── :core         Pure Kotlin/JVM library (domain models, image processing)
└── :sandbox      Experimental app for prototyping features before porting to :app
```

**Dependency graph:**
```
:app ──────> :core
:sandbox ──> :core
```

`:core` has zero Android dependencies. It compiles as a JVM library and can be unit-tested without an emulator.

**Build targets:** minSdk 29 (Android 10), targetSdk 36, Java 21, Kotlin 2.1.10, Compose BOM 2025.03.00, AGP 8.8.2.

---

## Package Layout

### :app

```
com.bcd.technotes/
├── TechNotesApplication.kt        @HiltAndroidApp entry point (Timber init)
├── MainActivity.kt                @AndroidEntryPoint, single Activity, enableEdgeToEdge()
│
├── di/
│   └── AppModule.kt               Hilt module: @Binds MediaRepositoryImpl → MediaRepository
│
├── viewmodel/
│   ├── CanvasViewModel.kt          Collage state, photo transforms, edit/crop/pan modes
│   └── PhotoPickerViewModel.kt     Media queries, selection state
│
├── data/
│   ├── model/
│   │   ├── PhotoTransform.kt       Per-photo transform (flip, rotate, crop, pan)
│   │   ├── ImageEditState.kt       Whole-image edit state + EditTool enum
│   │   └── PickerModels.kt         Photo, Album, PickerView enum
│   │
│   ├── repository/
│   │   ├── MediaRepository.kt      Interface: queryAllPhotos, queryAlbums, queryAlbumPhotos
│   │   └── MediaRepositoryImpl.kt  MediaStore queries on Dispatchers.IO
│   │
│   └── service/
│       └── BitmapService.kt        Decode, transform, stitch, adjust (AGSL + GPUImage fallback)
│
└── ui/
    ├── canvas/
    │   ├── CanvasScreen.kt          Root composable: permission, navigation, canvas + edit UI
    │   ├── CollageThumbBar.kt       Miniature collage preview with viewport indicator
    │   └── LayoutSelector.kt        Scrollable row of layout template previews
    │
    ├── photo/
    │   ├── PhotoPickerScreen.kt     4-view media picker (recent, albums, album photos, preview)
    │   ├── PhotoGrid.kt             3-column LazyVerticalGrid with selection checkmarks
    │   ├── AlbumGrid.kt             2-column album cards (cover + name + count)
    │   ├── PhotoPreview.kt          HorizontalPager with pinch-to-zoom per page
    │   └── SelectedPhotosStrip.kt   Animated thumbnail strip of selected photos
    │
    ├── editor/
    │   ├── WholeImageEditScreen.kt  Single-image editor: crop, flip, rotate, adjust
    │   └── AdjustmentPanel.kt       FilterChip tabs + slider for 8 adjustment types
    │
    ├── util/
    │   ├── EditableTouchImageView.kt  Custom View: edit overlays, crop handles, reorder, pan
    │   ├── WholeImageEditView.kt      Custom View: single-image crop overlay
    │   └── TouchImageViewExt.kt       Extension for double-tap zoom calculation
    │
    └── theme/
        └── Theme.kt                 Material 3 theme
```

Empty placeholder packages exist for future features: `calibration/`, `export/`, `layers/`, `project/`, `properties/`, `symbols/`, `toolbar/`.

### :core

```
com.bcd.technotes.core/
├── model/
│   ├── Annotation.kt              13 sealed annotation types (Line, Rectangle, etc.)
│   ├── AnnotationStyle.kt         Stroke/fill properties, transform matrix
│   ├── CollageLayout.kt           CollageLayout, CollageCell, layoutsForCount()
│   ├── Geometry.kt                Serializable PointF, SizeF, RectF
│   ├── Layer.kt                   Layer, LayerType enum
│   ├── PhotoAdjustments.kt        9-parameter adjustment model, AdjustmentType enum
│   └── Project.kt                 Project, PhotoReference, ScaleCalibration
│
├── processing/
│   ├── ColorSpace.kt              srgbToLinear(), linearToSrgb(), extractLuminance()
│   └── GuidedFilter.kt            Edge-preserving filter using integral images (SAT)
│
└── serialization/
    └── ProjectSerializer.kt       kotlinx.serialization for Project persistence
```

---

## Dependency Injection (Hilt)

Single-activity architecture with Hilt DI:

```
@HiltAndroidApp
TechNotesApplication
    │
    ▼
@AndroidEntryPoint
MainActivity
    │
    ├── @HiltViewModel CanvasViewModel(BitmapService, Application)
    └── @HiltViewModel PhotoPickerViewModel(MediaRepository)
```

**AppModule** (SingletonComponent scope):
```kotlin
@Binds
abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
```

`BitmapService` and `MediaRepositoryImpl` are `@Inject constructor` classes — Hilt provides them automatically without explicit `@Provides`. `Application` is provided by Hilt out of the box.

ViewModels are obtained in composables via `hiltViewModel()`.

---

## Navigation

**No Compose Navigation library is used.** Screen switching is manual, using `remember { mutableStateOf() }` booleans in `CanvasScreen`:

```kotlin
var showPicker by remember { mutableStateOf(false) }
var showEditor by remember { mutableStateOf(false) }
```

Routing logic at the top of `CanvasScreen`:
```kotlin
if (showPicker) {
    PhotoPickerScreen(onPhotosConfirmed = { ... }, onDismiss = { showPicker = false })
    return
}
if (showEditor && backgroundBitmap != null) {
    WholeImageEditScreen(bitmap = ..., onDone = { ... showEditor = false })
    return
}
// ... main canvas UI
```

Within `PhotoPickerScreen`, a `PickerView` enum drives the 4 sub-views:
```kotlin
var currentView by remember { mutableStateOf(PickerView.RECENT) }
```

`BackHandler` composables handle the Android back button at each level, updating the state enum or dismissing the screen.

---

## State Management

### CanvasViewModel

All state exposed as `StateFlow<T>` via the `MutableStateFlow` + `asStateFlow()` pattern. UI collects via `collectAsState()`.

**State properties:**

| Property | Type | Purpose |
|----------|------|---------|
| `backgroundBitmap` | `Bitmap?` | Displayed canvas image (single photo or stitched collage) |
| `selectedUris` | `List<Uri>` | Photo URIs in current collage (empty for single photo) |
| `photoTransforms` | `List<PhotoTransform>` | Parallel list of per-photo transforms |
| `photoBounds` | `List<RectF>` | Pixel coordinates of each photo on the stitched canvas |
| `photoVisibleRects` | `List<RectF>` | Normalized visible region per photo (for pan tracking) |
| `editMode` | `Boolean` | Per-photo editing active |
| `activePhotoIndex` | `Int` | Selected photo index (-1 = none) |
| `cropMode` | `Boolean` | Crop overlay visible |
| `panEnabled` | `Boolean` | Pan mode active (grid layouts only) |
| `activeLayout` | `CollageLayout?` | Current collage template |
| `availableLayouts` | `List<CollageLayout>` | Valid templates for current photo count |

**Private state:**
- `bitmapCache: MutableMap<Uri, Bitmap>` — in-memory decoded bitmap cache
- `canvasWidth: Int` — display width × 2 for resolution scaling

### Reactive Collage Pipeline

The `init` block sets up two coroutine collectors in `viewModelScope`:

**1. Layout auto-selection** — collects `selectedUris`, updates `availableLayouts` and `activeLayout` based on photo count:
```
selectedUris.collect → layoutsForCount(count) → availableLayouts + activeLayout
```

**2. Collage stitching** — combines three flows and re-stitches on any change:
```
combine(selectedUris, photoTransforms, activeLayout).collect →
    decode missing bitmaps →
    BitmapService.stitchCollage() →
    emit backgroundBitmap + photoBounds + photoVisibleRects
```

Any mutation to URIs, transforms, or layout triggers an automatic re-stitch. Individual edit actions (flip, rotate, crop, pan, reorder) just update `photoTransforms`, and the pipeline handles the rest.

### PhotoPickerViewModel

| Property | Type | Purpose |
|----------|------|---------|
| `recentPhotos` | `List<Photo>` | All device photos, date-descending |
| `albums` | `List<Album>` | Device albums with cover + count |
| `albumPhotos` | `List<Photo>` | Photos in currently selected album |
| `selectedPhotoIds` | `Set<Long>` | IDs of selected photos |
| `selectedPhotos` | `List<Photo>` | Selected Photo objects (preserves selection order) |

Uses a `MutableMap<Long, Photo>` cache to maintain selection order. `loadAlbums()` is idempotent (skips if already loaded).

---

## Data Layer

### MediaRepository

Interface with three suspend functions:
```kotlin
suspend fun queryAllPhotos(): List<Photo>
suspend fun queryAlbums(): List<Album>
suspend fun queryAlbumPhotos(bucketId: Long): List<Photo>
```

`MediaRepositoryImpl` queries `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` on `Dispatchers.IO`. Albums are deduplicated via `LinkedHashMap<Long, Album>` keyed by `BUCKET_ID`.

### BitmapService

Injected via `@Inject constructor(application: Application)`. Handles all bitmap operations:

**Loading:**
- `decodeBitmap(uri)` — two-pass decode: bounds-only pass for `inSampleSize` calculation, then full decode at ≤ 2× display height

**Transforms:**
- `applyTransform(bitmap, photoTransform)` — applies flip → rotate → crop in sequence; returns the original bitmap if the transform is identity; recycles intermediate bitmaps
- `cropBitmap(bitmap, normalizedRect)` — crop using normalized [0,1] coordinates
- `flipBitmap(bitmap, horizontal)` — mirror via `Matrix.postScale`
- `rotateBitmap(bitmap, degrees)` — rotate via `Matrix.postRotate`
- `applyEditState(source, imageEditState)` — applies crop → flip → rotate (for whole-image editor)

**Collage stitching:**
- `stitchCollage(bitmaps, transforms, layout, canvasWidth)` → `CollageResult`
  - Delegates to `stitchHorizontalRow()` when `layout.aspectRatio == 0` (content-determined height, scaled widths, no upscaling)
  - Delegates to `stitchGrid()` otherwise (fixed aspect ratio, center-crop with panX/panY offsets, source/dest rect mapping via `Canvas.drawBitmap`)

**Adjustments:**
- `computeBaseDetailTexture(bitmap)` → RGBA_F16 bitmap containing base luminance, detail, and original luminance per pixel. Uses `GuidedFilter` from `:core` with integral-image SAT. ~120ms CPU.
- `applyAdjustments(bitmap, adjustments, baseDetail)` — branches on API level:
  - **Android 13+ (AGSL path):** `RuntimeShader` with a custom shader that processes in linear color space — shadows/highlights via guided-filter base/detail remapping, brightness via EV stops, warmth/tint via D65 chromatic adaptation through XYZ color space, contrast via LogC tone curve, saturation in HSL, sharpening via 9-tap Gaussian unsharp mask with edge gating, and film grain dithering in shadows
  - **Android < 13 (GPUImage path):** filter chain of `GPUImageBrightnessFilter`, `GPUImageContrastFilter`, `GPUImageSaturationFilter`, `GPUImageWhiteBalanceFilter`, `GPUImageHighlightShadowFilter`, `GPUImageSharpenFilter`

---

## Data Models

### :app models (Android-dependent, use `android.graphics.RectF`)

```kotlin
data class PhotoTransform(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,            // 0, 90, 180, 270
    val cropRect: RectF? = null,      // normalized [0,1]
    val panX: Float = 0.5f,           // 0=left, 0.5=center, 1=right
    val panY: Float = 0.5f            // 0=top, 0.5=center, 1=bottom
)

data class CollageResult(
    val bitmap: Bitmap,
    val photoBounds: List<RectF>,     // pixel coords on canvas
    val photoVisibleRects: List<RectF> // normalized visible area
)

data class ImageEditState(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null,
    val adjustments: PhotoAdjustments = PhotoAdjustments()
)

enum class EditTool { NONE, CROP, ADJUST }

data class Photo(val id: Long, val uri: Uri)
data class Album(val id: Long, val name: String, val coverUri: Uri, val photoCount: Int)
enum class PickerView { RECENT, ALBUMS, ALBUM_PHOTOS, PHOTO_PREVIEW }
```

### :core models (pure Kotlin, kotlinx.serialization)

```kotlin
data class CollageLayout(
    val id: String,
    val cells: List<CollageCell>,     // normalized [0,1] rectangles
    val aspectRatio: Float            // 0 = horizontal row mode
)
data class CollageCell(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class PhotoAdjustments(
    val brightness: Float = 0f,       // -100..+100
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpness: Float = 0f,        // 0..100
    val vignette: Float = 0f
)

data class Project(...)               // Full project structure (annotations, layers, etc.)
sealed class Annotation(...)          // 13 annotation types
data class Layer(...)                 // Canvas layers
```

---

## Custom View Integration

Two custom views extend `TouchImageView` (MikeOrtiz, v3.7.1) and are embedded in Compose via `AndroidView`:

### EditableTouchImageView

Used in `CanvasScreen` for the main canvas. Adds overlay rendering and gesture handling on top of the library's zoom/pan/fling:

- **Edit mode overlays:** draws photo bounds as colored rectangles (cyan for active, white for inactive, dimmed for others) via `onDraw` override
- **Crop mode:** 8-handle overlay (4 corners + 4 midpoints), dimmed region outside crop, handle drag with min-size constraints
- **Drag-to-reorder:** touch event interception detects horizontal drag (1D) or any-direction drag (2D grid); frame-based animation engine (immune to `animator_duration_scale`); haptic feedback per swap
- **Pan mode:** drag to reposition cropped photo within grid cell

**Callbacks to ViewModel:**
```kotlin
var onReorderComplete: ((newOrder: List<Int>) -> Unit)?
var onPanComplete: ((panX: Float, panY: Float) -> Unit)?
var getTransformedBitmap: ((index: Int) -> Bitmap?)?
```

### WholeImageEditView

Used in `WholeImageEditScreen`. Simplified variant with only crop overlay (no reorder, no pan, no multi-photo bounds).

### Compose bridge pattern

```kotlin
AndroidView(
    factory = { ctx ->
        EditableTouchImageView(ctx).apply {
            // one-time setup: listeners, callbacks, config
        }
    },
    update = { view ->
        // sync ViewModel state → View properties on every recomposition
        view.setImageBitmap(bitmap)
        view.photoBounds = photoBounds
        view.editMode = editMode
        view.activePhotoIndex = activePhotoIndex
        // ...
    }
)
```

---

## Core Module Processing

### GuidedFilter

Edge-preserving smoothing filter used to decompose an image into base (low-frequency) and detail (high-frequency) layers. This powers the highlight/shadow adjustment pipeline.

- Pure Kotlin, no Android dependencies
- Uses summed-area table (SAT) with `DoubleArray` for numerical precision
- Box filter queries with clamped boundaries
- `filter(luminance, radius, eps)` returns the base layer; detail = luminance - base

### ColorSpace

Utility functions for color space conversions:
- `srgbToLinear(c)` / `linearToSrgb(c)` — sRGB gamma transfer functions
- `extractLuminance(r, g, b)` — BT.709 luminance coefficients (0.2126, 0.7152, 0.0722)

---

## Key Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Hilt | 2.56.2 | Dependency injection |
| TouchImageView | 3.7.1 | Zoom, pan, fling, double-tap zoom base class |
| Coil 3 | 3.1.0 | Async image loading with thumbnail caching (256px) |
| GPUImage | 2.1.0 | CPU-based image filter fallback for Android < 13 |
| Telephoto | 0.14.0 | Zoomable image support (used with Coil in previews) |
| kotlinx.serialization | 1.7.3 | JSON serialization for core models |
| kotlinx.collections.immutable | 0.3.8 | Immutable collections for Compose stability |
| Timber | 5.0.1 | Debug logging |
| LeakCanary | 2.14 | Memory leak detection (debug only) |

---

## Threading Model

- **Main thread:** Compose UI, state collection, view drawing
- **`Dispatchers.IO`:** bitmap decoding (`decodeBitmap`), collage stitching (`stitchCollage`), MediaStore queries (all repository methods)
- **`viewModelScope`:** coroutine scope for ViewModel work; cancels on ViewModel clear
- **Frame-based animations:** `EditableTouchImageView` uses `postOnAnimation` / `Choreographer` for drag-to-reorder animations, bypassing `ValueAnimator` (immune to `animator_duration_scale`)
