# 28 — Atoms: Drawing Primitives
## All 65 Annotation Elements + Arrowhead System

> **Shared dependencies for most primitives:** CE-06 (rendering) and PD-05 (type system) are required by nearly all drawing primitives. Some also depend on PD-06 (geometry) and CE-03 (coordinates) — check Doc 26 for each atom's exact dependency list. Dependencies below only list what goes beyond CE-06 + PD-05.

---

## Group A — Two-Point Elements (Drag Gesture)

### DP-01 · Straight Line
**Phase:** 1 · **Size:** S
`LineElement`: startPoint, endPoint, strokeWidth, dashType (SOLID/DASHED/DOTTED/STITCH), arrowheadStart?, arrowheadEnd?. `LineRenderer`. Drag gesture: DOWN→MOVE→UP. Preview line during drag. Endpoints draggable after placement.

### DP-02 · Arrowhead Renderer (20-Style System)
**Phase:** 1 · **Size:** M · **Depends:** DP-01
Standalone `ArrowheadRenderer`. 20 styles across 4 categories — **Filled:** Standard, Narrow, Wide, Barbed. **Open:** Standard, Narrow, Barbed, Notched. **Geometric:** Dot, Circle, Diamond, Square, Slash. **Technical:** Datum triangle, Architectural tick, Origin indicator, Dimension tick, ISO arrow. **Decorative:** Feathered, Double. **Critical: inset-point pattern** — line endpoint pulled back by arrowhead depth so line never pokes through fills. `ArrowheadMetrics.getInsetDistance(style, size)`.

**Acceptance:** No line through filled heads at any zoom. All 20 visually distinct. Scale with stroke width.

### DP-03 · Arrow (Single-Head)
**Phase:** 1 · **Size:** S · **Depends:** DP-01, DP-02
`LineElement` with arrowheadEnd configured. Style configurable via properties.

### DP-04 · Arrow (Double-Head)
**Phase:** 1 · **Size:** S · **Depends:** DP-01, DP-02
Both arrowheadStart and arrowheadEnd. Both use inset-point.

### DP-05 · Leader + Text
**Phase:** 1 · **Size:** M · **Depends:** DP-01, DP-02, DP-25
`LeaderElement`: line + text label at endpoint + arrowhead at start. Keyboard auto-appears on finger-up. Smart label placement (CE-09). Label draggable independently. Connector line between label and endpoint.

### DP-06 · Break Line Zigzag — **Phase 2** · S · Depends: DP-01
### DP-07 · Break Line Wave — **Phase 2** · S · Depends: DP-01
### DP-08 · Stitch Line (Footwear) — **Phase 2** · S · Depends: DP-01
Merged into LineElement as DashType.STITCH.
### DP-09 · Zigzag Inline (Electronics) — **Phase 2** · S · Depends: DP-01
### DP-10 · Coil / Spring (Electronics) — **Phase 2** · S · Depends: DP-01
### DP-11 · Wave / Sine (Electronics) — **Phase 2** · S · Depends: DP-01
### DP-12 · Grainline Arrow (Footwear) — **Phase 2** · S · Depends: DP-01
Double-headed arrow with centered "GRAINLINE" label.

---

## Group B — Measurement Elements (Multi-Tap)

### DP-13 · Dimension Line (Two-Tap)
**Phase:** 1 · **Size:** M · **Depends:** DP-01, DP-02
`DimensionElement`: point1, point2, extensionLineHeight, measurementText, precision (0–3 decimals), unit. Tap-tap gesture. Extension lines perpendicular. Arrowheads both ends (default: architectural tick). Text centered above, auto-rotates for readability. Calibration-aware (DP-17).

### DP-14 · Angular Dimension (Three-Tap)
**Phase:** 1 · **Size:** M · **Depends:** DP-13
`AngularDimensionElement`: vertex, point1, point2. Arc between lines with angle in degrees. Handles acute, right, obtuse. Reflex option via properties.

### DP-15 · Radius/Diameter Dimension — **Phase 2** · S · Depends: DP-13
### DP-16 · Area Measurement — **Phase 2** · M · Depends: DP-64, PD-06
Polygon-based. Semi-transparent fill + centered area text. Calibration-aware.
### DP-17 · Calibration Session — **Phase 2** · M · Depends: DP-13
Two-tap on known reference → enter real-world distance → pixels-per-unit. Stored per project. All dimensions switch from "px" to real units.

---

## Group C — Shapes (Tap-Place)

All: tap → shape at point → 1.5s soft placement → tap elsewhere confirms.

