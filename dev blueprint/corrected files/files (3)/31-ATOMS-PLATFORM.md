# 31 — Atoms: Platform
## Integration · AI · Collaboration · Voice · Video & Multi-Media

> **Phase 2–4 systems.** Network effects, intelligence, multi-user, voice input, video annotation.

---

## Area 13 — Integration Layer (IL)

### IL-01 · Share Sheet Export (Level 1)
**Phase:** 1 · **Size:** S · **Depends:** EX-01, EX-05
Integration wrapper around EX-05. Adds integration-layer context: routes to configured CMMS if present, falls back to standard Share Sheet if not. The distinction from EX-05 is that IL-01 checks for Level 2+ integrations before defaulting to Android Share Sheet. If no integration is configured, behavior is identical to EX-05.

### IL-02 · Annotation Envelope Format Spec
**Phase:** 1 · **Size:** M · **Depends:** EX-02, PD-05
Universal export container with 5 outputs: visual (JPEG), structured (JSON), metadata, evidence (SHA-256/C2PA), summary. Schema versioned, machine-readable, open format.

### IL-03 · Offline Sync Queue (Outbox)
**Phase:** 2 · **Size:** M · **Depends:** PD-01
`SyncQueueEntity`: payload, destination, status (PENDING/SENDING/SENT/FAILED), retryCount. WorkManager processing. Exponential backoff. Survives force-kill. Max 1000 items.

### IL-04 · Zapier Webhook (Level 2)
**Phase:** 2 · **Size:** M · **Depends:** IL-02, IL-03
Webhook URL in settings → Envelope POSTed on completion. Published Zapier template. Retry on 5xx.

### IL-05 · CMMS Adapter Interface
**Phase:** 3 · **Size:** M · **Depends:** IL-02
`CmmsAdapter`: authenticate, pushAnnotation, pullContext (optional), reportOutcome (optional). Levels L3 (one-way) / L4 (bidirectional). Hilt multibinding discovery.

### IL-06 · Procore Adapter (Level 3) — 3/L/IL-05. OAuth 2.0, observation log, tag mapping.
### IL-07 · MaintainX Adapter — 3/M/IL-05
### IL-08 · UpKeep Adapter — 3/M/IL-05
### IL-09 · ServiceTitan Adapter — 3/M/IL-05

### IL-10 · Context Whisper (CMMS → TechNotes)
**Phase:** 3 · **Size:** M · **Depends:** IL-05
Pull asset context: age, history, open work orders. Info bar. Informational only. <2s load. Cache locally.

### IL-11 · Priority Tint — 3/S/IL-10. Overdue→amber, critical→red toolbar tint.
### IL-12 · Return Signal — 3/M/IL-05. Outcome feeds back: RESOLVED/REPAIRED/DEFERRED/REJECTED badge.
### IL-13 · Maximo Adapter — 4/M/IL-05
### IL-14 · SAP PM Adapter — 4/M/IL-05
### IL-15 · .tnote Open Schema — 4/M/IL-02
### IL-16 · Multi-CMMS Routing — 4/L/IL-05. Route per project/site for subcontractors.
### IL-17 · Rehydration Protocol — 4/M/IL-02. Reconstruct project from Envelope.

---

## Area 14 — AI & Intelligence (AI)

### ~~AI-01~~ · Shape Recognition — **MERGED into IC-14 (Doc 29)**
*AI-01 was a duplicate of IC-14 (Smart Paper shape recognition). Both described the same geometric analysis implementation — circularity, corners, aspect ratio, pure geometry, no ML. The scope is now fully covered by IC-14 in Doc 29. AI-08 (Sketch-to-structured-data) dependency updated from AI-01 to IC-14.*

### AI-02 · OCR for Equipment Nameplates — 2/M/CC-01
ML Kit Text Recognition, offline. >90% on clean nameplates. Structured extraction of common patterns.

### AI-03 · Gemini Nano On-Device Setup
**Phase:** 3 · **Size:** M · **Depends:** —
Inference abstraction. ~200MB model, Wi-Fi download. Battery/thermal monitoring. Graceful degradation.

