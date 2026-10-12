# Photo Overlay System — Complete Implementation Spec

> **THE single source of truth for Claude Code.**  
> Contains: overlay architecture, coordinate system integration, clipping mask shapes, pan/zoom behavior, transparency controls, color-range masking (chroma key), edge feathering, blend modes, AGSL shader pipeline, and data model.

---

## TABLE OF CONTENTS

1. [Core Principle: One Matrix Rules All](#1-core-principle)
2. [Coordinate System Integration](#2-coordinate-system-integration)
3. [Overlay Data Model](#3-overlay-data-model)
4. [Clipping Mask Shapes (8 types)](#4-clipping-mask-shapes)
5. [Two Matrices: viewMatrix vs overlayMatrix](#5-two-matrices)
6. [Rendering: Photo + Overlays + Annotations With Same Matrix](#6-rendering)
7. [Touch Input: Screen → Image → Source Conversion](#7-touch-input)
8. [Gesture Handling: Canvas vs Overlay Gestures](#8-gesture-handling)
9. [Transparency & Blend Modes](#9-transparency-and-blend-modes)
10. [Color-Range Masking (Chroma Key)](#10-color-range-masking)
11. [Edge Feathering](#11-edge-feathering)
12. [AGSL Shader Pipeline (API 33+)](#12-agsl-shader-pipeline)
13. [Fallback Pipeline (API < 33)](#13-fallback-pipeline)
14. [Stroke & Handle Compensation](#14-stroke-handle-compensation)
15. [UI Controls](#15-ui-controls)
16. [Serialization](#16-serialization)
17. [Performance](#17-performance)
18. [Implementation Slices](#18-implementation-slices)

---

## 1. CORE PRINCIPLE: ONE MATRIX RULES ALL

A **Photo Overlay** composites a selected region from a **source photo** onto the **base photo**. The overlay follows the **exact same coordinate system principle** as annotations: store coordinates in image-space, apply ONE shared viewMatrix to everything.

**When the user zooms or pans the canvas, the base photo, all overlays, and all annotations move together — because they are all drawn inside the same canvas.concat(viewMatrix) block.**

```
┌─────────────────────────────────────────────────────────────────────┐
│                           SCREEN                                    │
│                                                                     │
│   ┌───────────────────────────────────────────────────────────┐     │
│   │                 viewMatrix applied                         │     │
│   │                                                           │     │
│   │   ┌─────────────────────────────────────────────────┐     │     │
│   │   │              BASE PHOTO BITMAP                   │     │     │
│   │   │                                                  │     │     │
│   │   │   ┌───────────────────────────┐                  │     │     │
│   │   │   │   OVERLAY (clipped)       │                  │     │     │
│   │   │   │   ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐ │  stored in       │     │     │
│   │   │   │   │  SOURCE PHOTO      │ │  base photo      │     │     │
│   │   │   │   │  (positioned by    │ │  image-pixel     │     │     │
│   │   │   │   │   overlayMatrix)   │ │  coordinates     │     │     │
│   │   │   │   └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘ │                  │     │     │
│   │   │   └───────────────────────────┘                  │     │     │
│   │   │                                                  │     │     │
│   │   │   ●───── dim ────●   ← annotations also in      │     │     │
│   │   │   │  120.00       │    image-pixel coordinates   │     │     │
│   │   │                                                  │     │     │
│   │   └─────────────────────────────────────────────────┘     │     │
│   └───────────────────────────────────────────────────────────┘     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

When user zooms in 2×:
  - viewMatrix.postScale(2, 2, pivotX, pivotY)
  - Base photo renders at 2×
  - Overlay renders at 2×           ← SAME MATRIX
  - Annotations render at 2×        ← SAME MATRIX
  - They ALL stay perfectly aligned because same viewMatrix
```

This is IDENTICAL to how annotations work. The overlay is just another thing drawn inside the viewMatrix block, with coordinates stored in base-photo image-space.

---

## 2. COORDINATE SYSTEM INTEGRATION

### Three Coordinate Spaces

```
SPACE 1: SOURCE PHOTO SPACE
  - Origin (0,0) = top-left of the source/overlay photo
  - Units = source photo pixels
  - Used for: color-range masking (pixel sampling), eyedropper
  - NEVER stored directly — always converted to base-photo image-space via overlayMatrix

SPACE 2: BASE PHOTO IMAGE-SPACE (where everything lives)
  - Origin (0,0) = top-left of the base photo
  - Units = base photo pixels
  - Used for: mask shape coordinates, overlay position, annotations, all storage
  - This is THE coordinate system. Same as annotations.

SPACE 3: SCREEN SPACE (what the user touches)
  - Origin (0,0) = top-left of the View
  - Affected by: viewMatrix (zoom + pan)
  - Touch events arrive here → converted to image-space via inverseMatrix
```

### The Matrix Chain

```
SOURCE PHOTO PIXELS
        │
        │  overlayMatrix (scale + rotate + translate)
        │  Positions source photo within base photo's coordinate system.
        │  Changes ONLY when user explicitly edits the overlay.
        ▼
BASE PHOTO IMAGE-SPACE
        │
        │  viewMatrix (zoom + pan of the whole canvas)
        │  Applied to EVERYTHING: base photo, overlays, annotations.
        │  Changes when user pinch-zooms or pans the canvas.
        ▼
SCREEN SPACE (pixels on display)
```

### Why This Works

The overlay's clipping mask (rectangle, circle, etc.) is stored in base-photo image-space — exactly like a dimension line's endpoints. The source photo is positioned within that space via overlayMatrix. When viewMatrix changes (user zooms/pans), the mask shape, the source photo content, the base photo, and all annotations transform identically — because they're all drawn in the same canvas.concat(viewMatrix) block.

**The overlay NEVER needs to know about zoom/pan.** It just exists in image-space. The viewMatrix handles the rest — same as every annotation.

### Proof By Example

```
SETUP:
  - Base photo: 4000×3000 pixels
  - Overlay mask: Rectangle centered at (2000, 1500), 800×600 image pixels
  - Source photo: positioned at overlayMatrix offset (1600, 1200) in base image-space
  - Annotation: Dimension line from (1800, 1400) to (2200, 1400)

ZOOM TO 2×:
  viewMatrix.postScale(2, 2, screenCenterX, screenCenterY)
  
  Result:
  - Base photo: drawn at 2× → fills more screen
  - Overlay mask rect: center (2000, 1500) → maps to 2× screen position via viewMatrix
  - Source photo inside mask: drawn at (1600, 1200) in image-space → 2× on screen
  - Dimension line: drawn at (1800, 1400) → (2200, 1400) → 2× on screen
  
  → Everything scaled and shifted identically. 
  → Overlay stays anchored to the same spot on the base photo.
  → Overlay boundary aligns with annotations drawn near it.
  
  NOTHING CHANGED except viewMatrix. No overlay coordinates were modified.
```

---

## 3. OVERLAY DATA MODEL

```kotlin
@Serializable
data class PhotoOverlay(
    val id: String = UUID.randomUUID().toString(),
    
    // ── SOURCE PHOTO ──
    val sourcePhotoUri: String,                // URI to overlay source image
    val sourceWidth: Int,                      // original source dimensions (pixels)
    val sourceHeight: Int,
    
    // ── CLIPPING MASK (coordinates in BASE PHOTO image-space) ──
    val maskShape: MaskShape,                  // defines the visible region on the base photo
    
    // ── OVERLAY MATRIX: positions source photo in base-photo image-space ──
    // These values define the overlayMatrix that maps source pixels → image-space.
    // They do NOT interact with viewMatrix. viewMatrix is handled by the canvas.
    var internalPanX: Float = 0f,              // translation X in base photo image pixels
    var internalPanY: Float = 0f,              // translation Y in base photo image pixels
    var internalZoom: Float = 1f,              // scale of source within image-space
    var internalRotation: Float = 0f,          // degrees, rotation of source within image-space
    
    // ── APPEARANCE ──
    var opacity: Float = 0.7f,                 // 0.0 (invisible) to 1.0 (opaque)
    var blendMode: OverlayBlendMode = OverlayBlendMode.NORMAL,
    var featherRadius: Float = 0f,             // image-space pixels, 0 = hard edge
    
    // ── COLOR-RANGE MASKS ──
    val colorMasks: MutableList<ColorRangeMask> = mutableListOf(),
    var colorMaskMode: ColorMaskMode = ColorMaskMode.SHOW_MATCHING,
    
    // ── LAYER ──
    var zOrder: Int = 0,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false
)
```

**Critical storage rule:** maskShape coordinates, internalPanX/Y, and featherRadius are ALL in **base photo image pixels** — the same unit system as annotation coordinates. Never screen pixels. Never density-dependent. This ensures overlays stay pinned to the base photo at any zoom level.

---

## 4. CLIPPING MASK SHAPES (8 types)

All coordinates in **base photo image-space pixels** — same as annotation points.

```kotlin
@Serializable
sealed class MaskShape {
    // 1. RECTANGLE
    @Serializable
    data class Rectangle(
        val center: SerPointF,             // image-space pixels
        val width: Float,                  // image-space pixels
        val height: Float,                 // image-space pixels
        val rotation: Float = 0f,          // degrees, around center
        val cornerRadius: Float = 0f       // image-space pixels
    ) : MaskShape()

    // 2. ELLIPSE
    @Serializable
    data class Ellipse(
        val center: SerPointF, val radiusX: Float,
        val radiusY: Float, val rotation: Float = 0f
    ) : MaskShape()

    // 3. CIRCLE
    @Serializable
    data class Circle(val center: SerPointF, val radius: Float) : MaskShape()

    // 4. POLYGON (3+ vertices, auto-closed)
    @Serializable
    data class Polygon(val points: List<SerPointF>) : MaskShape()

    // 5. FREEHAND CURVE (closed)
    // Reuses CatmullRomSmoother + SnapToCloseManager from FINAL spec §11
    @Serializable
    data class FreehandCurve(
        val controlPoints: List<SerPointF>,
        val isClosed: Boolean = true
    ) : MaskShape()

    // 6. STAR
    @Serializable
    data class Star(
        val center: SerPointF, val outerRadius: Float,
        val innerRadius: Float, val points: Int, val rotation: Float = 0f
    ) : MaskShape()

    // 7. PATH FROM ANNOTATION
    @Serializable
    data class FromAnnotation(val annotationId: String) : MaskShape()

    // 8. FULL FRAME (no clipping)
    @Serializable
    object FullFrame : MaskShape()
}

@Serializable
data class SerPointF(val x: Float, val y: Float)
```

### Generating Android Path from MaskShape

```kotlin
object MaskShapePathGenerator {
    /**
     * Returns a Path in BASE PHOTO IMAGE-SPACE.
     * Drawn inside canvas.concat(viewMatrix), so viewMatrix transforms it to screen.
     */
    fun toPath(shape: MaskShape): Path {
        return when (shape) {
            is MaskShape.Rectangle -> generateRectPath(shape)
            is MaskShape.Ellipse -> generateEllipsePath(shape)
            is MaskShape.Circle -> Path().apply {
                addCircle(shape.center.x, shape.center.y, shape.radius, Path.Direction.CW)
            }
            is MaskShape.Polygon -> Path().apply {
                if (shape.points.size >= 3) {
                    moveTo(shape.points[0].x, shape.points[0].y)
                    for (i in 1 until shape.points.size) lineTo(shape.points[i].x, shape.points[i].y)
                    close()
                }
            }
            is MaskShape.FreehandCurve -> CatmullRomSmoother().createPath(
                shape.controlPoints.map { PointF(it.x, it.y) }, closed = shape.isClosed
            )
            is MaskShape.Star -> generateStarPath(shape)
            is MaskShape.FromAnnotation -> Path() // resolved at render time
            is MaskShape.FullFrame -> Path().apply {
                addRect(-1e6f, -1e6f, 1e6f, 1e6f, Path.Direction.CW)
            }
        }
    }

    private fun generateRectPath(rect: MaskShape.Rectangle): Path {
        val path = Path()
        val rectF = RectF(
            rect.center.x - rect.width / 2, rect.center.y - rect.height / 2,
            rect.center.x + rect.width / 2, rect.center.y + rect.height / 2
        )
        if (rect.cornerRadius > 0f) path.addRoundRect(rectF, rect.cornerRadius, rect.cornerRadius, Path.Direction.CW)
        else path.addRect(rectF, Path.Direction.CW)
        if (rect.rotation != 0f) {
            val m = Matrix(); m.postRotate(rect.rotation, rect.center.x, rect.center.y); path.transform(m)
        }
        return path
    }

    private fun generateEllipsePath(e: MaskShape.Ellipse): Path {
        val path = Path()
        path.addOval(RectF(e.center.x - e.radiusX, e.center.y - e.radiusY,
            e.center.x + e.radiusX, e.center.y + e.radiusY), Path.Direction.CW)
        if (e.rotation != 0f) { val m = Matrix(); m.postRotate(e.rotation, e.center.x, e.center.y); path.transform(m) }
        return path
    }

    private fun generateStarPath(star: MaskShape.Star): Path {
        val path = Path()
        val total = star.points * 2
        val step = (2 * PI / total).toFloat()
        val start = Math.toRadians(star.rotation.toDouble() - 90.0).toFloat()
        for (i in 0 until total) {
            val r = if (i % 2 == 0) star.outerRadius else star.innerRadius
            val a = start + i * step
            val x = star.center.x + r * cos(a); val y = star.center.y + r * sin(a)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); return path
    }
}
```

---

## 5. TWO MATRICES: viewMatrix vs overlayMatrix

There are exactly TWO matrices involved. They serve completely different purposes and NEVER interfere with each other.

### viewMatrix (shared — NOT overlay-specific)

```
PURPOSE:   Zoom/pan the entire canvas (base photo + overlays + annotations)
OWNER:     AnnotationCanvasView (the view controller)
CHANGES:   When user pinch-zooms or two-finger-pans the canvas
AFFECTS:   EVERYTHING — base photo, every overlay, every annotation
STORED:    In the view, not in any data model. Transient display state.
```

### overlayMatrix (per-overlay — positions source photo in image-space)

```
PURPOSE:   Position/scale/rotate the SOURCE PHOTO within base-photo image-space
OWNER:     Each PhotoOverlay instance
CHANGES:   When user explicitly edits the overlay (drags source, pinches source)
AFFECTS:   ONLY the source photo content within this one overlay
STORED:    As internalPanX/Y, internalZoom, internalRotation in PhotoOverlay
```

### How They Combine at Render Time

```kotlin
override fun onDraw(canvas: Canvas) {
    // ┌─────────────────────────────────────────────────────────────┐
    // │  viewMatrix block — EVERYTHING inside moves together        │
    // │  when the user zooms or pans the canvas.                    │
    canvas.save()
    canvas.concat(viewMatrix)     // ← the ONE shared matrix
    
    //   Base photo at (0,0)
    photoBitmap?.let { canvas.drawBitmap(it, 0f, 0f, bitmapPaint) }
    
    //   Overlays (each uses overlayMatrix internally for source positioning,
    //   but the MASK and RESULT are in image-space, so viewMatrix handles them)
    for (layer in overlayLayers.filter { it.isVisible }.sortedBy { it.zOrder }) {
        drawOverlay(canvas, layer.overlay)
    }
    
    //   Annotations (image-space coordinates)
    for (element in annotations) { element.draw(canvas, currentZoom) }
    
    canvas.restore()
    // └─────────────────────────────────────────────────────────────┘
    
    // HUD elements (screen-space, OUTSIDE the matrix)
    drawHud(canvas)
    drawOverlayHandles(canvas)
}
```

### OverlayTransformManager

```kotlin
/**
 * Manages the overlayMatrix that positions the source photo in base-photo image-space.
 *
 * CRITICAL: This matrix has NOTHING to do with viewMatrix.
 *   - overlayMatrix maps: source-photo-pixels → base-photo-image-space
 *   - viewMatrix maps:    base-photo-image-space → screen-space
 *   - They combine automatically because the overlay is drawn inside canvas.concat(viewMatrix)
 *
 * When the user zooms the canvas:
 *   → viewMatrix changes → everything moves on screen → overlayMatrix is UNTOUCHED
 * When the user drags the source photo inside the overlay:
 *   → overlayMatrix changes → source repositions within mask → viewMatrix is UNTOUCHED
 */
class OverlayTransformManager(private val overlay: PhotoOverlay) {

    private val overlayMatrix = Matrix()
    private val inverseOverlayMatrix = Matrix()

    fun updateMatrix() {
        overlayMatrix.reset()
        overlayMatrix.postScale(overlay.internalZoom, overlay.internalZoom)
        if (overlay.internalRotation != 0f) {
            val cx = overlay.sourceWidth * overlay.internalZoom / 2f
            val cy = overlay.sourceHeight * overlay.internalZoom / 2f
            overlayMatrix.postRotate(overlay.internalRotation, cx, cy)
        }
        overlayMatrix.postTranslate(overlay.internalPanX, overlay.internalPanY)
        overlayMatrix.invert(inverseOverlayMatrix)
    }

    fun getMatrix(): Matrix = overlayMatrix
    fun getInverseMatrix(): Matrix = inverseOverlayMatrix

    /** Image-space → source-photo-space. For eyedropper. */
    fun imageToSource(imagePoint: PointF): PointF {
        val pts = floatArrayOf(imagePoint.x, imagePoint.y)
        inverseOverlayMatrix.mapPoints(pts)
        return PointF(pts[0], pts[1])
    }

    // Gesture handlers — deltas in BASE PHOTO IMAGE-SPACE (already converted from screen)
    fun pan(dxImage: Float, dyImage: Float) {
        overlay.internalPanX += dxImage
        overlay.internalPanY += dyImage
        updateMatrix()
    }

    fun zoom(scaleFactor: Float, pivotImageX: Float, pivotImageY: Float) {
        val newZoom = (overlay.internalZoom * scaleFactor).coerceIn(0.1f, 20f)
        val prevX = pivotImageX - overlay.internalPanX
        val prevY = pivotImageY - overlay.internalPanY
        val ratio = newZoom / overlay.internalZoom
        overlay.internalPanX = pivotImageX - prevX * ratio
        overlay.internalPanY = pivotImageY - prevY * ratio
        overlay.internalZoom = newZoom
        updateMatrix()
    }

    fun rotate(angleDelta: Float, pivotImageX: Float, pivotImageY: Float) {
        val cosA = cos(Math.toRadians(angleDelta.toDouble())).toFloat()
        val sinA = sin(Math.toRadians(angleDelta.toDouble())).toFloat()
        val dx = overlay.internalPanX - pivotImageX
        val dy = overlay.internalPanY - pivotImageY
        overlay.internalPanX = pivotImageX + dx * cosA - dy * sinA
        overlay.internalPanY = pivotImageY + dx * sinA + dy * cosA
        overlay.internalRotation = (overlay.internalRotation + angleDelta) % 360f
        updateMatrix()
    }

    fun fitToMask(maskBounds: RectF) {
        val scaleX = maskBounds.width() / overlay.sourceWidth
        val scaleY = maskBounds.height() / overlay.sourceHeight
        overlay.internalZoom = maxOf(scaleX, scaleY)
        overlay.internalPanX = maskBounds.left -
            (overlay.sourceWidth * overlay.internalZoom - maskBounds.width()) / 2f
        overlay.internalPanY = maskBounds.top -
            (overlay.sourceHeight * overlay.internalZoom - maskBounds.height()) / 2f
        overlay.internalRotation = 0f
        updateMatrix()
    }
}
```

---

## 6. RENDERING: PHOTO + OVERLAYS + ANNOTATIONS WITH SAME MATRIX

### Drawing a Single Overlay (inside the viewMatrix block)

```kotlin
/**
 * Draws one overlay. Called while canvas already has viewMatrix applied.
 * Everything here is in IMAGE-SPACE. viewMatrix handles screen transform.
 */
private fun drawOverlay(canvas: Canvas, overlay: PhotoOverlay, currentZoom: Float) {
    if (!overlay.isVisible) return
    val sourceBitmap = overlayBitmapCache[overlay.id] ?: return

    val maskPath = MaskShapePathGenerator.toPath(overlay.maskShape)
    val maskBounds = RectF()
    maskPath.computeBounds(maskBounds, true)
    maskBounds.inset(-overlay.featherRadius, -overlay.featherRadius)

    // saveLayer creates offscreen buffer for compositing
    val layerPaint = Paint().apply { alpha = (overlay.opacity * 255).toInt().coerceIn(0, 255) }
    canvas.saveLayer(maskBounds, layerPaint)

    // 1. Clip to mask shape (IMAGE-SPACE path)
    canvas.clipPath(maskPath)

    // 2. Draw source photo using overlayMatrix
    //    overlayMatrix positions source-photo-pixels within image-space.
    //    viewMatrix (already on canvas) then maps to screen. Both combine automatically.
    canvas.save()
    val manager = OverlayTransformManager(overlay)
    manager.updateMatrix()
    canvas.concat(manager.getMatrix())  // overlayMatrix stacks on top of viewMatrix

    val drawBitmap = if (overlay.colorMasks.any { it.isEnabled }) {
        getColorMaskedBitmap(overlay, sourceBitmap)
    } else { sourceBitmap }
    canvas.drawBitmap(drawBitmap, 0f, 0f, bitmapPaint)
    canvas.restore()

    // 3. Feathering (DST_IN with blurred alpha mask)
    if (overlay.featherRadius > 0f) {
        val featheredMask = getFeatheredMask(overlay)
        val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(featheredMask, maskBounds.left, maskBounds.top, clipPaint)
    }

    // 4. Restore layer — applies opacity + blend mode
    canvas.restore()

    // 5. Selection border (also in image-space, compensated for zoom)
    if (overlay.id == selectedOverlayId) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.argb(180, 88, 166, 255)
            strokeWidth = effectiveStrokeWidth(2f, currentZoom)
            pathEffect = DashPathEffect(floatArrayOf(8f / currentZoom, 6f / currentZoom), 0f)
        }
        canvas.drawPath(maskPath, borderPaint)
    }
}
```

### Why Zoom/Pan Works Automatically

```
USER ZOOMS IN 2×:
  1. viewMatrix.postScale(2, 2, focusX, focusY)
  2. onDraw() → canvas.concat(viewMatrix)
  3. Base photo at (0,0) → viewMatrix → 2× on screen ✓
  4. Overlay mask at center (2000,1500) → viewMatrix → 2× screen pos ✓
  5. Source photo via overlayMatrix at (1600,1200) → viewMatrix → 2× screen pos ✓
  6. Annotation at (1800,1400) → viewMatrix → 2× screen pos ✓
  
  ALL moved identically. Overlay anchored to base photo. Zero special code.
  NOTHING CHANGED except viewMatrix. No overlay coordinates modified.

USER PANS LEFT 100 SCREEN PIXELS:
  1. viewMatrix.postTranslate(-100, 0)
  2. Everything shifts left by exactly 100 screen pixels together.
  Again, zero overlay-specific code.
```

---

## 7. TOUCH INPUT: SCREEN → IMAGE → SOURCE CONVERSION

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    val screenX = event.x
    val screenY = event.y

    // STEP 1: Screen → Image-space (SAME function as annotations)
    val imagePoint = screenToImage(screenX, screenY)

    // STEP 2 (only if interacting with overlay source): Image → Source-photo-space
    val activeOverlay = getActiveOverlay()
    if (activeOverlay != null) {
        val manager = OverlayTransformManager(activeOverlay)
        manager.updateMatrix()
        val sourcePoint = manager.imageToSource(imagePoint)
        // sourcePoint is in source-photo pixels — for eyedropper, etc.
    }

    when {
        event.pointerCount >= 2 -> handleCanvasZoomPan(event)  // viewMatrix
        activeOverlay != null && isOverlayEditMode -> handleOverlayGesture(event, imagePoint, activeOverlay)
        else -> handleNormalGesture(event, imagePoint)
    }
    return true
}

// SAME FUNCTIONS as in photo-overlay-coordinate-system.md
fun screenToImage(screenX: Float, screenY: Float): PointF {
    val pts = floatArrayOf(screenX, screenY)
    inverseMatrix.mapPoints(pts)
    return PointF(pts[0], pts[1])
}

fun imageToScreen(imageX: Float, imageY: Float): PointF {
    val pts = floatArrayOf(imageX, imageY)
    viewMatrix.mapPoints(pts)
    return PointF(pts[0], pts[1])
}
```

### Conversion Chain

```
TOUCH (screenX, screenY)
        │  inverseViewMatrix.mapPoints()      ← same as annotations
        ▼
IMAGE-SPACE (imageX, imageY)                  ← mask hit-test, mask resize
        │  inverseOverlayMatrix.mapPoints()   ← ONLY for source-photo ops
        ▼
SOURCE-PHOTO (sourceX, sourceY)               ← eyedropper, source pixel ops
```

---

## 8. GESTURE HANDLING: CANVAS vs OVERLAY GESTURES

### The Critical Distinction

```
CANVAS GESTURES (change viewMatrix — everything moves together):
  ├─ Two-finger pinch → zoom canvas
  ├─ Two-finger drag  → pan canvas
  └─ Double-tap        → zoom-to-fit / zoom-to-3×
  
  IDENTICAL to existing gesture handling. Overlays need ZERO changes.

OVERLAY GESTURES (change overlayMatrix — source moves within mask):
  ├─ One-finger drag INSIDE mask → pan source within mask
  ├─ Two-finger pinch INSIDE mask → zoom source (overlay edit mode only)
  ├─ Two-finger rotate INSIDE mask → rotate source
  └─ One-finger drag EDGE/CORNER → resize mask shape
  
  ONLY active when overlay is in EDIT MODE.
  ALL deltas converted to IMAGE-SPACE before passing to OverlayTransformManager.
```

### Gesture Routing

```kotlin
/**
 * Two-finger inside active overlay = source zoom/rotate.
 * Two-finger outside or no overlay selected = canvas zoom/pan (viewMatrix).
 */
fun routeOverlayPinch(detector: ScaleGestureDetector, overlay: PhotoOverlay?): PinchTarget {
    if (overlay == null || interactionMode == OverlayInteractionMode.NONE)
        return PinchTarget.CANVAS

    val focalImage = screenToImage(detector.focusX, detector.focusY)
    val path = MaskShapePathGenerator.toPath(overlay.maskShape)
    val bounds = RectF(); path.computeBounds(bounds, true)
    val region = Region()
    region.setPath(path, Region(bounds.left.toInt(), bounds.top.toInt(),
        bounds.right.toInt(), bounds.bottom.toInt()))

    return if (region.contains(focalImage.x.toInt(), focalImage.y.toInt()))
        PinchTarget.OVERLAY_SOURCE else PinchTarget.CANVAS
}

enum class PinchTarget { CANVAS, OVERLAY_SOURCE }
```

### Hit Testing (image-space, zoom-compensated handles)

```kotlin
enum class OverlayHitZone {
    INSIDE, EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT,
    CORNER_TL, CORNER_TR, CORNER_BL, CORNER_BR, ROTATE_HANDLE, OUTSIDE
}

fun hitTestOverlay(overlay: PhotoOverlay, imagePoint: PointF, currentZoom: Float, density: Float): OverlayHitZone {
    val path = MaskShapePathGenerator.toPath(overlay.maskShape)
    val bounds = RectF(); path.computeBounds(bounds, true)
    
    // Handle size: 22dp min on screen → convert to image-space
    // Same compensation as hitTestRadius() in coordinate system spec
    val handleSize = (22f * density) / currentZoom
    
    // Corners (highest priority)
    if (isNear(imagePoint, PointF(bounds.left, bounds.top), handleSize)) return OverlayHitZone.CORNER_TL
    if (isNear(imagePoint, PointF(bounds.right, bounds.top), handleSize)) return OverlayHitZone.CORNER_TR
    if (isNear(imagePoint, PointF(bounds.left, bounds.bottom), handleSize)) return OverlayHitZone.CORNER_BL
    if (isNear(imagePoint, PointF(bounds.right, bounds.bottom), handleSize)) return OverlayHitZone.CORNER_BR
    
    // Rotate handle
    val rh = PointF(bounds.centerX(), bounds.top - handleSize * 2)
    if (isNear(imagePoint, rh, handleSize)) return OverlayHitZone.ROTATE_HANDLE
    
    // Edges
    val et = handleSize * 0.7f
    if (abs(imagePoint.y - bounds.top) < et && imagePoint.x in bounds.left..bounds.right) return OverlayHitZone.EDGE_TOP
    if (abs(imagePoint.y - bounds.bottom) < et && imagePoint.x in bounds.left..bounds.right) return OverlayHitZone.EDGE_BOTTOM
    if (abs(imagePoint.x - bounds.left) < et && imagePoint.y in bounds.top..bounds.bottom) return OverlayHitZone.EDGE_LEFT
    if (abs(imagePoint.x - bounds.right) < et && imagePoint.y in bounds.top..bounds.bottom) return OverlayHitZone.EDGE_RIGHT
    
    // Inside
    val region = Region()
    region.setPath(path, Region(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt()))
    if (region.contains(imagePoint.x.toInt(), imagePoint.y.toInt())) return OverlayHitZone.INSIDE
    
    return OverlayHitZone.OUTSIDE
}
```

---

## 9. TRANSPARENCY & BLEND MODES

```kotlin
enum class OverlayBlendMode {
    NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN,
    COLOR_DODGE, COLOR_BURN, SOFT_LIGHT, HARD_LIGHT, DIFFERENCE, EXCLUSION
}

// PorterDuff fallback (all API levels): NORMAL, MULTIPLY, SCREEN, DARKEN, LIGHTEN
// BlendMode (API 29+): all 12 modes
// AGSL shader (API 33+): all 12 modes with custom implementation
```

---

## 10. COLOR-RANGE MASKING (CHROMA KEY)

```kotlin
@Serializable
data class ColorRangeMask(
    val id: String = UUID.randomUUID().toString(),
    var hueCenter: Float = 0f,     // 0-360°
    var hueRange: Float = 30f,     // ± degrees
    var satMin: Float = 0.2f, var satMax: Float = 1.0f,
    var valMin: Float = 0.2f, var valMax: Float = 1.0f,
    var tolerance: Float = 0.1f,
    var pickedColor: Int? = null,
    var isEnabled: Boolean = true
)

enum class ColorMaskMode { SHOW_MATCHING, HIDE_MATCHING }
```

Color masking operates in **SOURCE-PHOTO-SPACE** on the source bitmap pixels. The masked result is then positioned in image-space via overlayMatrix. Runs at ¼ resolution for performance.

### Eyedropper: screen → image → source → pixel

```kotlin
fun pickColorForMask(overlay: PhotoOverlay, sourceBitmap: Bitmap, imagePoint: PointF): ColorRangeMask {
    val manager = OverlayTransformManager(overlay)
    manager.updateMatrix()
    val sourcePoint = manager.imageToSource(imagePoint)
    val x = sourcePoint.x.toInt().coerceIn(0, sourceBitmap.width - 1)
    val y = sourcePoint.y.toInt().coerceIn(0, sourceBitmap.height - 1)
    val pixel = sourceBitmap.getPixel(x, y)
    val hsv = FloatArray(3); Color.colorToHSV(pixel, hsv)
    return ColorRangeMask(hueCenter = hsv[0], hueRange = 20f,
        satMin = (hsv[1] - 0.3f).coerceAtLeast(0f), satMax = 1f,
        valMin = (hsv[2] - 0.3f).coerceAtLeast(0f), valMax = 1f,
        tolerance = 0.15f, pickedColor = pixel)
}
```

---

## 11. EDGE FEATHERING

```kotlin
/**
 * featherRadius is in IMAGE-SPACE PIXELS (like annotation stroke width).
 * Scales naturally with viewMatrix — no special handling needed.
 */
fun featherMask(maskBitmap: Bitmap, radiusPx: Float): Bitmap {
    if (radiusPx <= 0f) return maskBitmap
    val toolkit = Toolkit()
    return toolkit.blur(maskBitmap, radiusPx.toInt().coerceIn(1, 25))
}
```

---

## 12. AGSL SHADER PIPELINE (API 33+)

Single GPU pass: color mask + clip + feather + blend. The shader operates in image-space; viewMatrix on the canvas transforms the result to screen automatically.

(Full AGSL shader code: see OVERLAY_COMPOSITE_SHADER in source — rgb2hsv, hue distance, smoothstep, packed HSV range uniforms, max 8 color ranges.)

---

## 13. FALLBACK PIPELINE (API < 33)

CPU/Canvas compositing with PorterDuff DST_IN for clipping. Result bitmap drawn inside the viewMatrix block at image-space coordinates. Zooms/pans with everything else.

---

## 14. STROKE & HANDLE COMPENSATION

Same scale-compensation pattern as annotations from photo-overlay-coordinate-system.md.

```kotlin
// Overlay border stroke: same as effectiveStrokeWidth() for annotations
fun overlayBorderWidth(logicalWidth: Float, currentZoom: Float): Float {
    val screenWidth = logicalWidth * currentZoom
    return when {
        screenWidth < 1f -> 1f / currentZoom
        screenWidth > logicalWidth * 2f -> logicalWidth * 2f / currentZoom
        else -> logicalWidth
    }
}

// Overlay handles: drawn in SCREEN-SPACE (outside viewMatrix), 
// positions from imageToScreen(). Constant 44dp tap target.
fun drawOverlayHandles(canvas: Canvas) {
    val overlay = getSelectedOverlay() ?: return
    val bounds = RectF()
    MaskShapePathGenerator.toPath(overlay.maskShape).computeBounds(bounds, true)
    
    val corners = listOf(
        imageToScreen(bounds.left, bounds.top),
        imageToScreen(bounds.right, bounds.top),
        imageToScreen(bounds.left, bounds.bottom),
        imageToScreen(bounds.right, bounds.bottom)
    )
    val r = 8f * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 88, 166, 255); style = Paint.Style.STROKE; strokeWidth = 2f * density
    }
    for (c in corners) { canvas.drawCircle(c.x, c.y, r, fill); canvas.drawCircle(c.x, c.y, r, stroke) }
}
```

---

## 15. UI CONTROLS

Overlay Properties Bottom Sheet: Opacity slider, Blend mode picker (thumbnails), Edge feather slider, Mask shape selector, Color masks (add/remove/eyedropper/hue wheel), Actions (lock/duplicate/z-order/delete).

---

## 16. SERIALIZATION

```kotlin
@Serializable
data class AnnotationProject(
    val photoUri: String, val photoWidth: Int, val photoHeight: Int,
    val elements: List<DrawingElement>,
    val overlays: List<PhotoOverlay>,   // all coords in image pixels
    val layers: List<LayerInfo>, val metadata: ProjectMetadata
)
```

All overlay coordinates in **base photo image pixels**. Source photos as URI references.

---

## 17. PERFORMANCE

```
CACHE L1: Source bitmap (loaded once, downscaled to viewport)
CACHE L2: Color-masked bitmap (invalidated on color range change, ¼ res)
CACHE L3: Composited overlay (invalidated on any property change)
LIVE EDITING: Skip color mask during gesture, reapply on UP.
Memory: total overlay bitmaps ≤ 25% of available heap.
```

---

## 18. IMPLEMENTATION SLICES

```
SLICE OV-1: Data model + basic rendering inside viewMatrix block
  - Verify overlay zooms/pans WITH base photo automatically
  - Rectangle + Circle masks, opacity
  
SLICE OV-2: overlayMatrix (independent source pan/zoom/rotate)
  - Gesture routing: canvas vs overlay
  - Touch: screen → image → source conversion chain

SLICE OV-3: All 8 mask shapes

SLICE OV-4: Blend modes (12, API-level-dependent)

SLICE OV-5: Edge feathering (image-space radius)

SLICE OV-6: Color-range masking + eyedropper

SLICE OV-7: AGSL GPU pipeline (API 33+)

SLICE OV-8: UI + handles (screen-space, compensated)

SLICE OV-9: Polish (undo/redo, export, memory)
```

---

## SUMMARY: THE RULES

```
RULE 1:  Overlays follow the SAME coordinate system as annotations.
         Store ALL overlay coordinates in base-photo image-pixel space.
         Never store screen coordinates.

RULE 2:  Use ONE viewMatrix for base photo, overlays, AND annotations.
         canvas.concat(viewMatrix) → draw photo → draw overlays → draw annotations → restore
         When the user zooms/pans, EVERYTHING moves together automatically.

RULE 3:  The overlayMatrix is a SEPARATE per-overlay matrix that positions the
         source photo within base-photo image-space. It does NOT interact with viewMatrix.
         overlayMatrix changes = source moves within mask.
         viewMatrix changes = everything moves on screen.

RULE 4:  Touch conversion uses the SAME screenToImage() as annotations.
         For source-photo ops, add imageToSource() via inverseOverlayMatrix.

RULE 5:  Two-finger gestures ALWAYS zoom/pan the canvas (viewMatrix),
         UNLESS overlay is in edit mode AND fingers inside mask,
         in which case they zoom/rotate the source (overlayMatrix).

RULE 6:  Overlay handles drawn in SCREEN-SPACE (outside viewMatrix),
         positions via imageToScreen(). 44dp tap target regardless of zoom.

RULE 7:  Stroke width for overlay borders: same compensation as annotations.

RULE 8:  Color-range masking operates in SOURCE-PHOTO-SPACE.
         Masked bitmap positioned in image-space via overlayMatrix.

RULE 9:  featherRadius in IMAGE-SPACE PIXELS. Scales with viewMatrix naturally.

RULE 10: Serialized as part of AnnotationProject with image-pixel coords.
```
