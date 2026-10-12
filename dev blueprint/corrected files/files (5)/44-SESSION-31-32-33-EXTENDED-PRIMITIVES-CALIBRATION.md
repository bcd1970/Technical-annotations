# 44 — Sessions 31–33: Extended Primitives + Calibration

> **Phase 2 drawing primitives that extend Phase 1 foundations.** All depend only on Phase 1 atoms. Can start immediately after Phase 1 is complete.

---

## S-31 · Extended Line Primitives

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-06 | Break line zigzag | S | DP-01 |
| DP-07 | Break line wave | S | DP-01 |
| DP-08 | Stitch line (footwear) | S | DP-01 |
| DP-09 | Zigzag inline (electronics) | S | DP-01 |
| DP-10 | Coil / spring (electronics) | S | DP-01 |
| DP-11 | Wave / sine (electronics) | S | DP-01 |
| DP-12 | Grainline arrow (footwear) | S | DP-01 |

### CONTEXT

All seven are two-point drag elements extending the straight line (DP-01). They share the same gesture model (DOWN→MOVE→UP), the same endpoint draggability, and the same undo/redo semantics. The difference is the path decoration between the two endpoints.

**Trade Kit Mapping:**
- Break zigzag/wave → P&ID, Mechanical
- Stitch line → Footwear & Garment (merged into LineElement as DashType.STITCH)
- Zigzag inline, coil, wave/sine → Electronics
- Grainline arrow → Footwear & Garment (double-headed arrow + centered "GRAINLINE" label)

### DATA MODELS

```kotlin
// DP-08 merges into existing LineElement
// LineElement.dashType = DashType.STITCH (new enum value)

@Serializable
data class BreakLineElement(
    override val id: String = UUID.randomUUID().toString(),
    val startPoint: ImagePoint,
    val endPoint: ImagePoint,
    val style: BreakLineStyle,  // ZIGZAG, WAVE
    val amplitude: Float = 8f,  // dp, zigzag/wave height
    val frequency: Int = 6,     // number of zigzag teeth or wave cycles
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
) : AnnotationElement()

@Serializable
data class DecoratedLineElement(
    override val id: String = UUID.randomUUID().toString(),
    val startPoint: ImagePoint,
    val endPoint: ImagePoint,
    val decoration: LineDecoration,  // ZIGZAG_INLINE, COIL, WAVE_SINE
    val amplitude: Float = 6f,
    val frequency: Float = 1.0f,    // cycles per 100dp
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
) : AnnotationElement()

// DP-12 uses LineElement with both arrowheadStart and arrowheadEnd
// + centered "GRAINLINE" text label (GrainlineArrowElement)
@Serializable
data class GrainlineArrowElement(
    override val id: String = UUID.randomUUID().toString(),
    val startPoint: ImagePoint,
    val endPoint: ImagePoint,
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val label: String = "GRAINLINE",
) : AnnotationElement()
```

### RENDERING

**Break line zigzag:** Generate zigzag path along the line direction. Each tooth is an equilateral triangle spanning the line. The break indicates a section has been removed — the line should have a visible gap at the break symbol.

**Break line wave:** Sinusoidal path along the line. Smooth cubic bezier approximation.

**Stitch line:** Already handled — add `DashType.STITCH` to existing enum (alternating long-short dashes simulating stitching).

**Zigzag inline / Coil / Wave-sine:** Continuous decorated path between start and end. Coil uses tight circular arcs. Wave uses sin(x) mapped to path.

**Grainline arrow:** Double-headed standard arrow with "GRAINLINE" text centered on the line, rotated to match line angle. Text auto-flips for readability (same logic as dimension text).

### KEY IMPLEMENTATION DETAILS

Path generation for all decorated lines:
1. Calculate line vector and length
2. Generate decoration path in local coordinates (0,0 → length,0)
3. Transform to image space using rotation matrix from line angle
4. Draw with `canvas.drawPath(path, paint)` using viewMatrix

### DELIVERABLES
1. `BreakLineRenderer.kt` — zigzag + wave break lines
2. `DecoratedLineRenderer.kt` — zigzag inline, coil, wave/sine
3. `GrainlineArrowRenderer.kt` — double-head + centered label
4. Data model classes in `:core:model`

### ACCEPTANCE
- All 7 variants render at all zoom levels
- Endpoints draggable after placement
- Decoration scales proportionally with zoom (amplitude in image-space, not screen-space)
- Trade Kit filtering correct
- Undo/redo works for all

---

## S-32 · Circle + Shape Variants

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-20 | Circle + divider | S | DP-18 |
| DP-21 | Circle + crosshairs | S | DP-18 |
| DP-22 | Concentric circle | S | DP-18 |
| DP-26 | Rectangle + patterns | S | DP-23 |
| DP-29 | Split circle | S | DP-18 |

### CONTEXT

Five shape variants extending the Phase 1 circle (DP-18) and rectangle (DP-23). All use the same resize handle system from Phase 1.

