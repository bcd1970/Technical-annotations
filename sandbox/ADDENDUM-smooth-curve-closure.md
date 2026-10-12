# Smooth Curve Closure — Research & Implementation Spec

> **Addendum to:** FINAL-complete-implementation-spec.md  
> **Problem:** When drawing a curve/spline and the endpoint approaches the start point, snap to it and close the path with no visible corner — just a smooth continuous loop.

---

## THE PROBLEM

```
                    ╲
     start ●────────╳  ← UGLY: sharp corner at closure point
            ╱          (C0 continuity only — positions match but tangents don't)
           ╱

                    ╲
     start ●─────────╮  ← GOOD: smooth seamless closure
            ╲        │   (C1 continuity — tangents also match)
             ╰───────╯
```

When the user draws a freehand curve and the endpoint gets close to the start point, we need:
1. **Snap detection** — recognize when endpoint is near start
2. **Smooth join** — ensure the tangent at the join matches on both sides (C1 continuity minimum)
3. **Visual feedback** — show the user it will close smoothly before they release

---

## RESEARCH FINDINGS: 3 APPROACHES RANKED

### ★ BEST: Centripetal Catmull-Rom Spline with Closed Loop

**Why best:** Catmull-Rom splines pass THROUGH the control points (unlike Bezier where control points are off-curve). This makes them perfect for freehand drawing — the sampled touch points ARE the control points. The centripetal variant (α=0.5) guarantees no cusps or self-intersections.

**How closure works:**
- For an open spline with points [P0, P1, P2, ..., Pn], each segment needs 4 points: Pi-1, Pi, Pi+1, Pi+2
- The first and last segments need "phantom" points beyond the endpoints
- For CLOSURE: wrap the array — the phantom points come from the other end of the loop

```
OPEN spline points:  [P0, P1, P2, P3, P4, P5]
  First segment needs: [???, P0, P1, P2]    ← missing predecessor
  Last segment needs:  [P3, P4, P5, ???]    ← missing successor

CLOSED spline points: [P0, P1, P2, P3, P4, P5] where P5 is snapped to ≈P0
  Remove P5 (it's now P0), then WRAP:
  Points become circular: [..., P4, P0, P1, P2, ...]
  
  Segment 0: [P4, P0, P1, P2]    ← P4 wraps from end
  Segment 1: [P0, P1, P2, P3]
  Segment 2: [P1, P2, P3, P4]
  Segment 3: [P2, P3, P4, P0]    ← P0 wraps from start
  Segment 4: [P3, P4, P0, P1]    ← closing segment, wraps both ways
```

**Result:** The tangent at the join point (P0) is automatically computed from its neighbors (P4 and P1), giving perfect C1 continuity. No special case math needed — the wrap handles everything.

**Key formula (centripetal variant, α=0.5):**
```
Tangent at Pi = (Pi+1 - Pi-1) / 2    (uniform case, simple)

For centripetal (better, no cusps):
  t_i = t_{i-1} + |P_i - P_{i-1}|^α     where α = 0.5
  Uses Barry-Goldman recursive evaluation
```

### ★★ GOOD: Cubic Bezier Spline with C1 Closure

**How it works:**
- Convert sampled points to cubic Bezier segments
- Each segment has 4 control points: anchor1, control1, control2, anchor2
- For smooth join: the last control point before anchor and first after must be collinear with the anchor (and equidistant for C2)

**Closure constraint (C1):**
```
At the join point (first = last anchor):
  controlBefore and controlAfter must be collinear with the anchor point
  
  i.e., the handle from the last segment's control2 → anchor
        must be mirrored to  anchor → first segment's control1
        
  controlAfter = anchor + (anchor - controlBefore)
  = 2 * anchor - controlBefore
```

**Downside:** Requires solving a tridiagonal system to find optimal control points for the whole spline. More complex than Catmull-Rom.

### ★★★ SIMPLEST: Android Path.close() + Post-Processing

**How it works:**
- Collect touch points during freehand drawing
- Use `Path.quadTo()` or `Path.cubicTo()` for smooth segments
- When snapping to close, DON'T use `Path.close()` (that creates a straight line = corner!)
- Instead: replace the last few segments with a smooth curve back to start

