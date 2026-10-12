# Session 03 — Drawing Surface + Coordinate Layer

## GOAL

Create the annotation drawing surface as a transparent overlay on top of the existing photo canvas, sharing the same `viewMatrix` so all coordinates live in image-space. Implement gesture routing that dispatches touch events to the correct handler based on current mode and active tool.

## DEPENDS ON

- **S01 (Mode System)** — `AppMode` enum, `currentMode` StateFlow, `setAppMode()` on EditableTouchImageView
- **S02 (Undo/Redo)** — `UndoManager` injection (drawing actions will push to undo stack)

## SPEC REFERENCES

Read before coding:
- `photo-overlay-coordinate-system.md` (entire document — this IS the spec for this session)
- `INTERACTION-SPEC.md` §8 (Gesture Vocabulary & Routing)
- `INTERACTION-SPEC.md` §18 (Chrome Auto-Hide Behavior)
- `IMPLEMENTATION-BLUEPRINT.md` Part 2: P-01 (Coordinate System — Image-Space Is Law)

## EXISTING CODE TO INTEGRATE WITH

| File | Module | What It Does | Integration Notes |
|------|--------|-------------|-------------------|
| `EditableTouchImageView.kt` | `:feature:canvas` | 969 lines — multi-photo touch, drag-to-reorder, crop handles. Has its own matrix system for per-photo transforms. | The annotation drawing surface sits ON TOP of this view, sharing the same viewMatrix. Do NOT replace this view — layer above it. |
| `CanvasScreen.kt` | `:feature:canvas` | Compose screen with AndroidView embedding | Add the annotation surface as a second AndroidView layered on top |
| `CanvasViewModel.kt` | `:feature:canvas` | State management | Add active tool state, drawing state, annotation list |
| `BitmapService.kt` | `:core:image` | Bitmap operations | Used by existing view; annotation surface uses same bitmaps for coordinate reference |
| `Annotation.kt` | `:core:model` | 13 annotation type models already defined | These are the data classes that the drawing surface will render |

**CRITICAL DECISION:** The existing `EditableTouchImageView` already has working zoom/pan/fling with its own matrix. The annotation surface must SHARE that matrix, not create its own. Two approaches:

1. **Overlay View approach:** A transparent `AnnotationSurfaceView` sits on top of `EditableTouchImageView` in a FrameLayout. It receives the viewMatrix from the photo view via a callback. Touch events pass through to the photo view when not drawing.
2. **Integrated approach:** Extend `EditableTouchImageView` to also draw annotations in its `onDraw()`.

Approach 2 is cleaner (one matrix, one touch handler) but modifies the 969-line file heavily. Approach 1 is safer but requires matrix synchronization. **Use approach 2** — add annotation rendering to the existing view's draw pipeline and add gesture routing to its touch handler. This avoids matrix sync issues.

## DELIVERABLES

### Core Drawing Layer (`:core:drawing`)

1. **`DrawableElement` interface** — bridge between data models (`:core:model`) and rendering
   ```kotlin
   interface DrawableElement {
       val element: DrawingElement  // from :core:model
       fun draw(canvas: Canvas, zoom: Float)
       fun hitTest(point: PointF, hitRadius: Float): Boolean
       fun getBounds(): RectF
   }
   ```

2. **`RenderUtils` object** — scale-aware rendering helpers
   - `effectiveStrokeWidth(logicalWidth, currentZoom)` — clamp to 1px min, 2× max on screen
   - `effectiveTextSize(logicalSizePx, currentZoom)` — clamp 8sp–32sp on screen
   - `hitTestRadius(imageRadius, currentZoom, density)` — ensure 44dp minimum touch target

3. **`AnnotationRenderer` object** — dispatches draw calls for each element type
   - For now: only render a `TestDrawingElement` (circle at a point) to validate the coordinate system
   - Real primitives come in S09–S17

