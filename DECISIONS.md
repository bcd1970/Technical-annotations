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
