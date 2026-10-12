# Technical Annotations — App Features & Interaction Flow

## Screens

### 1. Canvas Screen (Main Screen)
The primary screen. Displays a single photo or a stitched collage on a zoomable, pannable canvas.

- Pinch-to-zoom, double-tap to toggle fit/fill, fling with momentum (TouchImageView)
- Edge-to-edge display with system bar insets
- FAB opens the photo picker (requests storage permission on first tap)
- Long-press a photo in a collage enters edit mode
- Collage thumbnail bar at the bottom shows a miniature of the full collage with a viewport indicator when zoomed

### 2. Photo Picker Screen
Custom MediaStore-based picker (not the system picker).

- **Recent view** — 3-column grid of all device photos sorted by date
- **Albums view** — 2-column grid of album cards (cover photo, name, count)
- **Album photos view** — 3-column grid of photos within a selected album
- **Photo preview** — full-screen HorizontalPager with pinch-to-zoom and swipe; page counter at the top; pager disabled while zoomed

Selection behavior:
- Tap toggles selection; long-press opens preview
- Selected photos strip at the bottom with animated entry (300ms bounce-in) and exit (150ms shrink/slide/fade)
- Primary FAB: Edit icon for 1 photo, Dashboard icon with count badge for 2+
- Secondary FAB: toggles between Recent and Albums view

### 3. Whole-Image Edit Screen
Full-image editor launched from the per-photo edit toolbar.

- **Flip** — horizontal and vertical, instant apply
- **Rotate** — 90-degree increments, instant apply
- **Crop** — 8-handle overlay (4 corners + 4 midpoints), drag to resize, move entire rect; dimmed area outside crop; confirm bakes into source bitmap
- **Adjust** — tabbed panel with FilterChips and slider per adjustment type:
  - Brightness, Contrast, Saturation, Warmth, Tint, Highlights, Shadows, Sharpness
  - All range from -100 to +100 (Sharpness 0 to 100)
  - Live preview at 10-20fps during slider drag
  - Reset button returns all to defaults
- Back/Done returns edited bitmap to the canvas

---

## Per-Photo Editing (Collage Edit Mode)

Long-press any photo in a collage to enter edit mode. Active photo gets a cyan border; inactive photos get white borders; all others are dimmed.

### Toolbar actions (8 buttons)

| Action | Behavior |
|--------|----------|
| Flip Horizontal | Toggles horizontal mirror on active photo |
| Flip Vertical | Toggles vertical mirror on active photo |
| Rotate 90 | Cycles rotation: 0 -> 90 -> 180 -> 270 |
| Crop | Opens 8-handle crop overlay on active photo; normalized 0-1 rect relative to photo bounds |
| Pan | Grid layouts only — drag to reposition the visible portion of a cropped photo within its cell |
| Add Photo | Opens picker in "add to collage" mode; appends selected photos |
| Edit Whole Image | Opens WholeImageEditScreen for the entire collage bitmap |
| Remove Photo | Deletes active photo; falls back to single photo if < 2 remain, or empty canvas if 0 |

Active tool buttons are tinted blue; inactive are white.

### Crop mode toolbar
Replaces the edit toolbar with Cancel and Confirm buttons. Re-entering crop clears any existing crop first.

### Drag-to-reorder
- **Horizontal layouts** — 1D horizontal drag; swap on center-to-center crossing; haptic tick per swap
- **Grid layouts** — 2D any-direction drag with cell-index-based slot swapping
- Frame-based animation engine (immune to system animator_duration_scale)
- 80ms dim/lift on drag start, 120ms slide animation per swap
- Reorder disabled when zoomed or during multi-touch

---

## Collage Layouts

### Layout system
- CollageLayout model: list of normalized 0-1 cells + aspect ratio
- `aspectRatio = 0` preserves horizontal-row stitching (legacy behavior)
- `aspectRatio > 0` enables grid layouts with center-crop + pan offsets

### Layout catalog
- 2 photos: 3 layout options
- 3 photos: multiple options
- 4-6 photos: multiple options per count
- 7+ photos: dynamically generated grids

### Layout selector
Horizontal scrollable row of 48x48 layout previews with colored rectangles. Active layout highlighted with cyan border.

---

## Image Processing

### Bitmap handling
- Two-pass decode: bounds-only pass to compute inSampleSize, then decode at ~2x display resolution
- In-memory bitmap cache keyed by URI to avoid redundant decodes
- Transform pipeline per photo: rotate -> flip -> crop

### Collage stitching
- Horizontal row: uniform height (min of all heights), no upscaling
- Grid: center-crop with panX/panY offsets (default 0.5 = center)
- Returns stitched bitmap + per-photo pixel bounds + visible rects (normalized 0-1)

