# Session 28 — PDF Report Generation

## GOAL
Implement single-project PDF report: cover page, per-photo annotated pages with metadata tables, and summary page. Uses Android `PdfDocument` API with the same Canvas rendering as the annotation engine.

## DEPENDS ON
- **S-27** (EX-01, CE-11) — Burned-in JPEG export, canvas export

## ATOMS
- **EX-04** · PDF Report Generation (L)

## SPEC REFERENCES (inlined)

### EX-04 · PDF Report Generation
Single-project PDF: cover page (name, dates, technician, stats) → per-photo page (annotated photo + metadata + annotation table) → summary page (defect count, pass/fail, annotation types). Android `PdfDocument` (uses same Canvas API as annotation rendering). A4/Letter configurable. 20 photos <10s. 300dpi for printing.

## DELIVERABLES

### 1. PdfReportGenerator.kt
```kotlin
class PdfReportGenerator @Inject constructor(
    private val canvasExporter: CanvasExporter,
    private val projectRepo: ProjectRepository,
    private val photoRepo: PhotoRepository,
    private val annotationRepo: AnnotationRepository,
) {
    // fun generate(projectId: String, pageSize: PageSize, outputFile: File): File
    
    // Page 1: Cover
    //   Project name, date range, technician, total photos, total annotations
    //   Defect/pass/caution breakdown
    //   TechNotes logo
    
    // Pages 2-N: Per-Photo
    //   Annotated photo (scaled to fit, maintaining aspect ratio)
    //   Metadata row: GPS, timestamp, heading
    //   Annotation table: ID, type, semantic tag, notes
    
    // Last page: Summary
    //   Annotation type breakdown
    //   Defect summary table
    //   Evidence chain: SHA-256 hashes per photo
    //   Generation timestamp + technician signature line
}

enum class PageSize {
    A4,     // 595 x 842 points (210 x 297 mm)
    LETTER  // 612 x 792 points (8.5 x 11 inches)
}
```

## KEY IMPLEMENTATION DETAILS

### Android PdfDocument API
```kotlin
val document = PdfDocument()
val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
val page = document.startPage(pageInfo)
val canvas = page.canvas  // Standard Android Canvas — same API as annotation rendering

// Draw content using Canvas API
// ...

document.finishPage(page)
document.writeTo(FileOutputStream(outputFile))
document.close()
```

### Photo Scaling for PDF
```kotlin
// Fit photo within printable area (margins: 36pt = 0.5 inch)
val printableWidth = pageWidth - 72
val printableHeight = pageHeight - 200  // leave room for metadata + table
val scale = min(printableWidth / photoWidth, printableHeight / photoHeight)
```

### 300dpi Rendering
PDF points are 1/72 inch. For 300dpi printing, render bitmaps at 300/72 ≈ 4.17x scale. But for file size: render at 150dpi (2.08x) — good enough for field reports.

### Annotation Table Per Photo
```
| # | Type | Tag | Notes |
|---|------|-----|-------|
| D-001 | Defect Pin | Crack | Near weld joint |
| D-002 | Stamp | FAIL | Corrosion present |
```
Rendered using Canvas text drawing with manual column layout.

## TEST CRITERIA
- [ ] Cover page renders with all project metadata
- [ ] Per-photo pages show annotated photo + metadata + table
- [ ] Summary page shows breakdown and evidence hashes
- [ ] A4 and Letter page sizes work
- [ ] 20 photos generates <10s
- [ ] PDF opens in standard viewers (Google Drive, Adobe)
- [ ] Print quality acceptable at 300dpi
- [ ] File size reasonable (<20MB for 20-photo report)

## FILES TO CREATE / MODIFY

### Create
1. `feature-export/src/main/java/.../PdfReportGenerator.kt`
2. `feature-export/src/main/java/.../PdfPageLayout.kt`
3. `feature-export/src/main/java/.../PdfCoverPage.kt`
4. `feature-export/src/main/java/.../PdfSummaryPage.kt`

### Modify
- `feature-export/src/main/java/.../ShareSheetIntegration.kt` — add PDF sharing option

---

*Session 28 · Size L · 1 atom · PDF report — large single atom*

---

# Session 29a — Collage System: Layout Engine + Base Layouts

