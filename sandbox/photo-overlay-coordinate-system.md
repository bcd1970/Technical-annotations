# Photo-Annotation Overlay — Coordinate System & Zoom/Pan Architecture

> **For Claude Code.** How annotations stay anchored to the photo when zooming, panning, and rotating.

---

## THE CORE PRINCIPLE

Annotations and the photo share ONE transformation matrix. Store all annotation coordinates in **image-space** (relative to the photo pixels). Apply the same zoom/pan Matrix to both the photo bitmap and the annotation canvas. They move together because they live in the same coordinate system.

```
┌─────────────────────────────────────────────┐
│                SCREEN                        │
│                                              │
│    ┌───────────────────────────┐             │
│    │    viewMatrix applied     │             │
│    │  ┌─────────────────────┐  │             │
│    │  │     PHOTO BITMAP    │  │             │
│    │  │                     │  │             │
│    │  │  ●───── dim ────●   │  │  ← annotations stored as
│    │  │  │  120.00       │  │  │    image-pixel coordinates
│    │  │  ●  leader text  │  │  │    (x=450, y=320) in photo space
│    │  │                     │  │             │
│    │  └─────────────────────┘  │             │
│    └───────────────────────────┘             │
│                                              │
└─────────────────────────────────────────────┘

When user zooms in 2×:
  - viewMatrix.postScale(2, 2, pivotX, pivotY)
  - Photo renders at 2× → annotations render at 2× 
  - They stay perfectly aligned because same matrix
```

---

## TWO COORDINATE SPACES

### Image Space (where annotations live)

All annotation points are stored in **image pixel coordinates**:
- Origin (0,0) = top-left corner of the photo
- X axis = 0 to imageWidth (photo pixels)
- Y axis = 0 to imageHeight (photo pixels)
- Units = photo pixels (independent of screen density, zoom level, or view size)

```kotlin
// Example: a dimension line stored in image space
data class LinearDimension(
    val pointA: PointF,  // e.g., PointF(450f, 320f) — photo pixel coordinates
    val pointB: PointF,  // e.g., PointF(890f, 320f)
    val offset: Float,   // 40f (photo pixels)
    // ...
)
```

### Screen Space (what the user touches)

Touch events arrive in screen/view coordinates:
- Origin (0,0) = top-left corner of the View
- Affected by: zoom level, pan offset, device density
- NOT the same as image space (unless zoom=1 and pan=0)

**The bridge between them is the `viewMatrix`.**

---

## THE VIEW MATRIX

```kotlin
/**
 * The single Matrix that transforms image-space → screen-space.
 * Applied to BOTH the photo bitmap and the annotation canvas.
 *
 * Contains: scale (zoom) + translation (pan)
 * Optionally: rotation (if supporting photo rotation)
 */
class AnnotationCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // ── THE MATRIX ──
    private val viewMatrix = Matrix()       // image-space → screen-space
    private val inverseMatrix = Matrix()    // screen-space → image-space (for touch)

    // ── STATE ──
    private var currentZoom = 1f
    private val minZoom = 0.5f
    private val maxZoom = 10f
    private var photoBitmap: Bitmap? = null
    private val annotations = mutableListOf<DrawingElement>()

    // ── INITIAL FIT ──
    // Called when photo is loaded. Computes the matrix that fits
    // the photo into the view (like ImageView's fitCenter).
    fun setPhoto(bitmap: Bitmap) {
        photoBitmap = bitmap
        fitPhotoToView()
        invalidate()
    }

    private fun fitPhotoToView() {
        val bmp = photoBitmap ?: return
        viewMatrix.reset()

        // Scale to fit the view while maintaining aspect ratio
        val scaleX = width.toFloat() / bmp.width
        val scaleY = height.toFloat() / bmp.height
        val scale = minOf(scaleX, scaleY)

        // Center the photo in the view
        val dx = (width - bmp.width * scale) / 2f
        val dy = (height - bmp.height * scale) / 2f

        viewMatrix.setScale(scale, scale)
        viewMatrix.postTranslate(dx, dy)

        currentZoom = scale
        updateInverseMatrix()
    }

    private fun updateInverseMatrix() {
        viewMatrix.invert(inverseMatrix)
    }
}
```

