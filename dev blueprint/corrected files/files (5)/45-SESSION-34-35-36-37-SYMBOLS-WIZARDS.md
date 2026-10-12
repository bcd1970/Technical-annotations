# 45 — Sessions 34–37: Symbols + Engineering Wizards

> **Professional engineering symbol system.** Inline symbols on lines, standalone symbols, and wizard-built elements (GD&T, Welding, EU Material Grid). The competitive moat.

---

## S-34 · Inline Symbol System

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-33 | Inline symbol base system | M | DP-01, CE-07 |
| DP-34 | Bowtie valve | S | DP-33 |
| DP-35 | Inline triangle + bar | S | DP-33 |
| DP-36 | Diamond inline | S | DP-33 |
| DP-37 | Parallel plates | S | DP-33 |
| DP-38 | Cylinder / piston | S | DP-33 |
| DP-39 | Hydraulic spool | S | DP-33 |

### CONTEXT

Inline symbols are placed ON an existing line. The symbol sits at the intersection point, splitting the line visually. The symbol auto-rotates to match the parent line's angle and is draggable along the parent line (position stored as 0.0–1.0 parametric value).

**Gesture:** User selects inline symbol tool → taps on existing line → symbol appears at tap point. Hit-testing uses the parent line's hit area (CE-07).

**Trade Kit Mapping:**
- Bowtie valve, triangle+bar, diamond inline → P&ID
- Parallel plates → Electronics
- Cylinder/piston, hydraulic spool → Hydraulic

### DATA MODEL

```kotlin
@Serializable
data class InlineSymbolElement(
    override val id: String = UUID.randomUUID().toString(),
    val parentLineId: String,        // reference to parent LineElement
    val position: Float,             // 0.0 (start) to 1.0 (end) along parent
    val symbolType: InlineSymbolType, // BOWTIE, TRIANGLE_BAR, DIAMOND, PLATES, CYLINDER, SPOOL
    val scale: Float = 1.0f,         // relative to stroke width
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()

enum class InlineSymbolType {
    BOWTIE_VALVE,      // two triangles meeting at points (butterfly valve)
    TRIANGLE_BAR,      // triangle with perpendicular bar (check valve)
    DIAMOND_INLINE,    // diamond shape on line (generic valve)
    PARALLEL_PLATES,   // two parallel lines (capacitor)
    CYLINDER_PISTON,   // rectangle with rod (actuator)
    HYDRAULIC_SPOOL,   // rectangle with internal channels
}
```

### RENDERING

Each symbol is drawn in local coordinates centered at (0,0), then transformed:
1. Calculate position on parent line: `point = lerp(start, end, position)`
2. Calculate parent line angle: `angle = atan2(dy, dx)`
3. Apply rotation + translation matrix
4. Draw symbol paths

**Line splitting:** The parent line is rendered in two segments with a gap at the symbol location. Gap width = symbol bounding box width along line direction.

### KEY IMPLEMENTATION DETAILS

- If parent line is deleted, orphaned inline symbols are also deleted (cascade)
- If parent line endpoints are moved, inline symbol position updates automatically
- Symbol scale is proportional to parent line stroke width
- Multiple symbols can exist on the same line (each at different `position` value)

### DELIVERABLES
1. `InlineSymbolRenderer.kt` — base system + all 6 symbol SVG paths
2. `InlineSymbolPlacementHandler.kt` — tap-on-line gesture detection
3. `InlineSymbolElement.kt` — data model
4. SVG path constants for each symbol type

### ACCEPTANCE
- Symbols snap to parent line on placement
- Auto-rotate to line angle
- Draggable along parent (0.0–1.0)
- Parent line splits correctly at symbol
- Cascade delete when parent removed
- All 6 symbols visually distinct and trade-kit filtered

---

## S-35 · Symbol Placement + Basic Symbols

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-40 | Symbol placement base | M | CE-06, CE-07 |
| DP-41 | Op-amp | S | DP-40 |
| DP-42 | Logic gate (6 types) | S | DP-40 |
| DP-45 | Tolerance block | S | DP-40 |
| DP-46 | Surface finish symbol | S | DP-40 |
| DP-47 | Hardware mark (footwear) | S | DP-40 |
| DP-48 | Notch mark (footwear) | S | DP-40 |