## GOAL
Implement collage layout engine, 5 base layout templates (Before/After, Forensic Three-Shot, Quad View, Process Sequence, Hero+Details), semantic borders per cell, and border drag resizing.

## DEPENDS ON
- **S-08** (CE-01) — Base canvas (collage is a composite canvas)
- **S-05** (PD-03) — Photo data model

> **Note:** S-29 is split into S-29a and S-29b per Doc 33 guidance (11 atoms exceeds 5-file rule).

## ATOMS (S-29a)
- **CL-01** · Collage Layout Engine (L)
- **CL-02** · Before/After Layout (S)
- **CL-03** · Forensic Three-Shot (S)
- **CL-04** · Quad View (S)
- **CL-05** · Process Sequence (S)
- **CL-06** · Hero + Details (S)
- **CL-14** · Semantic Borders Per Cell (S)
- **CL-15** · Border Drag Resizing (M)

## SPEC REFERENCES (inlined)

### CL-01 · Collage Layout Engine
`CollageLayout` abstract class. `CollageEngine`: photos + layout → composite canvas. Cell model: photoUri, crop region, metadata toggle. Compose UI: picker + live preview. Handles 2–6 photos. Preview at 30fps, export matches exactly.

### CL-02–CL-06 · Layout Templates
- **Before/After:** Side-by-side, equal width, shared metadata
- **Forensic Three-Shot:** Context → mid-range → close-up, cascading sizes
- **Quad View:** 2×2 equal cells
- **Process Sequence:** Horizontal strip, numbered steps, 3–6 cells
- **Hero + Details:** One large cell + smaller detail cells stacked

### CL-14 · Semantic Borders Per Cell
Left-edge accent color from Semantic Ink per cell. Collage = visual status dashboard.

### CL-15 · Border Drag Resizing
Drag inner borders to resize cell proportions (PicMonkey pattern). Adjacent cells reflow. Minimum 20% enforced.

## DELIVERABLES

### 1. CollageLayout.kt (abstract) + CollageEngine.kt
### 2. Five layout implementations (BeforeAfter, ForensicThreeShot, QuadView, ProcessSequence, HeroDetails)
### 3. CollageScreen.kt (Compose UI: photo picker → layout selection → live preview)
### 4. BorderDragHandler.kt
### 5. SemanticBorderRenderer.kt

## KEY IMPLEMENTATION DETAILS

### Layout Architecture
```kotlin
abstract class CollageLayout {
    abstract val minPhotos: Int
    abstract val maxPhotos: Int
    abstract fun calculateCells(containerSize: Size, photoCount: Int): List<CellRect>
}

data class CellRect(
    val index: Int,
    val rect: RectF,           // normalized 0-1 within container
    val photoUri: String?,
    val semanticColor: Long?,  // left-edge accent
)
```

### User Flow
1. Select photos (2–6) from project
2. Choose layout template
3. Live preview with drag-to-reorder
4. Drag inner borders to resize
5. Per-cell semantic border color (from ink strip)
6. Export as single JPEG (S-29b)

### Border Drag
Detect touch on inner border (within 12dp). Drag adjusts adjacent cell proportions. Minimum 20% width/height enforced. Reflow all adjacent cells.

## TEST CRITERIA
- [ ] All 5 layouts render correctly for their supported photo counts
- [ ] Photo picker allows 2–6 selection
- [ ] Layout template switching preserves photo assignments
- [ ] Border drag resizes cells, minimum 20% enforced
- [ ] Semantic border colors render per cell
- [ ] Preview at 30fps
- [ ] Long-press + drag swaps cell contents

## FILES TO CREATE / MODIFY

### Create
1. `feature-collage/src/main/java/.../CollageLayout.kt` + `CollageEngine.kt`
2. `feature-collage/src/main/java/.../layouts/` (5 layout files)
3. `feature-collage/src/main/java/.../CollageScreen.kt`
4. `feature-collage/src/main/java/.../BorderDragHandler.kt`
5. `feature-collage/src/main/java/.../SemanticBorderRenderer.kt`

---

*Session 29a · Size L · 8 atoms · Collage foundation*

---

# Session 29b — Collage System: Cell Editing + Export

