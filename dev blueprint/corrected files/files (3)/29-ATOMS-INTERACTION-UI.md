# 29 — Atoms: Interaction & UI
## Chrome · Modes · Edit Pipeline · Collage · Onboarding · Design System

> **Everything the user sees and touches** that isn't a drawing primitive.

---

## Area 05 — Interaction & Chrome (IC)

### IC-01 · Three-Mode State Machine
**Phase:** 1 · **Size:** M · **Depends:** —
`AppMode { BROWSE, ANNOTATE, EDIT }`. Valid: BROWSE↔ANNOTATE, BROWSE↔EDIT. No direct ANNOTATE↔EDIT. Auto-save on transition. Mode survives process death. Transition <300ms.

### IC-02 · Mode Transition Animations
**Phase:** 1 · **Size:** S · **Depends:** IC-01, DS-02
Spring dampingRatio 0.85, stiffness High. Toolbar slide, FAB scale, all <300ms.

### IC-03 · Annotate Mode Toolbar (5 Categories + Text)
**Phase:** 1 · **Size:** M · **Depends:** IC-01, DS-01
`[Lines] [Measure] [Shapes] [Marks] [⋯] + [Text T]`. Bottom edge (thumb zone). Tap category → tray. Long-press → properties. Reconfigures per Trade Kit. Touch targets ≥56dp.

### IC-04 · Tool Tray (Horizontal Scroll)
**Phase:** 1 · **Size:** S · **Depends:** IC-03
LazyRow below category. Tool name + icon. Most recent variant remembered per category.

### IC-05 · Tool Persistence + 3-Tier Carry
**Phase:** 1 · **Size:** S · **Depends:** IC-03, PD-10
Tier 1 (session): color/tool/size persist annotation-to-annotation. Tier 2 (burst): equipment ID, numbering across photos. Tier 3 (global): defaults from PD-10.

### IC-06 · Semantic Ink Color Strip
**Phase:** 1 · **Size:** S · **Depends:** IC-01, DS-01
Left edge, vertical. Red=defect, Yellow=caution, Green=pass, Blue=info. Tap to switch. Current highlighted. Color carries semantic tag in export.

### IC-07 · Undo/Redo Engine (3 Stacks)
**Phase:** 1 · **Size:** M · **Depends:** PD-07, IC-01
Three independent UndoRedoManagers (ANNOTATE, EDIT, BROWSE=none). Two-finger double-tap → undo. Stack switched on mode change. Max 50 per stack.

### IC-08 · Undo Pill UI
**Phase:** 1 · **Size:** S · **Depends:** IC-07, DS-01
Left edge floating pill. Tap=undo, long-press=undo/redo options. Count badge. Reduced opacity during active gesture.

### IC-09 · Chrome Auto-Hide Controller
**Phase:** 1 · **Size:** S · **Depends:** IC-01
On gesture start: toolbar→4dp accent line, top bar fades. Undo pill stays. On end: spring return. Exception flags for future features. >95% canvas during gesture.

### IC-10 · Edit Mode Toolbar (6 Slots)
**Phase:** 1 · **Size:** M · **Depends:** IC-01, DS-01
`[Flip | Rotate | Crop | Adjust | Overlay | More]`. Adjust = inline sliders (not separate screen). More = vignette, grain, LUT.

### IC-11 · Browse Mode Chrome
**Phase:** 1 · **Size:** S · **Depends:** IC-01, DS-01
Top: project name + share + export. Bottom: photo strip. Right: pencil FAB + edit FAB.

### IC-12 · Long-Press → Properties Sheet
**Phase:** 1 · **Size:** S · **Depends:** IC-03
Bottom sheet with tool-specific properties (stroke, dash, arrowhead, fill, font). Changes apply to current and future uses (Tier 1).

### IC-13 · Soft Placement (1.5s Window)
**Phase:** 1 · **Size:** S · **Depends:** CE-06
Element draggable for 1.5s after placement. Tap elsewhere confirms early. Subtle pulse animation. Configurable 0.5–3.0s.

### IC-14 · Smart Paper Shape Recognition (includes former AI-01 scope)
**Phase:** 1 · **Size:** L · **Depends:** DP-30, DP-18, DP-23, DP-03
Freehand→recognized shape. Geometric analysis (circularity, corners, aspect ratio). Shapes: circle, rectangle, line, arrow, X, checkmark, triangle. 200ms snap preview + haptic. >70% confidence → snap, <70% → keep freehand. Wrong shape? Tap → 4-alternative picker, auto-dismiss 1.5s. Recognition <50ms. Pure geometry, no ML for Phase 1.

