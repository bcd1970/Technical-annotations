# Session 19 — Smart Paper + Draw=Annotate + Color as Gesture

## GOAL
Implement shape recognition engine (freehand→geometric shape), automatic Browse→Annotate transition on draw gesture, and thumb-slide-to-color-strip gesture. These three "intelligence" features make the app feel effortless.

## DEPENDS ON
- **S-14** (DP-18, DP-23) — CircleElement, RectangleElement (recognition targets)
- **S-15** (DP-03) — Arrow (recognition target)
- **S-16** (DP-30) — FreehandElement, CatmullRomSmoother (input to recognizer)
- **S-12** (IC-06) — Semantic ink color strip (Color as Gesture target)
- **S-09** (CE-04) — StatelessGestureRouter (Draw=Annotate gesture routing)

## ATOMS
- **IC-14** · Smart Paper Shape Recognition (L)
- **IC-20** · Draw = Enter Annotate (S)
- **IC-21** · Color as Gesture (S)

## SPEC REFERENCES (inlined)

### IC-14 · Smart Paper Shape Recognition
Freehand→recognized shape. Geometric analysis (circularity, corners, aspect ratio). Shapes: circle, rectangle, line, arrow, X, checkmark, triangle. 200ms snap preview + haptic. >70% confidence → snap, <70% → keep freehand. Wrong shape? Tap → 4-alternative picker, auto-dismiss 1.5s. Recognition <50ms. Pure geometry, no ML for Phase 1.

Dependencies: DP-30 (freehand), DP-18 (circle), DP-23 (rect), DP-03 (arrow).

### IC-20 · Draw = Enter Annotate (IMP-01)
In BROWSE: single-finger drag detected (velocity <300dp/s, sustained >80ms) → auto-transition to ANNOTATE mid-gesture. Stroke captured as first annotation. Pencil FAB remains as visual affordance but is no longer required. Special case: after camera capture, user lands in "primed" state where first gesture immediately draws — no FAB, no transition animation.

### IC-21 · Color as Gesture (IMP-02)
Option A: slide thumb to color strip → continue into draw (unbroken touch). One continuous gesture = color selection + first stroke.
Option B (S Pen): pressure=semantic color (light=blue, medium=yellow, firm=red).
Strip remains as fallback and current-color indicator.

## EXISTING CODE TO INTEGRATE WITH

### From S-16
- `FreehandElement.points` — raw point list fed to recognizer
- `CatmullRomSmoother` — smoothed path available for analysis

### From S-14
- Element constructors for recognized shapes (CircleElement, RectangleElement, etc.)

### From S-11
- `ModeStateMachine` — BROWSE→ANNOTATE transition for IC-20
- Mode transition animations from IC-02

### From S-12
- `SemanticInkStrip` — color strip component for IC-21 gesture
- `ToolPersistenceManager` — apply selected color

### From S-09
- `StatelessGestureRouter` — needs new routing for draw-in-browse detection

## DELIVERABLES

### 1. ShapeRecognizer.kt
```kotlin
class ShapeRecognizer {
    // fun recognize(points: List<ImagePoint>): RecognitionResult
    
    // Pipeline:
    // 1. Compute bounding box, centroid, total path length
    // 2. Circularity test: deviation from fitted circle
    // 3. Corner detection: angle changes > threshold
    // 4. Line test: points fit on a line (low perpendicular deviation)
    // 5. Arrow test: line + V-shape at one end
    // 6. X/checkmark: intersection detection
    // 7. Return best match with confidence 0.0-1.0
    
    // RecognitionResult: shape type, confidence, fitted parameters, alternatives
}

data class RecognitionResult(
    val shape: RecognizedShape,
    val confidence: Float,              // 0.0-1.0
    val fittedElement: AnnotationElement,
    val alternatives: List<RecognizedShape>, // up to 4
)

enum class RecognizedShape {
    CIRCLE, RECTANGLE, LINE, ARROW, TRIANGLE, X_MARK, CHECKMARK, UNKNOWN
}
```

### 2. CircularityAnalyzer.kt
```kotlin
object CircularityAnalyzer {
    // fun analyze(points: List<ImagePoint>): CircularityResult
    
    // Fit circle using least-squares:
    // 1. Compute centroid
    // 2. Average distance from centroid = radius
    // 3. Circularity = 1.0 - (stddev of distances / radius)
    // >0.85 = circle, 0.70-0.85 = possible circle
    
    // Also check: path is closed (end near start)
}
```

### 3. CornerDetector.kt
```kotlin
object CornerDetector {
    // fun detectCorners(points: List<ImagePoint>): List<CornerInfo>
    
    // Walk the path, compute angle change at each point (using neighbors)
    // Angle change > 30° = corner candidate
    // Cluster nearby corners (within 10dp)
    // 3 corners = triangle, 4 corners = rectangle/diamond, 2 = line/arrow
    
    // Rectangle test: 4 corners, roughly 90° each, aspect ratio
    // Arrow test: line body + V-shape (3 points at end)
}
```

### 4. DrawToAnnotateTransition.kt
```kotlin
class DrawToAnnotateTransition {
    // Monitors touch events in BROWSE mode
    // If: single finger, velocity < 300dp/s, sustained > 80ms
    // Then: transition to ANNOTATE mid-gesture
    //   - Do NOT restart touch sequence
    //   - Current touch becomes first stroke of annotation
    //   - Use current tool settings (color, width, tool type)
    
    // Distinction from navigation:
    //   Slow deliberate drag = drawing (< 300dp/s)
    //   Fast edge swipe = photo navigation (> 600dp/s, from edge)
    //   Two-finger = always zoom/pan
    
    // "Primed" state after camera capture:
    //   Skip velocity check, immediately start drawing
}
```