---

## RENDERING: DRAW PHOTO + ANNOTATIONS WITH SAME MATRIX

```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    // 1. Apply the view matrix to the canvas
    canvas.save()
    canvas.concat(viewMatrix)

    // 2. Draw the photo bitmap at its native position (0,0)
    photoBitmap?.let { bmp ->
        canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
    }

    // 3. Draw ALL annotations in image-space coordinates
    //    They automatically appear at the correct screen position
    //    because the canvas has the viewMatrix applied.
    for (element in annotations) {
        element.draw(canvas, currentZoom)
    }

    canvas.restore()

    // 4. Draw screen-space HUD elements OUTSIDE the matrix
    //    (toolbar indicators, snap guides, selection handles)
    drawHud(canvas)
}
```

**Why this works:** Both the bitmap and the annotations are drawn within the same `canvas.concat(viewMatrix)` block. When the user zooms or pans, only `viewMatrix` changes — the annotation coordinates stay the same, but they appear at the correct screen position because the matrix transforms them.

---

## TOUCH INPUT: SCREEN → IMAGE COORDINATE CONVERSION

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    // Touch coordinates arrive in SCREEN space
    val screenX = event.x
    val screenY = event.y

    // Convert to IMAGE space using the inverse matrix
    val imagePoint = screenToImage(screenX, screenY)

    // Now imagePoint.x and imagePoint.y are in photo-pixel coordinates
    // Use these for ALL annotation operations:
    //   - creating new elements
    //   - hit-testing existing elements
    //   - snap detection
    //   - moving/resizing elements

    when (currentTool) {
        Tool.DRAW -> handleDraw(event, imagePoint)
        Tool.SELECT -> handleSelect(event, imagePoint)
        Tool.PAN -> handlePan(event)  // pan uses screen coords, not image
        // ...
    }
    return true
}

/**
 * Converts screen (view) coordinates to image-space coordinates.
 * This is the CRITICAL function that keeps everything aligned.
 */
fun screenToImage(screenX: Float, screenY: Float): PointF {
    val pts = floatArrayOf(screenX, screenY)
    inverseMatrix.mapPoints(pts)
    return PointF(pts[0], pts[1])
}

/**
 * Converts image-space coordinates to screen coordinates.
 * Used for: positioning UI overlays, selection handles, tooltips.
 */
fun imageToScreen(imageX: Float, imageY: Float): PointF {
    val pts = floatArrayOf(imageX, imageY)
    viewMatrix.mapPoints(pts)
    return PointF(pts[0], pts[1])
}
```

---

## ZOOM: PINCH GESTURE

```kotlin
private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val scaleFactor = detector.scaleFactor
        val newZoom = (currentZoom * scaleFactor).coerceIn(minZoom, maxZoom)
        val actualFactor = newZoom / currentZoom

        // Scale around the pinch focal point (in screen space)
        // This keeps the point under the user's fingers stationary
        viewMatrix.postScale(
            actualFactor, actualFactor,
            detector.focusX, detector.focusY   // pivot = pinch center
        )

        currentZoom = newZoom
        updateInverseMatrix()
        invalidate()
        return true
    }
})
```

---

## PAN: ONE-FINGER OR TWO-FINGER DRAG

```kotlin
private var lastTouchX = 0f
private var lastTouchY = 0f
private var isPanning = false

private fun handlePan(event: MotionEvent) {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            lastTouchX = event.x
            lastTouchY = event.y
            isPanning = true
        }
        MotionEvent.ACTION_MOVE -> {
            if (isPanning) {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                // Translate in SCREEN space (post-translate)
                viewMatrix.postTranslate(dx, dy)
                updateInverseMatrix()

                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
            }
        }
        MotionEvent.ACTION_UP -> isPanning = false
    }
}
```

---

## PAN LIMITS: KEEP PHOTO VISIBLE

```kotlin
/**
 * Prevents the user from panning the photo completely off-screen.
 * Ensures at least 20% of the photo remains visible.
 */
