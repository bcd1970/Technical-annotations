# Session 18 — Dimensions (Linear + Angular)

## GOAL
Implement dimension line (two-tap measurement) and angular dimension (three-tap angle measurement). These are the measurement primitives used by engineers and inspectors.

## DEPENDS ON
- **S-15** (DP-02) — ArrowheadRenderer (dimension lines use arrowheads at both ends)

## ATOMS
- **DP-13** · Dimension Line — Two-Tap (M)
- **DP-14** · Angular Dimension — Three-Tap (M)

## SPEC REFERENCES (inlined)

### DP-13 · Dimension Line (Two-Tap)
`DimensionElement`: point1, point2, extensionLineHeight, measurementText, precision (0–3 decimals), unit. Tap-tap gesture. Extension lines perpendicular to measurement line. Arrowheads both ends (default: architectural tick). Text centered above, auto-rotates for readability. Calibration-aware (DP-17 in Phase 2).

### DP-14 · Angular Dimension (Three-Tap)
`AngularDimensionElement`: vertex, point1, point2. Arc drawn between the two lines with angle in degrees. Handles acute, right, and obtuse angles. Reflex option via properties.

## EXISTING CODE TO INTEGRATE WITH

### From S-15
- `ArrowheadRenderer` — dimension lines default to TECH_ARCH_TICK (architectural tick) arrowheads
- `ArrowheadRenderer.getInsetDistance()` — inset-point for dimension arrowheads

### From S-14
- `LineRenderer` — dimension lines share line-drawing logic
- `TextRenderer` / `StaticLayout` — measurement text rendering

### From S-10
- `RendererRegistry`, `HitTestEngine`, `SelectionManager`

### From S-12
- Measure category in toolbar: Dimension Line (default), Angular Dimension

## DELIVERABLES

### 1. DimensionRenderer.kt
```kotlin
class DimensionRenderer : AnnotationRenderer {
    // Renders DimensionElement:
    // 1. Two extension lines: perpendicular to measurement direction, height configurable
    // 2. Measurement line between extension line tops
    // 3. Arrowheads at both ends of measurement line (default: TECH_ARCH_TICK)
    // 4. Measurement text centered above measurement line
    // 5. Text auto-rotates to always be readable (never upside-down)
    
    // Extension line anatomy:
    //   point1 ──┤              ├── point2
    //            ├──── 45.2 ────┤
    //            extension       extension
    //            line            line
}
```

### 2. MultiTapHandler.kt
```kotlin
class MultiTapHandler {
    // Handles sequential tap gestures:
    // - Two-tap: first tap = point1, second tap = point2 → create DimensionElement
    // - Three-tap: first = vertex, second = point1, third = point2 → create AngularDimensionElement
    
    // State machine: IDLE → FIRST_TAP → SECOND_TAP → (THIRD_TAP for angular)
    // Preview: line from first tap to current touch position
    // Cancel: tap toolbar or two-finger gesture resets state
    // Haptic: CONFIRM on each tap
}
```

### 3. AngularDimensionRenderer.kt
```kotlin
class AngularDimensionRenderer : AnnotationRenderer {
    // Renders AngularDimensionElement:
    // 1. Two lines from vertex to point1 and point2
    // 2. Arc between the two lines at configured radius
    // 3. Angle value in degrees centered on arc
    // 4. Text auto-rotates for readability
    
    // Angle calculation:
    //   angle = atan2(p1.y-v.y, p1.x-v.x) - atan2(p2.y-v.y, p2.x-v.x)
    // Handle: acute (<90°), right (90°), obtuse (90°-180°), reflex (>180°)
}
```

## DATA MODELS

### DimensionElement
```kotlin
@Serializable
data class DimensionElement(
    override val id: String = UUID.randomUUID().toString(),
    val point1: ImagePoint,
    val point2: ImagePoint,
    val extensionLineHeight: Float = 20f,   // dp
    val measurementText: String? = null,     // null = auto-calculate pixel distance
    val precision: Int = 1,                  // 0-3 decimal places
    val unit: String = "px",                 // "px" until calibration (DP-17, Phase 2)
    val arrowheadStyle: ArrowheadStyle = ArrowheadStyle.TECH_ARCH_TICK,
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val fontSize: Float = 12f,
) : AnnotationElement()
```

