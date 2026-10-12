# Session 14 — Basic Drawing Elements (Lines + Shapes)

## GOAL
Implement the core 6 drawing elements: straight line, circle, rectangle, text annotation, diamond, and triangle. These are the foundation upon which all other drawing primitives are built. Two gesture patterns: drag-draw (line) and tap-place (shapes + text).

## DEPENDS ON
- **S-10** (CE-06, CE-07, CE-08) — Rendering pipeline, hit-testing, selection handles
- **S-03** (PD-05, PD-06) — AnnotationElement sealed class hierarchy, geometry primitives

## ATOMS
- **DP-01** · Straight Line (S)
- **DP-18** · Circle — Empty (S)
- **DP-23** · Rectangle — Empty (S)
- **DP-25** · Text Annotation (M)
- **DP-27** · Diamond (S)
- **DP-28** · Triangle (S)

## SPEC REFERENCES (inlined)

### DP-01 · Straight Line
`LineElement`: startPoint, endPoint, strokeWidth, dashType (SOLID/DASHED/DOTTED/STITCH), arrowheadStart?, arrowheadEnd?. `LineRenderer`. Drag gesture: DOWN→MOVE→UP. Preview line during drag. Endpoints draggable after placement.

- Shared dependencies: CE-06 (rendering) + PD-05 (type system)
- Arrowhead fields are nullable — arrowheads not implemented until S-15

### DP-18 · Circle (Empty)
`CircleElement`: center, radius, optional text/divider/crosshairs/innerRadius. Default 40dp. 4 cardinal resize handles.

- All shapes use tap-place gesture: tap → shape at point → 1.5s soft placement → tap elsewhere confirms
- Soft placement (IC-13) is a stub in this session — full implementation in S-20

### DP-23 · Rectangle (Empty)
`RectangleElement`: center, width, height, optional text/pattern/cornerRadius. Default 60×40dp. 8 handles (4 corners + 4 edges).

### DP-25 · Text Annotation
`TextElement`: position, text, fontSize, fontWeight, fontFamily (Plus Jakarta Sans / IBM Plex Mono), maxWidth (wraps), rotation. `StaticLayout` for multi-line. Tap→keyboard→type→tap elsewhere confirms. Optional semi-transparent background. Supports emoji and all localized IMEs.

- **Critical:** Text is both a standalone element AND a building block for DP-19 (circle+text), DP-24 (labeled rect), DP-05 (leader+text) in later sessions
- `StaticLayout` handles text measurement, wrapping, and multi-line layout

### DP-27 · Diamond
Rotated 45° rectangle. Uses same `RectangleElement` with `rotation = 45f` or a distinct `DiamondElement` subclass. 4 corner handles.

### DP-28 · Triangle
`TriangleElement`: center, size, orientation (UP/DOWN/LEFT/RIGHT). 3 vertex handles.

## EXISTING CODE TO INTEGRATE WITH

### From S-10 (Rendering Pipeline)
- `AnnotationRenderer` interface — each element type needs a renderer that implements this
- `RendererRegistry` — register new renderers here (LineRenderer, ShapeRenderer, TextRenderer)
- `HitTestEngine` — needs hit-test logic for each new element type:
  - Line: point-to-line-segment distance < threshold (24dp screen-space)
  - Circle: |point - center| compared to radius ± threshold
  - Rectangle/Diamond: point-in-rotated-rect
  - Triangle: point-in-triangle (barycentric coordinates)
  - Text: point-in-bounding-rect
- `SelectionManager` — handles already work for these shapes (cardinal for circle, 8-handle for rect, vertex for triangle/diamond)

### From S-03 (Data Models)
- `AnnotationElement` sealed class — add `LineElement`, `CircleElement`, `RectangleElement`, `TextElement`, `DiamondElement`, `TriangleElement` subclasses
- `ImagePoint`, `ScreenPoint` — all coordinates stored in image-space
- `BoundingBox` — used for hit-testing and selection

### From S-09 (Gesture System)
- `StatelessGestureRouter` — route single-finger gestures based on active tool
- `CoordinatePipeline` — screenToImage conversion on every touch event

### From S-12 (Toolbar)
- Tool selection state — which drawing tool is active
- Tool category mapping: Lines→DP-01, Shapes→DP-18/23/27/28, Text→DP-25

## DELIVERABLES

### 1. LineRenderer.kt
```kotlin
class LineRenderer : AnnotationRenderer {
    // Renders LineElement on Canvas
    // Draws preview line during active drag
    // Handles dash patterns: SOLID, DASHED, DOTTED
    // Arrowhead rendering delegated to stub (S-15)
    // Line thickness scales with viewMatrix (screen-space constant)
}
```

### 2. ShapeRenderer.kt
```kotlin
class ShapeRenderer : AnnotationRenderer {
    // Renders CircleElement, RectangleElement, DiamondElement, TriangleElement
    // Shared logic: stroke, fill (optional), selection handles
    // Circle: Canvas.drawOval with transformed bounds
    // Rectangle: Canvas.drawRoundRect (cornerRadius from element)
    // Diamond: Canvas.drawPath (4 points, rotated 45°)
    // Triangle: Canvas.drawPath (3 points based on orientation)
}
```

