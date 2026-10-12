# 43 — Phase 2 Session Blueprint Sequence
## Claude Code Work Orders — Phase 2 (Depth)

> **Purpose:** Extends Phase 1 (30 sessions, 127 atoms) with Phase 2 sessions. ~90 atoms across 25 sessions. Same rules, same discipline.
> Phase 2 assumes ALL Phase 1 sessions (S-01 through S-30) are complete and passing.

---

## Rules (same as Phase 1)

1. **One session = one focused task.** Max 5 files created/modified.
2. **Read STATUS.md first, update it last.** Every session.
3. **Run `./gradlew assembleDebug` before declaring done.**
4. **Commit at clean checkpoints** with descriptive messages.
5. **No parallel implementations.** Delete old before writing new.

---

## Phase 2 Build Tiers (from Doc 32)

| Tier | Atom Count | Key Theme |
|------|-----------|-----------|
| P2-0 | 47 | Only depends on Phase 1 atoms — can start immediately |
| P2-1 | 37 | Depends on P2-0 atoms |
| P2-2 | 8 | Depends on P2-0 + P2-1 |
| P2-3 | 2 | Final Phase 2 atoms |

---

## Session Map — Phase 2

### S-31 · Extended Line Primitives
**Tier:** P2-0 · **Atoms:** DP-06, DP-07, DP-08, DP-09, DP-10, DP-11, DP-12 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Seven line variants, all extending DP-01 (straight line). Break zigzag, break wave, stitch line (DashType.STITCH), zigzag inline, coil/spring, wave/sine, grainline arrow.

**Files:**
- `feature-canvas/src/main/java/.../render/BreakLineRenderer.kt` — zigzag + wave
- `feature-canvas/src/main/java/.../render/DecoratedLineRenderer.kt` — stitch, zigzag inline, coil, wave/sine
- `feature-canvas/src/main/java/.../render/GrainlineArrowRenderer.kt` — double-head + "GRAINLINE" label
- `core/model/src/main/java/.../BreakLineElement.kt` + `DecoratedLineElement.kt`

**Prerequisite:** Phase 1 complete (S-14 provides DP-01).
**Acceptance:** All 7 line variants render, persist, and work with undo. Trade Kit filtering correct (stitch=Footwear, zigzag inline=Electronics, etc.).

---

### S-32 · Circle + Shape Variants
**Tier:** P2-0 · **Atoms:** DP-20, DP-21, DP-22, DP-26, DP-29 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Circle+divider, circle+crosshairs, concentric circle, rectangle+patterns, split circle. All extend Phase 1 circle (DP-18) and rectangle (DP-23).

**Files:**
- `feature-canvas/src/main/java/.../render/CircleVariantRenderer.kt` — divider, crosshairs, concentric, split
- `feature-canvas/src/main/java/.../render/RectangleVariantRenderer.kt` — patterns (hatched, X-fill, dot-fill)
- `core/model/src/main/java/.../CircleVariantElement.kt`
- `core/model/src/main/java/.../RectangleVariantElement.kt`

**Prerequisite:** Phase 1 complete (S-14 provides DP-18, DP-23).
**Acceptance:** All variants render with correct internal geometry. Resize handles preserve internal proportions.

---

### S-33 · Radius/Diameter Dimension + Calibration
**Tier:** P2-0 · **Atoms:** DP-15, DP-17 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Radius/diameter dimension line (extends DP-13) and calibration session (reference object → px-to-mm mapping).

**Files:**
- `feature-canvas/src/main/java/.../render/RadialDimensionRenderer.kt`
- `feature-canvas/src/main/java/.../calibration/CalibrationSession.kt`
- `feature-canvas/src/main/java/.../calibration/CalibrationUI.kt`
- `core/model/src/main/java/.../RadialDimensionElement.kt`

**Prerequisite:** Phase 1 complete (S-18 provides DP-13).
**Acceptance:** R/Ø prefix auto-applied. Calibration persists per photo. All dimension elements switch from "px" to calibrated units after calibration.

---

### S-34 · Inline Symbol System
**Tier:** P2-0 + P2-1 · **Atoms:** DP-33, DP-34, DP-35, DP-36, DP-37, DP-38, DP-39 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Base inline symbol system (tap on existing line → place symbol at intersection) plus 6 inline symbols: bowtie valve, inline triangle+bar, diamond inline, parallel plates, cylinder/piston, hydraulic spool.