### AI-04 · AI Report Generation — 3/L/AI-03, EX-04. Narrative report. Labeled "AI-assisted."
### AI-05 · Dream Inspection — 3/L/AI-03, PD-04. Overnight processing. Morning report.
### AI-06 · Documentation Penumbra — 3/M/AI-03, PD-04. Background analysis, low-confidence suggestions.
### AI-07 · Video Anomaly Detection — 3/M/AI-03, CC-06. Vibration, leaks, sparking.
### AI-08 · Sketch-to-Structured-Data — 3/M/IC-14, PD-04
### AI-09 · Inspection Workflow Recognition — 4/L/AI-03, PD-04

### AI-10 · QR/Barcode Scan
**Phase:** 2 · **Size:** M · **Depends:** CC-01, PD-12
ML Kit Barcode Scanning → auto-fill equipment reference from asset list or CMMS.

### AI-11 · Handwriting Recognition — 3/M/AI-03. ML Kit Digital Ink, 300+ languages, on-device.
### AI-12 · Anomaly Memory (PatchCore) — 3/M/AI-03, PD-04
Learn "normal" from defect-free images. 99.1% AUROC MVTec AD. Few-shot. Nearest-neighbor scoring.

### AI-13 · Visual Change Detection — 3/M/AI-03, PD-03. Current vs historical, auto-highlight degradation.
### AI-14 · Difference Highlighting — 3/M/CL-02, AI-03. Pixel diff with colored markers.
### AI-15 · Content-Aware Auto-Crop — 2/S/CC-01. Face detection (~40ms) + saliency. Manual override.

---

## Area 15 — Collaboration (CO)

### CO-01 · Async Shared Annotation
**Phase:** 2 · **Size:** L · **Depends:** EX-01, PD-04
Share link → expert annotates (different color/layer) → push notification. REST upload/download. Web viewer for view-only.

### CO-02 · Comment Threads — 2/M/PD-04. Per-annotation threads. Speech bubble icon.
### CO-03 · Multi-User Layers — 2/M/PD-11, IC-18. Per-user layer with distinct color.
### CO-04 · CRDT Sync — 3/L/PD-04. Automerge + WebSocket. <1s sync. Offline merge.
### CO-05 · Team Admin Dashboard — 3/L/CO-01. Web dashboard.
### CO-06 · User Accounts — 2/M/ON-07. Optional. Email + Google Sign-In. Local data preserved.

---

## Area 16 — Voice & Alternative Input (VO)

### VO-01 · Voicemail Annotations
**Phase:** 2 · **Module:** `:feature-voice` · **Size:** M · **Depends:** CE-06, PD-04
Tap-hold annotation point → speak 5–10s → release. Audio clip as primary artifact. Mini waveform player at annotation location. WhatsApp-style (works with gloves). AudioRecord API, WAV, max 30s. App-internal storage.

**Acceptance:** Recording starts <100ms of hold threshold. Waveform 60fps. Works with gloves.

### VO-02 · On-Device Speech Transcription
**Phase:** 2 · **Size:** M · **Depends:** VO-01
sherpa-onnx or whisper.cpp (~50MB model). Fully offline. Background processing. Editable by technician. Language from locale. >85% accuracy on clear speech. Completes within 2× recording duration.

### VO-03 · Linked Evidence (Audio ↔ Transcript)
**Phase:** 2 · **Size:** S · **Depends:** VO-01, VO-02
Word-level timestamps. Tap phrase → hear original audio. Auditable chain: raw → transcript → structured.

### VO-04 · Sound as Diagnostic Data
**Phase:** 2 · **Size:** S · **Depends:** VO-01
5-second ambient clips attached to annotations. "Record what you hear" mode. Bearing defects, valve hiss. Optional spectrogram view.

### VO-05 · Speech-to-Structured-Annotation
**Phase:** 3 · **Size:** M · **Depends:** VO-02, AI-03
"Bearing 4B excessive play, recommend replacement" → auto-creates annotation. NLP entity extraction via Gemini Nano.

