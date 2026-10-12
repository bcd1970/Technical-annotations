# Session 14 — Drag-Area Shapes (Hatching Fill + Revision Cloud)

## GOAL

Implement the two drag-area primitives from Group D: #36 Hatching Fill (freehand boundary → clip → tile pattern fill with 8 engineering hatch patterns via BitmapShader) and #39 Revision Cloud (freehand loop → convert smooth boundary into semicircular bumps + auto-increment rev letter). Both reuse the `FreehandCurveDrawer` and `SnapToCloseManager` from S13, always forcing closure. Also introduces the unified `HatchPattern` tile generation system that S29 (Decorated Path) will later reuse.

## DEPENDS ON

- **S13 (Freehand Drawing)** — `FreehandCurveDrawer`, `CatmullRomSmoother`, `SnapToCloseManager`, `reducePoints()`, `FreehandCurveResult`
- **S11 (Shapes)** — Shapes tray already has stub entries for #36 and #39 (toast "Coming Soon")
- **S07 (Tool Palette)** — `ToolPalette`, `ToolRegistry`, `ActiveTool.Draw(toolId)`, `GestureGroup.DRAG_AREA`
- **S03 (Drawing Surface)** — `EditableTouchImageView`, `DrawableElement` interface, `GestureRouter`, `screenToImage()`, rendering pipeline
- **S09 (Lines + Selection)** — `SelectionHandler`, `CanvasViewModel` element CRUD, undo integration
- **S02 (Undo/Redo)** — `UndoManager` annotate stack

## SPEC REFERENCES