### Coordinate System (in `EditableTouchImageView`)

4. **`screenToImage(screenX, screenY): PointF`** — convert touch to image-space via inverseMatrix
5. **`imageToScreen(imageX, imageY): PointF`** — convert image-space to screen for HUD positioning
6. **`updateInverseMatrix()`** — called every time viewMatrix changes (zoom/pan/fling)

### Annotation Rendering Pipeline (in `EditableTouchImageView`)

7. **Layer rendering order in `onDraw()`:**
   ```
   canvas.save()
   canvas.concat(viewMatrix)
     Layer 0: Photo bitmap (existing)
     Layer 1: Completed annotations (new — iterate element list)
     Layer 2: Active drawing preview (new — current gesture element)
   canvas.restore()
     Layer 3: HUD — selection handles, snap guides (screen-space, outside matrix)
   ```

8. **Annotation cache bitmap** — completed annotations drawn to a cached bitmap, only rebuilt when annotations change. Active drawing element redraws every frame during gesture.

### Gesture Router

9. **`GestureRouter` class** — decides what a touch event does based on mode + tool + pointer count
   ```
   TWO+ FINGERS: Always zoom/pan (existing behavior), never draw. No exceptions.
   
   ONE FINGER (BROWSE mode):
     - Existing behavior: tap-to-select photo, drag-to-reorder, long-press-to-edit
   
   ONE FINGER (ANNOTATE mode):
     - No tool active: pan canvas
     - SELECT tool: hit-test → select/move element, or pan if empty space
     - TEXT tool: place text cursor at tap point
     - Any draw tool: route to tool's gesture group (two-point, tap-place, etc.)
   
   ONE FINGER (EDIT mode):
     - Existing per-photo editing behavior (flip, rotate, crop handles)
   ```

10. **`isDrawing` state** — exposed to CanvasViewModel for chrome auto-hide (S05) and undo pill auto-hide (S02)
    - Set `true` on ACTION_DOWN + 10dp movement with a drawing tool active
    - Set `false` on ACTION_UP
    - Only in ANNOTATE mode

### Active Tool State

11. **`ActiveTool` sealed class** in `:core:model`
    ```kotlin
    sealed class ActiveTool {
        object None : ActiveTool()         // No tool selected — one-finger pans
        object Select : ActiveTool()       // Selection mode
        object Text : ActiveTool()         // Text placement mode
        data class Draw(val toolId: Int) : ActiveTool()  // Drawing tool by primitive ID
    }
    ```

12. **`CanvasViewModel.activeTool: StateFlow<ActiveTool>`** — drives gesture routing

### Annotations State

13. **`CanvasViewModel.annotations: StateFlow<List<DrawableElement>>`** — list of all placed annotations
14. **`CanvasViewModel.selectedElement: StateFlow<DrawableElement?>`** — currently selected annotation
15. **`CanvasViewModel.activeDrawing: StateFlow<DrawableElement?>`** — element being drawn (live preview)

## DATA MODELS

```kotlin
// In :core:model — new file ActiveTool.kt
sealed class ActiveTool {
    object None : ActiveTool()
    object Select : ActiveTool()
    object Text : ActiveTool()
    data class Draw(val toolId: Int, val toolName: String) : ActiveTool()
}
```

```kotlin
// In :core:drawing — new file DrawableElement.kt
/**
 * Bridge between data model (DrawingElement) and canvas rendering.
 * Each concrete primitive (line, circle, dimension, etc.) implements this.
 */
interface DrawableElement {
    val id: String
    val element: DrawingElement
    
    /** Draw this element on a canvas that already has viewMatrix applied. */
    fun draw(canvas: Canvas, zoom: Float)
    
    /** Draw selection handles and bounding box (screen-space, called outside matrix). */
    fun drawHandles(canvas: Canvas, viewMatrix: Matrix)
    
    /** Test if a point (in image-space) hits this element. */
    fun hitTest(point: PointF, hitRadius: Float): Boolean
    
    /** Get axis-aligned bounding box in image-space. */
    fun getBounds(): RectF
}
```