private fun clampPan() {
    val bmp = photoBitmap ?: return
    val values = FloatArray(9)
    viewMatrix.getValues(values)

    val scale = values[Matrix.MSCALE_X]
    val transX = values[Matrix.MTRANS_X]
    val transY = values[Matrix.MTRANS_Y]

    val scaledW = bmp.width * scale
    val scaledH = bmp.height * scale
    val margin = 0.2f  // 20% must remain visible

    // Clamp horizontal
    val minX = -(scaledW * (1 - margin))
    val maxX = width * (1 - margin)
    val clampedX = transX.coerceIn(minX, maxX)

    // Clamp vertical
    val minY = -(scaledH * (1 - margin))
    val maxY = height * (1 - margin)
    val clampedY = transY.coerceIn(minY, maxY)

    if (clampedX != transX || clampedY != transY) {
        viewMatrix.postTranslate(clampedX - transX, clampedY - transY)
        updateInverseMatrix()
    }
}
```

---

## STROKE WIDTH: SCALE-AWARE RENDERING

Annotations drawn within the matrix automatically scale with zoom. But some visual properties should NOT scale linearly:

### What scales with zoom (correct — looks like zooming into a real drawing):
- Line geometry (positions, lengths, paths)
- Shape sizes (circles, rectangles)
- Hatch pattern spacing
- Arrowhead geometry

### What needs compensation (to stay readable/usable):
- Stroke width (lines shouldn't become invisible when zoomed out)
- Text size (must remain readable)
- Touch hit targets (must remain tappable)
- Selection handles (must remain grabbable)

```kotlin
/**
 * Calculates the effective stroke width for rendering inside the matrix.
 * The annotation stores a "logical" stroke width in image-space.
 * We adjust it so it never becomes too thin or too thick on screen.
 *
 * @param logicalWidth  The stored stroke width in image pixels
 * @param currentZoom   Current zoom level from viewMatrix
 * @return              Adjusted width to use in Paint.strokeWidth
 */
fun effectiveStrokeWidth(logicalWidth: Float, currentZoom: Float): Float {
    val screenWidth = logicalWidth * currentZoom

    // Minimum: 1 physical pixel (never invisible)
    // Maximum: 2× the logical width (never absurdly thick)
    val minScreenPx = 1f    // always visible
    val maxScreenPx = logicalWidth * 2f

    return when {
        screenWidth < minScreenPx -> minScreenPx / currentZoom  // boost when zoomed out
        screenWidth > maxScreenPx -> maxScreenPx / currentZoom  // cap when zoomed in
        else -> logicalWidth  // normal range, no adjustment
    }
}

/**
 * Calculates text size that stays readable at any zoom level.
 * Text stored in image-space px, but clamped to screen-space bounds.
 */
fun effectiveTextSize(logicalSizePx: Float, currentZoom: Float): Float {
    val screenSizePx = logicalSizePx * currentZoom
    val minScreenSp = 8f   // never smaller than 8sp equivalent
    val maxScreenSp = 32f  // never larger than 32sp equivalent

    return when {
        screenSizePx < minScreenSp -> minScreenSp / currentZoom
        screenSizePx > maxScreenSp -> maxScreenSp / currentZoom
        else -> logicalSizePx
    }
}
```

### Hit Target Compensation

```kotlin
/**
 * Touch hit targets must be at least 44dp (Android accessibility guideline)
 * regardless of zoom level. Convert the required screen-space hit area
 * back to image-space for accurate hit testing.
 */
fun hitTestRadius(imageSpaceRadius: Float, currentZoom: Float, density: Float): Float {
    val minScreenDp = 22f  // 22dp radius = 44dp diameter
    val minImagePx = minScreenDp * density / currentZoom
    return maxOf(imageSpaceRadius, minImagePx)
}
```

---

## GESTURE MODE SWITCHING

The same finger gesture must do different things depending on context:

```kotlin
enum class GestureMode {
    DRAW,       // one-finger draws annotation
    PAN,        // one-finger pans (or two-finger always pans)
    SELECT      // one-finger selects/moves annotations
}