**Files:**
- `feature-canvas/src/main/java/.../render/InlineSymbolRenderer.kt` — base system + all 6
- `feature-canvas/src/main/java/.../gesture/InlineSymbolPlacementHandler.kt` — tap-on-line gesture
- `core/model/src/main/java/.../InlineSymbolElement.kt` — sealed hierarchy
- `feature-canvas/src/main/res/drawable/` — SVG assets for 6 symbols

**Prerequisite:** Phase 1 complete (S-14 provides DP-01, S-10 provides CE-07).
**Acceptance:** Symbols snap to parent line. Auto-rotate to line angle. Draggable along parent. Line splits at symbol position.

---

### S-35 · Symbol Placement + Basic Symbols
**Tier:** P2-0 + P2-1 · **Atoms:** DP-40, DP-41, DP-42, DP-45, DP-46, DP-47, DP-48 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Symbol placement base system (browser grid + tap-place) and 7 basic symbols: op-amp, logic gate (AND/OR/NOT/NAND/NOR/XOR), tolerance block, surface finish, hardware mark, notch mark.

**Files:**
- `feature-canvas/src/main/java/.../render/SymbolRenderer.kt` — base + all symbols
- `feature-canvas/src/main/java/.../ui/SymbolBrowserGrid.kt` — filterable by active kit
- `core/model/src/main/java/.../SymbolElement.kt`
- `feature-canvas/src/main/res/drawable/symbols/` — SVG assets

**Prerequisite:** Phase 1 complete (S-10 provides CE-06, CE-07).
**Acceptance:** Browser grid filters by active Trade Kit. Tap-place. Scale/rotate handles. SVGs crisp at all zoom.

---

### S-36 · GD&T + Welding Symbol Wizards
**Tier:** P2-1 · **Atoms:** DP-43, DP-44 · **Size:** XL
**Module:** `:feature-canvas`

**Task:** Two 5-step wizards — GD&T Feature Control Frame (ASME Y14.5, 14 geometric characteristics) and Welding Symbol (AWS A2.4, arrow/other side, AWS↔ISO switching).

**Files:**
- `feature-canvas/src/main/java/.../wizard/GDTWizard.kt` — 5-step Compose flow
- `feature-canvas/src/main/java/.../wizard/WeldingSymbolWizard.kt` — 5-step + standard switch
- `feature-canvas/src/main/java/.../render/GDTRenderer.kt` — auto-sizing frame
- `feature-canvas/src/main/java/.../render/WeldingSymbolRenderer.kt`
- `core/model/src/main/java/.../GDTElement.kt` + `WeldingSymbolElement.kt`

**Prerequisite:** S-35 (DP-40 symbol placement base).
**Acceptance:** GD&T <10s to complete. Welding <15s. AWS↔ISO switch flips arrow side correctly. Auto-sizing renders correctly.

---

### S-37 · EU Material Grid + Safety/Hazard + NDT
**Tier:** P2-1 · **Atoms:** DP-49, DP-54, DP-55 · **Size:** L
**Module:** `:feature-canvas`

**Task:** EU Material Grid wizard (Directive 94/11/EC), ~200 ISO 7010 safety/hazard symbols (extends L10N-04 picker from Phase 1), NDT designations.

**Files:**
- `feature-canvas/src/main/java/.../wizard/EUMaterialGridWizard.kt`
- `feature-canvas/src/main/java/.../render/EUMaterialGridRenderer.kt`
- `feature-canvas/src/main/java/.../render/SafetySymbolRenderer.kt` — extends ISO 7010 picker
- `core/model/src/main/java/.../EUMaterialGridElement.kt`

**Prerequisite:** S-35 (DP-40), Phase 1 S-25 (L10N-04 ISO 7010 picker).
**Acceptance:** EU grid wizard completes in <10s. Safety symbols from L10N-04 placeable as annotation elements. NDT symbols render correctly.

---

### S-38 · Area Measurement + Cross-Photo Annotations
**Tier:** P2-1 · **Atoms:** DP-16, DP-65 · **Size:** M
**Module:** `:feature-canvas` + `:feature-collage`

