# 26 — Feature Atom Registry: Master Index
## Every Implementable Unit in TechNotes — 265 Atoms · 17 Areas

> **Purpose:** Single-page lookup for all atom IDs. Each atom maps to a detail document (27–31). Claude Code sessions reference atom IDs from this index.

---

## Reading This Index

- **ID format:** `AREA-##` — e.g. `PD-03` = Project & Data atom 03
- **Phase:** 1 = Foundation, 2 = Depth, 3 = Platform, 4 = Expansion
- **Size:** S = partial session (2–3 files), M = full session (4–5 files), L = multi-session (6+ files)

---

## Area 01 — Project & Data (PD) → Doc 27

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| PD-01 | Room database setup | 1 | M | — |
| PD-02 | Project data model + DAO + repository | 1 | S | PD-01 |
| PD-03 | Photo data model + DAO + repository | 1 | M | PD-01, PD-02 |
| PD-04 | Annotation data model + DAO | 1 | M | PD-01, PD-03, PD-05 |
| PD-05 | Annotation element sealed class hierarchy | 1 | M | — |
| PD-06 | Geometry primitives + coordinate transforms | 1 | S | — |
| PD-07 | Undo/redo command model | 1 | S | PD-05 |
| PD-08 | Evidence chain (SHA-256 at capture) | 1 | S | PD-03 |
| PD-09 | Auto-metadata capture service | 1 | M | PD-03 |
| PD-10 | Settings/preferences data store | 1 | S | PD-01 |
| PD-11 | Annotation layer data model | 2 | S | PD-04 |
| PD-12 | Equipment/asset linkage model | 4 | S | PD-04 |
| PD-13 | Search index (full-text) | 4 | M | PD-02, PD-03, PD-04 |
| PD-14 | Per-annotation provenance metadata | 1 | S | PD-04 |

## Area 02 — Canvas Engine (CE) → Doc 27

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| CE-01 | EditableTouchImageView (base canvas) | 1 | L | PD-06 |
| CE-02 | viewMatrix system (single matrix for all) | 1 | M | CE-01 |
| CE-03 | Touch-to-image coordinate pipeline | 1 | S | CE-01, CE-02, PD-06 |
| CE-04 | StatelessGestureRouter | 1 | M | CE-01 |
| CE-05 | Two-finger zoom/pan handler | 1 | S | CE-04 |
| CE-06 | Annotation rendering pipeline (onDraw) | 1 | M | CE-01, CE-02, PD-05 |
| CE-07 | Hit-testing engine | 1 | M | PD-06, CE-03 |
| CE-08 | Selection & manipulation handles | 1 | M | CE-07 |
| CE-09 | Smart label placement (8-position greedy) | 1 | S | CE-06, PD-06 |
| CE-10 | Alignment guides (Figma-style snapping) | 1 | S | CE-06, PD-06 |
| CE-11 | Canvas export (render to bitmap) | 1 | M | CE-06 |

## Area 03 — Camera & Capture (CC) → Doc 27

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| CC-01 | CameraX viewfinder + capture | 1 | M | — |
| CC-02 | Camera pre-warming at app launch | 1 | S | CC-01 |
| CC-03 | Photo metadata injection | 1 | S | CC-01, PD-09 |
| CC-04 | Gallery photo picker | 1 | S | PD-03 |
| CC-05 | Split-second buffer (pre-capture) | 2 | M | CC-01 |
| CC-06 | Video capture + frame extraction | 2 | L | CC-01 |

## Area 04 — Drawing Primitives (DP) → Doc 28

### Group A: Two-Point (drag)
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-01 | Straight line | 1 | S | CE-06, PD-05 |
| DP-02 | Arrowhead renderer (20-style) | 1 | M | DP-01 |
| DP-03 | Arrow (single-head) | 1 | S | DP-01, DP-02 |
| DP-04 | Arrow (double-head) | 1 | S | DP-01, DP-02 |
| DP-05 | Leader + text | 1 | M | DP-01, DP-02, DP-25 |
| DP-06 | Break line zigzag | 2 | S | DP-01 |
| DP-07 | Break line wave | 2 | S | DP-01 |
| DP-08 | Stitch line (footwear) | 2 | S | DP-01 |
| DP-09 | Zigzag inline (electronics) | 2 | S | DP-01 |
| DP-10 | Coil / spring (electronics) | 2 | S | DP-01 |
| DP-11 | Wave / sine (electronics) | 2 | S | DP-01 |
| DP-12 | Grainline arrow (footwear) | 2 | S | DP-01 |