### Adjustment pipeline (Android 13+)
GPU-accelerated via AGSL RuntimeShader:
- HSL color space conversions
- LogC tone curve for contrast
- Chromatic white balance (warmth/tint via D65 color space)
- Guided filter decomposition for highlight/shadow masks with detail-aware remapping
- Adaptive 9-tap Gaussian unsharp mask sharpening with tone masking and halo clamping
- Film grain dithering in shadows to prevent banding
- sRGB <-> Linear gamma conversions
- Luminance-ratio color preservation (no hue shift)

### Adjustment pipeline (Android < 13)
Falls back to GPUImage library (CPU-based) with gamma-curve algorithms.

### Guided filter base/detail computation
- Pure Kotlin implementation in :core module using DoubleArray integral images
- RGBA_F16 bitmap encoding for base/detail/luminance textures
- ~120ms CPU computation with fallback path and seamless upgrade via LaunchedEffect

---

## Interaction Flow

```
App Launch
  |
  v
Canvas Screen (empty)
  |-- Permission request (READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE)
  |-- Tap FAB
  |
  v
Photo Picker Screen
  |-- Browse: Recent photos / Albums / Album photos
  |-- Long-press photo -> Full-screen preview (swipe between photos)
  |-- Tap photos to select (selection strip appears at bottom)
  |
  |-- Select 1 photo -> Tap Edit FAB
  |   |
  |   v
  |   Canvas Screen (single photo)
  |   |-- Pinch-to-zoom, double-tap fit/fill, fling
  |   |-- Long-press -> Edit mode
  |   |   |-- Flip H/V, Rotate 90, Crop, Edit Whole Image
  |   |   |-- Tap outside or back -> Exit edit mode
  |   |
  |   |-- Edit Whole Image ->
  |       |
  |       v
  |       Whole-Image Edit Screen
  |       |-- Flip, Rotate, Crop
  |       |-- Adjust: Brightness, Contrast, Saturation, Warmth,
  |       |           Tint, Highlights, Shadows, Sharpness
  |       |-- Done -> Return to canvas with edits applied
  |
  |-- Select 2+ photos -> Tap Dashboard FAB
      |
      v
      Canvas Screen (collage)
      |-- Collage thumbnail bar at bottom (viewport indicator when zoomed)
      |-- Layout selector (if multiple layouts available for photo count)
      |-- Tap layout preview -> Switch collage layout
      |
      |-- Long-press any photo -> Edit mode
          |
          |-- Per-photo toolbar appears:
          |   Flip H | Flip V | Rotate | Crop | Pan | Add | Edit | Remove
          |
          |-- Tap another photo -> Switch active selection
          |-- Drag photo -> Reorder (1D horizontal or 2D grid)
          |
          |-- Crop -> 8-handle overlay
          |   |-- Drag handles or move rect
          |   |-- Confirm -> Apply crop (normalized 0-1)
          |   |-- Cancel -> Discard
          |
          |-- Pan (grid only) -> Drag to reposition visible area
          |
          |-- Add Photo -> Picker (add mode) -> Append to collage
          |
          |-- Remove Photo -> Delete from collage
          |   |-- If 1 remains -> Switch to single photo view
          |   |-- If 0 remain -> Empty canvas
          |
          |-- Edit Whole Image -> Whole-Image Edit Screen
          |
          |-- Back -> Exit edit mode
```

---

## Architecture

| Layer | Components |
|-------|-----------|
| Activity | MainActivity (single activity, Hilt, Compose) |
| ViewModels | CanvasViewModel (collage state, transforms, edit mode), PhotoPickerViewModel (media queries, selection) |
| Repositories | MediaRepository / MediaRepositoryImpl (MediaStore queries on IO dispatcher) |
| Services | BitmapService (decode, transform, stitch, adjustments) |
| Data Models | PhotoTransform, CollageResult, CollageLayout, PhotoAdjustments, ImageEditState, Album, Photo |
| UI Screens | CanvasScreen, PhotoPickerScreen, WholeImageEditScreen |
| UI Components | CollageThumbBar, LayoutSelector, PhotoGrid, AlbumGrid, PhotoPreview, SelectedPhotosStrip, AdjustmentPanel |
| Custom Views | EditableTouchImageView (edit/crop/reorder/pan overlays), WholeImageEditView (single-image crop), TouchImageViewExt |
| Core Module | CollageLayout model, PhotoAdjustments, GuidedFilter (pure Kotlin) |
| DI | Hilt — AppModule binds MediaRepository |

---

## Permissions

- `READ_MEDIA_IMAGES` (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android < 13, maxSdkVersion 32)
