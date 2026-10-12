# 32 — Dependency Graph & Build Order
## Adjacency List + Tiered Build Sequence for All 265 Atoms

> **Purpose:** Defines what must be built before what. Atoms are grouped into build tiers — a tier can only start when all its dependencies from prior tiers are complete. Within a tier, atoms are independent and can be built in parallel (independent Claude Code sessions).

---

## How Build Tiers Work

- **Tier 0:** No dependencies — can start immediately
- **Tier 1:** Depends only on Tier 0 atoms
- **Tier N:** Depends on atoms from Tiers 0 through N-1
- Within any tier, atoms are independent and can be built in parallel
- Tiers are Phase-filtered: Phase 1 tiers first, then Phase 2 extends the graph

---

## Phase 1 Build Tiers (127 atoms → shippable v1)

### Tier 0 — Zero Dependencies (start here)
```
DS-01    Design tokens
PD-01    Room database setup
PD-05    Annotation element sealed class hierarchy
PD-06    Geometry primitives + coordinate transforms
IC-01    Three-mode state machine
CC-01    CameraX viewfinder + capture
ON-01    App splash + cold launch
L10N-01  String resource infrastructure
```
**8 atoms · 5 parallel tracks: Design System, Data Layer, Data Models, Camera, Localization**

> **Note on PD-01:** Room database setup has no hard code dependency on any other atom. However, in practice, having PD-05 and PD-06 designed first makes writing Room type converters easier. Within Tier 0, start PD-05/PD-06 before or alongside PD-01.

---

### Tier 1 — Depends on Tier 0
```
DS-02    Animation system                        ← DS-01
DS-03    Dark-first theme                        ← DS-01
DS-04    Touch target sizing                     ← DS-01
DS-05    Icon set                                ← DS-01
DS-06    Typography setup                        ← DS-01
PD-02    Project data model                      ← PD-01
PD-07    Undo/redo command model                 ← PD-05
PD-10    Settings/preferences                    ← PD-01
CE-01    EditableTouchImageView                  ← PD-06
CC-02    Camera pre-warming                      ← CC-01
IC-03    Annotate mode toolbar                   ← IC-01, DS-01
IC-06    Semantic ink color strip                 ← IC-01, DS-01
IC-09    Chrome auto-hide                        ← IC-01
IC-10    Edit mode toolbar                       ← IC-01, DS-01
IC-11    Browse mode chrome                      ← IC-01, DS-01
IC-23    Haptic vocabulary                       ← DS-01
L10N-03  donottranslate.xml                      ← L10N-01
L10N-04  ISO 7010 safety symbol picker           ← DS-01, L10N-01
L10N-05  Plurals + RTL testing                   ← L10N-01
L10N-14  Date/time/number formatting             ← L10N-01
L10N-17  locales_config.xml auto-gen             ← L10N-01
L10N-18  Disable AAB language splits             ← L10N-01
```
**22 atoms**

---

### Tier 2 — Depends on Tier 0–1
```
CE-02    viewMatrix system                       ← CE-01
CE-04    StatelessGestureRouter                  ← CE-01
PD-03    Photo data model                        ← PD-01, PD-02
IC-02    Mode transition animations              ← IC-01, DS-02
IC-04    Tool tray                               ← IC-03
IC-05    Tool persistence                        ← IC-03, PD-10
IC-07    Undo/redo engine                        ← PD-07, IC-01
IC-12    Long-press → properties                 ← IC-03
IC-24    Spring physics animation                ← DS-02
IC-28    Frequency-sorted ink strip              ← IC-06, PD-10
ON-04    Project hub                             ← PD-02, DS-01
ON-07    Settings screen                         ← PD-10, DS-01
TK-01    Trade kit data model                    ← PD-01, PD-10
EP-01    Flip & rotate                           ← IC-10
EP-02    Crop tool                               ← IC-10, CE-01
EP-03    AGSL RuntimeShader                      ← IC-10
```
**16 atoms**

---