### Group B: Measurement (multi-tap)
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-13 | Dimension line (two-tap) | 1 | M | DP-01, DP-02 |
| DP-14 | Angular dimension (three-tap) | 1 | M | DP-13 |
| DP-15 | Radius/diameter dimension | 2 | S | DP-13 |
| DP-16 | Area measurement | 2 | M | DP-64, PD-06 |
| DP-17 | Calibration session | 2 | M | DP-13 |

### Group C: Shapes (tap-place)
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-18 | Circle (empty) | 1 | S | CE-06, PD-05 |
| DP-19 | Circle + text | 1 | S | DP-18, DP-25 |
| DP-20 | Circle + divider (P&ID) | 2 | S | DP-18 |
| DP-21 | Circle + crosshairs (welding) | 2 | S | DP-18 |
| DP-22 | Concentric circle | 2 | S | DP-18 |
| DP-23 | Rectangle (empty) | 1 | S | CE-06, PD-05 |
| DP-24 | Labeled rectangle | 1 | S | DP-23, DP-25 |
| DP-25 | Text annotation | 1 | M | CE-06, PD-05 |
| DP-26 | Rectangle + patterns | 2 | S | DP-23 |
| DP-27 | Diamond | 1 | S | CE-06, PD-05 |
| DP-28 | Triangle | 1 | S | CE-06, PD-05 |
| DP-29 | Split circle (P&ID) | 2 | S | DP-18 |

### Group D: Freehand, Path & Area
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-30 | Freehand curve (Catmull-Rom + RDP) | 1 | M | CE-06, PD-05 |
| DP-31 | Hatching fill | 1 | M | CE-06, PD-05 |
| DP-32 | Revision cloud | 1 | M | DP-30 |
| DP-62 | Curve snap-to-close | 1 | S | DP-30, CE-03, IC-23 |
| DP-63 | Polyline (multi-point, double-tap complete) | 1 | M | CE-06, PD-05, CE-03 |
| DP-64 | Polygon (closed polyline + fill) | 1 | S | DP-63 |

### Group E: Inline Symbols (tap-on-line)
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-33 | Inline symbol base system | 2 | M | DP-01, CE-07 |
| DP-34 | Bowtie valve (P&ID) | 2 | S | DP-33 |
| DP-35 | Inline triangle+bar (P&ID) | 2 | S | DP-33 |
| DP-36 | Diamond inline (P&ID) | 2 | S | DP-33 |
| DP-37 | Parallel plates (electronics) | 2 | S | DP-33 |
| DP-38 | Cylinder/piston (hydraulic) | 2 | S | DP-33 |
| DP-39 | Hydraulic spool | 2 | S | DP-33 |

### Group F: Wizard & Symbol
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-40 | Symbol placement base | 2 | M | CE-06, CE-07 |
| DP-41 | Op-amp (electronics) | 2 | S | DP-40 |
| DP-42 | Logic gate (electronics) | 2 | S | DP-40 |
| DP-43 | GD&T feature control frame wizard | 2 | L | DP-40 |
| DP-44 | Welding symbol wizard (AWS A2.4) | 2 | L | DP-40 |
| DP-45 | Tolerance block | 2 | S | DP-40 |
| DP-46 | Surface finish symbol | 2 | S | DP-40 |
| DP-47 | Hardware mark (footwear) | 2 | S | DP-40 |
| DP-48 | Notch mark (footwear) | 2 | S | DP-40 |
| DP-49 | EU material grid wizard | 2 | L | DP-40 |

### Group N: New Elements
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-50 | Pass/Fail/Caution stamps | 1 | M | CE-06, PD-05 |
| DP-51 | Defect markers (crack/corrosion/leak) | 1 | S | DP-50 |
| DP-52 | Numbered pins (auto-sequence) | 1 | M | CE-06, PD-05 |
| DP-53 | Flow arrows | 1 | S | DP-03 |
| DP-54 | Safety/hazard symbols (ISO 7010) | 2 | M | DP-40, L10N-04 |
| DP-55 | NDT designations | 2 | S | DP-40 |
| DP-56 | Auto-metadata overlay | 1 | M | CE-06, PD-09 |
| DP-65 | Cross-photo annotations (collage) | 2 | M | CL-01, CE-06 |