### CONTEXT

Standalone symbols placed by tap on canvas. Unlike inline symbols, these are independent — not attached to a line. The symbol browser shows a filterable grid based on active Trade Kit.

### DATA MODEL

```kotlin
@Serializable
data class SymbolElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val symbolId: String,          // references bundled SVG asset
    val scale: Float = 1.0f,
    val rotation: Float = 0f,      // degrees
    val strokeColor: Long,
    val fillColor: Long? = null,
) : AnnotationElement()
```

### SYMBOL BROWSER

```kotlin
@Composable
fun SymbolBrowserGrid(
    activeKit: TradeKit,
    onSymbolSelected: (String) -> Unit,
) {
    // Filterable grid organized by category
    // Categories: Electronics, Mechanical, P&ID, Footwear, etc.
    // Search bar at top
    // Recent/favorites section
    // Each cell: SVG preview + name
}
```

### DELIVERABLES
1. `SymbolRenderer.kt` — renders any SymbolElement from SVG data
2. `SymbolBrowserGrid.kt` — filterable Compose grid
3. `SymbolElement.kt` — data model
4. SVG assets for all 7 symbols (op-amp, 6 logic gates, tolerance, surface finish, hardware, notch)

### ACCEPTANCE
- Browser filters by active Trade Kit
- Tap-place gesture works
- Scale + rotate handles after placement
- SVGs render crisp at all zoom levels
- Search works in browser

---

## S-36 · GD&T + Welding Symbol Wizards

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-43 | GD&T Feature Control Frame wizard | L | DP-40 |
| DP-44 | Welding Symbol wizard (AWS A2.4) | L | DP-40 |

### CONTEXT

Two complex wizard-built elements. Each uses a multi-step Compose dialog that guides the user through building a technically correct engineering symbol. These are the competitive moat — no mobile app offers these.

### GD&T WIZARD (DP-43)

**5-step flow:**
1. **Geometric characteristic** — pick from 14 types per ASME Y14.5 (flatness, straightness, circularity, cylindricity, profile of line, profile of surface, angularity, perpendicularity, parallelism, position, concentricity, symmetry, circular runout, total runout)
2. **Tolerance value** — numeric input + material condition modifier (MMC Ⓜ, LMC Ⓛ, RFS — regardless of feature size)
3. **Primary datum** — letter picker (A–Z) + material condition
4. **Secondary datum** — optional, same as primary
5. **Tertiary datum** — optional, same as primary

**Output:** Auto-sizing feature control frame with compartments:

```
┌────┬──────────┬────┬────┬────┐
│ ⌖  │ 0.05 Ⓜ  │ A  │ B  │ C  │
└────┴──────────┴────┴────┴────┘
```

```kotlin
@Serializable
data class GDTElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val characteristic: GDTCharacteristic,    // 14 types
    val tolerance: Float,
    val toleranceModifier: MaterialCondition?, // MMC, LMC, RFS
    val primaryDatum: DatumRef?,
    val secondaryDatum: DatumRef?,
    val tertiaryDatum: DatumRef?,
    val scale: Float = 1.0f,
    val strokeColor: Long,
) : AnnotationElement()

@Serializable
data class DatumRef(
    val letter: Char,        // A-Z
    val modifier: MaterialCondition?,
)
```

### WELDING SYMBOL WIZARD (DP-44)

**5-step flow:**
1. **Weld type** — fillet, groove (V/bevel/U/J/flare-V/flare-bevel), plug, slot, spot, seam, stud, surfacing, edge
2. **Arrow side / Other side** — which side gets the weld symbol
3. **Size / length / pitch** — numeric inputs for weld dimensions
4. **Supplementary symbols** — field weld flag, weld-all-around, melt-through, backing, spacer, contour (flush/convex/concave)
5. **Tail content** — process code, specification, procedure number (optional)