### 3. TextRenderer.kt
```kotlin
class TextRenderer : AnnotationRenderer {
    // Renders TextElement using StaticLayout
    // Handles font selection (Plus Jakarta Sans / IBM Plex Mono)
    // Multi-line wrapping at maxWidth
    // Optional semi-transparent background
    // Cursor rendering during active edit
    // Emoji support via system font fallback
}
```

### 4. DragDrawHandler.kt
```kotlin
class DragDrawHandler {
    // Handles DOWN→MOVE→UP gesture for line drawing
    // On DOWN: record startPoint (image-space)
    // On MOVE: update endPoint, trigger preview redraw
    // On UP: create LineElement, push AddAnnotation command to undo stack
    // Preview line rendered with current tool color/width
}
```

### 5. TapPlaceHandler.kt
```kotlin
class TapPlaceHandler {
    // Handles single-tap placement for shapes and text
    // On TAP: create element at tap point (image-space)
    // Apply default size (circle: 40dp, rect: 60×40dp, etc.)
    // Push AddAnnotation command to undo stack
    // For text: show keyboard, create TextElement with empty text
    // On keyboard dismiss or tap-elsewhere: confirm text
    // If text is empty on confirm: remove element (no empty annotations)
}
```

## DATA MODELS

### LineElement
```kotlin
@Serializable
data class LineElement(
    override val id: String = UUID.randomUUID().toString(),
    val startPoint: ImagePoint,
    val endPoint: ImagePoint,
    val strokeWidth: Float = 3f,       // dp
    val strokeColor: Long,              // ARGB
    val dashType: DashType = DashType.SOLID,
    val arrowheadStart: ArrowheadStyle? = null,  // stub for S-15
    val arrowheadEnd: ArrowheadStyle? = null,    // stub for S-15
) : AnnotationElement()

enum class DashType { SOLID, DASHED, DOTTED }
```

### CircleElement
```kotlin
@Serializable
data class CircleElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val radius: Float = 40f,            // dp, converted to image-space on placement
    val strokeWidth: Float = 3f,
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()
```

### RectangleElement
```kotlin
@Serializable
data class RectangleElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val width: Float = 60f,             // dp
    val height: Float = 40f,            // dp
    val strokeWidth: Float = 3f,
    val strokeColor: Long,
    val fillColor: Long? = null,
    val cornerRadius: Float = 0f,
    val rotation: Float = 0f,           // degrees
) : AnnotationElement()
```

### TextElement
```kotlin
@Serializable
data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val text: String,
    val fontSize: Float = 16f,          // sp
    val fontWeight: FontWeight = FontWeight.NORMAL,
    val fontFamily: FontFamily = FontFamily.PLUS_JAKARTA,
    val maxWidth: Float? = null,        // image-space pixels, null = no wrap
    val rotation: Float = 0f,
    val strokeColor: Long,              // text color
    val backgroundColor: Long? = null,  // optional semi-transparent bg
) : AnnotationElement()

enum class FontFamily { PLUS_JAKARTA, IBM_PLEX_MONO }
enum class FontWeight { NORMAL, MEDIUM, SEMIBOLD, BOLD }
```

### DiamondElement
```kotlin
@Serializable
data class DiamondElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val width: Float = 50f,
    val height: Float = 50f,
    val strokeWidth: Float = 3f,
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()
```

### TriangleElement
```kotlin
@Serializable
data class TriangleElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val size: Float = 50f,
    val orientation: TriangleOrientation = TriangleOrientation.UP,
    val strokeWidth: Float = 3f,
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()

enum class TriangleOrientation { UP, DOWN, LEFT, RIGHT }
```

## KEY IMPLEMENTATION DETAILS

### Coordinate System
- All element coordinates stored in **image-space** (pixels relative to original photo)
- On placement: `screenToImage(tapPoint)` converts to image-space
- Default sizes (40dp, 60×40dp, etc.) converted from dp→image-space using current zoom level
- This means shapes placed while zoomed in appear smaller in absolute image pixels (correct — they occupy the same visual area the user intended)

### Drag-Draw vs Tap-Place
- **Drag-draw** (line): DOWN records start, MOVE updates end with preview, UP finalizes
- **Tap-place** (shapes, text): single TAP creates element at center with default size
- The `StatelessGestureRouter` differentiates by active tool category:
  - Lines category → `DragDrawHandler`
  - Shapes/Text category → `TapPlaceHandler`

### Rendering Order
- All elements render in `onDraw()` via `RendererRegistry.render(canvas, element, viewMatrix)`
- Render in creation order (oldest first, newest on top)
- Selection handles render LAST (always on top)
- Preview line renders on top of all elements but under handles

### Hit-Testing per Element Type
- **Line:** perpendicular distance from point to line segment. Threshold = max(24dp, strokeWidth + 12dp) in screen-space
- **Circle:** |distance from center - radius|. Threshold = max(24dp, strokeWidth + 12dp). Inside filled circle also hits.
- **Rectangle:** point-in-rotated-rect using inverse rotation transform
- **Diamond:** point-in-rotated-rect (45° rotation)
- **Triangle:** barycentric coordinate test
- **Text:** point-in-bounding-rect from `StaticLayout.getWidth()/getHeight()`

