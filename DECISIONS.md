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