**Task:** Area measurement (polygon → calculated area with calibration) and cross-photo annotations (arrows spanning collage cell gutters).

**Files:**
- `feature-canvas/src/main/java/.../render/AreaMeasurementRenderer.kt`
- `feature-collage/src/main/java/.../render/CrossPhotoAnnotationRenderer.kt`
- `core/model/src/main/java/.../AreaMeasurementElement.kt`
- `core/model/src/main/java/.../CrossPhotoAnnotationElement.kt`

**Prerequisite:** S-33 (DP-17 calibration), Phase 1 S-16 (DP-64 polygon), S-29 (CL-01 collage).
**Acceptance:** Area calculation accurate for convex + concave polygons. Cross-photo arrows render across cell gutters in collage.

---

### S-39 · Camera Extensions
**Tier:** P2-0 · **Atoms:** CC-05, CC-06 · **Size:** M
**Module:** `:feature-camera`

**Task:** Split-second buffer (ring buffer captures 1–2s before shutter press) and video capture + frame extraction (CameraX video + Media3 ExperimentalFrameExtractor).

**Files:**
- `feature-camera/src/main/java/.../capture/SplitSecondBuffer.kt` — ring buffer
- `feature-camera/src/main/java/.../capture/VideoCapture.kt` — CameraX video recording
- `feature-camera/src/main/java/.../capture/FrameExtractor.kt` — Media3 thumbnails

**Prerequisite:** Phase 1 complete (S-21 provides CC-01).
**Acceptance:** Buffer captures 1–2s pre-shutter. Video records 3–15s. Frame extraction <3s for 15s clip.

---

### S-40 · Data Layer Extensions
**Tier:** P2-0 · **Atoms:** PD-11, EX-06, EX-07, EX-09, EX-11 · **Size:** L
**Module:** `:core:model` + `:feature-export`

**Task:** Annotation layer model (per-user layers), C2PA content credentials, SHA-256 audit log, batch export, version timeline.

**Files:**
- `core/model/src/main/java/.../AnnotationLayer.kt` — layer model + Room entity
- `feature-export/src/main/java/.../c2pa/C2PACredentialSigner.kt`
- `feature-export/src/main/java/.../audit/SHA256AuditLog.kt`
- `feature-export/src/main/java/.../batch/BatchExporter.kt`
- `feature-export/src/main/java/.../timeline/VersionTimeline.kt`

**Prerequisite:** Phase 1 complete (S-06 provides PD-04, S-27 provides EX-01).
**Acceptance:** Layers isolate annotations by user/session. C2PA signing works. SHA-256 chain verifiable. Batch export handles 50+ photos. Timeline shows edit history.

---

### S-41 · Edit Mode Extensions
**Tier:** P2-0 · **Atoms:** EP-04, EP-05, EP-06, EP-07 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Four edit tools: vignette, film grain, color LUT presets, overlay browser.

**Files:**
- `feature-canvas/src/main/java/.../edit/VignetteShader.kt` — AGSL shader
- `feature-canvas/src/main/java/.../edit/FilmGrainShader.kt` — AGSL shader
- `feature-canvas/src/main/java/.../edit/ColorLUTManager.kt` — .cube LUT loading
- `feature-canvas/src/main/java/.../edit/OverlayBrowser.kt` — grid picker for all edit tools

**Prerequisite:** Phase 1 complete (S-22 provides IC-10, EP-03 AGSL).
**Acceptance:** Vignette + grain real-time preview. LUT presets render <100ms. Overlay browser scrollable.

---

### S-42 · Studio Mode
**Tier:** P2-1 + P2-2 · **Atoms:** IC-16, IC-17, IC-18 · **Size:** L
**Module:** `:feature-canvas`

**Task:** Studio mode activation (precision tools, desktop-class UI), full properties panel, and layer management UI.

**Files:**
- `feature-canvas/src/main/java/.../mode/StudioModeController.kt`
- `feature-canvas/src/main/java/.../ui/StudioPropertiesPanel.kt` — full property inspector
- `feature-canvas/src/main/java/.../ui/LayerManagementPanel.kt` — visibility, reorder, merge
- `feature-canvas/src/main/java/.../ui/StudioChrome.kt` — studio-specific toolbar layout