### Tier 3 — Depends on Tier 0–2
```
CE-03    Touch-to-image coordinate pipeline      ← CE-01, CE-02, PD-06
CE-05    Two-finger zoom/pan                     ← CE-04
CE-06    Annotation rendering pipeline           ← CE-01, CE-02, PD-05
PD-04    Annotation data model                   ← PD-01, PD-03, PD-05
PD-08    Evidence chain                          ← PD-03
PD-09    Auto-metadata capture                   ← PD-03
CL-01    Collage layout engine                   ← CE-01, PD-03
IC-08    Undo pill UI                            ← IC-07, DS-01
IC-21    Color as gesture                        ← IC-06, CE-04
IC-22    Back-tap undo                           ← IC-07
IC-27    Swipe-between-photos                    ← IC-01, IC-05, CE-04
CC-04    Gallery photo picker                    ← PD-03
ON-02    First-launch onboarding                 ← ON-01, TK-01
ON-05    Project creation                        ← ON-04, PD-02
ON-08    Warm launch restoration                 ← ON-04, IC-01
L10N-02  Locale switching                        ← L10N-01, ON-07
TK-02    Kit activation engine                   ← TK-01, IC-03
TK-04    Kit ↔ tool resolver                     ← TK-01, PD-05
TK-06    Kit catalog browse UI                   ← TK-01, DS-01
TK-11    Kit semantic colors                     ← TK-01, IC-06
```
**20 atoms**

---

### Tier 4 — Depends on Tier 0–3
```
CE-07    Hit-testing engine                      ← PD-06, CE-03
CE-09    Smart label placement                   ← CE-06, PD-06
CE-10    Alignment guides                        ← CE-06, PD-06
CE-11    Canvas export                           ← CE-06
DP-01    Straight line element                   ← CE-06, PD-05
DP-18    Circle (empty)                          ← CE-06, PD-05
DP-23    Rectangle (empty)                       ← CE-06, PD-05
DP-25    Text annotation                         ← CE-06, PD-05
DP-27    Diamond                                 ← CE-06, PD-05
DP-28    Triangle                                ← CE-06, PD-05
DP-30    Freehand curve                          ← CE-06, PD-05
DP-31    Hatching fill                           ← CE-06, PD-05
DP-50    Pass/Fail/Caution stamps                ← CE-06, PD-05
DP-52    Numbered pins                           ← CE-06, PD-05
DP-56    Auto-metadata overlay                   ← CE-06, PD-09
DP-63    Polyline                                ← CE-06, PD-05, CE-03
IC-13    Soft placement                          ← CE-06
EX-02    Sidecar JSON export                     ← PD-04, PD-05
PD-14    Per-annotation provenance               ← PD-04
TK-03    Five launch kits                        ← TK-01, TK-02
CC-03    Photo metadata injection                ← CC-01, PD-09
ON-03    Work question UI                        ← ON-02
CL-02    Before/After layout                     ← CL-01
CL-03    Forensic Three-Shot                     ← CL-01
CL-04    Quad View                               ← CL-01
CL-05    Process Sequence                        ← CL-01
CL-06    Hero + Details                          ← CL-01
CL-07    Per-cell zoom/pan/swap                  ← CL-01, CE-05
CL-08    Metadata bar per cell                   ← CL-01, PD-09
CL-14    Semantic borders per cell               ← CL-01, IC-06
CL-15    Border drag resizing                    ← CL-01
```
**31 atoms**

---

### Tier 5 — Depends on Tier 0–4
```
CE-08    Selection & manipulation handles        ← CE-07
DP-02    Arrowhead renderer                      ← DP-01
DP-19    Circle + text                           ← DP-18, DP-25
DP-24    Labeled rectangle                       ← DP-23, DP-25
DP-32    Revision cloud                          ← DP-30
DP-51    Defect markers                          ← DP-50
DP-62    Curve snap-to-close                     ← DP-30, CE-03, IC-23
DP-64    Polygon                                 ← DP-63
EX-01    Burned-in JPEG export                   ← CE-11, PD-04
IC-26    Quick-label dictionary                  ← DP-25, PD-01
IC-29    Text without keyboard blackout          ← DP-25, CE-02
IC-30    Photo-aware label placement             ← CE-09
IL-02    Annotation Envelope format              ← EX-02, PD-05
VO-06    Engineering autocomplete                ← PD-01, DP-25
```
**14 atoms**

---

### Tier 6 — Depends on Tier 0–5
```
DP-03    Arrow (single-head)                     ← DP-01, DP-02
DP-04    Arrow (double-head)                     ← DP-01, DP-02
DP-05    Leader + text                           ← DP-01, DP-02, DP-25
DP-13    Dimension line                          ← DP-01, DP-02
EX-03    Metadata bar burn-in                    ← EX-01, PD-09
EX-04    PDF report generation                   ← EX-01, PD-02
EX-05    Android Share Sheet                     ← EX-01, EX-02
EX-10    Collage export                          ← CL-01, EX-01
IC-32    Safety bar                              ← PD-04, EX-01
ON-10    Inspection completion ritual            ← IC-01, EX-01
```
**10 atoms**

---

