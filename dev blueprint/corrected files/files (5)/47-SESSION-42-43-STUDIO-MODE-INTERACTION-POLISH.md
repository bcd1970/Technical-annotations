# 47 — Sessions 42–43: Studio Mode + Interaction Polish

> **Desktop-class precision tools on mobile** (Studio Mode) and **four interaction upgrades** that make the annotation workflow feel intelligent.

---

## S-42 · Studio Mode

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| IC-16 | Studio mode activation | M | IC-01, IC-03 |
| IC-17 | Studio properties panel | M | IC-16 |
| IC-18 | Studio layer management | M | IC-16, PD-11 |

### CONTEXT

Studio Mode transforms TechNotes from a fast field tool into a precision annotation workstation. It's designed for after-field use — sitting at a desk, reviewing and refining annotations. The Chrome layout shifts: toolbar moves to the left edge (vertical), a full properties panel opens on the right, and a layer management panel appears.

**Activation:** Long-press the mode indicator OR Settings → Studio Mode toggle. Persists until explicitly exited. Not available during camera capture.

### STUDIO CHROME LAYOUT

```
┌─────────────────────────────────────────────┐
│ [Tool]                          [Properties] │
│ [Tool]     ┌───────────────┐    Stroke: 2px │
│ [Tool]     │               │    Color: ■■■  │
│ [Tool]     │    CANVAS     │    Dash: ─ ─ ─ │
│ [Tool]     │               │    Arrow: ▶    │
│ [Tool]     │               │    Fill: none  │
│ [Tool]     └───────────────┘                │
│ [────── Layer Panel ──────]                 │
│ 👁 Layer 1 (me)  ■ #4488FF               │
│ 👁 Layer 2 (expert)  ■ #FF4444            │
│ 🔒 Layer 3 (archived)                     │
└─────────────────────────────────────────────┘
```

### STUDIO PROPERTIES PANEL (IC-17)

Full property inspector for the selected annotation element. Unlike the long-press properties sheet (IC-12, Phase 1), this panel is always visible and updates in real-time as the selection changes.

```kotlin
@Composable
fun StudioPropertiesPanel(
    selectedElement: AnnotationElement?,
    onPropertyChange: (PropertyChange) -> Unit,
) {
    // Common properties (all elements):
    //   - Stroke width (slider)
    //   - Stroke color (full color picker, not just semantic strip)
    //   - Opacity (slider)
    //   - Lock/unlock
    //   - Layer assignment
    
    // Type-specific properties:
    //   - Lines: dash type, arrowhead start/end, endpoint coordinates
    //   - Shapes: fill color, fill pattern, dimensions
    //   - Text: font size, font weight, alignment
    //   - Dimension: precision, unit, extension line height
    //   - Symbols: symbol type, scale, rotation
    
    // Changes apply immediately (live preview)
    // All changes go through undo system
}
```

### LAYER MANAGEMENT (IC-18)

Layer panel shows all annotation layers for the current photo. Uses PD-11 (S-40) layer model.