**Prerequisite:** S-40 (PD-11 annotation layers), Phase 1 (IC-01, IC-03).
**Acceptance:** Studio mode toggle works. Properties panel shows all element properties. Layers reorder, hide/show, and merge correctly.

---

### S-43 · Canvas Interaction Polish Phase 2
**Tier:** P2-0 · **Atoms:** IC-15, IC-19, IC-25, IC-31 · **Size:** M
**Module:** `:feature-canvas`

**Task:** Context ring (radial menu on long-press), intent-reading transitions, annotation summary ribbon, ghost overlay capture.

**Files:**
- `feature-canvas/src/main/java/.../ui/ContextRing.kt` — radial menu
- `feature-canvas/src/main/java/.../mode/IntentReader.kt` — gesture velocity/direction → predict mode
- `feature-canvas/src/main/java/.../ui/AnnotationSummaryRibbon.kt` — "3 defects · 1 measurement"
- `feature-canvas/src/main/java/.../overlay/GhostOverlayCapture.kt` — 30% opacity previous photo

**Prerequisite:** Phase 1 complete (S-09 CE-04, S-11 IC-01, S-06 PD-04, S-21 CC-01).
**Acceptance:** Context ring appears on 500ms hold. Intent-reading transitions smooth. Ribbon auto-dismiss 3s. Ghost overlay toggleable.

---

### S-44 · Localization — 13-Language Expansion
**Tier:** P2-0 · **Atoms:** L10N-06 · **Size:** L
**Module:** All modules

**Task:** Add 7 additional locale string files: Dutch (nl), Polish (pl), Swedish (sv), Czech (cs), Romanian (ro), Chinese Simplified (zh-CN), Chinese Traditional (zh-TW).

**Files:**
- `core/ui/src/main/res/values-nl/strings.xml`
- `core/ui/src/main/res/values-pl/strings.xml`
- `core/ui/src/main/res/values-sv/strings.xml`
- `core/ui/src/main/res/values-cs/strings.xml`
- `core/ui/src/main/res/values-ro/strings.xml` + `-zh-rCN/` + `-zh-rTW/`

**Prerequisite:** Phase 1 complete (S-07 provides L10N-01 base).
**Acceptance:** All 13 languages render correctly. Chinese min 14sp body text. Plurals correct for Polish (few/many). No untranslated strings.

---

### S-45 · Terminology Database + IATE Pipeline
**Tier:** P2-0 + P2-1 · **Atoms:** L10N-07, L10N-08, L10N-09, L10N-16 · **Size:** L
**Module:** `:core:data`

**Task:** Room terminology database (concepts + terms), IATE TBX import pipeline, structured annotation i18n with ISO 14224 failure codes, dual-standard reference fields.

**Files:**
- `core/data/src/main/java/.../terminology/TerminologyDatabase.kt` — Room tables
- `core/data/src/main/java/.../terminology/IATETBXImporter.kt` — build-time TBX parser
- `core/data/src/main/java/.../terminology/ISO14224Codes.kt` — ~38 problem + ~20 cause codes
- `core/data/src/main/java/.../terminology/DualStandardResolver.kt` — ISO↔ASME↔DIN mapping
- `core/data/src/main/assets/terminology.db` — pre-built SQLite (~5MB)

**Prerequisite:** S-44 (L10N-06 for 13-language base), Phase 1 (S-04 PD-01).
**Acceptance:** <5ms lookup. ~2000 terms × 5 languages. ISO 14224 codes render in user locale. Dual-standard shows both conventions.

---

### S-46 · ML Kit Translation + Glossary
**Tier:** P2-1 + P2-2 · **Atoms:** L10N-10, L10N-11, L10N-12 · **Size:** M
**Module:** `:core:data`

**Task:** ML Kit offline translation (last-resort for free-text notes), pre-translated phrase cache, terminology glossary overlay for reviewing translations.

**Files:**
- `core/data/src/main/java/.../translation/MLKitTranslator.kt` — on-demand model download
- `core/data/src/main/java/.../translation/PhraseCache.kt` — Room table of pre-translated phrases
- `feature-canvas/src/main/java/.../ui/GlossaryOverlay.kt` — inline terminology glossary
- `core/data/src/main/java/.../translation/TranslationPipeline.kt` — terminology DB → ML Kit fallback