### VO-06 · Engineering Autocomplete
**Phase:** 1 · **Module:** `:core:data` · **Size:** M · **Depends:** PD-01, DP-25
Room DB with engineering terms, standard codes, equipment IDs. Trie prefix matching <5ms. Domain dictionaries per Trade Kit (~5000 terms each). "corr-" → corrosion, corroded, corrective action. "NEC 4" → NEC 408.30. Recent/frequent prioritized. Integrated into all text inputs.

**Acceptance:** Suggestions <50ms. Works fully offline.

### VO-07 · Voice-Directed Spatial Annotation
**Phase:** 3 · **Size:** L · **Depends:** AI-03, VO-02
"Mark the crack at 2 o'clock on the flange" → VLM (Gemini Nano) parses spatial ref + object ref + defect type → places annotation.

### VO-08 · Bone Conduction Audio (Shokz) — 3/S/VO-02
Voice commands in, audio confirmations out. Works with hearing protection. Bluetooth routing.

---

## Area 17 — Video & Multi-Media (VD)

### VD-01 · Video Notepad
**Phase:** 2 · **Module:** `:feature-video` · **Size:** L · **Depends:** CC-06, CE-06
"Video Notepad" for diagnostic clips (not an editor). Thumbnail filmstrip (Media3 FrameExtractor). Tap thumbnail → frame → annotation mode (same tools as photo). Temporal range handles: set annotation appear/disappear timestamps. Playback with timed annotations. Export: MP4 burned-in or sidecar JSON with temporal data. Mode toggle: View (pinch=zoom) vs Draw.

**Acceptance:** Filmstrip <3s for 15s clip. Frame annotation pixel-accurate. Temporal playback correct. Same tool palette as photos.

### VD-02 · Annotation Bursts
**Phase:** 2 · **Size:** S · **Depends:** VD-01
During playback: tap to mark moments (`player.currentPosition`). Visual markers on timeline. Annotate on second pass. Fast "flag then annotate" workflow.

### VD-03 · Before/After Video Comparison
**Phase:** 2 · **Size:** M · **Depends:** VD-01
Side-by-side synced playback. Shared transport controls. Optional difference highlighting (AI-14).

### VD-04 · Multi-Video Parallel Playback
**Phase:** 2 · **Size:** L · **Depends:** VD-01, CL-01
2–4 ExoPlayer/Media3 instances at 480p–720p. SurfaceView rendering. `getMaxSupportedInstances()` runtime query. Grid layouts: 1+1, 2×2, 1+3. Shared scrub bar + independent mode.

**Acceptance:** 4× 480p 30fps on S25 Ultra. 2 streams on lower devices. No MediaCodec deadlock.

### VD-05 · Event-Based Sync Points — 2/S/VD-04
Mark common moment per video → playback aligns from sync points. Kinovea pattern.

### VD-06 · Tap-to-Focus Audio — 2/S/VD-04
Tap cell → that cell's audio plays, others muted. Speaker icon indicator.

### VD-07 · Cinemagraph
**Phase:** 3 · **Size:** L · **Depends:** VD-01
Still frame → paint motion mask → masked areas loop, rest frozen. Bounce (reciprocating) + repeat (continuous rotation).

### VD-08 · Annotation Object Tracking
**Phase:** 3 · **Size:** L · **Depends:** VD-01, AI-03
KLT/SLAM keeps annotations on moving subjects. Arrow follows vibrating bearing as camera moves.

### VD-09 · Progressive Reveal — 2/S/CL-01
Collage cells start blacked out, reveal sequentially. Timer or tap-to-advance. Narrative presentation.

### VD-10 · FLIR Thermal Video
**Phase:** 3 · **Size:** L · **Depends:** VD-01, CC-06
Side-by-side thermal + visible. Full gradient AND full detail simultaneously. FLIR SDK. Lepton XDS support.

---

*April 2026 · TechNotes Atoms: Platform v2.0 · Integration, AI, Collaboration, Voice, Video*