/**
 * Gesture routing rules:
 *
 * TWO FINGERS:
 *   - Always zoom (pinch) + pan (drag), regardless of active tool
 *   - This NEVER creates annotations
 *
 * ONE FINGER (depends on active tool):
 *   - Draw tool active → one-finger draws (pan disabled)
 *   - Select tool active → one-finger selects/moves
 *   - Pan tool active → one-finger pans
 *
 * ALTERNATIVE (common in annotation apps):
 *   - One finger on EMPTY space → pan
 *   - One finger on ANNOTATION → select/move
 *   - One finger with DRAW tool → draw
 *   - Two fingers always → zoom/pan
 */
fun routeGesture(event: MotionEvent, activeTool: Tool): GestureMode {
    val pointerCount = event.pointerCount

    if (pointerCount >= 2) {
        // Two+ fingers = ALWAYS zoom/pan, no exceptions
        return GestureMode.PAN
    }

    // Single finger
    return when (activeTool) {
        Tool.PAN -> GestureMode.PAN
        Tool.SELECT -> {
            // Check if finger is on an existing annotation
            val imagePoint = screenToImage(event.x, event.y)
            val hitElement = findElementAt(imagePoint, currentZoom)
            if (hitElement != null) GestureMode.SELECT else GestureMode.PAN
        }
        else -> GestureMode.DRAW  // any drawing tool
    }
}
```

---

## DOUBLE-TAP TO ZOOM

```kotlin
private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

    override fun onDoubleTap(e: MotionEvent): Boolean {
        // Zoom to 2× centered on tap point, or back to fit if already zoomed
        val targetZoom = if (currentZoom > minZoom * 1.5f) {
            minZoom  // zoom out to fit
        } else {
            minZoom * 3f  // zoom to 3× of fit level
        }

        animateZoomTo(targetZoom, e.x, e.y, durationMs = 300)
        return true
    }
})

/**
 * Smooth animated zoom to a target level, centered on a screen point.
 */
private fun animateZoomTo(targetZoom: Float, pivotX: Float, pivotY: Float, durationMs: Long) {
    val startZoom = currentZoom
    val startMatrix = Matrix(viewMatrix)

    ValueAnimator.ofFloat(0f, 1f).apply {
        duration = durationMs
        interpolator = DecelerateInterpolator()
        addUpdateListener { anim ->
            val fraction = anim.animatedValue as Float
            val zoom = startZoom + (targetZoom - startZoom) * fraction
            val factor = zoom / currentZoom

            viewMatrix.set(startMatrix)
            val totalFactor = zoom / startZoom
            viewMatrix.postScale(totalFactor, totalFactor, pivotX, pivotY)

            currentZoom = zoom
            updateInverseMatrix()
            clampPan()
            invalidate()
        }
        start()
    }
}
```

---

## RENDERING LAYERS: PERFORMANCE OPTIMIZATION

```kotlin
/**
 * For performance, separate the rendering into layers:
 *
 * Layer 0: Photo bitmap (cached, rarely changes)
 * Layer 1: Completed annotations (cached as bitmap, changes on add/edit/delete)
 * Layer 2: Active drawing (redraws every frame during gesture)
 * Layer 3: HUD (selection handles, snap guides — screen-space, outside matrix)
 *
 * Only redraw the layer that changed. The photo bitmap never needs redrawing
 * unless the photo itself changes.
 */
class LayeredCanvas {
    private var annotationCache: Bitmap? = null
    private var annotationCacheDirty = true

    fun invalidateAnnotations() {
        annotationCacheDirty = true
    }

    fun draw(canvas: Canvas, viewMatrix: Matrix, zoom: Float) {
        canvas.save()
        canvas.concat(viewMatrix)

        // Layer 0: Photo (always draw, GPU caches the bitmap)
        photoBitmap?.let { canvas.drawBitmap(it, 0f, 0f, bitmapPaint) }

        // Layer 1: Cached annotation bitmap
        if (annotationCacheDirty) {
            rebuildAnnotationCache(zoom)
        }
        annotationCache?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Layer 2: Active drawing element (during gesture)
        activeElement?.draw(canvas, zoom)

        canvas.restore()

        // Layer 3: HUD (screen-space)
        drawSelectionHandles(canvas)
        drawSnapGuides(canvas)
    }
}
```

---

## NORMALIZED COORDINATES (ALTERNATIVE STORAGE FORMAT)

For portability across different photo resolutions, you can optionally store annotations in **normalized 0..1 coordinates** instead of pixel coordinates:

```kotlin
/**
 * Converts image-pixel coordinates to normalized 0..1 range.
 * Useful for: exporting annotations, syncing across devices,
 * applying same annotations to different photo resolutions.
 */