**Prerequisite:** S-45 (L10N-07 terminology DB).
**Acceptance:** ML Kit downloads on Wi-Fi only. ~30MB per language pack. Glossary highlights dangerous technical mistranslations. Phrase cache <5ms retrieval.

---

### S-47 · Trade Kit Extensions
**Tier:** P2-0 · **Atoms:** TK-05, TK-07, TK-08, TK-10 · **Size:** M
**Module:** `:core:model` + `:feature-canvas`

**Task:** .tnkit file format (export/import custom kits), kit customization (long-press-drag into toolbar slots), contextual kit suggestion, kit-specific collage templates.

**Files:**
- `core/model/src/main/java/.../tradekit/TnKitFormat.kt` — serialization/deserialization
- `feature-canvas/src/main/java/.../ui/KitCustomizationDragHandler.kt`
- `feature-canvas/src/main/java/.../tradekit/ContextualKitSuggestion.kt` — usage pattern detection
- `feature-collage/src/main/java/.../templates/KitCollageTemplates.kt`

**Prerequisite:** Phase 1 complete (S-23 provides TK-01, TK-02).
**Acceptance:** .tnkit round-trip works. Drag customization persists. Suggestion max 1/session. Kit templates appear in collage layout picker.

---

### S-48 · Collage Extensions
**Tier:** P2-0 + P2-1 · **Atoms:** CL-09, CL-10, CL-11, CL-13, CL-16, CL-17 · **Size:** L
**Module:** `:feature-collage`

**Task:** Smart layout suggestion (auto-detect Before/After, Quad, Sequence), domain templates, freeform layout (free drag/resize/overlap), synchronized zoom/pan, content-aware auto-crop, auto-match exposure.

**Files:**
- `feature-collage/src/main/java/.../layout/SmartLayoutSuggester.kt` — photo analysis → layout
- `feature-collage/src/main/java/.../layout/FreeformLayout.kt` — unconstrained positioning
- `feature-collage/src/main/java/.../edit/SynchronizedZoomController.kt` — chain-link linked cells
- `feature-collage/src/main/java/.../edit/AutoCropEngine.kt` — saliency + face detection
- `feature-collage/src/main/java/.../edit/ExposureMatchEngine.kt` — white balance + exposure normalization

**Prerequisite:** Phase 1 complete (S-29 provides CL-01, CL-07), S-47 (TK-10 for kit templates), Phase 1 S-22 (EP-03 AGSL).
**Acceptance:** Smart suggestion correct >70% of cases. Freeform drag/resize/overlap works. Linked cells zoom together. Auto-crop centers subject. Exposure match visually consistent.

---

### S-49 · Navigation Phase 2
**Tier:** P2-0 · **Atoms:** ON-06, ON-09, ON-11 · **Size:** M
**Module:** `:app` + `:feature-canvas`

**Task:** Search (Room FTS / LIKE queries — Phase 2 scope per cross-phase resolution), situation-first onboarding (photo-recognition tiles), deep linking.

**Files:**
- `app/src/main/java/.../search/SearchScreen.kt` — grouped results
- `app/src/main/java/.../search/SearchRepository.kt` — Room FTS queries
- `app/src/main/java/.../onboarding/SituationFirstOnboarding.kt` — "Which looks like your work?"
- `app/src/main/java/.../deeplink/DeepLinkHandler.kt`

**Prerequisite:** Phase 1 complete (S-24 provides ON-02, S-06 provides PD-04).
**Acceptance:** Search results <200ms. Deep links navigate to correct annotation. Situation-first tiles render photos.

---

### S-50 · Integration Layer — Offline Queue + Zapier
**Tier:** P2-0 + P2-1 · **Atoms:** IL-03, IL-04 · **Size:** M
**Module:** `:core:data` + `:feature-export`

**Task:** Offline sync queue (WorkManager, exponential backoff, survives force-kill) and Zapier webhook adapter (POST Annotation Envelope on completion).