```kotlin
// In :core:drawing — new file RenderUtils.kt
object RenderUtils {
    
    /**
     * Clamp stroke width so lines are always visible but never absurdly thick.
     * @param logicalWidth stored width in image pixels
     * @param currentZoom current viewMatrix scale factor
     * @return width to use in Paint.strokeWidth (in image-space units)
     */
    fun effectiveStrokeWidth(logicalWidth: Float, currentZoom: Float): Float {
        val screenWidth = logicalWidth * currentZoom
        val minScreenPx = 1f
        val maxScreenPx = logicalWidth * 2f
        return when {
            screenWidth < minScreenPx -> minScreenPx / currentZoom
            screenWidth > maxScreenPx -> maxScreenPx / currentZoom
            else -> logicalWidth
        }
    }
    
    /**
     * Clamp text size to readable range regardless of zoom.
     */
    fun effectiveTextSize(logicalSizePx: Float, currentZoom: Float): Float {
        val screenSizePx = logicalSizePx * currentZoom
        val minScreenSp = 8f
        val maxScreenSp = 32f
        return when {
            screenSizePx < minScreenSp -> minScreenSp / currentZoom
            screenSizePx > maxScreenSp -> maxScreenSp / currentZoom
            else -> logicalSizePx
        }
    }
    
    /**
     * Ensure hit targets are at least 44dp regardless of zoom.
     */
    fun hitTestRadius(imageRadius: Float, currentZoom: Float, density: Float): Float {
        val minScreenDp = 22f  // 44dp diameter
        val minImagePx = minScreenDp * density / currentZoom
        return maxOf(imageRadius, minImagePx)
    }
}
```

```kotlin
// In :core:drawing — new file GestureRouter.kt
/**
 * Determines what action a touch event should trigger.
 * Called by EditableTouchImageView on every touch event.
 */
object GestureRouter {
    
    enum class GestureAction {
        ZOOM_PAN,           // Two-finger: always zoom/pan
        BROWSE_INTERACT,    // Browse mode existing behavior
        PAN_CANVAS,         // One-finger pan (no tool or empty space)
        SELECT_ELEMENT,     // Hit an existing element
        MOVE_ELEMENT,       // Drag selected element
        DRAW,               // Active draw tool gesture
        PLACE_TEXT,         // Text tool tap
        EDIT_PHOTO          // Edit mode photo manipulation
    }
    
    fun route(
        pointerCount: Int,
        mode: AppMode,
        activeTool: ActiveTool,
        hitElement: DrawableElement?,  // null if tap hit empty space
        isElementSelected: Boolean
    ): GestureAction {
        // Two+ fingers: ALWAYS zoom/pan
        if (pointerCount >= 2) return GestureAction.ZOOM_PAN
        
        // Single finger routing by mode
        return when (mode) {
            AppMode.BROWSE -> GestureAction.BROWSE_INTERACT
            AppMode.EDIT -> GestureAction.EDIT_PHOTO
            AppMode.ANNOTATE -> when (activeTool) {
                is ActiveTool.None -> GestureAction.PAN_CANVAS
                is ActiveTool.Select -> {
                    when {
                        hitElement != null && !isElementSelected -> GestureAction.SELECT_ELEMENT
                        hitElement != null && isElementSelected -> GestureAction.MOVE_ELEMENT
                        else -> GestureAction.PAN_CANVAS
                    }
                }
                is ActiveTool.Text -> GestureAction.PLACE_TEXT
                is ActiveTool.Draw -> GestureAction.DRAW
            }
        }
    }
}
```

## KEY IMPLEMENTATION DETAILS

### Integrating with EditableTouchImageView

The existing view has its own matrix for photo rendering. The key integration points:

```kotlin
// Add to EditableTouchImageView:

// === ANNOTATION STATE ===
private var annotations: List<DrawableElement> = emptyList()
private var activeDrawing: DrawableElement? = null
private var selectedElement: DrawableElement? = null
private var annotationCacheBitmap: Bitmap? = null
private var annotationCacheDirty = true

// === COORDINATE CONVERSION ===
private val inverseMatrix = Matrix()

fun screenToImage(screenX: Float, screenY: Float): PointF {
    val pts = floatArrayOf(screenX, screenY)
    inverseMatrix.mapPoints(pts)
    return PointF(pts[0], pts[1])
}

fun imageToScreen(imageX: Float, imageY: Float): PointF {
    val pts = floatArrayOf(imageX, imageY)
    // Use the existing view matrix (already manages zoom/pan)
    getImageMatrix().mapPoints(pts)  // or however the existing matrix is exposed
    return PointF(pts[0], pts[1])
}

// Call this every time the view matrix changes (zoom, pan, fling)
private fun updateInverseMatrix() {
    getImageMatrix().invert(inverseMatrix)
}
```

### Draw Pipeline Addition

```kotlin
// In EditableTouchImageView.onDraw():
// AFTER existing photo drawing, BEFORE existing HUD:

override fun onDraw(canvas: Canvas) {
    // === EXISTING: Draw photo with matrix ===
    super.onDraw(canvas)  // or however photos are currently drawn
    
    // === NEW: Draw annotations in same matrix space ===
    canvas.save()
    canvas.concat(getViewMatrix())  // Same matrix as photo
    
    // Completed annotations (from cache or direct)
    for (element in annotations) {
        element.draw(canvas, getCurrentZoom())
    }
    
    // Active drawing preview (during gesture)
    activeDrawing?.draw(canvas, getCurrentZoom())
    
    canvas.restore()
    
    // === NEW: HUD layer (screen-space) ===
    selectedElement?.drawHandles(canvas, getViewMatrix())
}
```

### Touch Event Routing

```kotlin
// In EditableTouchImageView.onTouchEvent():
// ADD gesture routing before existing touch handling

override fun onTouchEvent(event: MotionEvent): Boolean {
    // Always update inverse matrix
    updateInverseMatrix()
    
    val imagePoint = screenToImage(event.x, event.y)
    val hitElement = findElementAt(imagePoint, getCurrentZoom())
    
    val action = GestureRouter.route(
        pointerCount = event.pointerCount,
        mode = currentAppMode,
        activeTool = currentActiveTool,
        hitElement = hitElement,
        isElementSelected = selectedElement != null
    )
    
    return when (action) {
        GestureAction.ZOOM_PAN -> {
            // Existing two-finger handling
            handleExistingZoomPan(event)
        }
        GestureAction.BROWSE_INTERACT -> {
            // Existing browse mode handling (drag-to-reorder, etc.)
            handleExistingBrowse(event)
        }
        GestureAction.EDIT_PHOTO -> {
            // Existing edit mode handling
            handleExistingEdit(event)
        }
        GestureAction.PAN_CANVAS -> {
            // Reuse existing pan logic
            handleExistingPan(event)
        }
        GestureAction.DRAW -> {
            // NEW: forward to draw handler (stubbed — real tools in S09+)
            onDrawingStateChanged?.invoke(true)  // notify isDrawing for auto-hide
            handleDraw(event, imagePoint)
        }
        GestureAction.SELECT_ELEMENT -> {
            handleSelectElement(hitElement!!, imagePoint)
            true
        }
        GestureAction.MOVE_ELEMENT -> {
            handleMoveElement(event, imagePoint)
        }
        GestureAction.PLACE_TEXT -> {
            handlePlaceText(imagePoint)
            true
        }
    }
}
```

### Element Hit Testing