Operations:
- **Visibility toggle** — eye icon per layer
- **Lock toggle** — lock icon (locked layers can't be edited)
- **Reorder** — drag to reorder draw order
- **Rename** — tap layer name to edit
- **Merge** — long-press → "Merge down"
- **Delete** — swipe to delete (with confirmation)
- **Add layer** — "+" button
- **Color** — per-layer color indicator

### DELIVERABLES
1. `StudioModeController.kt` — activation, chrome swap, state persistence
2. `StudioPropertiesPanel.kt` — full property inspector
3. `LayerManagementPanel.kt` — layer list with operations
4. `StudioChrome.kt` — vertical toolbar layout

### ACCEPTANCE
- Studio mode toggle <300ms transition
- Properties panel updates in real-time with selection
- Layer visibility/lock/reorder work correctly
- All changes go through undo system
- Studio mode persists across process death
- Works in landscape orientation

---

## S-43 · Canvas Interaction Polish Phase 2

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| IC-15 | Context ring (radial menu) | M | CE-04 |
| IC-19 | Intent-reading transitions | M | IC-01 |
| IC-25 | Annotation summary ribbon | S | IC-01, PD-04 |
| IC-31 | Ghost overlay capture | M | CC-01, CE-06 |

### CONTEXT RING (IC-15)

Radial menu that appears on long-press (500ms) on canvas. Shows context-appropriate actions in a circular arrangement around the touch point. Replaces long-press → bottom sheet for common actions.

```kotlin
@Composable
fun ContextRing(
    position: Offset,           // screen coordinates of long-press
    context: ContextRingContext, // determines which actions appear
    onAction: (ContextAction) -> Unit,
) {
    // Context-dependent items (max 8):
    // On empty canvas: Paste, Add Photo, Text, Stamp
    // On annotation: Delete, Duplicate, Properties, Lock, Send to Back/Front
    // On selection: Group, Align, Distribute
    
    // Visual: semi-transparent ring with icon segments
    // Drag finger to segment → highlight → release → action
    // Release on center → dismiss
}
```

**Gesture:** Long-press → ring appears → drag to segment (without lifting) → release to select. This is a single continuous gesture — faster than long-press → wait → tap.

### INTENT-READING TRANSITIONS (IC-19)

Predictive mode transitions based on gesture analysis. Instead of explicit mode switches, the system reads the user's likely intent from gesture velocity, direction, and context:

- Fast swipe from edge → probably navigating (→ Browse mode)
- Slow deliberate touch → probably annotating (→ Annotate mode)
- Two-finger approach → probably zooming (stay in current mode)
- Tap on annotation → probably selecting (→ if in Browse, stay; if in Annotate, select)

```kotlin
class IntentReader(
    private val modeStateMachine: ModeStateMachine,
) {
    fun analyzeGestureStart(
        position: Offset,
        velocity: Offset,
        fingerCount: Int,
        currentMode: AppMode,
    ): IntentPrediction
    
    // Prediction confidence threshold: 0.8
    // Below threshold: don't auto-switch, let user complete gesture
    // Above threshold: pre-animate transition (200ms head start)
}
```

### ANNOTATION SUMMARY RIBBON (IC-25)

When transitioning from ANNOTATE → BROWSE, a brief ribbon appears: "3 defects · 1 measurement · D-001–D-003". Auto-dismiss after 3 seconds. Tappable to expand into full annotation list.

### GHOST OVERLAY CAPTURE (IC-31)

When revisiting a location, the viewfinder shows the previous photo at 30% opacity as a ghost overlay. Annotations from the previous session appear as outline overlays. Helps ensure consistent framing across visits.

```kotlin
class GhostOverlayCapture(
    private val photoRepository: PhotoRepository,
) {
    // Match by: GPS proximity (<50m) + equipment ID (if tagged)
    // Display: 30% opacity previous photo
    // Annotations: outline-only (no fill), dashed stroke
    // Toggle: button in camera viewfinder
    // Checklist overlay: which annotations from last time are still relevant
    
    suspend fun findGhostPhoto(
        currentGps: Location?,
        currentEquipmentId: String?,
    ): GhostOverlay?
}
```

### DELIVERABLES
1. `ContextRing.kt` — radial menu Compose component
2. `IntentReader.kt` — gesture analysis + prediction
3. `AnnotationSummaryRibbon.kt` — transitional summary
4. `GhostOverlayCapture.kt` — viewfinder ghost overlay

### ACCEPTANCE
- Context ring appears in <100ms after 500ms hold threshold
- Intent reader >80% accuracy on mode prediction
- Ribbon auto-dismisses at 3s, tappable to expand
- Ghost overlay matches GPS <50m, 30% opacity, toggleable
- All features work at 60fps

---

*April 2026 · TechNotes Phase 2 Sessions 42–43 v1.0*
