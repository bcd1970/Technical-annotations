# Technical Annotations - Implementation Status

**Date:** 2026-04-03
**Branch:** `restructure/multi-module-setup` (uncommitted, in progress)
**Base:** `main` (last commit: `0c1f1c0` - guided filter pipeline)
**Version:** 2.0.0 | SDK 36 | minSdk 29 | JVM 21

---

## Branch State

The working tree has **uncommitted changes** representing a major restructuring from a flat 3-module layout (`app`, `core`, `sandbox`) into a 13-module architecture. Old files under `app/src/` and `core/src/` are deleted; new module directories under `core/*/` and `feature/*/` are untracked. The restructure is **not yet committed**.

---

## Module Map

| Module | Type | Status | Files | Lines | Purpose |
|--------|------|--------|-------|-------|---------|
| `:app` | Application | Active | 4 | ~120 | Entry point, navigation, DI |
| `:sandbox` | Application | Active | ~8 | ~800 | Isolated feature experiments |
| `:core:model` | JVM Library | Active | 9 | ~1,100 | Pure Kotlin data models |
| `:core:ui` | Android Library | Active | 10 | ~400 | Monochrome design system |
| `:core:image` | Android Library | Active | 4 | ~700 | Bitmap ops, guided filter |
| `:core:drawing` | Android Library | **Stub** | 4 | 12 | Stroke smoothing, arrows |
| `:core:overlay` | Android Library | **Stub** | 3 | 9 | Blend modes, masks |
| `:core:undo` | Android Library | **Stub** | 1 | 3 | Undo/redo stack |
| `:core:project` | Android Library | **Stub** | 4 | 24 | Persistence, auto-save |
| `:feature:canvas` | Android Library | Active | 6 | ~2,100 | Collage editor |
| `:feature:picker` | Android Library | Active | 9 | ~1,100 | Photo/album selection |
| `:feature:editor` | Android Library | Active | 3 | ~580 | Whole-image editing |
| `:feature:export` | Android Library | **Stub** | 2 | 11 | Share/save export |

**Total:** 13 modules, ~60 source files, ~5,200 lines of Kotlin (excluding tests)

---

## Dependency Graph

```
:app
 +-- :feature:canvas  --> :core:model, :core:image, :core:ui
 +-- :feature:picker   --> :core:model, :core:ui
 +-- :feature:editor   --> :core:model, :core:image, :core:ui
 +-- :feature:export   --> (stub)
 +-- :core:model       --> (pure JVM, no deps)
 +-- :core:ui          --> :core:model
 +-- :core:image       --> :core:model
 +-- :core:drawing     --> :core:model (stub)
 +-- :core:overlay     --> :core:model (stub)
 +-- :core:undo        --> :core:model (stub)
 +-- :core:project     --> :core:model (stub)

:sandbox
 +-- :core:* (same core modules, independent UI)
```

Features never depend on each other. Cross-feature routing lives in `:app`'s `AppNavigation.kt`.

---

## Feature Status

### Implemented & Working

| Feature | Module(s) | Key Files | Notes |
|---------|-----------|-----------|-------|
| **Photo picker** | `:feature:picker` | PhotoPickerScreen, PhotoPickerViewModel, MediaRepositoryImpl | Album view, recent photos, multi-select, preview, animated strip |
| **Collage canvas** | `:feature:canvas` | CanvasScreen, CanvasViewModel, EditableTouchImageView | Multi-photo layout, 20+ layouts, drag-to-reorder, per-photo crop/rotate/flip, haptic feedback |
| **Whole-image editor** | `:feature:editor` | WholeImageEditScreen, WholeImageEditView, AdjustmentPanel | Crop (8 handles), rotate, flip, adjustments (8 params) |
| **Image processing** | `:core:image` | BitmapService, GuidedFilter, ColorSpace | Decode, crop, stitch collage, guided filter decomposition, AGSL shaders, GPUImage fallback |
| **Layout engine** | `:core:model` | CollageLayout.kt | Normalized cell-based layouts, dynamic grid for 7+ photos |
| **Annotation models** | `:core:model` | Annotation.kt, AnnotationStyle.kt | 13 annotation types (line, rect, circle, ellipse, polygon, freehand, text, callout, numbered marker, dimension, angle, area, symbol, image overlay) |
| **Project model** | `:core:model` | Project.kt, Layer.kt, Geometry.kt | Full project structure with layers, calibration, transforms |
| **Design system** | `:core:ui` | TechNotesTheme, components | Monochrome theme, Material3, Jakarta Sans + IBM Plex Mono, 6 custom components |
| **Navigation** | `:app` | AppNavigation.kt | Manual routing: Canvas -> Picker -> Editor, single-photo auto-edit |
| **DI** | `:app` | AppModule.kt, TechNotesApplication.kt | Hilt with @HiltAndroidApp, @HiltViewModel |

