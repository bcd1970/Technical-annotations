# 33 — Session Blueprint Sequence
## Claude Code Work Orders — Phase 1

> **Purpose:** Each session is one Claude Code task. Atoms are grouped by shared dependencies and the 5-file rule. Sessions follow the dependency graph (Doc 32 v1.1) — no session starts until its prerequisite sessions are complete. Each session is self-contained with all context inline.

---

## Rules

1. **One session = one focused task.** Max 5 files created/modified.
2. **Read STATUS.md first, update it last.** Every session.
3. **Run `./gradlew assembleDebug` before declaring done.**
4. **Commit at clean checkpoints** with descriptive messages.
5. **No parallel implementations.** Delete old before writing new.

---

## Session Map — Phase 1 (Tiers 0–7)

### S-01 · Design System Foundation
**Tier:** 0–1 · **Atoms:** DS-01, DS-03, DS-06 · **Size:** M
**Module:** `:core:ui`

**Task:** Create the design system module with all tokens, dark theme, and typography.

**Files:**
- `core/ui/src/main/java/.../theme/TechNotesTheme.kt` — Compose theme wrapper
- `core/ui/src/main/java/.../theme/Color.kt` — color palette (monochrome + semantic)
- `core/ui/src/main/java/.../theme/Type.kt` — typography (Plus Jakarta Sans + IBM Plex Mono)
- `core/ui/src/main/res/font/` — font files

**Acceptance:** App compiles with theme applied. Dark theme renders. Typography correct.

---

### S-02 · Design System Polish
**Tier:** 1–2 · **Atoms:** DS-02, DS-04, DS-05, IC-23, IC-24 · **Size:** M
**Module:** `:core:ui`

> **Note:** IC-23 and IC-24 are IC-area atoms that live in `:core:ui` (not `:feature-canvas`) because they are shared design system infrastructure used by all modules.

**Task:** Animation system, touch targets, icons, haptic vocabulary, spring physics.

**Files:**
- `core/ui/src/main/java/.../theme/Motion.kt` — spring specs, duration constants
- `core/ui/src/main/java/.../theme/Dimensions.kt` — touch targets, spacing
- `core/ui/src/main/java/.../haptics/HapticFeedback.kt` — CONFIRM/TICK/REJECT/SUCCESS
- `core/ui/src/main/res/drawable/` — icon SVGs

**Acceptance:** Haptic patterns fire correctly on S25 Ultra. Icons render crisp.

---

### S-03 · Data Models — Pure Kotlin
**Tier:** 0–1 · **Atoms:** PD-05, PD-06, PD-07 · **Size:** M
**Module:** `:core:model`

**Task:** Annotation element sealed class hierarchy, geometry primitives, undo command model. All pure Kotlin, zero Android deps.

**Files:**
- `core/model/src/main/java/.../AnnotationElement.kt` — sealed class with all Phase 1 subclasses
- `core/model/src/main/java/.../Geometry.kt` — ImagePoint, ScreenPoint, BoundingBox, LineSegment, transforms
- `core/model/src/main/java/.../UndoCommand.kt` — sealed class + UndoRedoManager
- `core/model/src/test/java/.../` — JUnit tests for geometry + undo

**Acceptance:** All elements serialize/deserialize round-trip. Geometry transforms correct. Undo stress test (100+ ops) passes.

---

### S-04 · Room Database + Project Model
**Tier:** 0–1 · **Atoms:** PD-01, PD-02, PD-10 · **Size:** M
**Module:** `:core:data`

**Task:** Database setup, project entity/DAO/repository, preferences DataStore.

**Files:**
- `core/data/src/main/java/.../db/TechNotesDatabase.kt`
- `core/data/src/main/java/.../db/TypeConverters.kt`
- `core/data/src/main/java/.../project/ProjectEntity.kt` + `ProjectDao.kt` + `ProjectRepository.kt`
- `core/data/src/main/java/.../prefs/UserPreferencesDataStore.kt`