### DP-18 · Circle (Empty)
**Phase:** 1 · **Size:** S
`CircleElement`: center, radius, optional text/divider/crosshairs/innerRadius. Default 40dp. 4 cardinal resize handles.

### DP-19 · Circle + Text — **Phase 1** · S · Depends: DP-18, DP-25
Text centered inside. Circle auto-sizes to fit.

### DP-20 · Circle + Divider (P&ID) — **Phase 2** · S · Depends: DP-18
### DP-21 · Circle + Crosshairs (Welding) — **Phase 2** · S · Depends: DP-18
### DP-22 · Concentric Circle — **Phase 2** · S · Depends: DP-18

### DP-23 · Rectangle (Empty)
**Phase:** 1 · **Size:** S
`RectangleElement`: center, width, height, optional text/pattern/cornerRadius. Default 60×40dp. 8 handles.

### DP-24 · Labeled Rectangle — **Phase 1** · S · Depends: DP-23, DP-25
### DP-25 · Text Annotation
**Phase:** 1 · **Size:** M
`TextElement`: position, text, fontSize, fontWeight, fontFamily (Plus Jakarta/IBM Plex), maxWidth (wraps), rotation. `StaticLayout` for multi-line. Tap→keyboard→type→tap elsewhere. Optional semi-transparent background. Supports emoji and all localized IMEs.

### DP-26 · Rectangle + Patterns — **Phase 2** · S · Depends: DP-23
### DP-27 · Diamond — **Phase 1** · S. Rotated 45° rectangle.
### DP-28 · Triangle — **Phase 1** · S. Orientation UP/DOWN/LEFT/RIGHT.
### DP-29 · Split Circle (P&ID) — **Phase 2** · S · Depends: DP-18

---

## Group D — Freehand, Path & Area

### DP-30 · Freehand Curve (Catmull-Rom + RDP)
**Phase:** 1 · **Size:** M
`FreehandElement`: point list. `CatmullRomSmoother` (centripetal α=0.5). RDP point reduction (60–80% reduction). Pipeline: raw→RDP→Catmull-Rom→cached Path.

**Acceptance:** Smooth, no lag during drawing. 500-point curve at 60fps.

### DP-31 · Hatching Fill
**Phase:** 1 · **Size:** M
`HatchingFillElement`: boundary polygon + pattern (SINGLE/CROSS/DOTS). Hatch angle/spacing configurable. Lines clipped to boundary.

### DP-32 · Revision Cloud
**Phase:** 1 · **Size:** M · **Depends:** DP-30
Boundary path with scalloped arc edges. Arc amplitude configurable. Smooth corner handling.

### DP-62 · Curve Snap-to-Close (Smooth Endpoint Joining)
**Phase:** 1 · **Size:** S · **Depends:** DP-30, CE-03, IC-23

**Deliverables:**
- During freehand drawing: if current point enters ~20dp screen-space radius of start point, visual magnet indicator appears (filled dot + snap ring on start point)
- Lift finger inside radius → curve closes with smooth Catmull-Rom join (start point = first AND last control point, C1 continuity)
- Lift outside → open curve as normal
- Haptic CLOCK_TICK on snap engagement
- Closed curves gain `isClosed: Boolean` + optional semi-transparent fill
- Snap radius in screen-space (constant regardless of zoom)

**Acceptance:** No visible seam at join. Snap feels natural. Closed curve can become filled region. Works at all zoom levels.

### DP-63 · Polyline (Multi-Point Path)
**Phase:** 1 · **Size:** M · **Depends:** CE-03

**Deliverables:**
- `PolylineElement`: ordered vertex list, isClosed Boolean
- Gesture: tap vertex → tap next → ... → double-tap to complete
- Straight segments between vertices. Preview line from last vertex to touch point
- Each vertex draggable after placement
- Works with snap-to-close (DP-62) — approaching first vertex snaps to polygon

**Acceptance:** Unlimited vertices (practical ~100). Double-tap completes without extra vertex. Each vertex independently draggable.

### DP-64 · Polygon (Closed Polyline + Fill)
**Phase:** 1 · **Size:** S · **Depends:** DP-63

**Deliverables:**
- PolylineElement with `isClosed=true` + fill properties
- Semi-transparent fill, color from semantic ink with reduced opacity
- Area auto-calculated for closed polygon (feeds DP-16)
- Boundary stroke on top of fill

**Acceptance:** Fill correct for convex and concave. Area calculation accurate. Self-intersecting handled gracefully.

---

## Group E — Inline Symbols (Tap-on-Line)

