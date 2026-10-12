# Session 17 — Stamps + Pins + Hatching + Revision Cloud + Text Shapes

## GOAL
Implement condition stamps, defect markers, auto-sequencing numbered pins, hatching fill, revision cloud, circle+text, and labeled rectangle. These are the "Marks" category tools plus composite shapes.

## DEPENDS ON
- **S-14** (DP-18, DP-23, DP-25) — CircleElement, RectangleElement, TextElement, TapPlaceHandler
- **S-16** (DP-30) — FreehandElement, CatmullRomSmoother (revision cloud uses scalloped arcs)

## ATOMS
- **DP-50** · Pass/Fail/Caution Stamps (M)
- **DP-51** · Defect Markers — crack/corrosion/leak (S)
- **DP-52** · Numbered Pins — Auto-Sequence (M)
- **DP-31** · Hatching Fill (M)
- **DP-32** · Revision Cloud (M)
- **DP-19** · Circle + Text (S)
- **DP-24** · Labeled Rectangle (S)

## SPEC REFERENCES (inlined)

### DP-50 · Pass/Fail/Caution Stamps
`StampElement` with stampType: PASS/FAIL/CAUTION/CRACK/CORROSION/LEAK. Circular with icon + semantic color. One-tap place, confirmation haptic (CONFIRM). Auto-tagged for export. Configurable size S/M/L.

### DP-51 · Defect Markers
Specialized stamps: CRACK, CORROSION, LEAK. Same `StampElement`, different icons and semantic colors. Separated for toolbar clarity — defect markers in "Marks" tray alongside stamps.

### DP-52 · Numbered Pins (Auto-Sequence)
`NumberedPinElement`: position, sequenceNumber, prefix (D-/E-/P- per kit). Auto-number on placement: first pin = D-001, second = D-002. Auto-renumber on delete (200ms slide animation). Pin visual: filled circle + number centered. Long-press pin → rename/renumber.

### DP-31 · Hatching Fill
`HatchingFillElement`: boundary polygon + pattern (SINGLE/CROSS/DOTS). Hatch angle and spacing configurable. Lines clipped to boundary polygon.

### DP-32 · Revision Cloud
Boundary path with scalloped arc edges. Arc amplitude configurable. Smooth corner handling. Uses freehand drawing gesture (from DP-30) to define boundary, then replaces straight edges with cloud arcs.

### DP-19 · Circle + Text
Text centered inside circle. Circle auto-sizes to fit text. Combines CircleElement + TextElement as composite. Tap to place → keyboard opens → type → circle grows to fit.

### DP-24 · Labeled Rectangle
Text centered inside rectangle. Rectangle auto-sizes to fit text. Same pattern as circle+text.

## EXISTING CODE TO INTEGRATE WITH

### From S-14
- `TapPlaceHandler` — reuse for stamp and pin placement (single-tap)
- `CircleElement` — base for circle+text composite
- `RectangleElement` — base for labeled rectangle composite
- `TextElement` / `TextRenderer` — text rendering inside shapes
- `ShapeRenderer` — extend for composite shapes

### From S-16
- `FreehandElement` path drawing — revision cloud boundary drawn freehand
- `CatmullRomSmoother` — cloud arcs are similar curves

### From S-10
- `RendererRegistry` — register StampRenderer, NumberedPinRenderer, HatchingRenderer, RevisionCloudRenderer, TextShapeRenderer
- `HitTestEngine` — circular hit-test for stamps/pins, polygon for hatching/cloud

### From S-12
- Marks category in toolbar — stamps, defect markers, numbered pins
- Shapes category — hatching, revision cloud, circle+text, labeled rect

## DELIVERABLES

### 1. StampRenderer.kt
```kotlin
class StampRenderer : AnnotationRenderer {
    // Renders StampElement:
    // - Circular background with semantic color
    //   PASS = green, FAIL = red, CAUTION = amber
    //   CRACK/CORROSION/LEAK = red variants with defect icons
    // - Icon centered (checkmark, X, warning triangle, crack line, etc.)
    // - Size: S=32dp, M=48dp, L=64dp (configurable)
    // - Outer ring stroke + filled center
}
```

### 2. NumberedPinRenderer.kt
```kotlin
class NumberedPinRenderer : AnnotationRenderer {
    // Renders NumberedPinElement:
    // - Filled circle (semantic color) with white number centered
    // - Pin tail (teardrop shape pointing down)
    // - Number text: IBM Plex Mono for fixed-width alignment
    // - Format: "D-001" with prefix from active Trade Kit
    
    // Auto-numbering logic in ViewModel/Manager, not renderer
}
```

### 3. HatchingRenderer.kt
```kotlin
class HatchingRenderer : AnnotationRenderer {
    // Renders HatchingFillElement:
    // - Clip Canvas to boundary polygon
    // - Draw parallel lines at configured angle and spacing
    //   SINGLE: one direction
    //   CROSS: two perpendicular directions
    //   DOTS: dot pattern at grid intersections
    // - Boundary stroke on top
    
    // Uses Canvas.clipPath(boundaryPath) for clean clipping
    // Lines extend beyond boundary, clipping handles the rest
}
```