### Text Tool Flow
1. User taps canvas with Text tool active
2. `TapPlaceHandler` creates `TextElement` with empty text at tap point
3. Keyboard opens (soft input)
4. Live preview: text renders at annotation position as user types
5. Tap elsewhere OR keyboard dismiss → confirm
6. If text is empty → remove element silently (no undo entry)
7. If text has content → push `AddAnnotation` to undo stack

### StaticLayout Usage
```kotlin
val staticLayout = StaticLayout.Builder
    .obtain(text, 0, text.length, textPaint, maxWidth.toInt())
    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
    .setLineSpacing(0f, 1.2f)
    .build()
```

## RESOLVED CONFLICTS

| ID | Resolution |
|----|-----------|
| R-11 | Data model merges: Diamond is its own class (not rotated Rectangle) — cleaner hit-testing and vertex handles |
| R-14 | Circle variants (empty, +text, +divider, +crosshairs) share base CircleElement. Only empty circle in this session; variants in S-17 |
| R-15 | Rectangle variants share base RectangleElement. Only empty rectangle in this session; labeled rect in S-17 |

## STUB REQUIREMENTS

### Arrowhead Fields (for S-15)
- `LineElement.arrowheadStart` and `arrowheadEnd` are nullable, default null
- `LineRenderer` ignores these fields — no arrowhead rendering in this session
- `ArrowheadStyle` enum exists as empty placeholder

### Soft Placement (for S-20)
- After tap-place, element IS draggable via existing `SelectionManager` (S-10)
- Full 1.5s auto-confirm window with pulse animation deferred to S-20 (IC-13)
- For now: tap places, immediate selection, user drags if needed

### Keyboard-Aware Layout (for S-26)
- Text tool opens keyboard via standard `showSoftInput()`
- Photo compression (top 50%) when keyboard opens deferred to S-26 (IC-29)
- For now: keyboard may cover annotation position — functional but not polished

## TEST CRITERIA

### Line Element
- [ ] Drag from A to B creates a line with correct start/end points in image-space
- [ ] Preview line visible during drag
- [ ] Line renders correctly at all zoom levels (stroke width constant in screen-space)
- [ ] DASHED and DOTTED patterns render with correct intervals
- [ ] Endpoints draggable after selection (via S-10 handles)
- [ ] Hit-test works within 24dp of line at any zoom

### Circle Element
- [ ] Tap places circle at correct image-space position
- [ ] Default radius 40dp appears correct relative to photo
- [ ] 4 cardinal resize handles visible on selection
- [ ] Resize via handle drag works proportionally
- [ ] Hit-test on stroke AND inside filled circle

### Rectangle Element
- [ ] Tap places rectangle at correct position
- [ ] Default 60×40dp size
- [ ] 8 handles (4 corner + 4 edge) visible on selection
- [ ] Corner handles = proportional resize, edge handles = free resize
- [ ] CornerRadius renders when non-zero

### Text Element
- [ ] Tap opens keyboard
- [ ] Text renders at annotation position as user types (live preview)
- [ ] Multi-line wrapping works at maxWidth
- [ ] Plus Jakarta Sans and IBM Plex Mono fonts render correctly
- [ ] Emoji renders correctly
- [ ] Empty text on confirm → element removed (no undo entry)
- [ ] Semi-transparent background renders when enabled

### Diamond Element
- [ ] Tap places diamond at correct position
- [ ] 4 vertex handles on selection
- [ ] Hit-test correct for rotated shape

### Triangle Element
- [ ] Tap places triangle with correct orientation (UP default)
- [ ] Orientation changes work (UP/DOWN/LEFT/RIGHT)
- [ ] 3 vertex handles on selection
- [ ] Hit-test correct using barycentric coordinates

### Integration
- [ ] All 6 elements persist via Room (serialize → save → reload → identical)
- [ ] Undo/redo works for all element types (AddAnnotation / RemoveAnnotation commands)
- [ ] 100 mixed elements render at 60fps on S25 Ultra
- [ ] Elements render correctly after photo zoom/pan (viewMatrix transforms)

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../render/LineRenderer.kt`
2. `feature-canvas/src/main/java/.../render/ShapeRenderer.kt`
3. `feature-canvas/src/main/java/.../render/TextRenderer.kt`
4. `feature-canvas/src/main/java/.../gesture/DragDrawHandler.kt`
5. `feature-canvas/src/main/java/.../gesture/TapPlaceHandler.kt`

### Modify
- `core/model/src/main/java/.../AnnotationElement.kt` — add 6 element subclasses
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt` — register 3 new renderers
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt` — route to DragDraw/TapPlace based on tool
- `feature-canvas/src/main/java/.../input/HitTestEngine.kt` — add per-type hit-test logic
- `core/data/src/main/java/.../db/TypeConverters.kt` — serialization for new element types

---

*Session 14 · Size L · 6 atoms · Phase 1 Drawing Primitives*