## GOAL
Implement per-cell zoom/pan/swap, metadata bar per cell, and collage export as single JPEG.

## DEPENDS ON
- **S-29a** (CL-01) — Collage layout engine
- **S-27** (EX-01) — Export pipeline

## ATOMS (S-29b)
- **CL-07** · Per-Cell Zoom/Pan/Swap (M)
- **CL-08** · Metadata Bar Per Cell (S)
- **EX-10** · Collage Export (M)

## SPEC REFERENCES (inlined)

### CL-07 · Per-Cell Zoom/Pan/Swap
Tap cell → cell-edit mode (blue border highlight). Pinch to zoom within cell. Drag to pan within cell. Long-press + drag → swap cells with animation.

### CL-08 · Metadata Bar Per Cell
Date, GPS, tech ID per cell. Configurable show/hide per cell. Uses PD-09 metadata.

### EX-10 · Collage Export
Collage as single JPEG at combined resolution. Metadata bars preserved. Sidecar includes per-cell references.

## DELIVERABLES

### 1. CellEditController.kt (zoom/pan within cell)
### 2. CellSwapHandler.kt (long-press drag swap)
### 3. CellMetadataBar.kt
### 4. CollageExporter.kt

## KEY IMPLEMENTATION DETAILS

### Cell-Edit Mode
Tap cell → enter cell-edit. Blue border highlight. All gestures scoped to that cell:
- Pinch: zoom photo within cell bounds (clip to cell rect)
- Drag: pan photo within cell
- Tap outside cell: exit cell-edit

### Collage Export
Render all cells at combined resolution into single Bitmap. Each cell renders its photo (with crop/zoom) + annotations + metadata bar. Use same Canvas API as CanvasExporter.

## TEST CRITERIA
- [ ] Tap cell enters cell-edit (blue border)
- [ ] Pinch/pan within cell works, clipped to bounds
- [ ] Long-press + drag swaps cells with animation
- [ ] Metadata bar per cell shows correct data
- [ ] Collage exports as single JPEG at full resolution
- [ ] Sidecar JSON includes per-cell photo references

## FILES TO CREATE / MODIFY

### Create
1. `feature-collage/src/main/java/.../CellEditController.kt`
2. `feature-collage/src/main/java/.../CellSwapHandler.kt`
3. `feature-collage/src/main/java/.../CellMetadataBar.kt`
4. `feature-export/src/main/java/.../CollageExporter.kt`

### Modify
- `feature-collage/src/main/java/.../CollageScreen.kt` — cell-edit mode integration

---

*Session 29b · Size M · 3 atoms · Collage cell interactions + export*

---

# Session 30 — Completion Ritual + Safety Bar + Flow Arrows + Micro-Share

## GOAL
Implement inspection completion ritual (summary + seal + lock), safety bar warning for unannotated photos, flow arrows, and micro-share overlay. This is the final session — completes Phase 1.

## DEPENDS ON
- **S-27** (EX-01, EX-02, EX-05) — Export pipeline (safety bar checks annotations, micro-share uses Share Sheet)
- **S-15** (DP-03) — Arrow element (flow arrows extend arrows with "FLOW" label)

## ATOMS
- **ON-10** · Inspection Completion Ritual (M)
- **IL-01** · Share Sheet Export — Level 1 Integration (S)
- **DP-53** · Flow Arrows (S)
- **IC-32** · Safety Bar (S)
- **IC-33** · Micro-Share Moment (S)

## SPEC REFERENCES (inlined)

### ON-10 · Inspection Completion Ritual
Summary: photo count, annotation count, defect/pass counts. Completion seal: timestamp + technician ID + SHA-256 hash of all photo hashes. Micro-share: "Send report?" one-tap. Project locks (read-only after seal).

### IL-01 · Share Sheet Export (Level 1)
Integration wrapper around EX-05. Checks for configured CMMS integrations (Phase 3), falls back to standard Share Sheet. In Phase 1: identical to EX-05 behavior.

### DP-53 · Flow Arrows
Arrow + "FLOW" label centered on line. Uses existing arrow rendering + text overlay. Label auto-rotates with arrow direction.

### IC-32 · Safety Bar
Before export: "3 photos have no annotations" warning. Non-blocking (user can proceed). Configurable (can disable in settings).