```kotlin
/**
 * Find the topmost annotation element at a given image-space point.
 * Uses compensated hit radius for touch accessibility.
 */
private fun findElementAt(imagePoint: PointF, currentZoom: Float): DrawableElement? {
    val hitRadius = RenderUtils.hitTestRadius(
        imageRadius = 5f,  // base radius in image pixels
        currentZoom = currentZoom,
        density = resources.displayMetrics.density
    )
    // Search in reverse order (topmost first)
    return annotations.lastOrNull { it.hitTest(imagePoint, hitRadius) }
}
```

### isDrawing State Callback

```kotlin
// In EditableTouchImageView:
var onDrawingStateChanged: ((Boolean) -> Unit)? = null

// Set true when: ANNOTATE mode + draw tool + ACTION_DOWN + 10dp movement
// Set false when: ACTION_UP

private var drawStartPoint: PointF? = null
private val DRAW_THRESHOLD_DP = 10f

private fun handleDraw(event: MotionEvent, imagePoint: PointF) {
    when (event.actionMasked) {
        ACTION_DOWN -> {
            drawStartPoint = PointF(event.x, event.y)
        }
        ACTION_MOVE -> {
            drawStartPoint?.let { start ->
                val dx = event.x - start.x
                val dy = event.y - start.y
                val distDp = sqrt(dx * dx + dy * dy) / resources.displayMetrics.density
                if (distDp > DRAW_THRESHOLD_DP) {
                    onDrawingStateChanged?.invoke(true)
                    drawStartPoint = null  // only fire once
                }
            }
            // Forward to active tool's gesture handler (stubbed)
        }
        ACTION_UP -> {
            onDrawingStateChanged?.invoke(false)
            drawStartPoint = null
        }
    }
}
```

### TestDrawingElement (for validation)

```kotlin
// In :core:drawing — TestDrawingElement.kt (sandbox testing only)
/**
 * Simple circle annotation for validating coordinate system.
 * Tap canvas in ANNOTATE mode with draw tool → places a circle at tap point.
 * Circle should stay anchored to photo when zooming/panning.
 */
class TestCircleElement(
    override val id: String,
    val center: PointF,       // IMAGE-SPACE coordinates
    val radius: Float = 20f,  // IMAGE-SPACE pixels
    val color: Int = Color.RED
) : DrawableElement {
    
    override val element: DrawingElement get() = TODO("wire to real model later")
    
    override fun draw(canvas: Canvas, zoom: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = this@TestCircleElement.color
            style = Paint.Style.STROKE
            strokeWidth = RenderUtils.effectiveStrokeWidth(3f, zoom)
        }
        canvas.drawCircle(center.x, center.y, radius, paint)
    }
    
    override fun drawHandles(canvas: Canvas, viewMatrix: Matrix) {
        // Selection handles (screen-space) — stub
    }
    
    override fun hitTest(point: PointF, hitRadius: Float): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y
        val dist = sqrt(dx * dx + dy * dy)
        return abs(dist - radius) < hitRadius
    }
    
    override fun getBounds(): RectF = RectF(
        center.x - radius, center.y - radius,
        center.x + radius, center.y + radius
    )
}
```

## RESOLVED CONFLICTS

None specific to this session. The coordinate system spec (`photo-overlay-coordinate-system.md`) is unambiguous and has no conflicts with other specs.

**Key rule from P-01:** ALL coordinates stored in image-space. Every touch converted via `inverseMatrix` before storing. No exceptions.

## STUB REQUIREMENTS

| Feature | What to stub | Session that implements |
|---------|-------------|----------------------|
| Real drawing primitives | `TestCircleElement` for now — real lines/shapes in S09+ | S09–S17 |
| Chrome auto-hide notification | `onDrawingStateChanged` callback fires — chrome hide logic in S05 | S05 |
| Selection handles | `drawHandles()` empty — real handles in S09 | S09 |
| Text placement | `handlePlaceText()` logs imagePoint — real text tool in S12 | S12 |
| Annotation cache bitmap | Direct rendering for now — caching optimization when annotation count grows | S09+ |
| Snap guides in HUD layer | HUD layer exists but empty — snap visuals in S08 | S08 |