**Files:**
- `core/data/src/main/java/.../sync/SyncQueueEntity.kt` — Room entity
- `core/data/src/main/java/.../sync/SyncQueueWorker.kt` — WorkManager
- `feature-export/src/main/java/.../integration/ZapierWebhookAdapter.kt`
- `feature-export/src/main/java/.../integration/IntegrationSettingsScreen.kt` — webhook URL config

**Prerequisite:** Phase 1 complete (S-27 provides IL-02 Annotation Envelope).
**Acceptance:** Queue survives force-kill + reboot. Exponential backoff on 5xx. Zapier receives valid Envelope JSON. Max 1000 queued items.

---

### S-51 · Voice Annotations
**Tier:** P2-0 + P2-1 · **Atoms:** VO-01, VO-02 · **Size:** L
**Module:** `:feature-voice` (new module)

**Task:** Voicemail annotations (tap-hold → speak → release, WhatsApp-style) and on-device speech transcription (sherpa-onnx or whisper.cpp, ~50MB model).

**Files:**
- `feature-voice/src/main/java/.../VoicemailAnnotation.kt` — AudioRecord, WAV, max 30s
- `feature-voice/src/main/java/.../WaveformView.kt` — mini waveform player at annotation location
- `feature-voice/src/main/java/.../SpeechTranscriber.kt` — offline model, background processing
- `feature-voice/build.gradle.kts` — new module setup
- `core/model/src/main/java/.../VoicemailElement.kt`

**Prerequisite:** Phase 1 complete (S-10 CE-06, S-06 PD-04).
**Acceptance:** Recording starts <100ms of hold threshold. Waveform 60fps. Transcription >85% accuracy on clear speech. Works with gloves. Fully offline.

---

### S-52 · Voice Extensions
**Tier:** P2-2 · **Atoms:** VO-03, VO-04 · **Size:** S
**Module:** `:feature-voice`

**Task:** Linked evidence (word-level timestamps, tap phrase → hear audio) and sound as diagnostic data (5s ambient clips, optional spectrogram).

**Files:**
- `feature-voice/src/main/java/.../LinkedEvidence.kt` — word-level timestamp mapping
- `feature-voice/src/main/java/.../DiagnosticAudioRecorder.kt` — ambient capture mode
- `feature-voice/src/main/java/.../SpectrogramView.kt` — optional frequency visualization

**Prerequisite:** S-51 (VO-01, VO-02).
**Acceptance:** Tap any transcribed word → plays corresponding audio segment. Spectrogram renders in real-time.

---

### S-53 · AI Features
**Tier:** P2-0 · **Atoms:** AI-02, AI-10, AI-15 · **Size:** M
**Module:** `:feature-camera` + `:core:image`

**Task:** OCR for equipment nameplates (ML Kit Text Recognition, offline), QR/barcode scanning (ML Kit Barcode, auto-fill equipment ref), content-aware auto-crop (face detection + saliency).

**Files:**
- `feature-camera/src/main/java/.../ai/NameplateOCR.kt` — ML Kit text → structured extraction
- `feature-camera/src/main/java/.../ai/BarcodeScanner.kt` — ML Kit barcode → equipment ref
- `core/image/src/main/java/.../crop/ContentAwareCrop.kt` — saliency + face detection

**Prerequisite:** Phase 1 complete (S-21 provides CC-01).
**Acceptance:** OCR >90% on clean nameplates. QR/barcode scan <500ms. Auto-crop centers subject correctly.

---

### S-54 · Collaboration System
**Tier:** P2-0 + P2-2 · **Atoms:** CO-01, CO-02, CO-03, CO-06 · **Size:** XL
**Module:** `:feature-collab` (new module)

**Task:** Async shared annotation (share link → expert marks up → push notification), comment threads (per-annotation), multi-user layers (per-user color), user accounts (optional, email + Google Sign-In).

**Files:**
- `feature-collab/build.gradle.kts` — new module setup
- `feature-collab/src/main/java/.../sharing/AsyncShareManager.kt` — REST upload/download
- `feature-collab/src/main/java/.../comments/CommentThread.kt` — per-annotation threads
- `feature-collab/src/main/java/.../auth/UserAccountManager.kt` — Firebase Auth (email + Google)
- `feature-collab/src/main/java/.../layers/MultiUserLayerController.kt`