Read before coding:
- `FINAL-complete-implementation-spec.md` §7 (Group D: Drag-Area Primitives — #36 HatchFill, #39 RevisionCloud data classes, gesture description, rendering approach)
- `GRAPHIC-ELEMENT-EDITING-SPEC.md` §7 (Area Fills & Patterns — §7.1 Hatching Fill properties, §7.4 Revision Cloud properties: bump radius, bump direction, rev letter, fill tint)
- `IMPLEMENTATION-BLUEPRINT.md` Part 1: audit finding 3.7 (Merge: Fill Patterns Hatching #36 + Decorated Path #46 → unified FillPattern enum + shared tile generation)
- `NEW-GRAPHIC-TOOLS-SPEC.md` §2.3 (PathFillPattern enum — reference for future-proofing the tile system)
- `SESSION-13-freehand.md` (FreehandCurveDrawer API, SnapToCloseManager states, integration points)

## EXISTING CODE TO INTEGRATE WITH

| File | Module | What It Does | Integration Notes |
|------|--------|-------------|-------------------|
| `FreehandCurveDrawer.kt` | `:core:drawing` | Raw points → reduced → smoothed → snap-to-close from S13 | Reuse directly. Both tools use it identically — just always need closed=true result. |
| `SnapToCloseManager.kt` | `:core:drawing` | Snap detection + visual feedback from S13 | Reuse directly. Same visual feedback during drag-area draw. |
| `CatmullRomSmoother.kt` | `:core:drawing` | Spline → Path from S13 | Reuse for hatching boundary. NOT used for revision cloud outline (bumps replace the smooth path). |
| `FreehandDrawHandler.kt` | `:core:drawing` | Gesture handler from S13 | Reference pattern. Create new `DragAreaDrawHandler` with similar structure but force-closed behavior. |
| `GestureRouter.kt` | `:core:drawing` | Routes by mode+tool from S03 | Route DRAG_AREA tools (#36, #39) to `DragAreaDrawHandler` |
| `SelectionHandler.kt` | `:core:drawing` | Universal select/move/delete from S09 | Reuse for selecting placed hatch fills and revision clouds |
| `CanvasViewModel.kt` | `:feature:canvas` | Element CRUD from S09 | Wire drag-area completion → `addElement()` + undo |
| `EditableTouchImageView.kt` | `:feature:canvas` | Touch handling + rendering from S03 | Render live preview boundary during draw. Render hatch/cloud after placement. |
| `ElementFactory.kt` | `:core:drawing` | Creates elements by toolId from S09 | Add tool IDs 36, 39 |
| `ToolRegistry.kt` | `:core:model` | Tool definitions from S07 | #36 and #39 already registered with `GestureGroup.DRAG_AREA` — remove stub toast |

## DELIVERABLES

### DragAreaDrawHandler

1. **`DragAreaDrawHandler` class** in `:core:drawing`:
   - Wraps `FreehandCurveDrawer` with forced-closure semantics:
     ```
     STATE MACHINE:
     IDLE → (ACTION_DOWN with drag-area tool) → DRAWING
       Create FreehandCurveDrawer. Set isDrawing = true.
       Record active toolId (36 or 39) to determine element type on completion.
     DRAWING → (ACTION_MOVE) → DRAWING
       Feed point to drawer. Get preview path. Get snap state.
       Render preview boundary + snap feedback.
       For hatching: preview shows boundary outline only.
       For revision cloud: preview shows smooth boundary (bumps generated on completion).
     DRAWING → (ACTION_UP) → COMPLETING
       Get FreehandCurveResult.
       If !isClosed: auto-close by appending start point as final point
         → re-run CatmullRomSmoother with closed=true.
       Route to element creation based on toolId.
     COMPLETING → IDLE
       Element placed. isDrawing = false.
     ```

2. **Auto-close behavior:** Unlike standalone freehand (S13) which allows open curves, drag-area tools ALWAYS produce closed boundaries. If the user lifts without snapping to the start point, the system auto-closes:
   - Append the start point to the control points list
   - Re-smooth with `CatmullRomSmoother.createPath(points, closed = true)`
   - No error message — closing is always implicit for area tools

### HatchPatternTileGenerator

3. **`HatchPatternTileGenerator` object** in `:core:drawing`:
   - Generates repeatable `Bitmap` tiles for each of the 8 hatch patterns
   - Tile size is calculated from `spacing` and `scale` parameters
   - API:
     ```kotlin
     object HatchPatternTileGenerator {
         fun generateTile(
             pattern: HatchPattern,
             color: Int,
             spacing: Float,          // px, line spacing (2–30)
             lineWeight: Float,       // px, stroke width within tile (0.25–4.0)
             scale: Float             // multiplier (0.25–4.0)
         ): Bitmap

         fun createShader(
             tile: Bitmap,
             angle: Float,            // degrees (0–180)
             offsetX: Float,          // 0.0–1.0 fraction of tile width
             offsetY: Float           // 0.0–1.0 fraction of tile height
         ): BitmapShader
     }
     ```

4. **8 hatch pattern tile renderers** (each draws into a tile Bitmap with Canvas):

   ```kotlin
   enum class HatchPattern {
       STEEL_45,            // Parallel lines at 45° (default angle 45°)
       CONCRETE_STIPPLE,    // Random dots (density from spacing)
       EARTH_CROSSHATCH,    // Two crossing line sets (default 0° + 90°)
       BRICK,               // Offset rectangular courses
       WOOD_GRAIN,          // Wavy parallel lines
       FLUID_WAVE,          // Sine wave lines
       INSULATION_ZIG,      // Zigzag lines
       GLASS_BLANK          // Empty (boundary only, no fill)
   }
   ```

5. **Tile rendering details per pattern:**
   ```
   STEEL_45:
     Tile size: spacing × spacing
     Draw: single diagonal line from (0,0) to (tileW,tileH) at lineWeight
     BitmapShader REPEAT handles the rest. Default angle: 45°.

   CONCRETE_STIPPLE:
     Tile size: (spacing × 4) × (spacing × 4)  — larger for randomness
     Draw: scatter small dots (radius = lineWeight) using seeded Random
     Seed from pattern origin for consistency. ~8-12 dots per tile.

   EARTH_CROSSHATCH:
     Tile size: spacing × spacing
     Draw: one horizontal line + one vertical line, both at lineWeight
     Result: grid pattern when tiled. Default angle: 0°.

   BRICK:
     Tile size: (spacing × 4) × (spacing × 2)
     Draw: 2 rows of rectangles, second row offset by half.
     Lines at lineWeight. Interior empty.

   WOOD_GRAIN:
     Tile size: spacing × (spacing × 6)
     Draw: wavy line using quadTo(), amplitude = spacing/3.
     Single line per tile width; tiling creates parallel waves.

   FLUID_WAVE:
     Tile size: spacing × (spacing × 4)
     Draw: sine wave, period = tileWidth, amplitude = spacing/2.
     Similar to wood grain but more regular/geometric.

   INSULATION_ZIG:
     Tile size: spacing × (spacing × 2)
     Draw: zigzag (V-shapes) top to bottom.
     Each V: width = spacing, height = spacing.

   GLASS_BLANK:
     Return 1×1 transparent Bitmap. No fill drawn.
     Only boundary stroke (if enabled) will be visible.
   ```

6. **BitmapShader configuration:**
   ```kotlin
   fun createShader(tile: Bitmap, angle: Float, offsetX: Float, offsetY: Float): BitmapShader {
       val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
       val matrix = Matrix()
       matrix.postRotate(angle)
       matrix.postTranslate(tile.width * offsetX, tile.height * offsetY)
       shader.setLocalMatrix(matrix)
       return shader
   }
   ```

### HatchFillElement

7. **`HatchFillElement` class** implementing `DrawableElement`:
   - Renders: clip canvas to boundary path → fill with BitmapShader → optional boundary stroke
   ```kotlin
   draw(canvas: Canvas) {
       canvas.save()
       canvas.clipPath(boundaryPath)  // Clip to freehand boundary

       if (pattern != HatchPattern.GLASS_BLANK) {
           val tile = HatchPatternTileGenerator.generateTile(pattern, color, spacing, lineWeight, scale)
           val shader = HatchPatternTileGenerator.createShader(tile, angle, offsetX, offsetY)
           fillPaint.shader = shader
           canvas.drawPaint(fillPaint)  // Fills entire clipped area with tiled pattern
       }

       canvas.restore()

       // Boundary stroke (outside clip, so it's not clipped)
       when (boundaryStroke) {
           BoundaryStroke.SOLID -> canvas.drawPath(boundaryPath, solidStrokePaint)
           BoundaryStroke.DASHED -> canvas.drawPath(boundaryPath, dashedStrokePaint)
           BoundaryStroke.NONE -> { /* no boundary */ }
       }
   }
   ```

8. **Tile caching:** Cache the generated `Bitmap` tile. Invalidate only when pattern, color, spacing, lineWeight, or scale changes. The shader matrix (angle, offset) can be updated without regenerating the tile.

### RevisionCloudRenderer

9. **`RevisionCloudRenderer` object** in `:core:drawing`:
   - Converts a smooth closed boundary path into a "bumpy" revision cloud path
   - Algorithm:
     ```
     INPUT:  Smooth closed Path (from CatmullRomSmoother)
     PARAMS: bumpRadius (5–30 px, default 12), bumpDirection (OUTWARD/INWARD)

     STEP 1: Walk path with PathMeasure
       - Total length = pathMeasure.length
       - Sample interval = bumpRadius * 2 (one bump per diameter)
       - Number of bumps = floor(totalLength / sampleInterval)
       - Adjust interval slightly so bumps distribute evenly:
         actualInterval = totalLength / numBumps

     STEP 2: At each sample point, generate a semicircular arc
       - Get position and tangent: pathMeasure.getPosTan(distance, pos, tan)
       - Perpendicular direction = rotate tangent 90°
         (OUTWARD: away from interior; INWARD: toward interior)
       - Arc center = midpoint between current sample and next sample
       - Arc: from current sample to next sample, bulging outward by bumpRadius

     STEP 3: Build cloud Path
       - For each pair of adjacent sample points:
         val midX = (p1.x + p2.x) / 2 + perpX * bumpRadius
         val midY = (p1.y + p2.y) / 2 + perpY * bumpRadius
         cloudPath.quadTo(midX, midY, p2.x, p2.y)
       - Close path at end

     OUTPUT: Path with bumpy outline
     ```

10. **Determining "outward" direction:**
    - Use the signed area of the boundary polygon (shoelace formula)
    - If area > 0 → counterclockwise winding → outward = left of tangent
    - If area < 0 → clockwise winding → outward = right of tangent
    - For INWARD direction: flip the perpendicular

### RevisionCloudElement

11. **`RevisionCloudElement` class** implementing `DrawableElement`:
    ```kotlin
    draw(canvas: Canvas) {
        // Optional fill tint
        if (fillTintOpacity > 0f) {
            val tintPaint = Paint().apply {
                color = fillTintColor
                alpha = (fillTintOpacity * 255).toInt()
                style = Paint.Style.FILL
            }
            canvas.drawPath(cloudPath, tintPaint)
        }

        // Cloud outline (bumps)
        canvas.drawPath(cloudPath, strokePaint)

        // Rev letter label
        if (revLetter != null) {
            drawRevLabel(canvas, revLetter, labelPosition)
        }
    }
    ```

12. **Rev letter auto-increment:**
    - `CanvasViewModel` tracks `usedRevLetters: Set<Char>` across all revision clouds in current project
    - On new cloud placement: assign next available letter (A→B→C→...→Z→AA→AB...)
    - Letter rendered at `labelPosition` (handle on cloud edge, draggable)
    - Text uses current annotation color + style, enclosed in a small triangle/diamond indicator

### DragAreaDrawHandler Live Preview

13. **During draw (rendered in `EditableTouchImageView`):**
    ```
    HATCHING TOOL (#36):
      - Preview: boundary outline in current color, 70% opacity, 1.5dp stroke
      - Snap-to-close feedback (same rings/glow as S13)
      - On completion: hatching fill appears inside boundary

    REVISION CLOUD (#39):
      - Preview: smooth boundary outline in current color, 70% opacity
      - Snap-to-close feedback
      - On completion: smooth outline is REPLACED by bumpy cloud outline
        (visual transformation — user sees the smooth curve become a cloud)
    ```

## DATA MODELS

```kotlin
// In :core:model — HatchFillData.kt
@Serializable
data class HatchFillData(
    val controlPoints: List<SerializablePointF>,  // reduced freehand points defining boundary
    val isClosed: Boolean = true,                 // always true for area fills
    val pattern: HatchPattern = HatchPattern.STEEL_45,
    val angle: Float = 45f,                       // 0–180°
    val scale: Float = 1.0f,                      // 0.25–4.0
    val spacing: Float = 8f,                      // 2–30 px
    val patternLineWeight: Float = 0.5f,          // 0.25–4.0 px
    val patternOffsetX: Float = 0f,               // 0.0–1.0
    val patternOffsetY: Float = 0f,               // 0.0–1.0
    val boundaryStroke: BoundaryStroke = BoundaryStroke.NONE,
    val color: Int,                               // pattern + boundary color
    val strokeWidth: Float = 1.5f,                // boundary stroke width
    val layer: String,
    val opacity: Float = 1.0f,
    val isLocked: Boolean = false
)

@Serializable
enum class HatchPattern {
    STEEL_45,
    CONCRETE_STIPPLE,
    EARTH_CROSSHATCH,
    BRICK,
    WOOD_GRAIN,
    FLUID_WAVE,
    INSULATION_ZIG,
    GLASS_BLANK
}

@Serializable
enum class BoundaryStroke {
    NONE,    // pattern only, no boundary line
    SOLID,   // solid boundary line
    DASHED   // dashed boundary line
}
```

```kotlin
// In :core:model — RevisionCloudData.kt
@Serializable
data class RevisionCloudData(
    val controlPoints: List<SerializablePointF>,  // reduced freehand points defining smooth boundary
    val isClosed: Boolean = true,                 // always true
    val bumpRadius: Float = 12f,                  // 5–30 px
    val bumpDirection: BumpDirection = BumpDirection.OUTWARD,
    val revLetter: String? = null,                // "A", "B", ..., "AA", etc.
    val labelPosition: SerializablePointF? = null,// handle on cloud edge
    val fillTintColor: Int = 0,                   // 0 = no fill
    val fillTintOpacity: Float = 0f,              // 0–0.3
    val color: Int,                               // stroke color
    val strokeWidth: Float = 2f,
    val layer: String,
    val opacity: Float = 1.0f,
    val isLocked: Boolean = false
)

@Serializable
enum class BumpDirection {
    OUTWARD,   // standard — bumps protrude away from interior
    INWARD     // demolition convention — bumps protrude toward interior
}
```

## KEY IMPLEMENTATION DETAILS

### Hatching Fill Rendering Pipeline

```
1. Reconstruct boundary Path from controlPoints via CatmullRomSmoother(closed=true)
2. Generate tile Bitmap:
   - Size depends on pattern + spacing + scale
   - Draw pattern lines/dots into tile at lineWeight
   - Cache tile — only regenerate when pattern params change
3. Create BitmapShader(tile, REPEAT, REPEAT)
4. Apply rotation + offset via shader local matrix:
   - Matrix.postRotate(angle, tileW/2, tileH/2)
   - Matrix.postTranslate(tileW * offsetX, tileH * offsetY)
5. canvas.save() → canvas.clipPath(boundaryPath)
6. fillPaint.shader = shader → canvas.drawPaint(fillPaint)
7. canvas.restore()
8. Draw boundary stroke if != NONE
```

### Revision Cloud Bump Generation Algorithm

```kotlin
fun generateCloudPath(
    boundaryPath: Path,
    bumpRadius: Float,
    direction: BumpDirection
): Path {
    val measure = PathMeasure(boundaryPath, true)  // force closed
    val totalLength = measure.length
    if (totalLength < bumpRadius * 4) return boundaryPath  // too small for bumps

    // Calculate even bump distribution
    val numBumps = max(4, (totalLength / (bumpRadius * 2)).roundToInt())
    val interval = totalLength / numBumps

    // Determine winding direction for "outward"
    val windingSign = computeWindingSign(boundaryPath)  // +1 or -1
    val dirSign = if (direction == OUTWARD) windingSign else -windingSign

    val cloudPath = Path()
    val pos = FloatArray(2)
    val tan = FloatArray(2)

    // Get first point
    measure.getPosTan(0f, pos, tan)
    cloudPath.moveTo(pos[0], pos[1])

    for (i in 0 until numBumps) {
        val d0 = i * interval
        val d1 = (i + 1) * interval

        measure.getPosTan(d0, pos, tan)
        val p0x = pos[0]; val p0y = pos[1]
        val perpX0 = -tan[1]; val perpY0 = tan[0]  // perpendicular to tangent

        measure.getPosTan(d1, pos, tan)
        val p1x = pos[0]; val p1y = pos[1]

        // Control point: midpoint offset by bumpRadius in perpendicular direction
        val midX = (p0x + p1x) / 2f + perpX0 * bumpRadius * dirSign
        val midY = (p0y + p1y) / 2f + perpY0 * bumpRadius * dirSign

        cloudPath.quadTo(midX, midY, p1x, p1y)
    }

    cloudPath.close()
    return cloudPath
}

// Shoelace formula for winding direction
fun computeWindingSign(path: Path): Float {
    // Approximate path as polygon from PathMeasure samples
    val measure = PathMeasure(path, true)
    val n = 50  // sample count
    val step = measure.length / n
    val points = mutableListOf<FloatArray>()
    val pos = FloatArray(2)
    for (i in 0 until n) {
        measure.getPosTan(i * step, pos, null)
        points.add(floatArrayOf(pos[0], pos[1]))
    }
    var area = 0f
    for (i in points.indices) {
        val j = (i + 1) % points.size
        area += points[i][0] * points[j][1]
        area -= points[j][0] * points[i][1]
    }
    return if (area >= 0f) 1f else -1f
}
```

### Hit-Testing

- **HatchFillElement:** Reconstruct boundary `Path` → use `Path.computeBounds()` for coarse AABB check → use `Region` with `clipPath` for precise point-in-path:
  ```kotlin
  fun hitTest(point: PointF, hitRadius: Float): Boolean {
      val bounds = RectF()
      boundaryPath.computeBounds(bounds, true)
      bounds.inset(-hitRadius, -hitRadius)
      if (!bounds.contains(point.x, point.y)) return false

      // Precise: use Region for point-in-path
      val region = Region()
      val clip = Region(bounds.left.toInt(), bounds.top.toInt(),
                        bounds.right.toInt(), bounds.bottom.toInt())
      region.setPath(boundaryPath, clip)
      return region.contains(point.x.toInt(), point.y.toInt())
  }
  ```

- **RevisionCloudElement:** Same approach but with the cloud (bumpy) path for hit-testing. The fill tint area is the smooth boundary, but the visual outline is bumpy — use the smooth boundary for hit-testing (more intuitive).

### Performance

- **Tile generation:** O(1) per tile — draw a few lines/dots into a small bitmap. Typical tile: 16×16 to 64×64 px. Very fast.
- **BitmapShader fill:** GPU-accelerated. Drawing `canvas.drawPaint()` with a BitmapShader inside a clip is fast regardless of area size.
- **Tile cache:** Store `HashMap<TileCacheKey, Bitmap>` keyed on (pattern, color, spacing, lineWeight, scale). Evict on memory pressure.
- **Cloud path generation:** O(n) where n = number of bumps (typically 20–100). Generated once on placement, cached in element. Regenerated only when bumpRadius or bumpDirection changes.
- **Quarter-resolution preview:** Not needed here — tile generation is cheap. But if future property editing adds live slider preview for hatch params, follow the ¼ resolution pattern from GRAPHIC-ELEMENT-EDITING-SPEC performance notes.

### Unified Tile System (Audit 3.7 compliance)

Per implementation-audit finding 3.7, the tile generation system must be designed to serve BOTH #36 Hatching Fill and #46 Decorated Path (S29). To achieve this:

- `HatchPatternTileGenerator` is a standalone utility in `:core:drawing` — no dependency on any element type
- `HatchPattern` enum covers the 8 engineering patterns for #36
- S29 will later extend with additional `PathFillPattern` entries (stitch patterns, motive patterns)
- The tile generator API accepts enum + params → returns Bitmap — element-agnostic
- Decorated Path (S29) will call the same `generateTile()` + `createShader()` methods, just with a wider pattern enum and corridor-relative angle computation

## RESOLVED CONFLICTS

**Audit 3.7 (Fill Pattern Merge):** Both #36 and #46 use BitmapShader tiling. This session creates the shared tile generation infrastructure. The `HatchPattern` enum in this session covers #36's 8 patterns. S29 will introduce the extended `PathFillPattern` enum from NEW-GRAPHIC-TOOLS-SPEC §2.3 that includes stitch patterns, motive patterns, and technical patterns. At that point, `HatchPattern` either gets extended or a unified super-enum is created. For now, keep `HatchPattern` focused on the 8 engineering types — avoid premature over-engineering.

**IMPLEMENTATION-BLUEPRINT Session 12 vs SESSION-MASTER-PLAN:** The original IMPLEMENTATION-BLUEPRINT (Session 12) included hatching and revision cloud in the shapes session. The SESSION-MASTER-PLAN correctly separated them into S14, acknowledging the dependency on S13's freehand infrastructure. SESSION-MASTER-PLAN is authoritative.

## STUB REQUIREMENTS

| Feature | What to stub | Session that implements |
|---------|-------------|----------------------|
| Hatch pattern picker UI | Use default pattern (STEEL_45) on placement. No picker UI yet. | S18 (Properties Sheet) |
| Pattern angle/scale/spacing sliders | Use defaults (45°, 1.0×, 8px). No slider UI yet. | S18 (Properties Sheet) |
| Pattern line weight slider | Use default (0.5px). No slider UI yet. | S18 (Properties Sheet) |
| Pattern offset sliders | Use defaults (0%, 0%). No slider UI yet. | S18 (Properties Sheet) |
| Boundary stroke picker | Use default (NONE). No picker yet. | S18 (Properties Sheet) |
| Revision cloud bump radius slider | Use default (12px). No slider yet. | S18 (Properties Sheet) |
| Revision cloud bump direction toggle | Use default (OUTWARD). No toggle yet. | S18 (Properties Sheet) |
| Revision cloud fill tint controls | Use default (no fill). No controls yet. | S18 (Properties Sheet) |
| Rev letter label dragging | Place label at top-center of cloud bounding box. Not draggable yet. | S18 (Properties Sheet) |
| #37 Perforation Pattern | Listed in More tray (footwear). Show toast "Coming Soon". | Future (footwear domain pack) |
| #38 Component Zone | Listed in More tray (footwear). Show toast "Coming Soon". | Future (footwear domain pack) |

## TEST CRITERIA

### Hatching Fill (#36)

1. **Draw hatching boundary:** Select Hatching Fill from Shapes tray → draw freehand loop on canvas → release in SNAPPED state → area fills with STEEL_45 pattern.
2. **Auto-close:** Draw hatching boundary but DON'T snap to start → release → boundary auto-closes → hatching fill still appears correctly.
3. **Pattern rendering:** Hatching lines are parallel, evenly spaced, at 45° angle. Lines tile seamlessly (no visible seams at tile boundaries).
4. **Boundary stroke (default NONE):** No visible boundary line — just the hatching pattern.
5. **Select hatching fill:** With Select tool, tap inside a hatching fill → selected (handles appear around bounding box).
6. **Move hatching fill:** Drag selected hatching fill → entire fill (boundary + pattern) moves.
7. **Delete hatching fill:** Select → delete → removed. Undo → restored with correct pattern.
8. **Zoom behavior:** Zoom in on hatching fill → pattern stays at correct image-space scale (doesn't change density with zoom).
9. **Multiple hatching fills:** Place 3 fills with boundaries that overlap → each independently rendered, correct stacking order.
10. **Snap-to-close feedback:** During draw, when nearing start point: blue ring (APPROACHING) then green glow (SNAPPED) — same as S13 freehand.
11. **Chrome auto-hide:** Toolbar and top bar auto-hide during boundary drawing, return on release.

### Revision Cloud (#39)

12. **Draw revision cloud:** Select Revision Cloud from Shapes tray → draw freehand loop → release → smooth boundary converts to bumpy cloud outline.
13. **Bump appearance:** Cloud outline consists of uniform semicircular bumps protruding outward. Bumps are evenly distributed along the boundary.
14. **Rev letter:** Cloud has letter "A" displayed near the top-center. Second cloud gets "B", etc.
15. **Auto-close:** Draw cloud without snapping to start → auto-closes → bumps still generated correctly.
16. **Select cloud:** Tap on cloud outline → selected. Tap inside cloud → selected (if fill area test passes).
17. **Move cloud:** Drag selected cloud → entire cloud moves (bumps + label).
18. **Delete cloud:** Select → delete → removed. Undo → restored. Rev letter freed for reuse? No — rev letters don't recycle.
19. **Default: no fill tint:** Cloud interior is transparent (photo visible through it).
20. **Small cloud:** Draw a very small loop → should still produce at least 4 bumps (minimum). If area is tiny, bump radius auto-reduces.
21. **Large cloud:** Draw a large loop → many bumps, all evenly sized and distributed.

### Integration

22. **Tool palette flow:** Shapes tray → Hatching Fill no longer shows "Coming Soon" toast → activates DRAG_AREA draw mode.
23. **Tool palette flow:** Shapes tray → Revision Cloud no longer shows "Coming Soon" toast → activates DRAG_AREA draw mode.
24. **Undo integration:** Place hatching → undo → removed. Place cloud → undo → removed. Redo → restored.
25. **Layer correctness:** Both element types respect the active layer setting.
26. **Performance:** Drawing boundary at 60fps feels smooth. Hatching fill renders instantly after release. Cloud bump generation causes no visible delay.

## FILES TO CREATE / MODIFY

### Create

| Module | Package | File | Purpose |
|--------|---------|------|---------|
| `:core:drawing` | `com.bcd.technotes.core.drawing` | `DragAreaDrawHandler.kt` | Gesture handler: wraps FreehandCurveDrawer with forced-closure + element routing |
| `:core:drawing` | `com.bcd.technotes.core.drawing.patterns` | `HatchPatternTileGenerator.kt` | Generates Bitmap tiles for 8 hatch patterns + BitmapShader creation |
| `:core:drawing` | `com.bcd.technotes.core.drawing.elements` | `HatchFillElement.kt` | DrawableElement: clip boundary + BitmapShader fill + optional boundary stroke |
| `:core:drawing` | `com.bcd.technotes.core.drawing.elements` | `RevisionCloudElement.kt` | DrawableElement: bumpy cloud outline + optional fill tint + rev letter label |
| `:core:drawing` | `com.bcd.technotes.core.drawing` | `RevisionCloudRenderer.kt` | Converts smooth boundary path to bumpy cloud path (PathMeasure + quadTo arcs) |
| `:core:model` | `com.bcd.technotes.core.model` | `HatchFillData.kt` | Serializable data class + HatchPattern enum + BoundaryStroke enum |
| `:core:model` | `com.bcd.technotes.core.model` | `RevisionCloudData.kt` | Serializable data class + BumpDirection enum |

### Modify

| Module | File | Changes |
|--------|------|---------|
| `:core:drawing` | `GestureRouter.kt` | Route DRAG_AREA tools (#36, #39) to `DragAreaDrawHandler` |
| `:core:drawing` | `ElementFactory.kt` | Add tool ID 36 → `HatchFillElement`, tool ID 39 → `RevisionCloudElement` |
| `:feature:canvas` | `EditableTouchImageView.kt` | Render live preview boundary during drag-area draw (reuse freehand preview path rendering). Render hatch fills and revision clouds in annotation pass. |
| `:feature:canvas` | `CanvasViewModel.kt` | Wire drag-area completion → `addElement()` + undo. Track `usedRevLetters` set for auto-increment. |
| `:core:drawing` | `DrawableElement.kt` | Register `HatchFillElement` and `RevisionCloudElement` as implementations |
| `:core:model` | `ToolRegistry.kt` / tool definitions | Remove "Coming Soon" stubs for #36 and #39. Set active with DRAG_AREA gesture group. |

### Unit Tests

| Module | Test File | What It Tests |
|--------|-----------|--------------|
| `:core:drawing` | `HatchPatternTileGeneratorTest.kt` | Each of 8 patterns generates non-null Bitmap of expected dimensions. GLASS_BLANK returns 1×1 transparent. Tile dimensions scale with spacing/scale params. |
| `:core:drawing` | `RevisionCloudRendererTest.kt` | Cloud path from circular boundary has correct bump count. Small boundary → minimum 4 bumps. OUTWARD vs INWARD produces different paths. Winding sign detection correct for CW/CCW paths. |
| `:core:drawing` | `DragAreaDrawHandlerTest.kt` | Auto-close: open freehand result → handler produces closed boundary. Tool routing: toolId 36 → HatchFillElement, toolId 39 → RevisionCloudElement. |
| `:core:model` | `HatchFillDataTest.kt` | Serialization round-trip (serialize → deserialize → equals). All enum values serialize correctly. |
| `:core:model` | `RevisionCloudDataTest.kt` | Serialization round-trip. BumpDirection enum serialization. Rev letter tracking (next letter assignment). |

---

*Session 14 Blueprint — v1.0*