### Group X: Deferred
| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DP-57 | Decorated path | 4 | M | DP-30 |
| DP-58 | Linear array wizard | 4 | M | DP-40 |
| DP-59 | Radial array wizard | 4 | M | DP-40 |
| DP-60 | Pattern brush | 4 | M | DP-30 |
| DP-61 | Symmetry guide | 4 | S | CE-06 |

## Area 05 — Interaction & Chrome (IC) → Doc 29

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| IC-01 | Three-mode state machine | 1 | M | — |
| IC-02 | Mode transition animations | 1 | S | IC-01, DS-02 |
| IC-03 | Annotate mode toolbar (5 cat + text) | 1 | M | IC-01, DS-01 |
| IC-04 | Tool tray (horizontal scroll) | 1 | S | IC-03 |
| IC-05 | Tool persistence + 3-tier carry | 1 | S | IC-03, PD-10 |
| IC-06 | Semantic ink color strip | 1 | S | IC-01, DS-01 |
| IC-07 | Undo/redo engine (3 stacks) | 1 | M | PD-07, IC-01 |
| IC-08 | Undo pill UI | 1 | S | IC-07, DS-01 |
| IC-09 | Chrome auto-hide controller | 1 | S | IC-01 |
| IC-10 | Edit mode toolbar (6 slots) | 1 | M | IC-01, DS-01 |
| IC-11 | Browse mode chrome | 1 | S | IC-01, DS-01 |
| IC-12 | Long-press → properties sheet | 1 | S | IC-03 |
| IC-13 | Soft placement (1.5s window) | 1 | S | CE-06 |
| IC-14 | Smart Paper shape recognition (includes AI-01 scope) | 1 | L | DP-30, DP-18, DP-23, DP-03 |
| IC-15 | Context ring (radial menu) | 2 | M | CE-04 |
| IC-16 | Studio mode activation | 2 | M | IC-01, IC-03 |
| IC-17 | Studio mode properties panel | 2 | M | IC-16 |
| IC-18 | Studio mode layer management | 2 | M | IC-16, PD-11 |
| IC-19 | Intent-reading transitions (IMP-18) | 2 | M | IC-01 |
| IC-20 | Draw = Enter Annotate (IMP-01) | 1 | S | IC-01, IC-14 |
| IC-21 | Color as gesture (IMP-02) | 1 | S | IC-06, CE-04 |
| IC-22 | Back-tap undo (IMP-13) | 1 | S | IC-07 |
| IC-23 | Haptic vocabulary system | 1 | S | DS-01 |
| IC-24 | Spring physics animation | 1 | S | DS-02 |
| IC-25 | Annotation summary ribbon (IMP-20) | 2 | S | IC-01, PD-04 |
| IC-26 | Quick-label dictionary (IMP-05) | 1 | M | DP-25, PD-01 |
| IC-27 | Swipe-between-photos in Annotate (IMP-06) | 1 | M | IC-01, IC-05, CE-04 |
| IC-28 | Frequency-sorted ink strip (IMP-07) | 1 | S | IC-06, PD-10 |
| IC-29 | Text without keyboard blackout (IMP-08) | 1 | M | DP-25, CE-02 |
| IC-30 | Photo-aware label placement (IMP-09) | 1 | S | CE-09 |
| IC-31 | Ghost overlay capture (IMP-11) | 2 | M | CC-01, CE-06 |
| IC-32 | Safety bar / completeness check (IMP-15) | 1 | S | PD-04, EX-01 |
| IC-33 | Micro-share moment (IMP-17) | 1 | S | EX-05, ON-10 |

## Area 06 — Edit Pipeline (EP) → Doc 29

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| EP-01 | Image flip & rotate | 1 | S | IC-10 |
| EP-02 | Image crop tool | 1 | M | IC-10, CE-01 |
| EP-03 | AGSL RuntimeShader adjustments | 1 | M | IC-10 |
| EP-04 | Vignette shader | 2 | S | EP-03 |
| EP-05 | Film grain shader | 2 | S | EP-03 |
| EP-06 | Color LUT presets | 2 | M | EP-03 |
| EP-07 | Overlay browser + blend modes | 2 | M | IC-10 |