**Why not best:** Only smooths the last portion, doesn't guarantee global smoothness.

---

## RECOMMENDED IMPLEMENTATION: Centripetal Catmull-Rom

### Complete Kotlin Implementation

```kotlin
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.*

/**
 * Centripetal Catmull-Rom spline with optional closed-loop support.
 * 
 * Converts sampled touch points into a smooth Path that can be drawn on Canvas.
 * When closed=true, the path forms a smooth loop with C1 continuity at the join.
 *
 * Usage:
 *   val smoother = CatmullRomSmoother(alpha = 0.5f, tension = 0f)
 *   val path = smoother.createPath(points, closed = true)
 *   canvas.drawPath(path, paint)
 */
class CatmullRomSmoother(
    private val alpha: Float = 0.5f,    // 0=uniform, 0.5=centripetal (best), 1=chordal
    private val tension: Float = 0f,     // 0=standard, 1=straight lines
    private val segmentsPerCurve: Int = 16  // smoothness (more = smoother but heavier)
) {
    
    /**
     * Creates a smooth Path through the given points.
     * 
     * @param points  The control points (from touch samples)
     * @param closed  If true, creates a smooth closed loop
     * @return        A smooth android.graphics.Path ready for canvas.drawPath()
     */
    fun createPath(points: List<PointF>, closed: Boolean = false): Path {
        if (points.size < 2) return Path()
        if (points.size == 2) return Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            if (closed) close()
        }
        
        val path = Path()
        val pts = if (closed) points.toMutableList() else points
        
        // For closed: we need to wrap — last point connects back to first
        // Remove duplicate endpoint if it's the same as start (from snap)
        val workingPoints = if (closed && pts.last().distTo(pts.first()) < 1f) {
            pts.dropLast(1)
        } else {
            pts
        }
        
        val n = workingPoints.size
        if (n < 3) return Path().apply {
            moveTo(workingPoints[0].x, workingPoints[0].y)
            for (i in 1 until n) lineTo(workingPoints[i].x, workingPoints[i].y)
            if (closed) close()
        }
        
        path.moveTo(workingPoints[0].x, workingPoints[0].y)
        
        val segmentCount = if (closed) n else n - 1
        
        for (i in 0 until segmentCount) {
            // Get 4 points for this segment, with wrapping for closed loops
            val p0 = workingPoints[wrapIndex(i - 1, n, closed)]
            val p1 = workingPoints[wrapIndex(i, n, closed)]
            val p2 = workingPoints[wrapIndex(i + 1, n, closed)]
            val p3 = workingPoints[wrapIndex(i + 2, n, closed)]
            
            // For the FIRST segment of an OPEN curve: mirror p0
            val effectiveP0 = if (!closed && i == 0) {
                PointF(2 * p1.x - p2.x, 2 * p1.y - p2.y) // reflect p2 through p1
            } else p0
            
            // For the LAST segment of an OPEN curve: mirror p3
            val effectiveP3 = if (!closed && i == segmentCount - 1) {
                PointF(2 * p2.x - p1.x, 2 * p2.y - p1.y) // reflect p1 through p2
            } else p3
            
            // Convert Catmull-Rom to cubic Bezier control points
            val (cp1, cp2) = catmullRomToBezier(effectiveP0, p1, p2, effectiveP3)
            
            // Add cubic Bezier segment to path
            path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
        }
        
        if (closed) {
            path.close() // This close() is safe now because the last cubicTo already
                         // smoothly arrives at p[0], so close() adds nothing visible
        }
        
        return path
    }
    
    /**
     * Converts a Catmull-Rom segment (4 points) into cubic Bezier control points.
     * This is the key math that makes it work with Android's Path.cubicTo().
     * 
     * Returns the two INNER control points (cp1, cp2) for cubicTo().
     * The start point (p1) is already the current path position.
     * The end point (p2) is passed directly to cubicTo().
     */
    private fun catmullRomToBezier(
        p0: PointF, p1: PointF, p2: PointF, p3: PointF
    ): Pair<PointF, PointF> {
        
        // Compute knot intervals using centripetal parameterization
        val d01 = p0.distTo(p1).pow(alpha)
        val d12 = p1.distTo(p2).pow(alpha)
        val d23 = p2.distTo(p3).pow(alpha)
        
        // Compute tangents at p1 and p2
        val t1x: Float
        val t1y: Float
        val t2x: Float
        val t2y: Float
        
        if (d01 < 0.0001f) {
            // Degenerate: p0 ≈ p1
            t1x = p2.x - p1.x
            t1y = p2.y - p1.y
        } else {
            // Standard centripetal tangent at p1
            t1x = (1f - tension) * d12 * (
                (p1.x - p0.x) / d01 - (p2.x - p0.x) / (d01 + d12) + (p2.x - p1.x) / d12
            )
            t1y = (1f - tension) * d12 * (
                (p1.y - p0.y) / d01 - (p2.y - p0.y) / (d01 + d12) + (p2.y - p1.y) / d12
            )
        }
        
        if (d23 < 0.0001f) {
            // Degenerate: p2 ≈ p3
            t2x = p2.x - p1.x
            t2y = p2.y - p1.y
        } else {
            // Standard centripetal tangent at p2
            t2x = (1f - tension) * d12 * (
                (p2.x - p1.x) / d12 - (p3.x - p1.x) / (d12 + d23) + (p3.x - p2.x) / d23
            )
            t2y = (1f - tension) * d12 * (
                (p2.y - p1.y) / d12 - (p3.y - p1.y) / (d12 + d23) + (p3.y - p2.y) / d23
            )
        }
        
        // Convert tangents to Bezier control points
        // cp1 = p1 + tangent1 / 3
        // cp2 = p2 - tangent2 / 3
        val cp1 = PointF(p1.x + t1x / 3f, p1.y + t1y / 3f)
        val cp2 = PointF(p2.x - t2x / 3f, p2.y - t2y / 3f)
        
        return cp1 to cp2
    }
    
    /**
     * Wraps an index for closed loops, or clamps for open curves.
     */
    private fun wrapIndex(i: Int, n: Int, closed: Boolean): Int {
        return if (closed) {
            ((i % n) + n) % n  // modulo that handles negatives correctly
        } else {
            i.coerceIn(0, n - 1)
        }
    }
    
    /** Euclidean distance between two points */
    private fun PointF.distTo(other: PointF): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /** Float.pow for non-integer exponents */
    private fun Float.pow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()
}
```

