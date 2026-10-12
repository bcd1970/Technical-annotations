# Session 16 — Freehand + Snap-to-Close + Polyline + Polygon

## GOAL
Implement freehand drawing with Catmull-Rom smoothing and RDP point reduction, smooth endpoint joining (snap-to-close), multi-point polyline, and filled polygon. These are the path-based drawing primitives.

## DEPENDS ON
- **S-10** (CE-06, CE-03) — Rendering pipeline, coordinate pipeline
- **S-02** (IC-23) — Haptic vocabulary (CLOCK_TICK for snap engagement)
- **S-03** (PD-05) — AnnotationElement sealed class

## ATOMS
- **DP-30** · Freehand Curve — Catmull-Rom + RDP (M)
- **DP-62** · Curve Snap-to-Close (S)
- **DP-63** · Polyline — Multi-Point Path (M)
- **DP-64** · Polygon — Closed Polyline + Fill (S)

## SPEC REFERENCES (inlined)

### DP-30 · Freehand Curve
`FreehandElement`: point list. `CatmullRomSmoother` (centripetal α=0.5). RDP point reduction (60–80% reduction). Pipeline: raw→RDP→Catmull-Rom→cached Path.

**Acceptance:** Smooth, no lag during drawing. 500-point curve at 60fps.

### DP-62 · Curve Snap-to-Close
During freehand drawing: if current point enters ~20dp screen-space radius of start point, visual magnet indicator appears (filled dot + snap ring on start point). Lift finger inside radius → curve closes with smooth Catmull-Rom join (start point = first AND last control point, C1 continuity). Lift outside → open curve as normal. Haptic CLOCK_TICK on snap engagement. Closed curves gain `isClosed: Boolean` + optional semi-transparent fill. Snap radius in screen-space (constant regardless of zoom).

### DP-63 · Polyline (Multi-Point Path)
`PolylineElement`: ordered vertex list, isClosed Boolean. Gesture: tap vertex → tap next → ... → double-tap to complete. Straight segments between vertices. Preview line from last vertex to touch point. Each vertex draggable after placement. Works with snap-to-close (DP-62) — approaching first vertex snaps to polygon.

### DP-64 · Polygon (Closed Polyline + Fill)
PolylineElement with `isClosed=true` + fill properties. Semi-transparent fill, color from semantic ink with reduced opacity. Area auto-calculated for closed polygon (feeds DP-16 in Phase 2). Boundary stroke on top of fill.

## EXISTING CODE TO INTEGRATE WITH

### From S-10
- `AnnotationRenderer` interface — implement FreehandRenderer, PolygonRenderer
- `RendererRegistry` — register new renderers
- `HitTestEngine` — path-based hit-testing (point near path, point in polygon)
- `SelectionManager` — vertex handles for polyline/polygon

### From S-02
- `HapticFeedback.CLOCK_TICK` — fire on snap-to-close engagement

### From S-09
- `StatelessGestureRouter` — route continuous drag to freehand, taps to polyline
- `CoordinatePipeline` — screenToImage for every touch point

### From S-12
- Shapes category in toolbar — add freehand, polyline tools to tool tray

## DELIVERABLES

### 1. CatmullRomSmoother.kt
```kotlin
object CatmullRomSmoother {
    // Centripetal Catmull-Rom spline (α = 0.5)
    // fun smooth(points: List<ImagePoint>, segmentsPerCurve: Int = 10): Path
    
    // For closed curves: first point = last point, ensure C1 continuity
    // Use knot parameterization: t_i = t_{i-1} + |p_i - p_{i-1}|^α
    // This prevents cusps and self-intersections
    
    // Returns Android Path for direct Canvas rendering
}
```

### 2. RDPReducer.kt
```kotlin
object RDPReducer {
    // Ramer-Douglas-Peucker point reduction
    // fun reduce(points: List<ImagePoint>, epsilon: Float): List<ImagePoint>
    
    // Target: 60-80% point reduction
    // epsilon calibrated to ~2dp in image-space at current zoom
    // Preserves sharp corners, removes collinear points
}
```