### 4. RevisionCloudRenderer.kt
```kotlin
class RevisionCloudRenderer : AnnotationRenderer {
    // Renders revision cloud (engineering convention):
    // - Take boundary path (drawn freehand or as rectangle)
    // - Replace straight edges with scalloped arcs
    // - Arc amplitude configurable (default ~10dp)
    // - Arcs always bulge outward from center
    // - Semi-transparent fill optional
    
    // Algorithm:
    // 1. Walk boundary path, extract points at regular intervals
    // 2. Between each pair of points, draw an arc (Canvas.drawArc or quadTo)
    // 3. Arc direction: perpendicular to edge, bulging outward
    // 4. Smooth transitions at corners
}
```

### 5. TextShapeRenderer.kt
```kotlin
class TextShapeRenderer : AnnotationRenderer {
    // Renders circle+text and labeled rectangle composites:
    // - Measure text using StaticLayout
    // - Auto-size shape to fit text + padding
    // - Circle: radius = max(textWidth, textHeight) / 2 + padding
    // - Rectangle: width = textWidth + paddingH, height = textHeight + paddingV
    // - Render shape first, then text centered inside
}
```

## DATA MODELS

### StampElement
```kotlin
@Serializable
data class StampElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val stampType: StampType,
    val size: StampSize = StampSize.M,
    val strokeColor: Long,              // auto-set from stampType semantic color
) : AnnotationElement()

enum class StampType {
    PASS, FAIL, CAUTION,                // condition stamps
    CRACK, CORROSION, LEAK              // defect markers (DP-51)
}
enum class StampSize { S, M, L }
```

### NumberedPinElement
```kotlin
@Serializable
data class NumberedPinElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val sequenceNumber: Int,
    val prefix: String = "D",           // from active Trade Kit
    val strokeColor: Long,
) : AnnotationElement() {
    val displayText: String get() = "$prefix-%03d".format(sequenceNumber)
}
```

### HatchingFillElement
```kotlin
@Serializable
data class HatchingFillElement(
    override val id: String = UUID.randomUUID().toString(),
    val boundaryVertices: List<ImagePoint>,
    val pattern: HatchPattern = HatchPattern.SINGLE,
    val angle: Float = 45f,             // degrees
    val spacing: Float = 8f,            // dp
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
) : AnnotationElement()

enum class HatchPattern { SINGLE, CROSS, DOTS }
```

### RevisionCloudElement
```kotlin
@Serializable
data class RevisionCloudElement(
    override val id: String = UUID.randomUUID().toString(),
    val boundaryPoints: List<ImagePoint>,
    val arcAmplitude: Float = 10f,      // dp
    val strokeWidth: Float = 2f,
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()
```

### CircleTextElement (DP-19)
```kotlin
@Serializable
data class CircleTextElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val text: String,
    val fontSize: Float = 14f,
    val strokeWidth: Float = 2f,
    val strokeColor: Long,
    val fillColor: Long? = null,
    val padding: Float = 8f,            // dp between text and circle edge
) : AnnotationElement()
```

### LabeledRectElement (DP-24)
```kotlin
@Serializable
data class LabeledRectElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val text: String,
    val fontSize: Float = 14f,
    val strokeWidth: Float = 2f,
    val strokeColor: Long,
    val fillColor: Long? = null,
    val cornerRadius: Float = 4f,
    val paddingH: Float = 12f,
    val paddingV: Float = 8f,
) : AnnotationElement()
```

## KEY IMPLEMENTATION DETAILS

### Auto-Numbering System (Numbered Pins)
```kotlin
class PinNumberingManager {
    // Maintains per-prefix sequence counter
    // On new pin: assign next number in sequence
    // On delete: renumber all subsequent pins with 200ms slide animation
    //   D-001, D-002, D-003 → delete D-002 → D-001, D-002 (was D-003)
    // Prefix from active Trade Kit:
    //   Electrical: "E", Pipe & Valve: "P", Welding: "W", General: "D", Footwear: "F"
    // Counter persists per project (Room query: max sequence number for prefix)
}
```

Renumber animation: each pin slides to its new position over 200ms using spring animation from S-02 (IC-24).

### Hatching Clipping Strategy
```kotlin
// 1. Save canvas state
canvas.save()
// 2. Create boundary path from vertices
val boundaryPath = Path().apply {
    moveTo(vertices[0].x, vertices[0].y)
    vertices.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}
// 3. Clip to boundary
canvas.clipPath(boundaryPath)
// 4. Draw parallel lines that extend well beyond boundary
for (offset in generateLineOffsets(angle, spacing, boundingBox)) {
    canvas.drawLine(offset.start, offset.end, hatchPaint)
}
// 5. Restore canvas
canvas.restore()
// 6. Draw boundary stroke on top (unclipped)
canvas.drawPath(boundaryPath, strokePaint)
```