### Snap-to-Close Detection & Visual Feedback

```kotlin
/**
 * Manages the snap-to-close behavior during freehand drawing.
 * 
 * FLOW:
 * 1. User draws freehand curve (collecting touch sample points)
 * 2. On each ACTION_MOVE: check if current point is within SNAP_RADIUS of start
 * 3. If within radius:
 *    a. Show visual feedback (glowing ring around start point)
 *    b. Show ghost preview of the smooth closure
 * 4. On ACTION_UP within snap radius:
 *    a. Snap endpoint to start point
 *    b. Generate smooth closed path using CatmullRomSmoother(closed=true)
 *    c. Replace raw points with smooth path
 */
class SnapToCloseManager(
    private val snapRadiusDp: Float = 20f,   // how close before snapping activates
    private val previewRadiusDp: Float = 30f  // when to start showing the preview
) {
    private var startPoint: PointF? = null
    private var isNearStart = false
    private var isSnapped = false
    
    /** Call when user starts drawing (ACTION_DOWN) */
    fun onDrawStart(point: PointF) {
        startPoint = point
        isNearStart = false
        isSnapped = false
    }
    
    /** Call on each touch move. Returns snap state. */
    fun onDrawMove(currentPoint: PointF, density: Float): SnapState {
        val start = startPoint ?: return SnapState.DRAWING
        val distance = start.distTo(currentPoint)
        val snapRadiusPx = snapRadiusDp * density
        val previewRadiusPx = previewRadiusDp * density
        
        return when {
            distance < snapRadiusPx -> {
                isNearStart = true
                isSnapped = true
                SnapState.SNAPPED  // endpoint locks to start, show "will close" indicator
            }
            distance < previewRadiusPx -> {
                isNearStart = true
                isSnapped = false
                SnapState.APPROACHING(
                    progress = 1f - (distance - snapRadiusPx) / (previewRadiusPx - snapRadiusPx)
                )
            }
            else -> {
                isNearStart = false
                isSnapped = false
                SnapState.DRAWING
            }
        }
    }
    
    /** Call on ACTION_UP. Returns whether the path should be closed. */
    fun onDrawEnd(): Boolean = isSnapped
    
    sealed class SnapState {
        object DRAWING : SnapState()           // normal drawing, far from start
        data class APPROACHING(
            val progress: Float                 // 0.0 = just entered preview zone, 1.0 = at snap edge
        ) : SnapState()
        object SNAPPED : SnapState()           // locked to start, will close
    }
    
    private fun PointF.distTo(other: PointF): Float {
        val dx = x - other.x; val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
```