### 3. FreehandRenderer.kt
```kotlin
class FreehandRenderer : AnnotationRenderer {
    // Renders FreehandElement:
    // 1. During drawing: render raw points as path (no smoothing yet, for responsiveness)
    // 2. On finger-up: run RDP → Catmull-Rom → cache smoothed Path
    // 3. For rendering: use cached smooth Path
    // 4. If isClosed: fill with semi-transparent fillColor
    
    // Snap-to-close visual indicator:
    // - Filled dot at start point when proximity detected
    // - Animated snap ring (growing/shrinking circle)
}
```

### 4. PolylineHandler.kt
```kotlin
class PolylineHandler {
    // Handles multi-tap polyline creation:
    // - First tap: create first vertex, start preview line
    // - Subsequent taps: add vertex, extend polyline
    // - Double-tap: complete polyline (last tap NOT added as extra vertex)
    // - Approaching first vertex: show snap indicator, close to polygon on tap
    
    // State: currentVertices: MutableList<ImagePoint>
    // Preview: line from last vertex to current touch position
}
```

### 5. PolygonRenderer.kt
```kotlin
class PolygonRenderer : AnnotationRenderer {
    // Renders PolylineElement (both open polyline and closed polygon)
    // Open: straight line segments between vertices
    // Closed (isClosed=true): 
    //   1. Fill with semi-transparent color (Path.FillType.EVEN_ODD)
    //   2. Stroke boundary on top
    //   3. Area calculation stored in element metadata
    
    // Vertex handles: one per vertex, all draggable
    // Edge midpoint handles for adding vertices (Phase 2)
}
```

## DATA MODELS

### FreehandElement
```kotlin
@Serializable
data class FreehandElement(
    override val id: String = UUID.randomUUID().toString(),
    val points: List<ImagePoint>,       // raw points (before smoothing)
    val smoothedPath: SerializablePath?, // cached — rebuilt on deserialization
    val isClosed: Boolean = false,
    val strokeWidth: Float = 3f,
    val strokeColor: Long,
    val fillColor: Long? = null,        // only when isClosed = true
) : AnnotationElement()
```

### PolylineElement
```kotlin
@Serializable
data class PolylineElement(
    override val id: String = UUID.randomUUID().toString(),
    val vertices: List<ImagePoint>,
    val isClosed: Boolean = false,
    val strokeWidth: Float = 3f,
    val strokeColor: Long,
    val fillColor: Long? = null,        // only when isClosed = true
    val area: Double? = null,           // calculated for closed polygons
) : AnnotationElement()
```

## KEY IMPLEMENTATION DETAILS

### Freehand Drawing Pipeline
```
Touch events → raw points (image-space)
           → [on finger-up] → RDP reduce (60-80% fewer points)
           → Catmull-Rom smooth (centripetal α=0.5)
           → cached Android Path
           → render via Canvas.drawPath()
```

During active drawing, render raw points directly (no smoothing) for zero-latency feedback. Smoothing only on finger-up.

### Centripetal Catmull-Rom (α = 0.5)
Why centripetal: prevents cusps and self-intersections that uniform (α=0) or chordal (α=1) produce. The sweet spot.

```kotlin
// Knot parameterization
fun knotInterval(p0: ImagePoint, p1: ImagePoint, alpha: Float): Float {
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    return (dx * dx + dy * dy).pow(alpha * 0.5f)
}
```

### Snap-to-Close Detection
```kotlin
// Check in SCREEN-SPACE (constant radius regardless of zoom)
val startScreen = imageToScreen(freehandPoints.first())
val currentScreen = imageToScreen(currentPoint)
val distance = (startScreen - currentScreen).magnitude()
val snapRadius = 20.dp.toPx()

if (distance < snapRadius) {
    showSnapIndicator(startScreen)
    haptic(CLOCK_TICK) // only once on enter
    if (fingerUp) closeCurve()
}
```

### Closed Curve Smoothing
For closed curves: duplicate first point as last point, then use the last-before-close and first-after-open as additional control points. This ensures C1 continuity at the join — no visible seam.