### IC-15 · Context Ring (Radial Menu) — **Phase 2** · M · Depends: CE-04
### IC-16 · Studio Mode Activation — **Phase 2** · M · Depends: IC-01, IC-03
### IC-17 · Studio Mode Properties Panel — **Phase 2** · M · Depends: IC-16
### IC-18 · Studio Mode Layer Management — **Phase 2** · M · Depends: IC-16, PD-11
### IC-19 · Intent-Reading Transitions (IMP-18) — **Phase 2** · M · Depends: IC-01

### IC-20 · Draw = Enter Annotate (IMP-01)
**Phase:** 1 · **Size:** S · **Depends:** IC-01, IC-14
In BROWSE: single-finger drag detected (velocity <300dp/s, sustained >80ms) → auto-transition to ANNOTATE mid-gesture. Stroke captured as first annotation. Pencil FAB remains as visual affordance but is no longer required. Special case: after camera capture, user lands in "primed" state where first gesture immediately draws — no FAB, no transition animation.

### IC-21 · Color as Gesture (IMP-02)
**Phase:** 1 · **Size:** S · **Depends:** IC-06, CE-04
Option A: slide thumb to color strip → continue into draw (unbroken touch). Option B (S Pen): pressure=semantic color (light=blue, medium=yellow, firm=red). Strip remains as fallback and current-color indicator.

### IC-22 · Back-Tap Undo (IMP-13)
**Phase:** 1 · **Size:** S · **Depends:** IC-07
Physical back-tap (Android 14+) → undo. Shake-to-undo fallback. Supplements two-finger double-tap for gloved users.

### IC-23 · Haptic Vocabulary
**Phase:** 1 · **Size:** S · **Module:** `:core:ui` (exception — shared infrastructure, not `:feature-canvas`) · **Depends:** DS-01
`HapticFeedback` object: CONFIRM (placement), CLOCK_TICK (snap), REJECT (invalid), SUCCESS (double-pulse completion). Intensity: NONE/LIGHT/STRONG per PD-10.

### IC-24 · Spring Physics Animation
**Phase:** 1 · **Size:** S · **Module:** `:core:ui` (exception — shared infrastructure, not `:feature-canvas`) · **Depends:** DS-02
Shared `SpringSpec` configs. All <300ms. Compose `animateFloatAsState` with spring.

### IC-25 · Annotation Summary Ribbon (IMP-20) — **Phase 2** · S · Depends: IC-01, PD-04
"3 defects · 1 measurement · D-001–D-003" on ANNOTATE→BROWSE. Auto-dismiss 3s.

### IC-26 · Quick-Label Dictionary (IMP-05)
**Phase:** 1 · **Size:** M · **Depends:** DP-25, PD-01
After 3+ uses: most-used text labels as suggestion chips above keyboard. Frequency-sorted, recency-boosted. Room DB stores usage count + last-used. Team tier: synced vocabulary. Tap chip = instant insert.

**Acceptance:** Suggestions <100ms from keyboard open. Top 5 shown. All text fields contribute to dictionary.

### IC-27 · Swipe-Between-Photos in Annotate (IMP-06)
**Phase:** 1 · **Size:** M · **Depends:** IC-01, IC-05, CE-04

**CRITICAL.** Eliminates 3 taps per photo transition.

Fast edge-initiated horizontal swipe (>600dp/s, starting from edge 15%, spanning >60% width) navigates next/previous photo WITHOUT leaving Annotate mode. Distinguished from drawing by zone+velocity+span. All carry-forwards apply: tool, color, size, equipment ID, numbering (D-003 → D-004 on swipe).

**Acceptance:** Zero false positives during drawing. Numbering carries across. 60fps transition. Photo strip updates.

### IC-28 · Frequency-Sorted Ink Strip (IMP-07)
**Phase:** 1 · **Size:** S · **Depends:** IC-06, PD-10
Color strip reorders by usage frequency per kit. Welding → red first. Maintenance → green first. Tracked per kit in PD-10. Most-used pre-selected on new photo.

### IC-29 · Text Without Keyboard Blackout (IMP-08)
**Phase:** 1 · **Size:** M · **Depends:** DP-25, CE-02
1. Photo compresses to top 50% when keyboard opens — target always visible. 2. Live preview bubble at annotation position, real-time as user types. 3. Double-tap blank canvas → immediate text cursor (no tool selection).

**Acceptance:** Compression <200ms. Preview updates per-keystroke. Double-tap works without text tool selected.