### Tier 7 — Depends on Tier 0–6
```
DP-14    Angular dimension                       ← DP-13
DP-53    Flow arrows                             ← DP-03
IC-14    Smart Paper shape recognition           ← DP-30, DP-18, DP-23, DP-03
IC-33    Micro-share moment                      ← EX-05, ON-10
IL-01    Share Sheet export (Level 1)            ← EX-01, EX-05
```
**5 atoms**

---

### Tier 8 — Final Phase 1 (depends on Tier 0–7)
```
IC-20    Draw = Enter Annotate                   ← IC-01, IC-14
```
**1 atom — Phase 1 complete**

---

## Phase 1 Summary: 9 Tiers · 127 atoms

| Tier | Atoms | Key Deliverables |
|------|-------|-----------------|
| 0 | 8 | Design tokens, Room DB, element types, geometry, state machine, camera, splash, strings |
| 1 | 22 | Theme, animation, typography, icons, toolbars, project model, prefs, undo model, base canvas, haptics, symbols |
| 2 | 16 | viewMatrix, gesture router, photo model, undo engine, tool tray, project hub, settings, trade kit model, edit tools |
| 3 | 20 | Coordinates, zoom/pan, rendering pipeline, annotation data, evidence, collage engine, onboarding, kits |
| 4 | 31 | Drawing primitives, hit-testing, label placement, canvas export, collage layouts, sidecar JSON, provenance |
| 5 | 14 | Arrowheads, selection handles, burned-in export, text shapes, snap-to-close, autocomplete |
| 6 | 10 | Arrows, dimensions, Share Sheet, PDF, metadata bar, safety bar, completion ritual |
| 7 | 5 | Angular dimension, flow arrows, Smart Paper, micro-share, Share Sheet L1 |
| 8 | 1 | Draw = Enter Annotate |

---

## Phase 2 Build Tiers (90 atoms — extends Phase 1 graph)

### Tier P2-0 — Phase 2 atoms with only Phase 1 dependencies
```
PD-11    Annotation layer model                  ← PD-04
CC-05    Split-second buffer                     ← CC-01
CC-06    Video capture + frame extraction         ← CC-01
DP-06    Break line zigzag                       ← DP-01
DP-07    Break line wave                         ← DP-01
DP-08    Stitch line                             ← DP-01
DP-09    Zigzag inline                           ← DP-01
DP-10    Coil/spring                             ← DP-01
DP-11    Wave/sine                               ← DP-01
DP-12    Grainline arrow                         ← DP-01
DP-15    Radius/diameter dimension               ← DP-13
DP-20    Circle + divider                        ← DP-18
DP-21    Circle + crosshairs                     ← DP-18
DP-22    Concentric circle                       ← DP-18
DP-26    Rectangle + patterns                    ← DP-23
DP-29    Split circle                            ← DP-18
DP-33    Inline symbol base                      ← DP-01, CE-07
DP-40    Symbol placement base                   ← CE-06, CE-07
EX-06    C2PA content credentials                ← EX-01, PD-08
EX-07    SHA-256 audit log                       ← PD-08
EX-09    Batch export                            ← EX-01
EX-11    Version timeline                        ← PD-04, PD-14
IC-15    Context ring                            ← CE-04
IC-19    Intent-reading transitions              ← IC-01
IC-25    Annotation summary ribbon               ← IC-01, PD-04
IC-31    Ghost overlay capture                   ← CC-01, CE-06
IL-03    Offline sync queue                      ← PD-01
TK-05    .tnkit file format                      ← TK-01
TK-07    Kit customization                       ← TK-02, IC-03
TK-08    Contextual kit suggestion               ← TK-01
CL-09    Smart layout suggestion                 ← CL-01, PD-03
CL-10    Domain templates                        ← CL-01, TK-01
CL-11    Freeform layout                         ← CL-01
CL-13    Synchronized zoom/pan                   ← CL-07
ON-09    Situation-first onboarding              ← ON-02
AI-02    OCR nameplates                          ← CC-01
AI-15    Content-aware auto-crop                 ← CC-01
AI-10    QR/barcode scan                         ← CC-01, PD-12
CO-02    Comment threads                         ← PD-04
CO-06    User accounts                           ← ON-07
L10N-06  Full 13-language strings                ← L10N-01
L10N-07  Terminology database                    ← PD-01, L10N-01
EP-04    Vignette                                ← EP-03
EP-05    Film grain                              ← EP-03
EP-06    Color LUT presets                       ← EP-03
EP-07    Overlay browser                         ← IC-10
VO-01    Voicemail annotations                   ← CE-06, PD-04
DP-17    Calibration session                     ← DP-13
```
**47 atoms**

