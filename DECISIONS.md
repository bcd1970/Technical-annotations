# Decision Log

Each feature implementation tracks decisions, attempts, and outcomes.

---

## Format

### [Feature Name] — [Date]

**Goal:** What we're trying to achieve

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Description | SUCCESS / FAILED | Why it worked or didn't |

---

## Phase 1 — Project Setup

### Project Structure — 2026-03-23

**Goal:** Set up multi-module Android project for annotation app

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Multi-module: `:app`, `:sandbox`, `:core` | SUCCESS | Clean separation, sandbox-first workflow |
| 2 | `:core` as pure Kotlin (no Android deps) | SUCCESS | Enables fast JVM unit tests |
| 3 | Hilt for DI (matching existing project) | SUCCESS | Builds clean, familiar pattern |
| 4 | kotlinx.serialization for project files | SUCCESS | Sealed class polymorphism works, 14 annotation types round-trip correctly |

### Sandbox Initial UI — 2026-03-23

**Goal:** Create sandbox app for testing features

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Hardcoded test shapes on canvas | FAILED | No purpose, confusing to the user — random graphics with no context |
| 2 | "Pick a Photo" button at top-left | FAILED | Hard to reach with thumb on phone |
| 3 | Column layout (button on top, canvas below) | FAILED | Wastes space, not how an annotation app should feel |
| 4 | Clean canvas + FAB at bottom-right | SUCCESS | Minimal, thumb-friendly, shows only what's needed |

### System Bar Insets — 2026-03-24

**Goal:** FAB and future bottom bars must respect Android system navigation bar and status bar

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | `navigationBarsPadding()` on FAB | SUCCESS | FAB now sits above Android nav bar, canvas stays full-screen edge-to-edge |

### Photo Picker — 2026-03-24

**Goal:** Show albums and photos in a custom picker with smooth scrolling

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | System PickVisualMedia vs custom MediaStore picker | Custom chosen | System picker can't be customized; custom gives full UI control over albums/photos |
| 2 | Custom picker with MediaStore + Coil 3 thumbnails | SUCCESS | Albums in 2-col grid, photos in 3-col grid, 256px thumbnails, stable keys for smooth scrolling |
| 3 | Permission: READ_MEDIA_IMAGES (API 33+) / READ_EXTERNAL_STORAGE (API 29-32) | SUCCESS | Required for custom picker to access device photos |
| 4 | Open picker in recent photos grid by default | SUCCESS | Faster to pick a photo — most users want a recent one |
| 5 | Albums FAB at bottom-right to switch views | SUCCESS | Albums sorted by most recently modified photo first (DATE_MODIFIED DESC query order) |
| 6 | Distinct FAB icons: Folder for albums, GridView for recents | SUCCESS | Clear visual distinction between navigation targets |
| 7 | Single photo preview via long-press on grid thumbnail | SUCCESS | HorizontalPager for swipe, full-screen black background, page counter |
| 8 | Long press on preview photo to select (not tap) | SUCCESS | Avoids false taps during swipe/zoom — intentional action only |
| 9 | GridView FAB in preview to return to grid | SUCCESS | Returns to the grid the preview was launched from (recents or album photos) |

### Pinch-to-Zoom & Pan — 2026-03-24

**Goal:** Proper pinch-zoom around fingers and pan in photo preview and canvas

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Telephoto library for photo preview in HorizontalPager | SUCCESS | Handles pager/zoom conflict, double-tap, centroid zoom, zoom reset on page change |
| 2 | Content-space zoom formula (first attempt) | FAILED | Mismatched coordinate spaces between formula and withTransform rendering |
| 3 | Screen-space zoom formula: (offset - centroid) * (newScale/oldScale) + centroid + pan | SUCCESS | Consistent with translate-then-scale rendering order |
| 4 | Double-tap to toggle 1x / 2.5x on canvas | SUCCESS | Zooms centered on tap point |
| 5 | clipToBounds on canvas | SUCCESS | Prevents drawing outside canvas area |

### Port to Main App — 2026-03-24