**Circle+divider:** Circle with horizontal line through center — P&ID flow division symbol.
**Circle+crosshairs:** Circle with full crosshair — measurement/alignment reference.
**Concentric circle:** Two or more concentric circles — bearing housings, O-ring seats.
**Rectangle+patterns:** Rectangle with internal fill patterns (hatched, X-fill, dot-fill).
**Split circle:** Circle divided vertically, each half independently colorable — P&ID on/off valve state.

### DATA MODELS

```kotlin
@Serializable
data class CircleVariantElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val radius: Float,
    val variant: CircleVariant,  // DIVIDER, CROSSHAIRS, CONCENTRIC, SPLIT
    val concentricCount: Int = 2,        // for CONCENTRIC
    val concentricSpacing: Float = 8f,   // dp between rings
    val splitLeftColor: Long? = null,    // for SPLIT
    val splitRightColor: Long? = null,   // for SPLIT
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
) : AnnotationElement()

@Serializable
data class RectangleVariantElement(
    override val id: String = UUID.randomUUID().toString(),
    val topLeft: ImagePoint,
    val bottomRight: ImagePoint,
    val fillPattern: RectFillPattern,  // HATCHED, X_FILL, DOT_FILL
    val patternDensity: Float = 1.0f,  // multiplier
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()
```

### RENDERING

**Hatched fill:** 45° parallel lines at configurable density.
**X-fill:** Two sets of 45° lines (cross-hatch).
**Dot-fill:** Regular grid of dots.

All fill patterns clip to the parent shape boundary.

### DELIVERABLES
1. `CircleVariantRenderer.kt` — all 4 circle variants
2. `RectangleVariantRenderer.kt` — pattern fills
3. Data model classes
4. Tests for variant rendering

### ACCEPTANCE
- Internal geometry (dividers, crosshairs, concentric rings) scales with circle resize
- Split circle halves independently colorable
- Fill patterns clip correctly to shape boundary
- Concentric circles maintain even spacing during resize

---

## S-33 · Radius/Diameter Dimension + Calibration

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-15 | Radius / diameter dimension | M | DP-13 |
| DP-17 | Calibration session | M | DP-13 |

### CONTEXT

**DP-15** extends the dimension line system (DP-13 from Phase 1 S-18). Instead of measuring between two arbitrary points, it measures from center to edge of a circle (radius) or across a circle (diameter). Auto-prefixes "R" or "Ø".

**DP-17** is the calibration system that converts all measurements from pixels to real-world units. The technician photographs a known-size reference object (ruler, coin, business card) and sets the pixel-to-mm ratio.

### DATA MODELS

```kotlin
@Serializable
data class RadialDimensionElement(
    override val id: String = UUID.randomUUID().toString(),
    val center: ImagePoint,
    val edgePoint: ImagePoint,
    val mode: RadialMode,  // RADIUS, DIAMETER
    val precision: Int = 1,
    val unit: String = "px",  // "px" until calibrated, then "mm"/"in"
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val fontSize: Float = 12f,
) : AnnotationElement()

// Calibration is per-photo, stored in PhotoEntity
@Serializable
data class Calibration(
    val pixelsPerUnit: Float,          // px per mm (or inch)
    val unit: CalibrationUnit,         // MM, INCH
    val referenceLength: Float,        // known length in unit
    val referencePixels: Float,        // measured pixel distance
    val timestamp: Long,
)
```

### CALIBRATION FLOW

1. User taps "Calibrate" from measurement tool properties
2. "Place two points on a known distance" instruction
3. User taps two endpoints on reference object
4. Dialog: "What is this distance?" → number input + unit picker (mm/cm/m/in/ft)
5. System calculates `pixelsPerUnit = pixelDistance / realDistance`
6. All dimension elements on this photo update from "px" to calibrated units
7. Calibration badge appears on photo in project hub

### KEY IMPLEMENTATION DETAILS

**Radius dimension:** Draws leader line from circle center to edge, with "R 25.0mm" text.
**Diameter dimension:** Draws line through circle center, edge to edge, with "Ø 50.0mm" text.

Both auto-detect if placed near a CircleElement and snap to its geometry.

Calibration persists in PhotoEntity (Room). When a calibrated photo is exported, calibration metadata is included in the Annotation Envelope.

### DELIVERABLES
1. `RadialDimensionRenderer.kt`
2. `CalibrationSession.kt` — two-point calibration flow
3. `CalibrationUI.kt` — Compose dialog + instruction overlay
4. `RadialDimensionElement.kt` data model

### ACCEPTANCE
- "R" prefix for radius, "Ø" for diameter auto-applied
- Calibration persists per photo across sessions
- All existing dimension elements (DP-13, DP-14) switch to calibrated units after calibration
- Calibration badge visible in project hub
- Works with any reference object size

---

*April 2026 · TechNotes Phase 2 Sessions 31–33 v1.0*