## Area 07 — Collage System (CL) → Doc 29

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| CL-01 | Collage layout engine (5 base) | 1 | L | CE-01, PD-03 |
| CL-02 | Before/After layout | 1 | S | CL-01 |
| CL-03 | Forensic Three-Shot | 1 | S | CL-01 |
| CL-04 | Quad View | 1 | S | CL-01 |
| CL-05 | Process Sequence | 1 | S | CL-01 |
| CL-06 | Hero + Details | 1 | S | CL-01 |
| CL-07 | Per-cell zoom/pan/swap | 1 | M | CL-01, CE-05 |
| CL-08 | Metadata bar per cell | 1 | S | CL-01, PD-09 |
| CL-09 | Smart layout suggestion (IMP-12) | 2 | M | CL-01, PD-03 |
| CL-10 | Domain-specific templates | 2 | S | CL-01, TK-01 |
| CL-11 | Freeform collage layout | 2 | M | CL-01 |
| CL-12 | Video-to-GIF collage cells | 3 | L | CL-01, CC-06 |
| CL-13 | Synchronized zoom/pan (linked) | 2 | S | CL-07 |
| CL-14 | Semantic borders per cell | 1 | S | CL-01, IC-06 |
| CL-15 | Border drag resizing | 1 | M | CL-01 |
| CL-16 | Content-aware auto-crop | 2 | S | CL-07, AI-15 |
| CL-17 | Auto-match exposure | 2 | M | CL-01, EP-03 |

## Area 08 — Onboarding & Navigation (ON) → Doc 29

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| ON-01 | App splash + cold launch | 1 | S | — |
| ON-02 | First-launch onboarding (3-step) | 1 | M | ON-01, TK-01 |
| ON-03 | "What kind of work?" question UI | 1 | S | ON-02 |
| ON-04 | Project hub (home screen) | 1 | M | PD-02, DS-01 |
| ON-05 | Project creation flow | 1 | S | ON-04, PD-02 |
| ON-06 | Search (projects, photos, annotations) | 2 | M | PD-13 |
| ON-07 | Settings screen | 1 | M | PD-10, DS-01 |
| ON-08 | Warm launch state restoration | 1 | S | ON-04, IC-01 |
| ON-09 | Situation-first onboarding (IMP-04) | 2 | M | ON-02 |
| ON-10 | Inspection completion ritual (IMP-19) | 1 | M | IC-01, EX-01 |
| ON-11 | Deep linking (search → annotation) | 2 | S | ON-06, PD-04 |

## Area 09 — Design System (DS) → Doc 29

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| DS-01 | Design tokens | 1 | M | — |
| DS-02 | Animation system (spring physics) | 1 | S | DS-01 |
| DS-03 | Dark-first theme | 1 | S | DS-01 |
| DS-04 | Touch target sizing (56–64dp) | 1 | S | DS-01 |
| DS-05 | Icon set | 1 | M | DS-01 |
| DS-06 | Typography (Plus Jakarta + IBM Plex) | 1 | S | DS-01 |

## Area 10 — Export & Evidence (EX) → Doc 30

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| EX-01 | Burned-in JPEG export | 1 | M | CE-11, PD-04 |
| EX-02 | Sidecar JSON (Annotation Envelope) | 1 | M | PD-04, PD-05 |
| EX-03 | Metadata bar burn-in | 1 | S | EX-01, PD-09 |
| EX-04 | PDF report generation | 1 | L | EX-01, PD-02 |
| EX-05 | Android Share Sheet | 1 | S | EX-01, EX-02 |
| EX-06 | C2PA content credentials | 2 | L | EX-01, PD-08 |
| EX-07 | SHA-256 audit log export | 2 | S | PD-08 |
| EX-08 | Video annotation export | 3 | L | CC-06, CE-06 |
| EX-09 | Batch export (multi-photo) | 2 | M | EX-01 |
| EX-10 | Collage export | 1 | M | CL-01, EX-01 |
| EX-11 | Version timeline per photo | 2 | M | PD-04, PD-14 |

## Area 11 — Trade Kit System (TK) → Doc 30

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| TK-01 | Trade kit data model + registry | 1 | M | PD-01, PD-10 |
| TK-02 | Kit activation engine | 1 | M | TK-01, IC-03 |
| TK-03 | 5 launch kits | 1 | L | TK-01, TK-02 |
| TK-04 | Kit ↔ tool mapping resolver | 1 | S | TK-01, PD-05 |
| TK-05 | .tnkit file format | 2 | M | TK-01 |
| TK-06 | Kit catalog browse UI | 1 | M | TK-01, DS-01 |
| TK-07 | Kit customization (drag into slots) | 2 | S | TK-02, IC-03 |
| TK-08 | Contextual kit suggestion | 2 | S | TK-01 |
| TK-09 | Community kits | 3 | M | TK-05 |
| TK-10 | Kit-specific collage templates | 2 | S | TK-01, CL-10 |
| TK-11 | Kit-specific semantic colors | 1 | S | TK-01, IC-06 |