### IC-30 · Photo-Aware Label Placement (IMP-09)
**Phase:** 1 · **Size:** S · **Depends:** CE-09
Extends CE-09 with Canny edge detection (<8ms) for photo saliency. Labels pushed away from saliency hotspots AND 15% exclusion radius around annotation's own point.

### IC-31 · Ghost Overlay Capture (IMP-11)
**Phase:** 2 · **Size:** M · **Depends:** CC-01, CE-06
Viewfinder shows previous photo at 30% opacity. Annotations as outline overlays. Consistent framing for revisits. Checklist overlay. Toggle button.

### IC-32 · Safety Bar (IMP-15)
**Phase:** 1 · **Size:** S · **Depends:** PD-04, EX-01
Before export: "3 photos have no annotations" warning. Non-blocking. Configurable (can disable).

### IC-33 · Micro-Share Moment (IMP-17)
**Phase:** 1 · **Size:** S · **Depends:** EX-05, ON-10
On completion: "Send to...?" overlay with one-tap options (Share Sheet, email, recent). Auto-dismiss 5s. Per-photo and per-project.

---

## Area 06 — Edit Pipeline (EP)

### EP-01 · Flip & Rotate — **Phase 1** · S · Depends: IC-10
Flip H/V, rotate 90° CW/CCW, free rotation. Annotations transform with photo.

### EP-02 · Crop Tool — **Phase 1** · M · Depends: IC-10, CE-01
Draggable rectangle, aspect lock (free/1:1/4:3/16:9), grid overlay. Annotations outside crop deleted, coords remapped.

### EP-03 · AGSL RuntimeShader Adjustments — **Phase 1** · M · Depends: IC-10
AGSL (API 33+): brightness, contrast, saturation, exposure. Inline sliders below toolbar. Real-time preview. Non-destructive (original preserved). ColorMatrix fallback for API <33.

### EP-04 · Vignette — 2/S/EP-03
### EP-05 · Film Grain — 2/S/EP-03
### EP-06 · Color LUT Presets — 2/M/EP-03. 5–10 built-in film looks.
### EP-07 · Overlay Browser — 2/M/IC-10. Gallery/templates + blend modes + opacity.

---

## Area 07 — Collage System (CL)

### CL-01 · Collage Layout Engine
**Phase:** 1 · **Module:** `:feature-collage` · **Size:** L · **Depends:** CE-01, PD-03
`CollageLayout` abstract class. `CollageEngine`: photos + layout → composite canvas. Cell model: photoUri, crop region, metadata toggle. Compose UI: picker + live preview. Handles 2–6 photos. Preview at 30fps, export matches exactly.

### CL-02 · Before/After — 1/S/CL-01. Side-by-side, equal width, shared metadata.
### CL-03 · Forensic Three-Shot — 1/S/CL-01. Context→mid→close-up, cascading.
### CL-04 · Quad View — 1/S/CL-01. 2×2 equal cells.
### CL-05 · Process Sequence — 1/S/CL-01. Horizontal strip, numbered steps, 3–6 cells.
### CL-06 · Hero + Details — 1/S/CL-01. One large + smaller detail cells stacked.

### CL-07 · Per-Cell Zoom/Pan/Swap
**Phase:** 1 · **Size:** M · **Depends:** CL-01, CE-05
Tap cell → highlight → cell-edit. Pinch=zoom within cell, drag=pan within cell, long-press+drag=swap.

### CL-08 · Metadata Bar Per Cell — 1/S/CL-01, PD-09
Date, GPS, tech ID per cell. Configurable show/hide.

### CL-09 · Smart Layout Suggestion (IMP-12) — **Phase 2** · M · Depends: CL-01, PD-03
Analyzes photo set: 2 similar <30s apart → Before/After. 3–6 sequential pins → Process Sequence. 4 different angles → Quad. Layout pre-applied, user confirms or changes.

### CL-10 · Domain Templates — 2/S/CL-01, TK-01
### CL-11 · Freeform Layout — 2/M/CL-01. Free drag, resize, overlap, drop shadow.
### CL-12 · Video-to-GIF Cells — **Phase 3** · L · Depends: CL-01, CC-06

### CL-13 · Synchronized Zoom/Pan (Linked Cells)
**Phase:** 2 · **Size:** S · **Depends:** CL-07
Chain-link toggle per cell. Linked cells zoom/pan together for objective before/after at identical scale. Two-finger drag bypass (escape hatch).

### CL-14 · Semantic Borders Per Cell
**Phase:** 1 · **Size:** S · **Depends:** CL-01, IC-06
Left-edge accent color from Semantic Ink per cell. Collage = visual status dashboard. Persists in export. Tagged in sidecar JSON.