**Prerequisite:** Phase 1 complete (S-27 EX-01, S-06 PD-04), S-40 (PD-11 layers), S-42 (IC-18 layer management).
**Acceptance:** Share link generates. Expert annotations arrive in different color/layer. Comments thread per annotation. Google Sign-In works. Local data preserved after account creation.

---

### S-55 · Video Notepad
**Tier:** P2-1 · **Atoms:** VD-01, VD-09 · **Size:** XL
**Module:** `:feature-video` (new module)

**Task:** Core Video Notepad — diagnostic clip capture (3–15s), thumbnail filmstrip (Media3 FrameExtractor), tap frame → annotation mode (same tools as photo), temporal range handles, timed playback, export (MP4 burned-in or sidecar JSON). Plus progressive reveal for collage cells.

**Files:**
- `feature-video/build.gradle.kts` — new module
- `feature-video/src/main/java/.../VideoNotepadScreen.kt` — filmstrip + annotation + playback
- `feature-video/src/main/java/.../FrameAnnotationSurface.kt` — reuses Phase 1 drawing tools
- `feature-video/src/main/java/.../TemporalRangeController.kt` — appear/disappear timestamps
- `feature-video/src/main/java/.../VideoExporter.kt` — MP4 with burned-in annotations

**Prerequisite:** S-39 (CC-06 video capture + frame extraction), Phase 1 (S-10 CE-06).
**Acceptance:** Filmstrip <3s for 15s clip. Frame annotation pixel-accurate. Temporal playback correct. Same tool palette as photos. Export matches on-screen.

---

### S-56 · Video Extensions — Bursts + Before/After
**Tier:** P2-2 · **Atoms:** VD-02, VD-03 · **Size:** M
**Module:** `:feature-video`

**Task:** Annotation bursts (tap during playback → mark moment, annotate on second pass) and before/after video comparison (side-by-side synced playback).

**Files:**
- `feature-video/src/main/java/.../burst/BurstMarkerController.kt` — timeline markers
- `feature-video/src/main/java/.../burst/BurstAnnotationPass.kt` — second-pass annotation
- `feature-video/src/main/java/.../compare/BeforeAfterVideoComparison.kt`
- `feature-video/src/main/java/.../compare/SyncedTransportControls.kt`

**Prerequisite:** S-55 (VD-01 Video Notepad).
**Acceptance:** Burst markers visible on timeline. Second-pass annotation works on marked frames. Synced playback maintains <100ms drift.

---

### S-57 · Multi-Video Parallel Playback
**Tier:** P2-2 + P2-3 · **Atoms:** VD-04, VD-05, VD-06 · **Size:** XL
**Module:** `:feature-video`

**Task:** 2–4 ExoPlayer/Media3 instances in grid layouts (1+1, 2×2, 1+3). Shared scrub bar + independent mode. Event-based sync points (Kinovea pattern). Tap-to-focus audio.

**Files:**
- `feature-video/src/main/java/.../multi/MultiVideoScreen.kt` — grid layouts
- `feature-video/src/main/java/.../multi/VideoPlayerPool.kt` — ExoPlayer instance management
- `feature-video/src/main/java/.../multi/SyncPointController.kt` — event-based alignment
- `feature-video/src/main/java/.../multi/AudioFocusController.kt` — tap cell → audio
- `feature-video/src/main/java/.../multi/SharedScrubBar.kt`

**Prerequisite:** S-55 (VD-01), Phase 1 S-29 (CL-01 collage layouts).
**Acceptance:** 4× 480p 30fps on S25 Ultra. 2 streams on lower devices. No MediaCodec deadlock. Sync points align playback. Tap-to-focus routes audio instantly.

---

### S-58 · Play Store Compliance
**Tier:** P2-0 · **Atoms:** — (infrastructure) · **Size:** M
**Module:** `:app`

**Task:** Google Play Store compliance pass — targetSdk 36 verified, Data Safety declarations, AAB bundle requirements, privacy policy, content rating questionnaire, app signing.

**Files:**
- `app/build.gradle.kts` — verify targetSdk 36, signing configs
- `app/src/main/res/xml/data_safety.xml` — Data Safety form backup
- Documentation: `PLAY-STORE-CHECKLIST.md`

