# Session 15 — Arrowhead System + Arrows + Leader

## GOAL
Implement the 20-style arrowhead rendering system, single-head and double-head arrows, and leader line with text label. The arrowhead renderer is a standalone system reused by dimension lines (S-18), flow arrows (S-30), and future Phase 2 elements.

## DEPENDS ON
- **S-14** (DP-01, DP-25) — LineElement, TextElement, LineRenderer, DragDrawHandler

## ATOMS
- **DP-02** · Arrowhead Renderer — 20-Style System (M)
- **DP-03** · Arrow — Single-Head (S)
- **DP-04** · Arrow — Double-Head (S)
- **DP-05** · Leader + Text (M)

## SPEC REFERENCES (inlined)

### DP-02 · Arrowhead Renderer (20-Style System)
Standalone `ArrowheadRenderer`. 20 styles across 4 categories:

| Category | Styles |
|----------|--------|
| **Filled** | Standard, Narrow, Wide, Barbed |
| **Open** | Standard, Narrow, Barbed, Notched |
| **Geometric** | Dot, Circle, Diamond, Square, Slash |
| **Technical** | Datum triangle, Architectural tick, Origin indicator, Dimension tick, ISO arrow |
| **Decorative** | Feathered, Double |

**CRITICAL: inset-point pattern** — line endpoint pulled back by arrowhead depth so line never pokes through fills. `ArrowheadMetrics.getInsetDistance(style, size)` returns the distance to pull back.

Scale with stroke width. All 20 visually distinct at any zoom.

### DP-03 · Arrow (Single-Head)
`LineElement` with `arrowheadEnd` configured. Style configurable via properties sheet (IC-12, already stubbed).

### DP-04 · Arrow (Double-Head)
Both `arrowheadStart` and `arrowheadEnd` configured. Both use inset-point.

### DP-05 · Leader + Text
`LeaderElement`: line + text label at endpoint + arrowhead at start. Keyboard auto-appears on finger-up (end of drag). Smart label placement (CE-09 — stub, full implementation S-20). Label draggable independently. Connector line between label and endpoint.

## EXISTING CODE TO INTEGRATE WITH

### From S-14
- `LineElement` — already has nullable `arrowheadStart`/`arrowheadEnd` fields
- `LineRenderer` — extend to call `ArrowheadRenderer` when arrowhead fields are set
- `DragDrawHandler` — reuse for arrow drawing (same drag gesture, different element config)
- `TextElement` — reuse text rendering logic for leader labels
- `TextRenderer` — reuse for leader text

### From S-10
- `RendererRegistry` — register `LeaderRenderer`
- `HitTestEngine` — add leader hit-test (line + label bounding rect)
- `SelectionManager` — leader needs two draggable zones: line endpoints + label position

### From S-12
- `ToolPropertiesSheet` — arrowhead style picker appears in properties sheet for line/arrow tools
- Tool tray under Lines category shows: Straight Line, Arrow (single), Arrow (double), Leader+Text

## DELIVERABLES

### 1. ArrowheadRenderer.kt
```kotlin
object ArrowheadRenderer {
    // Renders arrowhead at given point with given angle
    // fun render(canvas: Canvas, style: ArrowheadStyle, tip: ImagePoint, 
    //            angle: Float, size: Float, paint: Paint, viewMatrix: Matrix)
    
    // Critical: inset-point calculation
    // fun getInsetDistance(style: ArrowheadStyle, size: Float): Float
    
    // For filled styles: line endpoint = tip - (direction * insetDistance)
    // For open/geometric styles: inset may be 0 or partial
    
    // Each style is a Path built relative to tip point:
    // - Filled Standard: classic triangle, inset = full depth
    // - Narrow: aspect ratio 3:1
    // - Wide: aspect ratio 1:1
    // - Barbed: concave base
    // - Dot/Circle: drawCircle at tip
    // - Diamond/Square: rotated rect at tip
    // - Slash: angled line perpendicular
    // - Architectural tick: 45° slash (no fill)
    // - Datum: filled triangle (GD&T)
    // - ISO: narrow filled (ISO 128 standard)
}
```

### 2. ArrowheadStyle.kt
```kotlin
enum class ArrowheadStyle {
    // Filled
    FILLED_STANDARD, FILLED_NARROW, FILLED_WIDE, FILLED_BARBED,
    // Open
    OPEN_STANDARD, OPEN_NARROW, OPEN_BARBED, OPEN_NOTCHED,
    // Geometric
    GEO_DOT, GEO_CIRCLE, GEO_DIAMOND, GEO_SQUARE, GEO_SLASH,
    // Technical
    TECH_DATUM, TECH_ARCH_TICK, TECH_ORIGIN, TECH_DIM_TICK, TECH_ISO,
    // Decorative
    DECO_FEATHERED, DECO_DOUBLE
}
```

### 3. LeaderRenderer.kt
```kotlin
class LeaderRenderer : AnnotationRenderer {
    // Renders LeaderElement:
    // 1. Line from arrowhead tip to elbow point
    // 2. Horizontal connector from elbow to label
    // 3. Arrowhead at tip (via ArrowheadRenderer)
    // 4. Text label (via StaticLayout, same as TextRenderer)
    
    // Label auto-placed at line endpoint initially
    // Label position stored independently (draggable)
    // Connector line always connects label to line endpoint
}
```

## DATA MODELS

### ArrowheadStyle (see enum above)