### AngularDimensionElement
```kotlin
@Serializable
data class AngularDimensionElement(
    override val id: String = UUID.randomUUID().toString(),
    val vertex: ImagePoint,
    val point1: ImagePoint,
    val point2: ImagePoint,
    val arcRadius: Float = 30f,             // dp
    val isReflex: Boolean = false,          // show >180° angle
    val precision: Int = 1,
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val fontSize: Float = 12f,
) : AnnotationElement()
```

## KEY IMPLEMENTATION DETAILS

### Text Readability Auto-Rotation
Measurement text must never appear upside-down. If the measurement line angle is between 90° and 270°, flip the text 180°:
```kotlin
val textAngle = if (lineAngle in 90f..270f) lineAngle - 180f else lineAngle
```

### Extension Lines
Extension lines are perpendicular to the measurement direction. They start slightly offset from the actual measurement point (gap ≈ 3dp) and extend upward by `extensionLineHeight`. The measurement line connects the tops of the extension lines.

### Pixel Distance Calculation
```kotlin
val distance = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
val text = "%.${precision}f $unit".format(distance)
```
Until calibration (DP-17, Phase 2), all measurements are in pixels.

### Angular Dimension Arc
```kotlin
// Calculate angles from vertex to each point
val angle1 = atan2((point1.y - vertex.y), (point1.x - vertex.x)).toDegrees()
val angle2 = atan2((point2.y - vertex.y), (point2.x - vertex.x)).toDegrees()

// Sweep angle (handle reflex)
var sweep = angle2 - angle1
if (!isReflex && sweep > 180) sweep -= 360
if (!isReflex && sweep < -180) sweep += 360
if (isReflex && abs(sweep) < 180) sweep = if (sweep > 0) sweep - 360 else sweep + 360

// Draw arc
canvas.drawArc(arcRect, angle1, sweep, false, paint)
```

### Hit-Testing
- **Dimension:** hit on measurement line, extension lines, or text bounding box
- **Angular:** hit on arc, reference lines, or text bounding box

## RESOLVED CONFLICTS
None specific to this session.

## STUB REQUIREMENTS

### Calibration (DP-17, Phase 2)
- Measurements display in "px" by default
- `DimensionElement.unit` field ready for real-world units
- DP-17 adds calibration session (tap known reference → enter distance → pixels-per-unit)

### Radius/Diameter Dimension (DP-15, Phase 2)
- Not implemented in this session. Depends on DP-13.

## TEST CRITERIA

### Dimension Line (DP-13)
- [ ] Two taps create dimension with correct measurement
- [ ] Extension lines perpendicular to measurement direction
- [ ] Text centered above measurement line
- [ ] Text never upside-down (auto-rotation works)
- [ ] Arrowheads render at both ends (default: architectural tick)
- [ ] Precision 0-3 decimals works
- [ ] Points draggable after placement → measurement updates
- [ ] All angles render correctly (horizontal, vertical, diagonal)

### Angular Dimension (DP-14)
- [ ] Three taps create angle measurement (vertex, point1, point2)
- [ ] Acute angles (<90°) display correctly
- [ ] Right angles (90°) display correctly with square indicator option
- [ ] Obtuse angles (90°-180°) display correctly
- [ ] Reflex option via properties shows >180° angle
- [ ] Arc renders smoothly
- [ ] Angle text centered on arc

### Integration
- [ ] Both tools in Measure toolbar category
- [ ] Undo/redo works
- [ ] Elements persist correctly

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../render/DimensionRenderer.kt`
2. `feature-canvas/src/main/java/.../gesture/MultiTapHandler.kt`
3. `feature-canvas/src/main/java/.../render/AngularDimensionRenderer.kt`

### Modify
- `core/model/src/main/java/.../AnnotationElement.kt` — add DimensionElement, AngularDimensionElement
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt` — register renderers
- `feature-canvas/src/main/java/.../input/HitTestEngine.kt` — dimension hit-testing
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt` — route to MultiTapHandler for Measure tools
- `core/data/src/main/java/.../db/TypeConverters.kt` — serialization

---

*Session 18 · Size M · 2 atoms · Measurement primitives*