### Revision Cloud Arc Generation
Walk the boundary path. Between each pair of consecutive points:
1. Calculate segment length
2. Number of arcs = segment length / (2 × arcAmplitude)
3. For each arc: `quadTo` with control point offset perpendicular to segment
4. Arc direction: always bulge outward (away from polygon centroid)
5. At corners: smooth transition by adjusting arc entry/exit angles

### Hatching Boundary Input
Hatching needs a closed boundary polygon. Two input methods:
1. **Draw boundary freehand** → snap-to-close (DP-62 from S-16)
2. **Tap existing shape** → use shape outline as boundary (rectangle, circle, polygon)

For S-17: implement method 1 only (draw boundary). Method 2 is a future enhancement.

### Stamp Semantic Colors
```kotlin
fun StampType.semanticColor(): Long = when(this) {
    PASS -> 0xFF4CAF50      // green
    FAIL -> 0xFFF44336      // red
    CAUTION -> 0xFFFF9800   // amber
    CRACK -> 0xFFE53935     // red variant
    CORROSION -> 0xFFD32F2F // dark red
    LEAK -> 0xFF1976D2      // blue (water)
}
```

## RESOLVED CONFLICTS
None specific to this session.

## STUB REQUIREMENTS

### Hatching via Existing Shape (Phase 2)
- Hatching always requires drawing a new boundary in this session
- Applying hatching fill to an existing shape deferred

### Pin Long-Press Rename (deferred polish)
- Long-press pin to rename/renumber is deferred to S-26 (interaction polish)
- For now: pins are placed with auto-number, no rename UI

## TEST CRITERIA

### Stamps (DP-50, DP-51)
- [ ] One-tap places stamp at correct position
- [ ] CONFIRM haptic fires on placement
- [ ] All 6 types render with correct icon and semantic color
- [ ] S/M/L sizes render correctly
- [ ] Stamps tagged for export metadata (stampType in JSON)

### Numbered Pins (DP-52)
- [ ] First pin = D-001, second = D-002, etc.
- [ ] Delete D-002 → D-003 renumbers to D-002 with 200ms slide animation
- [ ] Prefix changes with active Trade Kit (E-001, P-001, etc.)
- [ ] Pin number text renders centered in circle (IBM Plex Mono)
- [ ] 20+ pins render at 60fps

### Hatching Fill (DP-31)
- [ ] SINGLE, CROSS, DOTS patterns render correctly
- [ ] Hatch lines clipped cleanly to boundary (no overflow)
- [ ] Angle and spacing configurable via properties
- [ ] Boundary stroke visible on top of hatching
- [ ] Complex concave boundaries clip correctly

### Revision Cloud (DP-32)
- [ ] Arcs are smooth and evenly spaced
- [ ] Arcs bulge outward consistently
- [ ] Corner transitions are smooth (no sharp breaks)
- [ ] Arc amplitude configurable
- [ ] Optional fill renders under arcs

### Circle + Text (DP-19)
- [ ] Tap places circle, keyboard opens
- [ ] Circle auto-sizes as text is typed
- [ ] Text centered inside circle
- [ ] Multi-line text wraps and circle grows

### Labeled Rectangle (DP-24)
- [ ] Same as circle+text but rectangle shape
- [ ] Corner radius renders
- [ ] Rectangle grows horizontally and vertically to fit text

### Integration
- [ ] All new tools appear in correct toolbar categories (Marks: stamps/pins; Shapes: hatching/cloud/text shapes)
- [ ] Undo/redo works for all 7 element types
- [ ] All elements persist correctly via Room

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../render/StampRenderer.kt`
2. `feature-canvas/src/main/java/.../render/NumberedPinRenderer.kt`
3. `feature-canvas/src/main/java/.../render/HatchingRenderer.kt`
4. `feature-canvas/src/main/java/.../render/RevisionCloudRenderer.kt`
5. `feature-canvas/src/main/java/.../render/TextShapeRenderer.kt`

### Modify
- `core/model/src/main/java/.../AnnotationElement.kt` — add StampElement, NumberedPinElement, HatchingFillElement, RevisionCloudElement, CircleTextElement, LabeledRectElement
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt` — register 5 new renderers
- `feature-canvas/src/main/java/.../input/HitTestEngine.kt` — hit-test for new types
- `feature-canvas/src/main/java/.../gesture/TapPlaceHandler.kt` — stamp/pin tap handling
- `core/data/src/main/java/.../db/TypeConverters.kt` — serialization for new types

> **Note:** 7 atoms is the largest session. If 5-file rule is tight, consider splitting: S-17a = DP-50, DP-51, DP-52, DP-31 (stamps + pins + hatching); S-17b = DP-32, DP-19, DP-24 (cloud + text shapes).

---

*Session 17 · Size L · 7 atoms · Largest session in Phase 1*