fun imageToNormalized(imagePoint: PointF, imageWidth: Int, imageHeight: Int): PointF {
    return PointF(
        imagePoint.x / imageWidth,
        imagePoint.y / imageHeight
    )
}

fun normalizedToImage(normPoint: PointF, imageWidth: Int, imageHeight: Int): PointF {
    return PointF(
        normPoint.x * imageWidth,
        normPoint.y * imageHeight
    )
}
```

**When to use which:**
- **Image pixels** (default): simpler math, exact placement, best for single-device editing
- **Normalized 0..1**: best for export, serialization, cross-resolution support

The internal drawing engine should work in image pixels. Convert to/from normalized only at serialization boundaries (save/load/export).

---

## SERIALIZATION FORMAT

```kotlin
/**
 * Complete drawing project serialization.
 * The photo path + annotations in image-space coordinates = full reconstruction.
 */
@Serializable
data class AnnotationProject(
    val photoUri: String,
    val photoWidth: Int,
    val photoHeight: Int,
    val elements: List<DrawingElement>,     // all coordinates in image pixels
    val layers: List<LayerInfo>,
    val metadata: ProjectMetadata
)

@Serializable
data class ProjectMetadata(
    val title: String,
    val created: Long,
    val modified: Long,
    val scale: Float?,         // if known: 1 pixel = X mm (for real-world dimensions)
    val unitSystem: UnitSystem  // METRIC or IMPERIAL
)
```

---

## PHOTO SCALE CALIBRATION (OPTIONAL)

For real-world dimensions on photos, the user can calibrate:

```kotlin
/**
 * Lets the user draw a reference line on the photo and input the real-world length.
 * From this, we compute pixels-per-mm for accurate dimension measurements.
 *
 * Flow:
 * 1. User taps "Calibrate Scale"
 * 2. User draws a line between two known points on the photo
 * 3. User inputs the real-world distance (e.g., "150 mm")
 * 4. App computes: pixelsPerMm = lineLength_in_pixels / realWorldLength_in_mm
 * 5. All subsequent dimension values auto-compute in real-world units
 */
data class ScaleCalibration(
    val lineStartImage: PointF,   // image-space endpoints of reference line
    val lineEndImage: PointF,
    val realWorldLength: Float,   // mm
    val unit: UnitSystem
) {
    val pixelsPerMm: Float
        get() {
            val dx = lineEndImage.x - lineStartImage.x
            val dy = lineEndImage.y - lineStartImage.y
            val pixelLength = sqrt(dx * dx + dy * dy)
            return pixelLength / realWorldLength
        }

    fun pixelsToMm(pixels: Float): Float = pixels / pixelsPerMm
    fun mmToPixels(mm: Float): Float = mm * pixelsPerMm
}
```

---

## SUMMARY: THE RULES

```
RULE 1:  Store ALL annotation coordinates in image-pixel space.
         Never store screen coordinates.

RULE 2:  Use ONE viewMatrix for both the photo and annotations.
         canvas.concat(viewMatrix) → draw photo → draw annotations → canvas.restore()

RULE 3:  Convert touch input from screen→image using inverseMatrix.mapPoints().
         Convert image→screen when positioning HUD elements.

RULE 4:  Update inverseMatrix every time viewMatrix changes (zoom/pan).
         viewMatrix.invert(inverseMatrix)

RULE 5:  Stroke width and text size need compensation at extreme zoom levels.
         Min 1px on screen (never invisible), max 2× logical (never absurdly thick).

RULE 6:  Two-finger gestures ALWAYS zoom/pan, regardless of active tool.
         One-finger behavior depends on the active tool.

RULE 7:  For export/serialization, optionally convert to normalized 0..1 coords.
         Internal engine always works in image pixels.

RULE 8:  Cache completed annotations as a bitmap layer.
         Only redraw the active drawing element per frame during gestures.
```