**Prerequisite:** Phase 1 complete.
**Acceptance:** `./gradlew bundleRelease` produces valid AAB. Data Safety form complete. No policy violations flagged by Play Console pre-launch report.

---

## Phase 2 Session Summary

| Session | Atoms | Key Output | Prerequisite |
|---------|-------|-----------|-------------|
| S-31 | DP-06,07,08,09,10,11,12 | Extended line primitives (7 variants) | Phase 1 |
| S-32 | DP-20,21,22,26,29 | Circle + shape variants | Phase 1 |
| S-33 | DP-15,17 | Radius/diameter dim + calibration | Phase 1 |
| S-34 | DP-33,34–39 | Inline symbol system (7 atoms) | Phase 1 |
| S-35 | DP-40,41,42,45,46,47,48 | Symbol placement + 7 symbols | Phase 1 |
| S-36 | DP-43,44 | GD&T + welding wizards | S-35 |
| S-37 | DP-49,54,55 | EU grid + safety + NDT | S-35, S-25 |
| S-38 | DP-16,65 | Area measurement + cross-photo | S-33, S-29 |
| S-39 | CC-05,06 | Camera extensions (buffer + video) | Phase 1 |
| S-40 | PD-11, EX-06,07,09,11 | Data layer extensions | Phase 1 |
| S-41 | EP-04,05,06,07 | Edit mode extensions | Phase 1 |
| S-42 | IC-16,17,18 | Studio mode | S-40 |
| S-43 | IC-15,19,25,31 | Canvas interaction polish | Phase 1 |
| S-44 | L10N-06 | 13-language expansion | Phase 1 |
| S-45 | L10N-07,08,09,16 | Terminology DB + IATE + ISO 14224 | S-44 |
| S-46 | L10N-10,11,12 | ML Kit translation + glossary | S-45 |
| S-47 | TK-05,07,08,10 | Trade kit extensions | Phase 1 |
| S-48 | CL-09,10,11,13,16,17 | Collage extensions | S-47, Phase 1 |
| S-49 | ON-06,09,11 | Navigation Phase 2 | Phase 1 |
| S-50 | IL-03,04 | Integration layer (queue + Zapier) | Phase 1 |
| S-51 | VO-01,02 | Voice annotations + transcription | Phase 1 |
| S-52 | VO-03,04 | Voice extensions | S-51 |
| S-53 | AI-02,10,15 | AI features (OCR + QR + auto-crop) | Phase 1 |
| S-54 | CO-01,02,03,06 | Collaboration system | S-40, S-42 |
| S-55 | VD-01,09 | Video notepad | S-39 |
| S-56 | VD-02,03 | Video bursts + before/after | S-55 |
| S-57 | VD-04,05,06 | Multi-video parallel playback | S-55, Phase 1 |
| S-58 | — | Play Store compliance | Phase 1 |

**28 sessions · ~90 atoms · Phase 2 complete**

---

## Parallel Tracks — Phase 2

```
Track K (Drawing):    S-31 ∥ S-32 ∥ S-33 → S-34 ∥ S-35 → S-36 → S-37 → S-38
Track L (Data):       S-40 → S-42 → S-54
Track M (Camera):     S-39 → S-55 → S-56 → S-57
Track N (L10N):       S-44 → S-45 → S-46
Track O (Voice):      S-51 → S-52
Track P (UI):         S-41 ∥ S-43 ∥ S-47 → S-48
Track Q (Infra):      S-49 ∥ S-50 ∥ S-53 ∥ S-58
```

> **Tracks K, L, M, N, O, P, Q can all start in parallel** since their roots depend only on Phase 1.

**Critical path:** S-40 → S-42 → S-54 (data layer → studio mode → collaboration)
**Secondary critical path:** S-39 → S-55 → S-56 → S-57 (camera → video → multi-video)

---

## New Modules Introduced in Phase 2

| Module | Session | Purpose |
|--------|---------|---------|
| `:feature-voice` | S-51 | Voice annotations, transcription, diagnostic audio |
| `:feature-video` | S-55 | Video notepad, multi-video, temporal annotations |
| `:feature-collab` | S-54 | Collaboration, sharing, user accounts, multi-user layers |

---

*April 2026 · TechNotes Phase 2 Session Blueprint Sequence v1.0 · 28 sessions · ~90 atoms*