### IC-33 · Micro-Share Moment
On completion: "Send to...?" overlay with one-tap options (Share Sheet, email, recent contacts). Auto-dismiss 5s. Per-photo and per-project share.

## DELIVERABLES

### 1. CompletionRitualScreen.kt
```kotlin
@Composable
fun CompletionRitualScreen(projectId: String) {
    // Summary card: X photos, Y annotations, Z defects, W passes
    // "Seal Inspection" button
    // On seal: generate completion hash, lock project
    // Show micro-share overlay
    // Animation: seal stamp effect
}
```

### 2. FlowArrowRenderer.kt
```kotlin
class FlowArrowRenderer : AnnotationRenderer {
    // Extends arrow with "FLOW" label
    // Label centered on line, auto-rotates
    // Uses ArrowheadRenderer for head
    // Background behind text for readability
}
```

### 3. SafetyBarCheck.kt
```kotlin
class SafetyBarCheck {
    // fun check(project: Project): SafetyWarning?
    // Counts photos without annotations
    // Returns warning if count > 0
    // Non-blocking: user can dismiss and proceed
}
```

### 4. MicroShareOverlay.kt
```kotlin
@Composable
fun MicroShareOverlay(onShare: (ShareTarget) -> Unit, onDismiss: () -> Unit) {
    // "Send to...?" with Share Sheet, email, recent options
    // Auto-dismiss after 5s (LaunchedEffect + delay)
    // Fade-in/fade-out animation
}
```

## KEY IMPLEMENTATION DETAILS

### Completion Seal
```kotlin
data class CompletionSeal(
    val timestamp: Instant,
    val technicianId: String,
    val projectHash: String,  // SHA-256 of concatenated photo hashes
    val annotationCount: Int,
    val photoCount: Int,
)
```
After seal: project marked read-only in Room. All edit/annotate actions blocked. Unseal requires explicit "Reopen Inspection" action (creates new version).

### Project Locking
```kotlin
// In ProjectEntity:
val isSealed: Boolean = false
val sealedAt: Instant? = null

// In CanvasViewModel:
if (project.isSealed) {
    // Disable all annotation tools
    // Show "Sealed" badge in Browse mode
    // Allow viewing but not editing
}
```

### IL-01 vs EX-05
In Phase 1, IL-01 simply wraps EX-05 with a check:
```kotlin
fun share(context: Context, ...) {
    val cmmsAdapter = integrationRegistry.getActiveAdapter()
    if (cmmsAdapter != null) {
        cmmsAdapter.pushAnnotation(envelope)  // Phase 3
    } else {
        shareSheetIntegration.share(context, ...)  // EX-05
    }
}
```
Phase 1: always falls through to Share Sheet.

## TEST CRITERIA
- [ ] Completion shows correct summary (photo/annotation/defect counts)
- [ ] Seal generates SHA-256 hash of all photo hashes
- [ ] Project locks after seal (no editing possible)
- [ ] Flow arrows render with "FLOW" label, auto-rotated
- [ ] Safety bar shows warning for unannotated photos
- [ ] Safety bar is non-blocking (can dismiss and export)
- [ ] Micro-share overlay shows for 5s then auto-dismisses
- [ ] Share Sheet opens from micro-share
- [ ] IL-01 falls through to EX-05 (no CMMS in Phase 1)

## FILES TO CREATE / MODIFY

### Create
1. `feature-export/src/main/java/.../CompletionRitualScreen.kt`
2. `feature-canvas/src/main/java/.../render/FlowArrowRenderer.kt`
3. `feature-export/src/main/java/.../SafetyBarCheck.kt`
4. `feature-export/src/main/java/.../MicroShareOverlay.kt`

### Modify
- `core/data/src/main/java/.../project/ProjectEntity.kt` — isSealed, sealedAt fields
- `feature-canvas/src/main/java/.../CanvasViewModel.kt` — sealed project check
- `feature-export/src/main/java/.../ShareSheetIntegration.kt` — IL-01 wrapper
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt` — register FlowArrowRenderer

---

*Session 30 · Size M · 5 atoms · Phase 1 complete — 127 atoms across 31 sessions*