## Area 12 — Localization (L10N) → Doc 30

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| L10N-01 | String resource infra (en + 5 EU) | 1 | M | — |
| L10N-02 | Locale switching | 1 | S | L10N-01, ON-07 |
| L10N-03 | donottranslate.xml + xliff:g | 1 | S | L10N-01 |
| L10N-04 | ISO 7010 safety symbol picker | 1 | M | DS-01, L10N-01 |
| L10N-05 | Plurals + RTL testing | 1 | S | L10N-01 |
| L10N-06 | Full 13-language strings | 2 | L | L10N-01 |
| L10N-07 | Terminology database (concept-ID) | 2 | M | PD-01, L10N-01 |
| L10N-08 | IATE TBX import pipeline | 2 | M | L10N-07 |
| L10N-09 | Structured annotation i18n (ISO 14224) | 2 | M | L10N-07, PD-04 |
| L10N-10 | ML Kit offline translation | 2 | M | L10N-07 |
| L10N-11 | Pre-translated phrase cache | 2 | S | L10N-07 |
| L10N-12 | Terminology glossary overlay | 2 | S | L10N-07, L10N-10 |
| L10N-13 | DeepL API cloud translation | 3 | M | L10N-10 |
| L10N-14 | Date/time/number formatting | 1 | S | L10N-01 |
| L10N-15 | Chinese market variant | 3 | L | L10N-06 |
| L10N-16 | Dual-standard reference fields | 2 | S | L10N-07 |
| L10N-17 | locales_config.xml auto-gen | 1 | S | L10N-01 |
| L10N-18 | Disable AAB language splits | 1 | S | L10N-01 |

## Area 13 — Integration Layer (IL) → Doc 31

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| IL-01 | Share Sheet export (Level 1) | 1 | S | EX-01, EX-05 |
| IL-02 | Annotation Envelope format spec | 1 | M | EX-02, PD-05 |
| IL-03 | Offline sync queue (outbox) | 2 | M | PD-01 |
| IL-04 | Zapier webhook adapter (Level 2) | 2 | M | IL-02, IL-03 |
| IL-05 | CMMS adapter interface | 3 | M | IL-02 |
| IL-06 | Procore adapter (Level 3) | 3 | L | IL-05 |
| IL-07 | MaintainX adapter | 3 | M | IL-05 |
| IL-08 | UpKeep adapter | 3 | M | IL-05 |
| IL-09 | ServiceTitan adapter | 3 | M | IL-05 |
| IL-10 | Context Whisper (CMMS → TN) | 3 | M | IL-05 |
| IL-11 | Priority Tint | 3 | S | IL-10 |
| IL-12 | Return Signal | 3 | M | IL-05 |
| IL-13 | Maximo adapter | 4 | M | IL-05 |
| IL-14 | SAP PM adapter | 4 | M | IL-05 |
| IL-15 | .tnote open schema | 4 | M | IL-02 |
| IL-16 | Multi-CMMS routing | 4 | L | IL-05 |
| IL-17 | Rehydration protocol | 4 | M | IL-02 |

## Area 14 — AI & Intelligence (AI) → Doc 31

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| ~~AI-01~~ | ~~Shape recognition (Smart Paper)~~ | — | — | *Merged into IC-14 (v3.0) — same implementation* |
| AI-02 | OCR for equipment nameplates | 2 | M | CC-01 |
| AI-03 | Gemini Nano on-device setup | 3 | M | — |
| AI-04 | AI report generation | 3 | L | AI-03, EX-04 |
| AI-05 | Dream inspection (overnight) | 3 | L | AI-03, PD-04 |
| AI-06 | Documentation penumbra | 3 | M | AI-03, PD-04 |
| AI-07 | Video anomaly detection | 3 | M | AI-03, CC-06 |
| AI-08 | Sketch-to-structured-data | 3 | M | IC-14, PD-04 |
| AI-09 | Inspection workflow recognition | 4 | L | AI-03, PD-04 |
| AI-10 | QR/barcode → equipment context | 2 | M | CC-01, PD-12 |
| AI-11 | Handwriting recognition | 3 | M | AI-03 |
| AI-12 | Anomaly memory (PatchCore) | 3 | M | AI-03, PD-04 |
| AI-13 | Visual change detection | 3 | M | AI-03, PD-03 |
| AI-14 | Difference highlighting | 3 | M | CL-02, AI-03 |
| AI-15 | Content-aware auto-crop | 2 | S | CC-01 |