### Visual Feedback Rendering

```kotlin
/**
 * Draw the snap-to-close visual feedback on the canvas.
 * Called during freehand drawing when near the start point.
 */
fun drawSnapFeedback(
    canvas: Canvas,
    startPoint: PointF,
    snapState: SnapToCloseManager.SnapState,
    density: Float
) {
    when (snapState) {
        is SnapToCloseManager.SnapState.APPROACHING -> {
            // Pulsing ring around start point, growing as user approaches
            val maxRadius = 20f * density
            val currentRadius = maxRadius * (0.5f + 0.5f * snapState.progress)
            val alpha = (80 * snapState.progress).toInt()
            
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, 88, 166, 255) // semi-transparent blue
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
            }
            canvas.drawCircle(startPoint.x, startPoint.y, currentRadius, ringPaint)
            
            // Dashed circle showing snap zone
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(40, 88, 166, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
            }
            canvas.drawCircle(startPoint.x, startPoint.y, maxRadius, dashPaint)
        }
        
        is SnapToCloseManager.SnapState.SNAPPED -> {
            // Solid glowing ring = "will close smoothly"
            val radius = 14f * density
            
            // Glow
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 63, 185, 80) // green glow
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(8f * density, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(startPoint.x, startPoint.y, radius * 1.5f, glowPaint)
            
            // Ring
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, 63, 185, 80) // solid green
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
            }
            canvas.drawCircle(startPoint.x, startPoint.y, radius, ringPaint)
            
            // Dot at center
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(255, 63, 185, 80)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(startPoint.x, startPoint.y, 3f * density, dotPaint)
            
            // Haptic feedback (call once when entering SNAPPED state)
            // view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        
        else -> { /* DRAWING state = no feedback */ }
    }
}
```

### Point Reduction (Pre-Processing Before Spline)

Touch events generate WAY too many points (hundreds per second). Before feeding to the spline, reduce them:

```kotlin
/**
 * Ramer-Douglas-Peucker algorithm to reduce points while preserving shape.
 * This removes redundant points from dense touch sampling.
 * 
 * @param points   Raw touch samples
 * @param epsilon  Tolerance in dp (higher = fewer points, less accurate)
 *                 Recommended: 2.0dp for finger, 1.0dp for stylus
 * @return         Reduced point list
 */
fun reducePoints(points: List<PointF>, epsilon: Float): List<PointF> {
    if (points.size < 3) return points
    
    // Find the point with the maximum distance from the line (first→last)
    val first = points.first()
    val last = points.last()
    var maxDist = 0f
    var maxIndex = 0
    
    for (i in 1 until points.size - 1) {
        val dist = perpendicularDistance(points[i], first, last)
        if (dist > maxDist) {
            maxDist = dist
            maxIndex = i
        }
    }
    
    return if (maxDist > epsilon) {
        // Split and recurse
        val left = reducePoints(points.subList(0, maxIndex + 1), epsilon)
        val right = reducePoints(points.subList(maxIndex, points.size), epsilon)
        left.dropLast(1) + right
    } else {
        // All intermediate points are within tolerance — keep only endpoints
        listOf(first, last)
    }
}

private fun perpendicularDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val lenSq = dx * dx + dy * dy
    if (lenSq < 0.0001f) return point.distTo(lineStart)
    
    val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lenSq
    val proj = PointF(lineStart.x + t * dx, lineStart.y + t * dy)
    return point.distTo(proj)
}
```