**AWS ↔ ISO standard switching:** The arrow side placement convention is reversed between AWS A2.4 and ISO 2553. A toggle in the wizard flips the symbol placement:
- AWS: arrow side symbol BELOW reference line
- ISO: arrow side symbol ABOVE reference line

```kotlin
@Serializable
data class WeldingSymbolElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val arrowPoint: ImagePoint,          // where the arrow points on the workpiece
    val weldType: WeldType,
    val arrowSide: WeldSideConfig,
    val otherSide: WeldSideConfig?,
    val supplementary: Set<WeldSupplementary>,
    val tailContent: String?,
    val standard: WeldingStandard,       // AWS_A2_4, ISO_2553
    val scale: Float = 1.0f,
    val strokeColor: Long,
) : AnnotationElement()
```

### KEY IMPLEMENTATION DETAILS

Both wizards:
- Use `ModalBottomSheet` with step indicator (1/5, 2/5…)
- Animated transitions between steps
- Back navigation to previous steps
- Live preview of the element being built
- Complete button disabled until minimum required fields filled
- Element placed at pre-selected canvas position after wizard completes

### DELIVERABLES
1. `GDTWizard.kt` — 5-step Compose wizard
2. `WeldingSymbolWizard.kt` — 5-step + standard switch
3. `GDTRenderer.kt` — auto-sizing compartmented frame
4. `WeldingSymbolRenderer.kt` — reference line + arrow + symbols
5. Data model classes

### ACCEPTANCE
- GD&T completes in <10s for experienced user
- Welding completes in <15s
- AWS↔ISO toggle flips arrow side placement correctly
- Auto-sizing frame renders all configurations correctly
- Both wizards support edit (tap existing element → re-opens wizard with current values)

---

## S-37 · EU Material Grid + Safety/Hazard + NDT

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-49 | EU Material Grid wizard | L | DP-40 |
| DP-54 | Safety/hazard symbols (ISO 7010) | M | DP-40, L10N-04 |
| DP-55 | NDT designations | S | DP-40 |

### EU MATERIAL GRID (DP-49)

Wizard for EU Directive 94/11/EC footwear labeling. Generates a grid showing material composition for upper, lining, insole, and outsole.

**3-step wizard:**
1. **Component selection** — which parts to label (upper, lining+sock, outsole)
2. **Material per component** — leather, coated leather, textile, other (with ISO pictograms)
3. **Layout** — horizontal or vertical grid

```kotlin
@Serializable
data class EUMaterialGridElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val components: List<MaterialComponent>,
    val layout: GridLayout,  // HORIZONTAL, VERTICAL
    val scale: Float = 1.0f,
    val strokeColor: Long,
) : AnnotationElement()

@Serializable
data class MaterialComponent(
    val part: FootwearPart,     // UPPER, LINING_SOCK, OUTSOLE
    val material: MaterialType, // LEATHER, COATED_LEATHER, TEXTILE, OTHER
)
```

### SAFETY/HAZARD SYMBOLS (DP-54)

Extends the ISO 7010 picker from Phase 1 (L10N-04, S-25) to make symbols placeable as annotation elements on the canvas. ~200 symbols across 5 categories (P/W/M/E/F). Language-independent.

This is NOT a new picker — it reuses the existing ISO 7010 picker from L10N-04 but wires it to the symbol placement system (DP-40) so symbols can be placed on photos as annotations.

### NDT DESIGNATIONS (DP-55)

Non-destructive testing method symbols: RT (radiographic), UT (ultrasonic), MT (magnetic particle), PT (penetrant), VT (visual), ET (eddy current). Simple letter-code stamps with standardized formatting.

### DELIVERABLES
1. `EUMaterialGridWizard.kt` — 3-step wizard
2. `EUMaterialGridRenderer.kt` — grid with ISO pictograms
3. `SafetySymbolRenderer.kt` — extends L10N-04 picker → canvas placement
4. `NDTDesignationRenderer.kt` — letter-code stamps

### ACCEPTANCE
- EU grid wizard <10s
- Safety symbols from existing picker placeable as annotations
- NDT codes render in standard formatting
- All elements persist and export correctly

---

*April 2026 · TechNotes Phase 2 Sessions 34–37 v1.0*