## Area 15 — Collaboration (CO) → Doc 31

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| CO-01 | Async shared annotation | 2 | L | EX-01, PD-04 |
| CO-02 | Comment threads | 2 | M | PD-04 |
| CO-03 | Multi-user layers | 2 | M | PD-11, IC-18 |
| CO-04 | CRDT real-time sync | 3 | L | PD-04 |
| CO-05 | Team admin dashboard | 3 | L | CO-01 |
| CO-06 | User accounts + auth | 2 | M | ON-07 |

## Area 16 — Voice & Alternative Input (VO) → Doc 31

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| VO-01 | Voicemail annotations | 2 | M | CE-06, PD-04 |
| VO-02 | On-device speech transcription | 2 | M | VO-01 |
| VO-03 | Linked evidence (audio ↔ transcript) | 2 | S | VO-01, VO-02 |
| VO-04 | Sound as diagnostic data | 2 | S | VO-01 |
| VO-05 | Speech-to-structured-annotation | 3 | M | VO-02, AI-03 |
| VO-06 | Engineering autocomplete | 1 | M | PD-01, DP-25 |
| VO-07 | Voice-directed spatial annotation | 3 | L | AI-03, VO-02 |
| VO-08 | Bone conduction audio (Shokz) | 3 | S | VO-02 |

## Area 17 — Video & Multi-Media (VD) → Doc 31

| ID | Name | Ph | Sz | Depends |
|----|------|----|----|---------|
| VD-01 | Video notepad (filmstrip + frame) | 2 | L | CC-06, CE-06 |
| VD-02 | Annotation bursts | 2 | S | VD-01 |
| VD-03 | Before/after video comparison | 2 | M | VD-01 |
| VD-04 | Multi-video parallel playback | 2 | L | VD-01, CL-01 |
| VD-05 | Event-based sync points | 2 | S | VD-04 |
| VD-06 | Tap-to-focus audio | 2 | S | VD-04 |
| VD-07 | Cinemagraph | 3 | L | VD-01 |
| VD-08 | Annotation via object tracking | 3 | L | VD-01, AI-03 |
| VD-09 | Progressive reveal | 2 | S | CL-01 |
| VD-10 | FLIR thermal video | 3 | L | VD-01, CC-06 |

---

## Summary

| Area | Atoms | Ph1 | Ph2 | Ph3 | Ph4 |
|------|-------|-----|-----|-----|-----|
| Project & Data | 14 | 11 | 1 | 0 | 2 |
| Canvas Engine | 11 | 11 | 0 | 0 | 0 |
| Camera & Capture | 6 | 4 | 2 | 0 | 0 |
| Drawing Primitives | 65 | 25 | 35 | 0 | 5 |
| Interaction & Chrome | 33 | 26 | 7 | 0 | 0 |
| Edit Pipeline | 7 | 3 | 4 | 0 | 0 |
| Collage System | 17 | 10 | 6 | 1 | 0 |
| Onboarding & Nav | 11 | 8 | 3 | 0 | 0 |
| Design System | 6 | 6 | 0 | 0 | 0 |
| Export & Evidence | 11 | 6 | 4 | 1 | 0 |
| Trade Kit System | 11 | 6 | 4 | 1 | 0 |
| Localization | 18 | 8 | 8 | 2 | 0 |
| Integration Layer | 17 | 2 | 2 | 8 | 5 |
| AI & Intelligence | 14 | 0 | 3 | 10 | 1 |
| Collaboration | 6 | 0 | 4 | 2 | 0 |
| Voice & Alt Input | 8 | 1 | 4 | 3 | 0 |
| Video & Multi-Media | 10 | 0 | 7 | 3 | 0 |
| **TOTAL** | **265** | **127** | **94** | **31** | **13** |

---

*April 2026 · TechNotes Feature Atom Registry v3.0 · 265 atoms · 17 areas*
*Changes from v2.0: AI-01 merged into IC-14 (duplicate — same geometric analysis implementation, "Cross-ref IC-14" in Doc 31 confirmed overlap). AI-08 dependency updated from AI-01 to IC-14. Per-phase counts corrected across DP (was 27/28, now 25/35), IC (was 24/9, now 26/7), ON (was 7, now 8), CL Ph2 (was 5, now 6), L10N Ph2 (was 7, now 8), AI (was 1/3, now 0/3). Column totals now sum correctly to 265.*