### Complete Drawing Flow

```kotlin
/**
 * COMPLETE FREEHAND CURVE DRAWING FLOW
 * Integrates: touch sampling → point reduction → smooth spline → snap-to-close
 */
class FreehandCurveDrawer(
    private val smoother: CatmullRomSmoother = CatmullRomSmoother(),
    private val snapManager: SnapToCloseManager = SnapToCloseManager()
) {
    private val rawPoints = mutableListOf<PointF>()
    private var currentPath: Path? = null
    private var isClosed = false
    
    /** ACTION_DOWN */
    fun onTouchDown(x: Float, y: Float) {
        rawPoints.clear()
        rawPoints.add(PointF(x, y))
        snapManager.onDrawStart(PointF(x, y))
        isClosed = false
    }
    
    /** ACTION_MOVE — called on every touch move event */
    fun onTouchMove(x: Float, y: Float, density: Float): SnapToCloseManager.SnapState {
        val current = PointF(x, y)
        rawPoints.add(current)
        
        // Check snap
        val snapState = snapManager.onDrawMove(current, density)
        
        // Generate preview path (reduced + smoothed)
        val reduced = reducePoints(rawPoints, 2f * density)
        val previewClosed = snapState is SnapToCloseManager.SnapState.SNAPPED
        currentPath = smoother.createPath(reduced, closed = previewClosed)
        
        return snapState
    }
    
    /** ACTION_UP — finalize the curve */
    fun onTouchUp(): FreehandCurveResult {
        isClosed = snapManager.onDrawEnd()
        
        // Final point reduction (can be slightly more aggressive)
        val finalPoints = reducePoints(rawPoints, 2f)
        
        // Generate the final smooth path
        val finalPath = smoother.createPath(finalPoints, closed = isClosed)
        
        return FreehandCurveResult(
            path = finalPath,
            controlPoints = finalPoints,
            isClosed = isClosed
        )
    }
    
    /** Get the current preview path (for drawing during gesture) */
    fun getPreviewPath(): Path? = currentPath
}

data class FreehandCurveResult(
    val path: Path,
    val controlPoints: List<PointF>,
    val isClosed: Boolean
)
```

---

## WHY CENTRIPETAL CATMULL-ROM (α=0.5) IS THE BEST CHOICE

| Property | Uniform (α=0) | Centripetal (α=0.5) | Chordal (α=1) |
|---|---|---|---|
| Cusps / self-intersections | ❌ Can occur | ✅ Guaranteed none | ✅ Rare |
| Follows points tightly | ❌ Loose | ✅ Balanced | ❌ Too tight |
| Smooth visual result | ⚠️ OK | ✅ Best | ⚠️ Stiff |
| Performance | ✅ Fastest | ✅ Fast | ✅ Fast |
| Passes through all points | ✅ Yes | ✅ Yes | ✅ Yes |
| C1 continuity at joins | ✅ Yes | ✅ Yes | ✅ Yes |

The centripetal variant (α=0.5) is mathematically proven to never produce cusps or self-intersections within curve segments — the only parameterization with this guarantee (Yuksel et al., 2011).

---

## INTEGRATION WITH THE 43 PRIMITIVES

This freehand curve + closure system is used by:

| Primitive | How It Uses Curves |
|---|---|
| #39 Revision Cloud | Freehand loop → snap close → convert to cloud bumps |
| #38 Component Zone | Freehand boundary → snap close → fill with tint |
| #37 Perforation Zone | Freehand boundary → snap close → fill with dots |
| #36 Hatching Boundary | Manual boundary option → freehand loop → snap close → fill with pattern |
| **NEW: Freehand Curve** | Standalone drawing tool for custom shapes, outlines, annotations |

The `CatmullRomSmoother` is also useful for smoothing ANY polyline path (even non-closed), like stitch lines or decorative curves.