**Goal:** Port validated sandbox features to the main app

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Straight port with package rename | FAILED | Missed edge clamping — ported code without verifying all behaviors matched sandbox |
| 2 | Added port verification hook + CLAUDE.md rule | SUCCESS | PreToolUse hook on app/src/*.kt reminds to compare side-by-side before completing |
| 3 | Edge clamping added to both sandbox and app | SUCCESS | clampOffset() prevents image from being dragged off-screen, centers when smaller than canvas |
| 4 | Side-by-side verification after clamping port | SUCCESS | All logic matches: canvasSize, clampOffset, onSizeChanged, clamp calls in both handlers |
| 5 | Dynamic minScale for zoom-out clamping on canvas | SUCCESS | minScale = max(canvasW/imageW, canvasH/imageH) — image always fills screen |
| 6 | Preview zoom-out clamping | SUCCESS | Telephoto library handles this by default — no custom code needed |
| 7 | Side-by-side diff after minScale port | SUCCESS | Only diffs are unused imports in sandbox — all functional code identical |

### Fling/Momentum Implementation — 2026-03-24

**Goal:** Add smooth fling momentum to canvas pan (like swiping in a photo gallery)

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Custom awaitEachGesture + VelocityTracker tracking offset position | FAILED | VelocityTracker saw clamped positions → zero velocity |
| 2 | Same but tracking pan accumulator | FAILED | Velocity non-zero in logs but animateDecay block callback runs ONCE at end, not per-frame → instant jump |
| 3 | Same but tracking finger position | FAILED | Velocity correct but animateDecay block is end-callback, not per-frame — misunderstood API |
| 4 | Manual delay(16) loop with exponential decay | FAILED | Over-complicated, unreliable timing, abandoned before testing |
| 5 | Telephoto Modifier.zoomable() on Canvas | FAILED | Edge clamping broken, content location mapping wrong for Canvas composable |
| 6 | usuiat/Zoomable library (v2.4.0) | FAILED | Zero fling — library's onGestureEnd calls startFling but no visible momentum |
| 7 | Google's official pattern: while(true) + awaitPointerEventScope + drag + Animatable.animateDecay | FAILED | Fling existed but very rough/brisk — far from native scroll smoothness |
| 8 | Root cause: combinedClickable wrapper eating gestures in photo preview | FOUND | Removing it from preview showed telephoto/zoomable COULD work, but fling still brisk |
| 9 | splineBasedDecay instead of exponentialDecay on usuiat/Zoomable | FAILED | Zero visible change — library may not respect the parameter in v2.4.0 |
| 10 | **TouchImageView (native Android View via AndroidView)** | **SUCCESS** | MikeOrtiz/TouchImageView v3.7.1, Apache 2.0. Uses OverScroller.fling() — same engine as RecyclerView. Buttery smooth fling, zoom, edge clamping, double-tap. 10+ years battle-tested |

**Root cause of all Compose failures:** Compose's gesture/animation system (Animatable, animateDecay, VelocityTracker) does not produce the same fling quality as Android's native OverScroller. The OverScroller has been tuned over 15 years and is what makes all native Android scrolling feel smooth.

**Lesson:** For gestures that need native-quality feel (fling, scroll momentum), use Android's native View system wrapped in AndroidView rather than fighting Compose's gesture APIs.

### Preview Pager + Zoom Conflict — 2026-03-24

**Goal:** Prevent HorizontalPager from swiping pages when image is zoomed in

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Disable pager userScrollEnabled when TouchImageView.isZoomed | SUCCESS | OnTouchImageViewListener.onMove reports zoom state, pager respects it |

### Fit ↔ Fill Double-Tap Zoom — 2026-03-25

**Goal:** Double-tap toggles between fit (full image visible) and fill (image fills viewport) instead of default maxZoom

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | TouchImageView.doubleTapScale = fillScale / fitScale | SUCCESS | Calculated in post{} after image load. fillScale = max(vW/imgW, vH/imgH), fitScale = min(vW/imgW, vH/imgH). Applied to both canvas and preview |

### Port TouchImageView + Fit↔Fill to Main App — 2026-03-25

**Goal:** Replace Compose Canvas and Telephoto in main app with TouchImageView (matching sandbox)

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Replace Compose Canvas + custom gesture math with TouchImageView in CanvasScreen | SUCCESS | Removed all custom zoom/pan/clamp logic, offset/scale state. TouchImageView handles fling, zoom, edge clamping natively |
| 2 | Replace Telephoto ZoomableAsyncImage with TouchImageView in PhotoPreview | SUCCESS | Added isZoomed tracking, userScrollEnabled pager lock, doubleTapScale calculation — matches sandbox exactly |
| 3 | Side-by-side port verification | SUCCESS | All behaviors verified identical: TouchImageView setup, doubleTapScale, post{} blocks, zoom tracking, pager lock, long-press, page counter |

### Multi-Photo Selection — 2026-03-25

**Goal:** Add multi-photo selection to picker for collage workflow (tap toggles selection, editor FAB routes 1→canvas / 2+→collage)

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Tap toggles selection (was: select+exit), long-press still opens preview | SUCCESS | Selection state as Set<Long> with MutableMap<Long,Photo> cache for cross-view persistence |
| 2 | Checkbox + 30% overlay on selected grid photos | SUCCESS | CheckCircle icon at top-end, Color(0x4D000000) overlay — same pattern as PhotoCollageGlide |
| 3 | Selected photos thumbnail strip at bottom (Row + horizontalScroll) | SUCCESS | 52dp thumbnails with Coil, SmallFAB remove (x) buttons, sits below main content |
| 4 | Editor FAB + navigation FAB in horizontal Row above strip | SUCCESS | 1 photo = Edit icon, 2+ = Dashboard icon with count badge. Row layout avoids overlap with strip |
| 5 | Single tap in preview also toggles selection | SUCCESS | setOnClickListener on TouchImageView alongside existing setOnLongClickListener |
| 6 | App starts with recent photos grid (showPicker = true) | SUCCESS | Permission requested on launch via LaunchedEffect, then picker opens directly |
| 7 | Collage placeholder screen | SUCCESS | Dashboard icon + "X photos selected for collage" text when 2+ URIs confirmed |
| 8 | Title bar shows selection count | SUCCESS | "X selected" replaces "Recent" when selectedPhotoIds.isNotEmpty() |

### Thumbnail Strip Animations — 2026-03-25

**Goal:** Smooth pop-in animation for thumbnails and fling-style auto-scroll

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | LazyRow + Animatable.animateTo() with spring | FAILED | Animatable completes in 3ms — no MonotonicFrameClock driving frame-by-frame animation from coroutineScope.launch |
| 2 | LazyRow + animateFloatAsState with spring | FAILED | Also completes in one frame — graphicsLayer reads trigger instant resolution |
| 3 | LazyRow + AnimatedVisibility + MutableTransitionState | FAILED | LazyRow creates items off-screen, animation plays invisibly before scroll brings item into view |
| 4 | Row + AnimatedVisibility(visible=true) | FAILED | visible=true means "already visible" — no false→true transition to animate |
| 5 | Row + AnimatedVisibility + MutableTransitionState(false).apply{targetState=true} | FAILED | Transition completes in 35ms regardless of spring/tween params — AnimatedVisibility short-circuits in Row+horizontalScroll context |
| 6 | Row + updateTransition + animateFloat | FAILED | Same 40ms completion — Compose transition APIs all resolve instantly in this layout context |
| 7 | **Row + withFrameNanos manual frame loop + graphicsLayer** | **SUCCESS** | Manual loop with overshoot interpolation: 300ms pop-in (scale 0→1 + slide up + fade), 350ms decelerate auto-scroll. Bypasses all broken Compose animation APIs |

**Root cause of all Compose animation failures:** Compose's animation APIs (Animatable, animateFloatAsState, AnimatedVisibility, updateTransition) all resolve instantly when used with graphicsLayer inside a Row+horizontalScroll context. The withFrameNanos suspend function is the only reliable way to drive frame-by-frame animations in this scenario.

### Compliance & Security Audit Hook — 2026-03-25

**Goal:** Auto-remind to audit Google Play Store compliance and security when editing main app source files

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | PreToolUse hook on Edit/Write for app/src/*.kt with AUDIT.md tracking | SUCCESS | Checks permissions, secrets, SQL injection, PII logging, cleartext traffic. Findings logged as FIXED or DEFERRED in AUDIT.md |

### Port Multi-Photo Selection + Animations to Main App — 2026-03-25

**Goal:** Port all sandbox features (multi-select, thumbnail strip, confirm FAB, auto-permission, collage placeholder) to main app

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Port PhotoPickerScreen: multi-select state, selection overlay, FAB row, thumbnail strip with animations | SUCCESS | Full rewrite of app's PhotoPickerScreen to match sandbox — callback changed from onPhotoSelected(Uri) to onPhotosConfirmed(List<Uri>) |
| 2 | Port CanvasScreen: auto-permission, multi-photo handling, collage placeholder | SUCCESS | Added hasPermission state, LaunchedEffect auto-launch, selectedUris for multi-photo, CollagePlaceholder composable |
| 3 | Side-by-side verification | SUCCESS | Zero behavioral differences — every function, state variable, animation, gesture handler, and query function verified identical |
| 4 | Compliance & security audit | SUCCESS | No findings — parameterized queries, no secrets, no PII logging, correct permissions |
| 5 | Build + install both apps | SUCCESS | Both apps build clean, installed and launched on device |

### Preview Image Downsampling — 2026-03-25

**Goal:** Downsample large photos in preview to prevent OOM and improve pager swipe smoothness

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Two-pass BitmapFactory decode: bounds-only pass then inSampleSize decode at ~2x screen resolution | SUCCESS | Already in sandbox, now ported to app. Graceful fallback to setImageURI on exception |

### Code Simplification — 2026-03-25

**Goal:** Clean up code quality and efficiency issues found by review agents

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Extract navigateBack() in PhotoPickerScreen | SUCCESS | Duplicated 4-branch when block (BackHandler + TopAppBar + FAB) → single function, 3 call sites |
| 2 | Extract TouchImageView.updateDoubleTapScale() extension | SUCCESS | 8-line post{} block appeared 3x per module → one-liner call. New file: ui/util/TouchImageViewExt.kt |
| 3 | Async bitmap decode in CanvasScreen | SUCCESS | BitmapFactory.decodeStream moved from synchronous callback to LaunchedEffect + Dispatchers.IO via pendingUri state |

### Thumbnail Strip Exit Animation — 2026-03-25

**Goal:** Animate photo removal from thumbnail strip (shrink + slide down + fade out)

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Per-item isExiting state with LaunchedEffect(isExiting) key | FAILED | LaunchedEffect key change from false→true didn't reliably trigger the exit animation |
| 2 | Conditionally composed exit LaunchedEffect inside if(isExiting) | FAILED | Only works for X button click — grid deselection bypasses it because parent removes photo from list, destroying the composable before animation can play |
| 3 | **Parent-driven exit: photoCache + exitingIds + displayPhotos diffing** | **SUCCESS** | Strip maintains local photoCache of all seen photos and diffs selectedIds vs prevIds to detect removals. Removed photos stay in displayPhotos during 150ms exit animation. Works for both grid deselection and X button removal |

**Root cause of attempts 1-2:** When a photo is deselected via the grid, `selectedPhotoList` removes it immediately. The strip's `forEach` no longer includes it, so the `key(photo.id)` composable leaves composition, destroying all state and cancelling all LaunchedEffects. The exit animation never gets a chance to run.

**Solution:** The strip keeps its own display list that includes "exiting" photos sourced from a cache. The composable stays alive during the animation, then cleans itself up.

### Port Exit Animation + Simplification to Main App — 2026-03-25

**Goal:** Port thumbnail exit animation, navigateBack(), updateDoubleTapScale(), and async bitmap decode to main app

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Full /port workflow with side-by-side verification | SUCCESS | Zero behavioral differences confirmed. All composables, animations, query functions identical between sandbox and app |

### Double-Tap Zoom Fix (Preview Pager) — 2026-03-25

**Goal:** Double-tap should zoom exactly to fill (image edges reach screen edges) on every photo in the preview pager, not just the first 2-3

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Cap doubleTapScale at 2.5f | FAILED | Wrong fix — masked the bug. Fill IS the correct zoom for all aspect ratios |
| 2 | Calculate doubleTapScale immediately from bitmap dimensions + double-post fallback | FAILED | Works for first 2-3 preloaded pages, but beyond that view.width/height is 0 for newly created views — calculation silently skipped |
| 3 | **doOnLayout callback from androidx.core.view** | **SUCCESS** | Fires after view has dimensions AND after TouchImageView processes the new bitmap's layout. Guaranteed for both preloaded and newly created views |

**Root cause:** HorizontalPager preloads ~2-3 pages. Those views are already laid out so view.width/height are valid. Beyond that, new AndroidView instances are created on demand — view dimensions are 0 when the `update` block runs. `post{}` and even double-post fire before the view's first layout pass completes. `doOnLayout` correctly waits.

### Collage View — Horizontal Layout — 2026-03-25

**Goal:** Display 2+ selected photos as a horizontal collage in the canvas editor with uniform height, no padding, and full zoom/pan/fling support

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Stitch photos into single bitmap, display in TouchImageView | SUCCESS | Decode all photos with inSampleSize downsampling (~2x screen height), scale to uniform height (min of all heights — no upscaling), draw side-by-side on a Canvas, feed result bitmap to existing TouchImageView. Zoom, pan, fling, double-tap all work for free |

### Port Collage View to Main App — 2026-03-25

**Goal:** Port horizontal collage stitching from sandbox to main app

| # | Decision / Attempt | Outcome | Notes |
|---|-------------------|---------|-------|
| 1 | Replace CollagePlaceholder with stitchCollage + calculateInSampleSize, add collage LaunchedEffect, update imports | SUCCESS | Side-by-side verified — all functional code identical. No compliance/security findings. Both apps build and install clean |