### DP-33 · Inline Symbol Base System
**Phase:** 2 · **Size:** M · **Depends:** DP-01, CE-07
`InlineSymbolElement`: parent line ref, position 0.0–1.0, symbolType, auto-rotation to line angle. Symbol at line intersection, splits line. Draggable along parent.

### DP-34 · Bowtie Valve — 2/S/DP-33
### DP-35 · Inline Triangle+Bar — 2/S/DP-33
### DP-36 · Diamond Inline — 2/S/DP-33
### DP-37 · Parallel Plates — 2/S/DP-33
### DP-38 · Cylinder/Piston — 2/S/DP-33
### DP-39 · Hydraulic Spool — 2/S/DP-33

Each: own SVG renderer, fixed aspect ratio, scale with stroke width.

---

## Group F — Wizard & Symbol

### DP-40 · Symbol Placement Base
**Phase:** 2 · **Size:** M
`SymbolElement`: position, scale, rotation, symbolId. Browser grid filtered by active kit. SVG data from bundled assets.

### DP-41 · Op-Amp — 2/S/DP-40
### DP-42 · Logic Gate — 2/S/DP-40. Multiple types (AND/OR/NOT/NAND/NOR/XOR).

### DP-43 · GD&T Feature Control Frame Wizard
**Phase:** 2 · **Size:** L · **Depends:** DP-40
5-step wizard: geometric characteristic (14 types per ASME Y14.5) → tolerance + material condition → datum refs. Auto-sizing frame. <10s to complete.

### DP-44 · Welding Symbol Wizard (AWS A2.4)
**Phase:** 2 · **Size:** L · **Depends:** DP-40
5-step wizard: weld type → arrow/other side → size/length/pitch → supplementary symbols → tail content. AWS↔ISO standard switching (arrow side flips). <15s to complete.

### DP-45 · Tolerance Block — 2/S/DP-40
### DP-46 · Surface Finish Symbol — 2/S/DP-40
### DP-47 · Hardware Mark (Footwear) — 2/S/DP-40
### DP-48 · Notch Mark (Footwear) — 2/S/DP-40
### DP-49 · EU Material Grid Wizard — 2/L/DP-40

---

## Group N — New Elements

### DP-50 · Pass/Fail/Caution Stamps
**Phase:** 1 · **Size:** M
`StampElement` with stampType: PASS/FAIL/CAUTION/CRACK/CORROSION/LEAK. Circular with icon + semantic color. One-tap place, confirmation haptic. Auto-tagged for export. Configurable size S/M/L.

### DP-51 · Defect Markers — 1/S/DP-50. Specialized stamps.
### DP-52 · Numbered Pins (Auto-Sequence)
**Phase:** 1 · **Size:** M
`NumberedPinElement`: position, sequenceNumber, prefix (D-/E-/P- per kit). Auto-number, auto-renumber on delete (200ms slide animation). Pin visual: filled circle + number.

### DP-53 · Flow Arrows — 1/S/DP-03. Arrow + "FLOW" label.
### DP-54 · Safety/Hazard Symbols (ISO 7010) — 2/M/DP-40, L10N-04
~200 symbols. Hierarchical grid (P/W/M/E/F). Language-independent.

### DP-55 · NDT Designations — 2/S/DP-40
### DP-56 · Auto-Metadata Overlay
**Phase:** 1 · **Size:** M · **Depends:** PD-09
Semi-transparent bar: GPS, timestamp, technicianId, heading, project name. Auto-generated, configurable fields. Renders in export, toggleable in live view.

### DP-65 · Cross-Photo Annotations (Collage)
**Phase:** 2 · **Size:** M · **Depends:** CL-01, CE-06
Annotations spanning collage cell gutters. Arrow from detail→overview. Numbered sequences across cells. Measurement comparison lines. Stored with source+target cell references. Survives cell reordering.

---

## Group X — Deferred (Phase 4)

| ID | Name | Depends |
|----|------|---------|
| DP-57 | Decorated path | DP-30 |
| DP-58 | Linear array wizard | DP-40 |
| DP-59 | Radial array wizard | DP-40 |
| DP-60 | Pattern brush | DP-30 |
| DP-61 | Symmetry guide | CE-06 |

---

*April 2026 · TechNotes Atoms: Drawing Primitives v2.1 · 65 elements*
*Changes from v2.0: Implicit dependency statement corrected — CE-03 and PD-06 are NOT universal across all primitives (only CE-06 + PD-05 are shared by most). Check Doc 26 for exact deps per atom.*