### LeaderElement
```kotlin
@Serializable
data class LeaderElement(
    override val id: String = UUID.randomUUID().toString(),
    val tipPoint: ImagePoint,           // arrowhead end (where arrow points)
    val elbowPoint: ImagePoint,         // bend point
    val labelPosition: ImagePoint,      // independently draggable
    val text: String,
    val fontSize: Float = 14f,
    val fontFamily: FontFamily = FontFamily.PLUS_JAKARTA,
    val strokeWidth: Float = 2f,
    val strokeColor: Long,
    val arrowheadStyle: ArrowheadStyle = ArrowheadStyle.FILLED_STANDARD,
) : AnnotationElement()
```

## KEY IMPLEMENTATION DETAILS

### Inset-Point Pattern (CRITICAL)
The most important rendering detail in this session. Without it, the line visibly pokes through filled arrowheads:

```
WITHOUT INSET (wrong):          WITH INSET (correct):
    ▶──────────                    ▶─────────
    ^ line visible               ^ clean arrowhead
      through fill
```

Implementation:
1. Calculate arrowhead depth for the style
2. Pull back line endpoint by that depth along the line direction
3. Draw line to pulled-back point
4. Draw arrowhead at original tip
5. For double-head arrows: pull back BOTH endpoints

```kotlin
fun getInsetDistance(style: ArrowheadStyle, size: Float): Float = when(style) {
    FILLED_STANDARD -> size * 1.0f
    FILLED_NARROW -> size * 1.5f
    FILLED_WIDE -> size * 0.7f
    FILLED_BARBED -> size * 0.8f
    GEO_DOT, GEO_CIRCLE -> size * 0.5f
    GEO_DIAMOND, GEO_SQUARE -> size * 0.7f
    GEO_SLASH, TECH_ARCH_TICK, TECH_DIM_TICK -> 0f  // no inset needed
    TECH_DATUM -> size * 1.0f
    TECH_ISO -> size * 1.2f
    // ... etc
}
```

### Arrow Angle Calculation
```kotlin
val angle = atan2(
    (endPoint.y - startPoint.y).toDouble(),
    (endPoint.x - startPoint.x).toDouble()
).toFloat()
```

### Arrowhead Size Scaling
- Base arrowhead size = `strokeWidth * 4` (proportional to line)
- Arrowhead size configurable via properties sheet (S/M/L multiplier)
- All arrowheads render in image-space (scale with zoom like the line)

### Leader Line Gesture Flow
1. User selects Leader tool from Lines tray
2. Drag from tip point → elbow/endpoint direction
3. On finger-up: keyboard auto-appears
4. User types label text
5. On keyboard dismiss: label confirmed
6. Label position defaults to line endpoint + offset
7. Label is independently draggable after placement

### Leader Label Connector
- Thin horizontal line from label to nearest point on leader line
- Auto-adjusts when label is dragged
- Connector always horizontal (professional engineering convention)

## RESOLVED CONFLICTS

| ID | Resolution |
|----|-----------|
| R-05 | Arrowhead count: 20 styles across 5 categories (title "20 styles" is exact). Enum has 20 entries. |
| — | Arrowhead size: proportional to strokeWidth (not fixed), configurable via properties |

## STUB REQUIREMENTS

### Smart Label Placement (for S-20)
- Leader label placed at endpoint + fixed offset (right of line endpoint)
- Full 8-position greedy algorithm (CE-09) deferred to S-20
- For now: simple right-side offset

### Properties Sheet Arrowhead Picker (for S-12 integration)
- Properties sheet (IC-12) already exists from S-12
- Add arrowhead style grid picker to it (5×4 grid of all 20 styles)
- This is a modification, not a stub

## TEST CRITERIA

### Arrowhead Renderer
- [ ] All 20 styles render as visually distinct shapes
- [ ] No line pokes through any filled arrowhead at ANY zoom level (test 0.5x, 1x, 2x, 5x, 10x)
- [ ] Arrowheads scale proportionally with strokeWidth
- [ ] Arrowheads rotate correctly to match line angle (test 0°, 45°, 90°, 135°, 180°, 270°)
- [ ] Performance: 50 arrows with arrowheads render at 60fps

### Single-Head Arrow (DP-03)
- [ ] Drag creates arrow with arrowhead at end
- [ ] Arrowhead style changeable via properties sheet
- [ ] Inset-point prevents line through fill
- [ ] Arrow persists correctly (serialize → save → reload → identical)

### Double-Head Arrow (DP-04)
- [ ] Both ends have arrowheads
- [ ] BOTH endpoints use inset-point
- [ ] Different styles on each end (if configured)

### Leader + Text (DP-05)
- [ ] Drag creates leader line, keyboard appears on finger-up
- [ ] Text renders at label position
- [ ] Label draggable independently from line
- [ ] Connector line follows label position
- [ ] Empty text on confirm → element removed
- [ ] Leader persists correctly

### Integration
- [ ] Arrows appear in Lines tool tray (straight line, arrow, double arrow, leader)
- [ ] Tool selection persists (draw one arrow, next drawing is also an arrow)
- [ ] Undo/redo works for all arrow types

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../render/ArrowheadRenderer.kt`
2. `feature-canvas/src/main/java/.../render/LeaderRenderer.kt`
3. `feature-canvas/src/main/java/.../model/ArrowheadStyle.kt`

### Modify
- `core/model/src/main/java/.../AnnotationElement.kt` — add `LeaderElement` subclass
- `feature-canvas/src/main/java/.../render/LineRenderer.kt` — integrate ArrowheadRenderer calls + inset-point
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt` — register LeaderRenderer
- `feature-canvas/src/main/java/.../input/HitTestEngine.kt` — add leader hit-test
- `feature-canvas/src/main/java/.../ui/ToolPropertiesSheet.kt` — add arrowhead style picker
- `core/data/src/main/java/.../db/TypeConverters.kt` — LeaderElement serialization

---

*Session 15 · Size M · 4 atoms · Arrowhead system is reused by S-18, S-30*