### Polygon Area Calculation (Shoelace Formula)
```kotlin
fun calculateArea(vertices: List<ImagePoint>): Double {
    var area = 0.0
    for (i in vertices.indices) {
        val j = (i + 1) % vertices.size
        area += vertices[i].x * vertices[j].y
        area -= vertices[j].x * vertices[i].y
    }
    return abs(area) / 2.0
}
```
Stored in `PolylineElement.area` — used by DP-16 (Area Measurement) in Phase 2.

### Hit-Testing
- **Freehand:** min distance from point to any segment in the cached path. Threshold = max(24dp, strokeWidth + 12dp). For closed+filled: also point-in-path.
- **Polyline:** point-to-segment distance for each segment.
- **Polygon:** point-in-polygon (ray casting) for filled area, point-to-segment for edge.

### Double-Tap Completion
Polyline double-tap detection: if time between last two taps < 300ms and distance < 24dp, treat as double-tap → complete polyline WITHOUT adding the second tap as a vertex. The last vertex is the first tap of the double-tap.

## RESOLVED CONFLICTS
None specific to this session.

## STUB REQUIREMENTS

### Area Display (for S-18 / Phase 2)
- `PolylineElement.area` is calculated and stored but NOT rendered as text overlay
- DP-16 (Area Measurement) in Phase 2 adds the visual area display

### Edge Midpoint Handles (Phase 2)
- Polyline/polygon vertices are draggable after placement
- Adding new vertices by tapping edge midpoints deferred to Phase 2

## TEST CRITERIA

### Freehand Curve
- [ ] Drawing feels smooth with zero perceptible lag
- [ ] 500-point curve renders at 60fps on S25 Ultra
- [ ] RDP reduction removes 60-80% of points (log before/after count)
- [ ] Catmull-Rom produces smooth curves with no cusps
- [ ] Curve persists correctly (serialize → save → reload → smooth path rebuilt)

### Snap-to-Close
- [ ] Snap indicator (filled dot + ring) appears at ~20dp proximity in screen-space
- [ ] Haptic CLOCK_TICK fires once on entering snap zone
- [ ] Closing produces no visible seam at join point
- [ ] Snap radius is constant regardless of zoom (screen-space)
- [ ] Closed curve can receive semi-transparent fill
- [ ] Lifting finger outside snap zone produces open curve

### Polyline
- [ ] Each tap adds a vertex with preview line to current touch
- [ ] Double-tap completes without adding extra vertex
- [ ] Unlimited vertices (test with 50+)
- [ ] Each vertex independently draggable after placement
- [ ] Approaching first vertex shows snap indicator → closes to polygon

### Polygon
- [ ] Closed polyline renders with semi-transparent fill
- [ ] Fill renders correctly for both convex and concave shapes
- [ ] Self-intersecting polygons handled gracefully (no crash, EVEN_ODD fill)
- [ ] Area calculated and stored in element
- [ ] Boundary stroke renders on top of fill

### Integration
- [ ] Freehand and polyline tools appear in Shapes tool tray
- [ ] Undo/redo works for all path-based elements
- [ ] Selection shows vertex handles for polyline/polygon

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../math/CatmullRomSmoother.kt`
2. `feature-canvas/src/main/java/.../math/RDPReducer.kt`
3. `feature-canvas/src/main/java/.../render/FreehandRenderer.kt`
4. `feature-canvas/src/main/java/.../gesture/PolylineHandler.kt`
5. `feature-canvas/src/main/java/.../render/PolygonRenderer.kt`

### Modify
- `core/model/src/main/java/.../AnnotationElement.kt` — add FreehandElement, PolylineElement
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt` — register FreehandRenderer, PolygonRenderer
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt` — route continuous drag to freehand, multi-tap to polyline
- `feature-canvas/src/main/java/.../input/HitTestEngine.kt` — path and polygon hit-testing
- `core/data/src/main/java/.../db/TypeConverters.kt` — path element serialization

---

*Session 16 · Size L · 4 atoms · CatmullRom + RDP reused by S-17 (revision cloud), S-19 (Smart Paper)*