### Tier P2-1 — Depends on P2-0
```
DP-34–39  Inline symbols (6 atoms)              ← DP-33
DP-41–42  Op-amp, Logic gate                    ← DP-40
DP-43    GD&T wizard                             ← DP-40
DP-44    Welding symbol wizard                   ← DP-40
DP-45–48  Tolerance, Surface, Hardware, Notch   ← DP-40
DP-49    EU material grid wizard                 ← DP-40
DP-54    Safety/hazard symbols                   ← DP-40, L10N-04
DP-55    NDT designations                        ← DP-40
DP-16    Area measurement                        ← DP-64, PD-06
DP-65    Cross-photo annotations                 ← CL-01, CE-06
IL-04    Zapier webhook                          ← IL-02, IL-03
IC-16    Studio mode activation                  ← IC-01, IC-03
CO-01    Async shared annotation                 ← EX-01, PD-04
CO-03    Multi-user layers                       ← PD-11, IC-18 (IC-18 in P2-2)
VO-02    Speech transcription                    ← VO-01
L10N-08  IATE TBX import                         ← L10N-07
L10N-09  Structured annotation i18n              ← L10N-07, PD-04
L10N-10  ML Kit translation                      ← L10N-07
L10N-11  Phrase cache                            ← L10N-07
L10N-16  Dual-standard fields                    ← L10N-07
TK-10    Kit collage templates                   ← TK-01, CL-10
CL-16    Auto-crop                               ← CL-07, AI-15
CL-17    Auto-match exposure                     ← CL-01, EP-03
ON-06    Search                                  ← PD-13 (PD-13 is Phase 4 — see Cross-Phase Conflicts)
ON-11    Deep linking                            ← ON-06, PD-04
VD-01    Video notepad                           ← CC-06, CE-06
VD-09    Progressive reveal                      ← CL-01
```
**~37 atoms** (some grouped)

### Tier P2-2 — Depends on P2-0–1
```
IC-17    Studio properties panel                 ← IC-16
IC-18    Studio layer management                 ← IC-16, PD-11
VO-03    Linked evidence                         ← VO-01, VO-02
VO-04    Sound as diagnostic data                ← VO-01
VD-02    Annotation bursts                       ← VD-01
VD-03    Before/after video comparison           ← VD-01
VD-04    Multi-video parallel playback           ← VD-01, CL-01
L10N-12  Glossary overlay                        ← L10N-07, L10N-10
```
**8 atoms**

### Tier P2-3
```
VD-05    Event-based sync points                 ← VD-04
VD-06    Tap-to-focus audio                      ← VD-04
```
**2 atoms — Phase 2 complete**

---

## Phase 3 & 4 — Summary Only

Phase 3 (30 atoms) and Phase 4 (13 atoms) follow the same tier pattern. Key dependencies:

**Phase 3 roots:** AI-03 (Gemini Nano) enables AI-04 through AI-14. IL-05 (CMMS interface) enables IL-06 through IL-12. CO-04 (CRDT) enables real-time. VD-07/VD-08 extend VD-01.

**Phase 4:** PD-12 (assets), PD-13 (search), IL-13/14 (enterprise CMMS), IL-15–17 (open schema, routing, rehydration), AI-09, DP-57–61 (deferred elements).

---

## Cross-Phase Dependency Conflicts

| Atom | Declared Phase | Depends On | Conflict | Resolution |
|------|---------------|------------|----------|------------|
| ON-06 (Search) | 2 | PD-13 (Search Index) | PD-13 is Phase 4 | Phase 2: build ON-06 with Room FTS / simple LIKE queries. Phase 4: replace with PD-13 full search index. |
| AI-10 (QR scan) | 2 | PD-12 (Asset linkage) | PD-12 is Phase 4 | QR scan works without asset DB (extract text only). Asset linkage wired in Phase 4. |

---

*April 2026 · TechNotes Dependency Graph v2.0 · 9 Phase 1 tiers · 4 Phase 2 tiers*
*Changes from v1.1: Full tier recalculation — CE-06 moved from Tier 2 to Tier 3 (depends on CE-02 which is Tier 2). IC-03/IC-06/IC-09/IC-10/IC-11 promoted to Tier 1 (depend only on Tier 0). All downstream tiers recalculated algorithmically. Phase 1 expands from 8 tiers to 9 tiers (Tier 8: IC-20). AI-01 merged into IC-14 (duplicate — see Doc 26 v3.0). Total atoms 265 (was 266). ON-06 cross-phase conflict resolution specified.*