### CL-15 · Border Drag Resizing
**Phase:** 1 · **Size:** M · **Depends:** CL-01
Drag inner borders to resize cell proportions (PicMonkey pattern). Adjacent cells reflow. Minimum 20% enforced. Handle on press over border.

### CL-16 · Content-Aware Auto-Crop — 2/S/CL-07, AI-15
Saliency-based centering on cell placement. Face detection priority. Tap-to-reposition override.

### CL-17 · Auto-Match Exposure — 2/M/CL-01, EP-03
One-tap "Match": normalize white balance + exposure across all collage photos.

---

## Area 08 — Onboarding & Navigation (ON)

### ON-01 · Splash + Cold Launch — 1/S. 300ms max. Routes to onboarding or hub.
### ON-02 · First-Launch Onboarding — 1/M/ON-01, TK-01
3 steps (15s+30s+15s): work question → first photo → kit suggestion. No account. Auto-creates "My First Project".

### ON-03 · Work Question UI — 1/S/ON-02. 8 tiles, "More..." expandable.
### ON-04 · Project Hub — 1/M/PD-02, DS-01
LazyColumn of project cards. FAB new project. Top bar: logo+search+settings. Long-press context menu.

### ON-05 · Project Creation — 1/S/ON-04, PD-02. 3 taps max.
### ON-06 · Search — 2/M/PD-13. Grouped results, deep-link to annotation.

> **Cross-phase note:** PD-13 (full-text search index) is Phase 4. For Phase 2, implement ON-06 with Room FTS (Full-Text Search) or simple LIKE queries against existing Project/Photo/Annotation tables. Replace with PD-13's dedicated search index in Phase 4.
### ON-07 · Settings — 1/M/PD-10, DS-01. Account, kits, integrations, defaults, evidence, display.

### ON-08 · Warm Launch Restoration
**Phase:** 1 · **Size:** S · **Depends:** ON-04, IC-01

Restores to **Browse Mode** with the last-viewed photo and all annotation state visible. Even if the user was in Annotate or Edit mode when the app was killed, warm launch always returns to Browse as a **safety default** — prevents accidental annotation from stray touches on app resume. Camera warms in background. Photo strip position, scroll state, and project context all restored.

> **Design rationale:** Browse is the safe home state. The pencil FAB is one tap away from Annotate, and Draw=Enter Annotate (IC-20) means the user can start drawing immediately without even tapping the FAB. The cost of restoring to Browse is near-zero; the cost of an accidental annotation in a restored Annotate mode is real.

### ON-09 · Situation-First Onboarding (IMP-04) — 2/M/ON-02
Photo-recognition tiles instead of text. "Which looks like your work?"

### ON-10 · Completion Ritual (IMP-19)
**Phase:** 1 · **Size:** M · **Depends:** IC-01, EX-01
Summary: photo/annotation/defect/pass count. Completion seal (timestamp+tech+hash). Micro-share: "Send report?" one-tap. Project locks (read-only).

### ON-11 · Deep Linking
**Phase:** 2 · **Size:** S · **Depends:** ON-06, PD-04
Search result → navigate to project → photo → highlight specific annotation. Navigation Compose deep links.

---

## Area 09 — Design System (DS)

### DS-01 · Design Tokens — 1/M
Color palette (monochrome chrome + semantic colors + accent). Spacing 4dp base. Typography scale (Plus Jakarta Sans + IBM Plex Mono). Elevation (0/2/4/8dp). Corners (0/4/8/16dp). Compose Theme wrapper.

### DS-02 · Animation System — 1/S/DS-01
`TechNotesMotion`: spring specs, durations (150ms short, 300ms medium), easing.

### DS-03 · Dark-First Theme — 1/S/DS-01
Always dark. Zero chroma in chrome. Material 3 dark + custom overrides.

### DS-04 · Touch Targets — 1/S/DS-01
Min 56dp (recommended 64dp gloved). 12dp+ spacing. Tokens: `MinTouchTarget`, `GloveTouchTarget`.

### DS-05 · Icon Set — 1/M/DS-01
Tool, mode, nav, semantic icons. Monochrome, consistent stroke. SVG→ImageVector.

### DS-06 · Typography — 1/S/DS-01
Plus Jakarta Sans (regular/medium/semibold/bold) + IBM Plex Mono. Bundled fonts. Compose Typography.

---

*April 2026 · TechNotes Atoms: Interaction & UI v2.2*
*Changes from v2.1: IC-14 expanded with AI-01 scope (merged — same implementation). IC-23/IC-24 module location clarified (:core:ui, not :feature-canvas). ON-06 cross-phase fallback implementation specified (Room FTS for Phase 2).*