> **Note:** PD-01 has no hard dependencies (Tier 0), but Room type converters benefit from PD-05/PD-06 designs. Start S-03 alongside or slightly before S-04.

**Acceptance:** DB creates. Project CRUD works. Preferences persist across restart.

---

### S-05 · Photo Model + Evidence Chain
**Tier:** 2–3 · **Atoms:** PD-03, PD-08, PD-09 · **Size:** M
**Module:** `:core:data`

**Task:** Photo entity with metadata, SHA-256 evidence chain, auto-metadata capture service.

**Files:**
- `core/data/src/main/java/.../photo/PhotoEntity.kt` + `PhotoDao.kt` + `PhotoRepository.kt`
- `core/data/src/main/java/.../evidence/EvidenceChainService.kt`
- `core/data/src/main/java/.../metadata/MetadataCaptureService.kt`

**Prerequisite:** S-04 complete.
**Acceptance:** Photo persists with cascade delete. SHA-256 <200ms for 12MP. GPS/compass captured.

---

### S-06 · Annotation Model + Provenance
**Tier:** 3–4 · **Atoms:** PD-04, PD-14 · **Size:** M
**Module:** `:core:data`

**Task:** Annotation entity with per-annotation provenance tracking.

**Files:**
- `core/data/src/main/java/.../annotation/AnnotationEntity.kt` + `AnnotationDao.kt` + `AnnotationRepository.kt`
- `core/data/src/main/java/.../annotation/ProvenanceRecord.kt`

**Prerequisite:** S-03, S-04, S-05 complete.
**Acceptance:** Annotations persist with geometry. Bulk insert <50ms for 100 items. Provenance tracked per create/modify.

---

### S-07 · Localization Foundation
**Tier:** 0–1 · **Atoms:** L10N-01, L10N-03, L10N-05, L10N-14, L10N-17, L10N-18 · **Size:** M
**Module:** All modules

**Task:** String resources for en + 5 EU locales, donottranslate, plurals, date formatting, AAB config.

**Files:**
- `core/ui/src/main/res/values/strings.xml` (en base)
- `core/ui/src/main/res/values-de/strings.xml`, `-fr/`, `-es/`, `-it/`, `-pt/`
- `core/ui/src/main/res/values/donottranslate.xml`
- `app/src/main/res/resources.properties`
- `app/build.gradle.kts` — AAB language split disabled

**Acceptance:** App renders in all 6 languages. No untranslated strings. Locale switch works.

---

### S-08 · Base Canvas + viewMatrix
**Tier:** 1–2 · **Atoms:** CE-01, CE-02 · **Size:** L
**Module:** `:feature-canvas`

**Task:** EditableTouchImageView with viewMatrix system. The foundation of all drawing.

**Files:**
- `feature-canvas/src/main/java/.../canvas/EditableTouchImageView.kt`
- `feature-canvas/src/main/java/.../canvas/ViewMatrixController.kt`
- `feature-canvas/src/main/java/.../canvas/CanvasViewModel.kt`

**Prerequisite:** S-03 (geometry).
**Acceptance:** Photo loads and displays. Zoom/pan works at 60fps on 12MP. Scale limits enforced. Boundary clamping with spring-back.

---

### S-09 · Gesture System
**Tier:** 2–3 · **Atoms:** CE-03, CE-04, CE-05 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Touch-to-image pipeline, gesture router, two-finger handler.