### Adjustment Parameters (all working)

Brightness, Contrast, Saturation, Warmth, Tint, Highlights, Shadows, Sharpness

### Stub (Not Yet Implemented)

| Feature | Module | What Exists | What's Missing |
|---------|--------|-------------|----------------|
| **Drawing/annotation rendering** | `:core:drawing` | 4 empty class files | Stroke smoothing (Catmull-Rom), arrowheads, primitives, snapping |
| **Overlay compositing** | `:core:overlay` | 3 empty class files | Blend modes, mask rendering, overlay engine |
| **Undo/redo** | `:core:undo` | 1 empty class file | Command stack, history management |
| **Project persistence** | `:core:project` | Serializer (15 lines), 3 empty files | Repository impl, auto-save, file I/O |
| **Export** | `:feature:export` | 2 empty files | Share sheet, save-to-gallery, format options |
| **Annotation UI** | (no module yet) | Models in `:core:model` | Drawing canvas, selection, property editing |
| **Measurement tools** | (no module yet) | Models: DimensionLine, Angle, Area, ScaleCalibration | Calibration UI, real-world unit display |
| **Text annotations** | (no module yet) | Models: TextAnnotation, CalloutAnnotation | Text input, font selection, callout rendering |
| **Symbol library** | (no module yet) | Model: SymbolAnnotation with SymbolCategory enum | Symbol catalog, placement UI |

---

## Tests

| Module | Test File | Coverage |
|--------|-----------|----------|
| `:core:model` | CollageLayoutTest | Layout generation for 2-7+ photos |
| `:core:model` | GeometryTest | PointF, RectF operations |
| `:core:model` | PhotoAdjustmentsTest | Adjustment value get/set |
| `:core:image` | ColorSpaceTest | sRGB/linear conversions |
| `:core:image` | GuidedFilterTest | Guided filter decomposition |
| `:core:project` | SerializationTest | Project JSON round-trip |

No UI tests, no instrumented tests, no feature-module tests.

---

## Architecture Patterns

- **MVVM + Repository**: ViewModels expose `StateFlow`, repositories abstract data access
- **Hilt DI**: `@HiltAndroidApp`, `@HiltViewModel`, `@Inject` constructor injection
- **Manual navigation**: State flags in `AppNavigation.kt`, no Compose Navigation library
- **Sandbox-first pipeline**: Build in `:sandbox`, verify, then `/port` to `:feature:*`
- **Interop views**: `EditableTouchImageView` and `WholeImageEditView` are Android Views embedded in Compose via `AndroidView`

---

## Build Configuration

- **buildSrc/AppConfig.kt**: Shared constants (SDK versions, app ID, JVM target)
- **Version catalog**: `gradle/libs.versions.toml` for dependency management
- **JitPack**: Used for GPUImage library
- **Compose BOM**: Centralized Compose versioning
- **KSP**: For Hilt annotation processing

---

## Largest Files (complexity hotspots)

| File | Lines | Notes |
|------|-------|-------|
| EditableTouchImageView.kt | 969 | Multi-photo touch interaction, drag-to-reorder, crop handles |
| BitmapService.kt | 594 | Central bitmap operations hub |
| CanvasScreen.kt | 388 | Canvas composable with toolbar and permissions |
| CollageLayout.kt | 329 | Layout definitions and factory |
| PhotoPickerScreen.kt | 260 | Picker UI with 4 views |
| WholeImageEditScreen.kt | 255 | Editor composable with tool switching |
| CanvasViewModel.kt | 254 | Canvas state management |

---

## What's Next (Roadmap Gaps)

The app has a complete **photo collage + editing pipeline** but the core annotation domain is entirely unimplemented. The gap between current state and a working annotation app:

1. **Drawing engine** (`core:drawing`) - render annotations on canvas
2. **Annotation UI** - touch-based creation and selection of annotation shapes
3. **Undo/redo** (`core:undo`) - command stack for annotation edits
4. **Overlay compositing** (`core:overlay`) - blend annotations with photos
5. **Project persistence** (`core:project`) - save/load projects with annotations
6. **Export** (`feature:export`) - share annotated images
7. **Measurement tools** - calibration workflow, dimension display
8. **Text/callout tools** - text input and positioning
9. **Symbol library** - predefined technical symbols

The data models for all of these are already defined in `:core:model`.