## DATA MODELS
No new data models. Recognition produces existing element types (CircleElement, RectangleElement, LineElement, etc.).

## KEY IMPLEMENTATION DETAILS

### Recognition Pipeline (IC-14)
```
Freehand stroke complete (finger-up)
  → Run ShapeRecognizer.recognize(rawPoints)  [<50ms]
  → if confidence > 0.70:
      → 200ms preview: show fitted shape as overlay (semi-transparent)
      → Haptic CONFIRM
      → Replace FreehandElement with fitted element
      → Show undo affordance (tap → 4-alternative picker)
  → if confidence ≤ 0.70:
      → Keep as freehand curve (no recognition)
```

### 4-Alternative Picker
When user taps a just-recognized shape:
- Show 4 alternatives as small previews around the shape
- Alternatives: top 4 matches from recognizer (or FREEHAND as last option)
- Tap alternative → replace with that shape
- Auto-dismiss after 1.5s if no tap
- Implementation: Compose overlay positioned near the shape

### Circularity Algorithm
```kotlin
fun analyzeCircularity(points: List<ImagePoint>): Float {
    val cx = points.map { it.x }.average()
    val cy = points.map { it.y }.average()
    val distances = points.map { sqrt((it.x - cx).pow(2) + (it.y - cy).pow(2)) }
    val avgRadius = distances.average()
    val stdDev = sqrt(distances.map { (it - avgRadius).pow(2) }.average())
    return 1.0f - (stdDev / avgRadius).toFloat()
}
```

### Corner Detection Algorithm
```kotlin
fun detectCorners(points: List<ImagePoint>, windowSize: Int = 5): List<Int> {
    val corners = mutableListOf<Int>()
    for (i in windowSize until points.size - windowSize) {
        val before = points[i - windowSize]
        val at = points[i]
        val after = points[i + windowSize]
        val angle = angleBetween(before, at, after)
        if (angle < 150f) corners.add(i)  // sharp turn
    }
    return clusterCorners(corners, points, minDistance = 10.dp)
}
```

### Draw = Annotate Velocity Detection
```kotlin
// In StatelessGestureRouter, BROWSE mode:
if (pointerCount == 1 && mode == BROWSE) {
    val velocity = velocityTracker.calculateVelocity()
    val speed = sqrt(velocity.x.pow(2) + velocity.y.pow(2))
    val duration = currentTime - touchDownTime
    
    if (speed < 300.dp && duration > 80.ms) {
        // This is drawing, not navigation
        modeStateMachine.transition(ANNOTATE)
        // Forward all accumulated touch events to drawing handler
        drawingHandler.replayFrom(touchDownEvent)
    }
}
```

### Color as Gesture (IC-21)
Option A implementation:
1. Touch begins on color strip
2. User slides thumb to desired color (strip highlights)
3. Without lifting, continues drag onto canvas
4. Canvas receives drawing with selected color
5. Requires: detect that touch started in strip region, then migrated to canvas region

S Pen pressure mapping (Option B):
```kotlin
// Only for S Pen (source type check)
if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
    val pressure = event.pressure  // 0.0-1.0
    val color = when {
        pressure < 0.3f -> SemanticColor.BLUE     // informational
        pressure < 0.6f -> SemanticColor.YELLOW   // caution
        else -> SemanticColor.RED                  // critical/defect
    }
    applySemanticColor(color)
}
```

## RESOLVED CONFLICTS
None specific to this session.

## STUB REQUIREMENTS
None — all dependencies available from prerequisite sessions.

## TEST CRITERIA

### Shape Recognition (IC-14)
- [ ] Circle recognition >90% accuracy (test with 20 hand-drawn circles)
- [ ] Rectangle recognition >85%
- [ ] Arrow recognition >85%
- [ ] Line recognition >95% (simplest case)
- [ ] Triangle recognition >80%
- [ ] Recognition completes <50ms
- [ ] 200ms preview shows fitted shape
- [ ] Haptic CONFIRM on recognition
- [ ] Tap recognized shape → 4-alternative picker appears
- [ ] Select alternative → shape replaces
- [ ] Picker auto-dismisses after 1.5s
- [ ] Low confidence (<70%) → keeps freehand (no recognition attempt visible to user)

### Draw = Annotate (IC-20)
- [ ] Slow drag in Browse → transitions to Annotate mid-gesture
- [ ] Stroke captured as first annotation (no lost input)
- [ ] Fast edge swipe (>600dp/s) does NOT trigger transition (remains photo navigation)
- [ ] Two-finger gesture does NOT trigger transition
- [ ] After camera capture: "primed" state draws immediately
- [ ] Pencil FAB still works as alternative entry to Annotate

### Color as Gesture (IC-21)
- [ ] Slide from color strip to canvas in one unbroken touch
- [ ] Selected color applies to the stroke
- [ ] S Pen pressure mapping works (light=blue, medium=yellow, firm=red)
- [ ] Color strip still works as normal tap-to-select fallback

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../recognition/ShapeRecognizer.kt`
2. `feature-canvas/src/main/java/.../recognition/CircularityAnalyzer.kt`
3. `feature-canvas/src/main/java/.../recognition/CornerDetector.kt`
4. `feature-canvas/src/main/java/.../mode/DrawToAnnotateTransition.kt`

### Modify
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt` — draw-in-browse detection + color gesture routing
- `feature-canvas/src/main/java/.../render/FreehandRenderer.kt` — recognition integration on finger-up
- `feature-canvas/src/main/java/.../mode/ModeStateMachine.kt` — primed state after camera
- `feature-canvas/src/main/java/.../ui/SemanticInkStrip.kt` — cross-boundary touch handling

---

*Session 19 · Size L · 3 atoms · The "intelligence" session — makes the app feel effortless*