**Files:**
- `feature-canvas/src/main/java/.../input/CoordinatePipeline.kt`
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt`
- `feature-canvas/src/main/java/.../input/ZoomPanHandler.kt`

**Prerequisite:** S-08.
**Acceptance:** Coordinates accurate at all zoom levels. Two-finger always wins. Router <1ms.

---

### S-10 · Rendering Pipeline + Hit Testing + Selection
**Tier:** 3–5 · **Atoms:** CE-06, CE-07, CE-08 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Annotation rendering in onDraw, hit-testing, selection handles.

**Files:**
- `feature-canvas/src/main/java/.../render/AnnotationRenderer.kt` (interface)
- `feature-canvas/src/main/java/.../render/RendererRegistry.kt`
- `feature-canvas/src/main/java/.../input/HitTestEngine.kt`
- `feature-canvas/src/main/java/.../selection/SelectionManager.kt`

**Prerequisite:** S-08, S-09, S-03.
**Acceptance:** 100 annotations at 60fps. Hit-test <5ms. Selection handles visible at all zoom.

> **Note:** CE-06 is Tier 3, CE-07 is Tier 4, CE-08 is Tier 5. This session spans wide but all three are tightly coupled — rendering needs hit-testing which needs selection. Build sequentially within the session.

---

### S-11 · Mode State Machine + Chrome
**Tier:** 0–2 · **Atoms:** IC-01, IC-02, IC-09, IC-11 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Three-mode state machine, transitions, auto-hide, browse chrome.

**Files:**
- `feature-canvas/src/main/java/.../mode/ModeStateMachine.kt`
- `feature-canvas/src/main/java/.../mode/ChromeAutoHideController.kt`
- `feature-canvas/src/main/java/.../ui/BrowseChrome.kt`

**Prerequisite:** S-01.
**Acceptance:** Mode transitions <300ms. Chrome auto-hides during gesture. Mode survives process death.

---

### S-12 · Annotate Toolbar System
**Tier:** 1–2 · **Atoms:** IC-03, IC-04, IC-05, IC-06, IC-12, IC-28 · **Size:** L
**Module:** `:feature-canvas`

**Task:** 5-category toolbar, tool tray, persistence, semantic ink strip, properties sheet.

**Files:**
- `feature-canvas/src/main/java/.../ui/AnnotateToolbar.kt`
- `feature-canvas/src/main/java/.../ui/ToolTray.kt`
- `feature-canvas/src/main/java/.../ui/SemanticInkStrip.kt`
- `feature-canvas/src/main/java/.../ui/ToolPropertiesSheet.kt`
- `feature-canvas/src/main/java/.../tool/ToolPersistenceManager.kt`

**Prerequisite:** S-01, S-11.
**Acceptance:** 5 categories render. Tray scrolls. Tool persists across annotations. Color strip works. Long-press opens sheet.

---

### S-13 · Undo/Redo System
**Tier:** 2–3 · **Atoms:** IC-07, IC-08, IC-22 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Three mode-specific undo stacks, pill UI, back-tap undo.

**Files:**
- `feature-canvas/src/main/java/.../undo/UndoRedoController.kt` (wraps UndoRedoManager per mode)
- `feature-canvas/src/main/java/.../ui/UndoPill.kt`
- `feature-canvas/src/main/java/.../input/BackTapDetector.kt`

**Prerequisite:** S-03 (PD-07), S-11.
**Acceptance:** Undo/redo works per mode. Pill shows count. Two-finger double-tap triggers. Back-tap on Android 14+.

---

### S-14 · Basic Drawing Elements (Lines + Shapes)
**Tier:** 4 · **Atoms:** DP-01, DP-18, DP-23, DP-25, DP-27, DP-28 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Straight line, circle, rectangle, text, diamond, triangle — the core six.

**Files:**
- `feature-canvas/src/main/java/.../render/LineRenderer.kt`
- `feature-canvas/src/main/java/.../render/ShapeRenderer.kt` (circle, rect, diamond, triangle)
- `feature-canvas/src/main/java/.../render/TextRenderer.kt`
- `feature-canvas/src/main/java/.../gesture/TapPlaceHandler.kt`
- `feature-canvas/src/main/java/.../gesture/DragDrawHandler.kt`

**Prerequisite:** S-10.
**Acceptance:** All 6 elements draw, render, and persist correctly.

---

### S-15 · Arrowhead System + Arrows
**Tier:** 5–6 · **Atoms:** DP-02, DP-03, DP-04, DP-05 · **Size:** M
**Module:** `:feature-canvas`

**Task:** 20-style arrowhead renderer, single/double arrows, leader + text.

**Files:**
- `feature-canvas/src/main/java/.../render/ArrowheadRenderer.kt`
- `feature-canvas/src/main/java/.../render/LeaderRenderer.kt`
- `feature-canvas/src/main/java/.../model/ArrowheadStyle.kt`

**Prerequisite:** S-14 (DP-01, DP-25).
**Acceptance:** All 20 styles render. No line through filled heads (inset-point). Leader text keyboard appears on finger-up.

---

### S-16 · Freehand + Snap-to-Close + Polyline + Polygon
**Tier:** 4–5 · **Atoms:** DP-30, DP-62, DP-63, DP-64 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Freehand curves with Catmull-Rom/RDP, snap-to-close, multi-point polyline, polygon with fill.

**Files:**
- `feature-canvas/src/main/java/.../render/FreehandRenderer.kt`
- `feature-canvas/src/main/java/.../math/CatmullRomSmoother.kt`
- `feature-canvas/src/main/java/.../math/RDPReducer.kt`
- `feature-canvas/src/main/java/.../gesture/PolylineHandler.kt`
- `feature-canvas/src/main/java/.../render/PolygonRenderer.kt`

**Prerequisite:** S-10, S-02 (DP-62 requires IC-23 haptic vocabulary from S-02).
**Acceptance:** Smooth curves at 60fps. Snap-to-close with haptic. No visible seam. Double-tap completes polyline. Polygon fill correct.

---

### S-17 · Stamps + Pins + Hatching + Cloud + Text Shapes
**Tier:** 4–5 · **Atoms:** DP-50, DP-51, DP-52, DP-31, DP-32, DP-19, DP-24 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Condition stamps, defect markers, numbered pins, hatching fill, revision cloud, circle+text, labeled rectangle.

**Files:**
- `feature-canvas/src/main/java/.../render/StampRenderer.kt`
- `feature-canvas/src/main/java/.../render/NumberedPinRenderer.kt`
- `feature-canvas/src/main/java/.../render/HatchingRenderer.kt`
- `feature-canvas/src/main/java/.../render/RevisionCloudRenderer.kt`
- `feature-canvas/src/main/java/.../render/TextShapeRenderer.kt` (circle+text, labeled rect)

**Prerequisite:** S-14 (DP-18, DP-23, DP-25 for text shape variants), S-16 (DP-30 for revision cloud DP-32).
**Acceptance:** Stamps one-tap place. Pins auto-number (D-001, D-002). Renumber on delete with animation. Hatching clips to boundary. Cloud arcs smooth. Circle+text and labeled rect render correctly.

---

### S-18 · Dimensions (Linear + Angular)
**Tier:** 6–7 · **Atoms:** DP-13, DP-14 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Dimension line (two-tap), angular dimension (three-tap).

**Files:**
- `feature-canvas/src/main/java/.../render/DimensionRenderer.kt`
- `feature-canvas/src/main/java/.../gesture/MultiTapHandler.kt`
- `feature-canvas/src/main/java/.../render/AngularDimensionRenderer.kt`

**Prerequisite:** S-15 (arrowheads).
**Acceptance:** Text always readable. Extension lines correct at all angles. Angular handles acute/right/obtuse.

---

### S-19 · Smart Paper + Draw=Annotate + Color as Gesture
**Tier:** 3–8 · **Atoms:** IC-14, IC-20, IC-21 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Shape recognition engine, draw enters annotate mode, color as gesture.

**Files:**
- `feature-canvas/src/main/java/.../recognition/ShapeRecognizer.kt`
- `feature-canvas/src/main/java/.../recognition/CircularityAnalyzer.kt`
- `feature-canvas/src/main/java/.../recognition/CornerDetector.kt`
- `feature-canvas/src/main/java/.../mode/DrawToAnnotateTransition.kt`

**Prerequisite:** S-14, S-15, S-16 (all basic shapes must exist), S-12 (IC-06 for Color as Gesture), S-09 (CE-04 for Color as Gesture).

> **Note on prerequisites:** IC-14 depends on DP-30 (S-16), DP-18/DP-23 (S-14), DP-03 (S-15). IC-21 depends on IC-06 (S-12) and CE-04 (S-09). All must be complete before starting.

**Acceptance:** Circles >90%, arrows >85%. Recognition <50ms. Wrong shape → 4-alternative picker. Draw in browse → auto-enters annotate. Color gesture works with thumb-slide to ink strip.

---

### S-20 · Canvas Helpers
**Tier:** 3–5 · **Atoms:** CE-09, CE-10, IC-13, IC-30, DP-56 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Smart labels (+ photo-aware saliency), alignment guides, soft placement, metadata overlay.

**Files:**
- `feature-canvas/src/main/java/.../placement/LabelPlacer.kt`
- `feature-canvas/src/main/java/.../placement/AlignmentGuideEngine.kt`
- `feature-canvas/src/main/java/.../placement/SoftPlacementController.kt`
- `feature-canvas/src/main/java/.../render/MetadataOverlayRenderer.kt`

**Prerequisite:** S-10, S-05 (metadata service).
**Acceptance:** Labels avoid saliency <8ms. Guides appear smoothly. Soft placement 1.5s window. Overlay shows GPS/time/tech.

---

### S-21 · Camera + Photo Picker
**Tier:** 0–3 · **Atoms:** CC-01, CC-02, CC-03, CC-04 · **Size:** M
**Module:** `:feature-camera`

**Task:** CameraX capture, pre-warming, metadata injection, gallery picker.

**Files:**
- `feature-camera/src/main/java/.../CameraScreen.kt`
- `feature-camera/src/main/java/.../CameraViewModel.kt`
- `feature-camera/src/main/java/.../GalleryPicker.kt`

**Prerequisite:** S-05 (photo model + metadata service).
**Acceptance:** Viewfinder <500ms. Capture-to-display <1s. Pre-warm works. Multi-select gallery.

---

### S-22 · Edit Mode (Flip/Rotate/Crop/Adjust)
**Tier:** 1–2 · **Atoms:** IC-10, EP-01, EP-02, EP-03 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Edit toolbar, flip/rotate, crop, AGSL shader adjustments.

**Files:**
- `feature-canvas/src/main/java/.../edit/EditToolbar.kt`
- `feature-canvas/src/main/java/.../edit/CropTool.kt`
- `feature-canvas/src/main/java/.../edit/ShaderAdjustments.kt` (AGSL)
- `feature-canvas/src/main/res/raw/adjustments.agsl`

**Prerequisite:** S-11 (mode machine), S-08 (canvas).
**Acceptance:** AGSL renders in <16ms. Crop remaps annotation coords. Non-destructive.

---

### S-23 · Trade Kit System
**Tier:** 2–4 · **Atoms:** TK-01, TK-02, TK-03, TK-04, TK-06, TK-11 · **Size:** L
**Module:** `:core:data` + `:feature-canvas`

**Task:** Kit model, activation engine, 5 launch kits, tool resolver, catalog UI, semantic colors.

**Files:**
- `core/data/src/main/java/.../kit/TradeKit.kt` + `TradeKitRegistry.kt`
- `core/data/src/main/java/.../kit/KitActivationManager.kt`
- `feature-canvas/src/main/java/.../kit/KitCatalogScreen.kt`
- `core/data/src/main/java/.../kit/LaunchKits.kt` (5 kit definitions)

**Prerequisite:** S-03 (PD-05 for TK-04), S-04 (Room), S-12 (toolbar).
**Acceptance:** All 5 kits activate/deactivate. Toolbar reconfigures <100ms. Universal tools always available.

---

### S-24 · Onboarding + Project Hub
**Tier:** 0–4 · **Atoms:** ON-01, ON-02, ON-03, ON-04, ON-05, ON-08 · **Size:** M
**Module:** `:app` + `:feature-home`

**Task:** Splash, onboarding flow, work question, project hub, creation, warm launch.

**Files:**
- `app/src/main/java/.../SplashScreen.kt`
- `feature-home/src/main/java/.../onboarding/OnboardingFlow.kt`
- `feature-home/src/main/java/.../hub/ProjectHubScreen.kt`
- `feature-home/src/main/java/.../hub/ProjectHubViewModel.kt`

**Prerequisite:** S-01, S-04, S-11 (IC-01 for ON-08), S-23 (kits for onboarding).

> **Warm launch behavior (ON-08):** Restores to Browse Mode with the last-viewed photo and all annotation state visible. Even if the user was in Annotate Mode, warm launch returns to Browse as a safety default — prevents accidental annotation from stray touches on unlock. Camera warms in background.

**Acceptance:** Cold launch <300ms to splash. 60-second onboarding. Project hub scrolls 50 projects at 60fps. Warm launch restores to Browse with correct photo.

---

### S-25 · Settings + Locale Switching
**Tier:** 1–3 · **Atoms:** ON-07, L10N-02, L10N-04 · **Size:** M
**Module:** `:feature-settings`

**Task:** Settings screen, locale picker, ISO 7010 symbol picker.

**Files:**
- `feature-settings/src/main/java/.../SettingsScreen.kt`
- `feature-settings/src/main/java/.../SettingsViewModel.kt`
- `core/ui/src/main/java/.../symbols/IsoSymbolPicker.kt`
- `core/ui/src/main/res/raw/iso7010/` (SVG files)

**Prerequisite:** S-04, S-07.
**Acceptance:** Locale switches immediately. ISO symbols searchable in all locales.

---

### S-26 · Interaction Polish
**Tier:** 3–5 · **Atoms:** IC-26, IC-27, IC-29, VO-06 · **Size:** M
**Module:** `:feature-canvas` + `:core:data`

**Task:** Quick-label dictionary, swipe-between-photos, text keyboard fix, engineering autocomplete.

**Files:**
- `feature-canvas/src/main/java/.../input/SwipeBetweenPhotos.kt`
- `feature-canvas/src/main/java/.../text/KeyboardAwareLayout.kt`
- `core/data/src/main/java/.../autocomplete/EngineeringAutocomplete.kt`
- `core/data/src/main/java/.../autocomplete/QuickLabelDictionary.kt`

**Prerequisite:** S-14 (text element), S-12 (toolbar), S-09 (gesture router).
**Acceptance:** Swipe zero false positives during drawing. Autocomplete <50ms. Photo visible above keyboard.

> **Note:** IC-32 (Safety bar) and IC-33 (Micro-share) were previously listed here but belong in S-30 — both depend on export atoms (EX-01, EX-05) that are built in S-27.

---

### S-27 · Export Pipeline
**Tier:** 4–6 · **Atoms:** CE-11, EX-01, EX-02, EX-03, EX-05, IL-02 · **Size:** L
**Module:** `:feature-export` + `:feature-canvas`

**Task:** Canvas export, burned-in JPEG, sidecar JSON, metadata bar, Share Sheet, Annotation Envelope spec.

**Files:**
- `feature-canvas/src/main/java/.../export/CanvasExporter.kt`
- `feature-export/src/main/java/.../BurnedInExporter.kt`
- `feature-export/src/main/java/.../SidecarJsonWriter.kt`
- `feature-export/src/main/java/.../AnnotationEnvelope.kt`
- `feature-export/src/main/java/.../ShareSheetIntegration.kt`

**Prerequisite:** S-10 (rendering), S-06 (annotation data).
**Acceptance:** Export matches on-screen. 12MP + 50 annotations <3s. JSON validates. Share Sheet works.

---

### S-28 · PDF Report
**Tier:** 6 · **Atoms:** EX-04 · **Size:** L
**Module:** `:feature-export`

**Task:** PDF report generation — cover page, per-photo pages, summary.

**Files:**
- `feature-export/src/main/java/.../pdf/PdfReportGenerator.kt`
- `feature-export/src/main/java/.../pdf/PdfPageTemplates.kt`

**Prerequisite:** S-27.
**Acceptance:** 20 photos → PDF <10s. 300dpi for print. Renders in all PDF viewers.

---

### S-29 · Collage System
**Tier:** 3–6 · **Atoms:** CL-01, CL-02, CL-03, CL-04, CL-05, CL-06, CL-07, CL-08, CL-14, CL-15, EX-10 · **Size:** L (split into S-29a/S-29b if needed)
**Module:** `:feature-collage`

**Task:** Layout engine, 5 base layouts, cell editing, metadata bars, semantic borders, border resize, collage export.

**Files:**
- `feature-collage/src/main/java/.../layout/CollageLayout.kt` (abstract)
- `feature-collage/src/main/java/.../layout/` (5 layout implementations)
- `feature-collage/src/main/java/.../ui/CollageScreen.kt`
- `feature-collage/src/main/java/.../edit/CellEditor.kt`
- `feature-collage/src/main/java/.../export/CollageExporter.kt`

**Prerequisite:** S-08 (canvas), S-05 (photo model), S-27 (export).
**Acceptance:** All 5 layouts render. Cell zoom/pan/swap works. Semantic borders export. 30fps preview.

---

### S-30 · Completion + Flow Arrows + Safety Bar + Micro-Share
**Tier:** 6–7 · **Atoms:** ON-10, IL-01, DP-53, IC-32, IC-33 · **Size:** M
**Module:** `:feature-export` + `:feature-canvas`

**Task:** Inspection completion ritual, Share Sheet Level 1, flow arrows, safety bar, micro-share moment.

**Files:**
- `feature-export/src/main/java/.../CompletionRitualScreen.kt`
- `feature-canvas/src/main/java/.../render/FlowArrowRenderer.kt`
- `feature-export/src/main/java/.../SafetyBarCheck.kt`
- `feature-export/src/main/java/.../MicroShareOverlay.kt`

**Prerequisite:** S-27 (export — provides EX-01, EX-02, EX-05 needed by IC-32, IC-33, IL-01), S-15 (arrows — provides DP-03 needed by DP-53).
**Acceptance:** Completion shows summary. Project locks after seal. Flow arrows render with "FLOW" label. Safety bar warns about unannotated photos. Micro-share pill shows for 5s on completion.

---

## Phase 1 Session Summary

| Session | Atoms | Key Output | Prerequisite Sessions |
|---------|-------|-----------|----------------------|
| S-01 | DS-01,03,06 | Theme + typography | — |
| S-02 | DS-02,04,05, IC-23,24 | Animation + icons + haptics | S-01 |
| S-03 | PD-05,06,07 | Data models (pure Kotlin) | — |
| S-04 | PD-01,02,10 | Room DB + project model | — |
| S-05 | PD-03,08,09 | Photo model + evidence | S-04 |
| S-06 | PD-04,14 | Annotation model + provenance | S-03, S-04, S-05 |
| S-07 | L10N-01,03,05,14,17,18 | Localization (6 languages) | — |
| S-08 | CE-01,02 | Base canvas + viewMatrix | S-03 |
| S-09 | CE-03,04,05 | Gesture system | S-08 |
| S-10 | CE-06,07,08 | Rendering + hit-test + selection | S-08, S-09, S-03 |
| S-11 | IC-01,02,09,11 | Mode machine + chrome | S-01 |
| S-12 | IC-03,04,05,06,12,28 | Annotate toolbar system | S-01, S-11 |
| S-13 | IC-07,08,22 | Undo/redo system | S-03, S-11 |
| S-14 | DP-01,18,23,25,27,28 | Core 6 drawing elements | S-10 |
| S-15 | DP-02,03,04,05 | Arrowheads + arrows + leader | S-14 |
| S-16 | DP-30,62,63,64 | Freehand + polyline + polygon | S-10, S-02 |
| S-17 | DP-50,51,52,31,32,19,24 | Stamps + pins + hatching + cloud + text shapes | S-14, S-16 |
| S-18 | DP-13,14 | Dimension lines | S-15 |
| S-19 | IC-14,20,21 | Smart Paper + auto-mode + color gesture | S-14, S-15, S-16, S-12, S-09 |
| S-20 | CE-09,10, IC-13,30, DP-56 | Labels + guides + placement | S-10, S-05 |
| S-21 | CC-01,02,03,04 | Camera + picker | S-05 |
| S-22 | IC-10, EP-01,02,03 | Edit mode | S-11, S-08 |
| S-23 | TK-01,02,03,04,06,11 | Trade kit system | S-03, S-04, S-12 |
| S-24 | ON-01,02,03,04,05,08 | Onboarding + hub | S-01, S-04, S-11, S-23 |
| S-25 | ON-07, L10N-02,04 | Settings + locale + ISO symbols | S-04, S-07 |
| S-26 | IC-26,27,29, VO-06 | Interaction polish | S-14, S-12, S-09 |
| S-27 | CE-11, EX-01,02,03,05, IL-02 | Export pipeline | S-10, S-06 |
| S-28 | EX-04 | PDF report | S-27 |
| S-29 | CL-01..08,14,15, EX-10 | Collage system | S-08, S-05, S-27 |
| S-30 | ON-10, IL-01, DP-53, IC-32,33 | Completion + safety + micro-share | S-27, S-15 |

**30 sessions · 127 atoms · Phase 1 complete**

> **Note on S-29:** 11 atoms may exceed the 5-file rule. If splitting: S-29a = CL-01, CL-02..06, CL-15 (layout engine + base layouts); S-29b = CL-07, CL-08, CL-14, EX-10 (cell editing + export). If split, session count becomes 31.

---

## Parallel Tracks

Sessions that can run simultaneously (different Claude Code instances or sequential within a day):

```
Track A (Data):      S-03 ∥ S-04 → S-05 → S-06
Track B (Canvas):    S-08 → S-09 → S-10 → S-14 → S-15 → S-16 → S-17 → S-18 → S-19
Track C (UI):        S-01 → S-02 → S-11 → S-12 → S-13
Track D (Infra):     S-07 → S-25
Track E (Camera):    S-21
Track F (Kits):      S-23 (after S-03 + S-04 + S-12)
Track G (Nav):       S-24 (after S-01 + S-04 + S-11 + S-23)
Track H (Export):    S-27 → S-28 → S-30
Track I (Collage):   S-29
Track J (Polish):    S-20 → S-22 → S-26
```

> **Track A note:** S-03 and S-04 have no dependency on each other — they can run in parallel (∥). S-05 requires S-04. S-06 requires S-03, S-04, and S-05.

**Critical path:** S-03 → S-08 → S-09 → S-10 → S-14 → S-15 → S-19 → S-27 → S-30
**Estimated sessions on critical path:** 9

---

## Changes from v1.1

1. **Tier labels updated throughout** to match Doc 32 v2.0 recalculation (CE-06 moved to Tier 3; IC-03/06/10/11 promoted to Tier 1; all downstream tiers adjusted)
2. **S-06 prerequisite:** Added S-03 (PD-04 depends on PD-05 from S-03)
3. **S-16 prerequisite:** Added S-02 (DP-62 requires IC-23 haptic vocabulary)
4. **S-17 prerequisite:** Added S-16 (DP-32 revision cloud depends on DP-30 freehand from S-16)
5. **S-23 prerequisite:** Added S-03 (TK-04 depends on PD-05)
6. **S-24 prerequisite:** Added S-11 (ON-08 depends on IC-01)
7. **S-27 module:** Changed from `:feature-export` to `:feature-export` + `:feature-canvas` (CanvasExporter.kt is in `:feature-canvas`)
8. **Track A:** Changed S-03 → S-04 to S-03 ∥ S-04 (they are independent — no dependency between them)
9. **Critical path count:** Fixed from 10 to 9 (actual chain has 9 sessions)
10. **S-29 note:** Added explicit S-29a/S-29b split guidance for 11-atom session
11. **IC-23/IC-24 note:** Added to S-02 clarifying these IC atoms live in `:core:ui` as shared infrastructure

---

*April 2026 · TechNotes Session Blueprint Sequence v2.0 · 30 Phase 1 sessions*