## TEST CRITERIA

1. **Coordinate conversion roundtrip:** Tap canvas, log `screenToImage()` result. Apply `imageToScreen()` to result. Compare with original screen point. Must match within 1px.
2. **Annotation anchoring:** Place test circle at a photo point. Pinch-zoom 3× — circle stays at same photo location. Pan around — circle moves with photo.
3. **Zoom-level stroke width:** Place test circle. Zoom way out — stroke stays visible (minimum 1px). Zoom way in — stroke doesn't become absurdly thick.
4. **Gesture routing (BROWSE):** In Browse mode, tap/drag works exactly as before (no regression on photo selection, drag-to-reorder).
5. **Gesture routing (ANNOTATE + Draw):** In Annotate mode with draw tool, tap canvas → test circle placed at correct image-space coordinates.
6. **Gesture routing (ANNOTATE + no tool):** In Annotate mode with no tool, one-finger drag pans canvas (same as Browse).
7. **Two-finger always zoom/pan:** In Annotate mode with draw tool, two-finger pinch zooms (doesn't draw).
8. **isDrawing state:** In Annotate mode, start drawing gesture → `isDrawing = true` fires after 10dp movement. Release → `isDrawing = false`.
9. **Multiple annotations:** Place 5 test circles. All render. All anchor correctly on zoom/pan.
10. **Hit testing:** Tap on existing circle → selected (or logged). Tap empty space → nothing selected.
11. **Rotate device:** Annotations survive configuration change. Positions correct after rotation.
12. **Performance:** 10 annotations, continuous zoom/pan → no visible jank (60fps target).

## FILES TO CREATE / MODIFY

### Create

| Module | Package | File | Purpose |
|--------|---------|------|---------|
| `:core:drawing` | `com.bcd.technotes.core.drawing` | `DrawableElement.kt` | Rendering interface for annotations |
| `:core:drawing` | `com.bcd.technotes.core.drawing` | `RenderUtils.kt` | Scale-aware stroke/text/hit-target utilities |
| `:core:drawing` | `com.bcd.technotes.core.drawing` | `GestureRouter.kt` | Touch event routing by mode + tool |
| `:core:drawing` | `com.bcd.technotes.core.drawing` | `TestCircleElement.kt` | Sandbox test element for coordinate validation |
| `:core:model` | `com.bcd.technotes.core.model` | `ActiveTool.kt` | Sealed class for active tool state |

### Modify

| Module | File | Changes |
|--------|------|---------|
| `:feature:canvas` | `EditableTouchImageView.kt` | Add `screenToImage()`, `imageToScreen()`, `inverseMatrix`, annotation list + rendering in `onDraw()`, gesture routing in `onTouchEvent()`, `isDrawing` callback, `findElementAt()` |
| `:feature:canvas` | `CanvasViewModel.kt` | Add `activeTool: StateFlow<ActiveTool>`, `annotations: StateFlow<List<DrawableElement>>`, `selectedElement`, `isDrawing`, test action for placing circles |
| `:feature:canvas` | `CanvasScreen.kt` | Wire `isDrawing` from view to ViewModel, pass activeTool to view |
| `:core:drawing` | `build.gradle.kts` | Ensure dependency on `:core:model` |

### Unit Tests

| Module | Test File | What It Tests |
|--------|-----------|--------------|
| `:core:drawing` | `RenderUtilsTest.kt` | Stroke width clamping at extreme zoom, text size bounds, hit radius minimum |
| `:core:drawing` | `GestureRouterTest.kt` | All routing combinations: 2-finger always ZOOM_PAN, browse mode always BROWSE_INTERACT, annotate mode routes by tool |

---

*Session 03 Blueprint — v1.0*
