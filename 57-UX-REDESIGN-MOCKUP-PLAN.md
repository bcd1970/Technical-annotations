# 57 — UX Redesign · Mockup Plan

> **Comprehensive interaction map for TechNotes.** Decomposes every user-facing surface in the app into focused, single-concern mockups. Each mockup is buildable and reviewable in one Claude session. Together they define the new UX in full — nothing is left out.

---

## How this plan is meant to be used

1. Open a new Claude conversation in the TechNotes project. This file lives in project knowledge.
2. Say: **"Build mockup MXX from doc 57."** (Replace XX with the mockup number.)
3. Claude reads this plan, reads any referenced source docs, builds the focused HTML mockup, presents it.
4. Iterate on that single mockup until it feels right.
5. Move to the next mockup.

Each mockup is a **single self-contained interactive HTML file** in the TechNotes visual language (dark, monochrome chrome, Plus Jakarta Sans + IBM Plex Mono, semantic ink colors reserved for annotations). Mobile-responsive. Out-of-scope features are stubbed with visual feedback only.

---

## Why this plan exists

The home-screen interaction model has been redefined (Q01–Q02b). The project-hub-first launch is replaced by a **review-first photostream with bottom-tab navigation**. But the redesign has implications across every other surface — Edit Mode, annotation tools, Trade Kits, engineering symbols, collage, export, voice, video, CMMS, settings, errors, empty states, and every entry point. Each needs its place in the new model worked out.

Building this as one prototype produced a file too big to fine-tune. Splitting into focused mockups lets each interaction get the design attention it deserves — and lets each one inform implementation independently.

---

## Decisions locked in (from redesign Q01 → Q02b)

These are non-negotiable starting points for every mockup. Each mockup must respect all of them.

1. **Cold-launch lands on the unified photostream.** Not a project hub. Not the camera. The body of work is the home.
2. **"Project" is preserved internally but invisible at creation.** ProjectEntity, Room schema, completion-ritual seal, export bundle, CMMS envelope all stay. The user never sees a "+ New Project" affordance — projects auto-create and auto-name from photo bursts.
3. **Bottom-tab home with center shutter.** Three destinations: **Recents** (tab, default) · **Shutter** (center, persistent) · **Projects** (tab).
4. **Recents shows all photos chronologically.** Auto-grouped by time / date / session headers. Project-tagged photos carry a small project badge on the thumbnail; loose photos do not.
5. **Projects shows named / saved / in-progress projects** as cards with status pills (Open · Sealed · Synced) and filter chips. No "+ create" button anywhere.
6. **Camera shutter opens viewfinder full-screen**, captures with auto-metadata, returns the user to the just-captured photo in Browse Mode (primed for Draw=Enter Annotate per IMP-01 / S-19).
7. **Settings, search, and overflow** live behind the menu (`≡`) and search (`⌕`) icons in the brand bar, present on both Recents and Projects tabs.

---

## Open design questions (each resolved by a specific mockup)

| Q | Question | Resolved in |
|---|---|---|
| Q03 | What triggers a new auto-project? | M02 |
| Q04 | What happens when the user taps a photo from Recents? | M05 |
| Q05 | What does the inside of a project card look like when tapped? | M06 |
| Q06 | What does the `/N` brand counter mean? | M01 |
| Q07 | How do loose photos become a named project? | M07 |
| Q08 | What does the search icon search across? | M09 |
| Q09 | How do photos arriving via Android Share Sheet land? | M12 |
| Q10 | Where does the Edit Mode entry button live in the new model? | M16 |
| Q11 | Tool palette: 5 fixed categories or kit-adaptive? | M19 |
| Q12 | Smart Paper: on by default? Confidence threshold for snap? | M29 |
| Q13 | Trade Kit catalog: grid / list / carousel? | M33 |
| Q14 | Engineering symbol pickers: hierarchical tree or flat search? | M36 |
| Q15 | Photo strip — always visible in Browse or summoned? | M41 |
| Q16 | Share Waterfall — kit-adaptive options or fixed list? | M46 |
| Q17 | PDF report — auto-layout or template-based? | M47 |
| Q18 | Collage — photos-first (auto-suggest layout) or layout-first? | M48 |
| Q19 | Evidence Chain view — inline panel or dedicated screen? | M51 |
| Q20 | Multi-select — long-press or explicit "Select" toggle? | M52 |
| Q21 | Permission requests — just-in-time per feature or upfront? | M54 |
| Q22 | Studio Mode — separate UI mode or contextual reveal? | M63 |
| Q23 | Voice annotation entry — hold-anywhere or dedicated tool? | M65 |

---

## Mockup catalog (organized by section)

The mockup IDs are sequential across the whole plan. Each section groups related surfaces.

### Section A — Home shell & navigation (M01–M11)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M01 | Home navigation & tab switching | 1 | Q06 |
| M02 | Recents feed | 1 | Q03 |
| M03 | Projects list | 1 | — |
| M04 | Camera capture flow | 1 | — |
| M05 | Photo view + Browse / Annotate basic | 1 | Q04 |
| M06 | Project-internal view | 1 | Q05 |
| M07 | Loose-to-project promotion | 1 | Q07 |
| M08 | Completion ritual | 1 | — |
| M09 | Search | 1 | Q08 |
| M10 | Settings & menu top-level | 1 | — |
| M11 | First-launch experience | 1 | — |

### Section B — Entry points (M12–M15)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M12 | Share-into-TechNotes (Android Share Sheet handler) | 1 | Q09 |
| M13 | Deep-link entry (project / photo URL) | 2 | — |
| M14 | Notification entry | 3 | — |
| M15 | CMMS context entry (work order → photo) | 3 | — |

### Section C — Edit Mode (M16–M18)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M16 | Edit Mode entry + toolbar + flip / rotate / crop | 1 | Q10 |
| M17 | Edit Mode basic adjustments (brightness / contrast / saturation / exposure) | 1 | — |
| M18 | Edit Mode shaders (vignette / grain / LUT / curves / HSL) | 1 | — |

### Section D — Annotation engine (M19–M32)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M19 | Annotation tool palette structure (5 categories + Text) | 1 | Q11 |
| M20 | Freehand drawing | 1 | — |
| M21 | Lines & line primitives (break / stitch / zigzag / wave / coil / grainline) | 1 | — |
| M22 | Arrows & arrowheads | 1 | — |
| M23 | Circles, rectangles, shapes (with variants) | 1 | — |
| M24 | Polyline & polygon | 1 | — |
| M25 | Stamps & pins (with auto-numbering) | 1 | — |
| M26 | Hatching & revision cloud | 1 | — |
| M27 | Linear dimension lines + calibration session | 1 | — |
| M28 | Angular & radial dimension lines | 1 | — |
| M29 | Smart Paper shape recognition | 1 | Q12 |
| M30 | Color as Gesture (semantic ink) | 1 | — |
| M31 | Text annotation + Quick-label dictionary | 1 | — |
| M32 | Annotation selection & manipulation handles | 1 | — |

### Section E — Trade Kits (M33–M35)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M33 | Trade Kit catalog & discovery | 1 | Q13 |
| M34 | Trade Kit activation, preview, one-tap apply | 1 | — |
| M35 | Trade Kit customization + .tnkit import / export | 1 | — |

### Section F — Engineering symbols (M36–M40)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M36 | Welding symbol picker (AWS A2.4) | 1 | Q14 |
| M37 | GD&T feature control frames | 1 | — |
| M38 | NDT symbols | 1 | — |
| M39 | EU material labels | 1 | — |
| M40 | ISO 7010 safety symbol picker | 1 | — |

### Section G — Photo rhythm & capture modes (M41–M45)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M41 | Photo strip & swipe-between-photos (IMP-06) | 1 | Q15 |
| M42 | Burst mode capture | 1 | — |
| M43 | Ghost overlay capture (alignment) | 1 | — |
| M44 | Soft placement & alignment guides (IMP-09 / IMP-10) | 1 | — |
| M45 | Pinch-to-zoom & two-finger gestures | 1 | — |

### Section H — Export, share, collage, PDF (M46–M51)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M46 | Share / Export Waterfall | 1 | Q16 |
| M47 | PDF report builder | 1 | Q17 |
| M48 | Collage creation flow | 1 | Q18 |
| M49 | Collage layout templates (Before-After / Quad / Hero+Details / Process / Forensic) | 1 | — |
| M50 | Collage cell editing & cross-photo annotations | 1 | — |
| M51 | Photo metadata & Evidence Chain view | 1 | Q19 |

### Section I — Multi-select & batch (M52–M53)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M52 | Multi-select in Recents (selection mode, batch actions) | 1 | Q20 |
| M53 | Multi-select in Projects (batch operations, merge / split / archive) | 1 | — |

### Section J — System states (M54–M57)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M54 | Permission flows (camera, GPS, storage, notification) | 1 | Q21 |
| M55 | Error states & recovery | 1 | — |
| M56 | Empty states catalog | 1 | — |
| M57 | Loading & processing states | 1 | — |

### Section K — Settings deep-dives (M58–M62)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M58 | Account & subscription (Free / Pro / Team) | 2 | — |
| M59 | Trade Kit management settings | 1 | — |
| M60 | Integrations / CMMS settings | 3 | — |
| M61 | Evidence Chain settings (technician ID, SHA-256, C2PA) | 1 | — |
| M62 | Display, accessibility, language, units | 1 | — |

### Section L — Phase 2 advanced features (M63–M67)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M63 | Studio Mode (S-42 — engineering toolkit revealed at desk) | 2 | Q22 |
| M64 | Annotation layers (PD-11 visibility toggle) | 2 | — |
| M65 | Voice annotations (tap-hold-speak) | 2 | Q23 |
| M66 | Voice transcription & linked evidence | 2 | — |
| M67 | Sound diagnostic recording | 2 | — |

### Section M — Phase 2 video & GIF (M68–M71)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M68 | Video capture (CC-06) | 2 | — |
| M69 | Video frame annotation | 2 | — |
| M70 | Video-to-GIF export | 2 | — |
| M71 | Pre-capture buffer / split-second rewind (CC-05) | 2 | — |

### Section N — Phase 2 localization (M72–M74)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M72 | Locale switching (live UI update) | 2 | — |
| M73 | ML-Kit translation overlay | 2 | — |
| M74 | IATE / TBX terminology import | 2 | — |

### Section O — Phase 2 / 3 AI features (M75–M80)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M75 | Anomaly detection (PatchCore) | 2 | — |
| M76 | Object & material detection | 3 | — |
| M77 | OCR & scene text extraction | 2 | — |
| M78 | Auto-cropping (saliency) | 2 | — |
| M79 | Voice-to-structured-annotation | 3 | — |
| M80 | AI-suggested annotations | 3 | — |

### Section P — Phase 3 collaboration (M81–M85)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M81 | Share-for-review flow | 3 | — |
| M82 | Async expert annotation overlay | 3 | — |
| M83 | Real-time sync (CRDT) | 3 | — |
| M84 | Push notifications for collaboration | 3 | — |
| M85 | Conflict resolution UI | 3 | — |

### Section Q — Phase 3 CMMS integration (M86–M95)

| # | Mockup | Phase | Resolves |
|---|---|---|---|
| M86 | CMMS connection setup & OAuth | 3 | — |
| M87 | Work order linking | 3 | — |
| M88 | Asset selection & equipment context | 3 | — |
| M89 | Context Whisper banner | 3 | — |
| M90 | Priority Tint signal | 3 | — |
| M91 | Return Signal updates | 3 | — |
| M92 | Sync queue & status | 3 | — |
| M93 | Sync conflict resolution | 3 | — |
| M94 | Per-adapter UX variations (Procore, MaintainX, UpKeep, ServiceTitan, Maximo, SAP PM) | 3 | — |
| M95 | Zapier / Make webhook configuration | 2 | — |

### Cross-cutting appendices (A1–A10)

Specs, not mockups. Apply across every surface.

| # | Topic |
|---|---|
| A1 | Localization & i18n conventions |
| A2 | Accessibility (WCAG 2.2 AA) |
| A3 | Haptic feedback patterns |
| A4 | Auto-save behaviors |
| A5 | Touch target sizing |
| A6 | Animation timing tokens |
| A7 | Gesture vocabulary & gesture priority |
| A8 | Mode transition patterns |
| A9 | Modal patterns (sheets, dialogs, toasts) |
| A10 | Undo / redo system (mode-specific stacks) |

---

## Recommended build order

**Phase 1 — Core navigation & primary flows (build first, in sequence)**

```
M01 → M02 → M03 → M04 → M05 → M06 → M07 → M08 → M09 → M10 → M11
```

These establish the shell. Every subsequent mockup builds on M01.

**Phase 1 — Entry points (build after M01, parallel-safe)**

```
M12 (Share-into-TechNotes) — earliest priority outside core
```

**Phase 1 — Edit Mode (build after M05)**

```
M16 → M17 → M18
```

**Phase 1 — Annotation engine (build after M05, can branch parallel)**

```
M19 (palette structure) → all of M20–M32 can proceed in parallel
M27, M28 (dimensions) require calibration model — build M27 first
M29 (Smart Paper) requires M20, M22, M23 (primitives must exist)
M32 (selection) is cross-cutting — build after a representative set of primitives
```

**Phase 1 — Trade Kits + Engineering symbols (build after M33, can branch)**

```
M33 → M34 → M35
M36, M37, M38, M39, M40 — parallel after M19
```

**Phase 1 — Photo rhythm, export, multi-select, system states**

```
M41–M45 — after M05
M46–M51 — after M05, M06
M52, M53 — after M02, M03, M06
M54–M57 — cross-cutting, can build at any time
```

**Phase 1 — Settings**

```
M58 (deferred to Phase 2), M59, M61, M62 — Phase 1
M60 (CMMS settings) deferred to Phase 3
```

**Phase 2** — build mockups when those features approach implementation (S-44 onwards):

```
M63 (Studio Mode) — when S-42 implementation starts
M64 (Layers) — already shipped data layer (PD-11), UI mockup early
M65, M66, M67 (Voice) — when S-51-53 approach
M68, M69, M70, M71 (Video) — when S-39 / S-55-57 approach
M72, M73, M74 (Localization) — when S-44 / L10N-02 approach
M75–M80 (AI) — when each AI feature is scoped
M95 (Zapier) — Phase 2 deliverable
```

**Phase 3** — build mockups when CMMS / collab phases start:

```
M81–M85 (Collaboration) — when S-54 approaches
M86–M94 (CMMS) — when S-59–S-64 approach
M13–M15 (deep links, notifications, CMMS context) — Phase 2/3
```

**Appendices** — author in parallel with Phase 1 mockups; each appendix codifies patterns used across multiple mockups, so they should crystallize as the mockups land.

---

## Universal mockup standards

Every mockup file in this series must:

- Be a **single self-contained HTML file** with embedded CSS and JS — no build step.
- Use **Google Fonts** for Plus Jakarta Sans (display) and IBM Plex Mono (mono / data).
- Use the **TechNotes color palette** — dark, monochrome chrome, semantic ink (red / yellow / green / blue) reserved for annotations only.
- Render a **single phone frame** (~380 × 780) on desktop, fill the viewport on mobile.
- Be **mobile-first responsive** — opening on an S25 Ultra should feel real.
- Stub out-of-scope features with **visual feedback only** (no error, no broken state).
- Save to `/mnt/user-data/outputs/` with filename `redesign-MXX-<slug>.html`.

---

# Per-mockup specifications

The mockups below are specified at three depths:
- **Full spec** — Phase 1 mockups (M01–M62 where Phase 1). Goal · Scope IN · Scope OUT · States · Key interactions · Decisions to resolve · Reference docs.
- **Medium spec** — Phase 2 mockups. Goal · Scope · Builds on · Notes · Reference docs. Full spec deferred to when implementation approaches.
- **Light spec** — Phase 3 mockups. Goal · Notes. Full spec deferred to that phase.

---

## Section A — Home shell & navigation

---

### M01 — Home navigation & tab switching

**Goal.** Establish the bottom-tab shell — Recents · Shutter · Projects — and prove the navigation feels right.

**Scope IN.**
- Bottom tab bar with active state, switching between Recents and Projects.
- Top brand bar with logo, search icon, menu icon.
- Tap Shutter → camera view slides up (stub viewfinder, back returns).
- Smooth transition between tabs.

**Scope OUT.**
- Real photo content (use placeholder grid in Recents).
- Real project cards (use placeholder cards in Projects).
- Camera capture (just open + back).
- Search and menu actions (icons visible, no action).

**States to show.**
- Recents tab active (default cold-launch).
- Projects tab active.
- Camera view open (stub).
- Tap-down state on shutter.

**Key interactions.**
- Tap Recents → switch active, swap content.
- Tap Projects → same.
- Tap Shutter → camera slides up.
- Tap back from camera → camera slides down.

**Decisions to resolve.** **Q06.** Brand counter `/14` — keep it, and if so what does it count? (Today's photos? Today's annotations? Active project's photos? Nothing?) · Tab transition style (cross-fade / slide / none).

**Reference docs.** Doc 24 §6 Browse Mode chrome · Doc 02 Mode-Specific Chrome.

---

### M02 — Recents feed

**Goal.** Define the photostream content, layout, grouping logic, and per-photo interactions.

**Scope IN.**
- 3-column photo grid with semantic ink dot badges.
- Auto-grouped section headers (Today · 14:18, Today · 11:02, Yesterday, May 8 …).
- Project-membership badges on thumbnails for photos in named projects.
- Vertical scroll.
- Tap photo → visual transition (full-screen photo in M05 stub).
- Long-press photo → multi-select mode (visual feedback only; actions in M52).
- Empty state (true first-launch).

**Scope OUT.** Photo view itself (M05) · multi-select actions (M52).

**States to show.** Default (15–20 sample photos, 4–5 sections) · Long-press / multi-select active · Empty state with CTA · Scrolled deep.

**Key interactions.** Tap photo → preview opens · Long-press → enter multi-select · Tap more photos in multi-select → toggle.

**Decisions to resolve.** **Q03.** Auto-project boundary trigger — time gap (e.g. > 30 min idle), GPS change (e.g. > 500 m), each cold launch, each calendar day, or a combination · Visual differentiation of loose vs project-tagged photos · Should sealed-project photos look different in the stream?

**Reference docs.** Doc 24 §6 Multi-Photo Strip · Doc 02 Annotation Rhythm · Doc 25 IMP-06 Swipe-Between-Photos.

---

### M03 — Projects list

**Goal.** Define the Projects tab — card layout, status pills, filter chips, sort.

**Scope IN.**
- Vertical list of project cards.
- Each card: cover thumbnail, name, kit badge, meta (count + last modified), status pill.
- Filter chips: All · Open · Sealed · Synced (tappable, single-select).
- Sort affordance.
- Tap card → project-internal view (M06 stub).
- Long-press card → context menu (visual feedback only).
- Swipe-left → quick actions (visual only).
- Empty state.

**Scope OUT.** Project-internal view (M06) · real long-press actions (M06).

**States to show.** Default (4–6 projects, mixed statuses) · Filtered to Open · Filtered to Sealed · Empty state · Long-press revealed.

**Key interactions.** Tap chip → filter · Tap card → preview into M06 · Long-press → context menu · Tap sort icon → sort sheet.

**Decisions to resolve.** Cover image: latest photo OR 4-up composite · Status pill placement and color treatment · Kit badge: chip on card or inline in meta line.

**Reference docs.** Doc 24 §3 Project Hub (re-imagined) · Doc 02 Browse Mode chrome.

---

### M04 — Camera capture flow

**Goal.** Tap shutter → viewfinder → capture → return with new photo.

**Scope IN.**
- Tap shutter → camera view slides up from bottom.
- Full-screen viewfinder (gradient + grid overlay simulating live preview).
- Top: back arrow, flash mode toggle.
- Mode row: Photo (active) · Burst · Video.
- Live metadata strip (GPS, time — fake values).
- Shutter button — tap triggers flash effect + simulated capture.
- After capture: photo appears bottom-left as thumbnail OR lands on photo in Browse (decision below).
- Back arrow → cancel, return to previous tab.

**Scope OUT.** Real camera access · Real burst / video (buttons visible, no action) · Annotation after capture (M05).

**States to show.** Viewfinder ready (GPS locked) · Capture in progress (white flash) · Just-captured (thumbnail visible) · Flash mode cycling.

**Key interactions.** Tap shutter → flash + capture + return · Tap flash → cycle AUTO/ON/OFF · Tap mode → switch active mode · Tap back → return.

**Decisions to resolve.** **Critical:** after capture, where does the user land — back in Recents with new photo at top (review-first feel preserved), or directly on the photo in Browse Mode (Draw=Enter primed) · Should burst mode be visible in v1 or hidden until Phase 2.

**Reference docs.** Doc 24 §5 Photo Capture Flow · Doc 02 Camera Always Warm · CC-01 / CC-02 / CC-03 atom specs.

---

### M05 — Photo view + Browse / Annotate basic

**Goal.** The heart of the app — viewing a photo and drawing on it with semantic ink.

**Scope IN.**
- Tap photo from Recents / Projects → photo opens full-screen.
- Top bar: back, project context (if any), more menu.
- Photo fills the canvas.
- Semantic Ink strip on left edge (vertical, 4 colors).
- Undo / redo pill on left, below ink strip.
- Tool palette at bottom (5 categories + Text — stubbed structure).
- Freehand drawing with finger / pointer → SVG path in active ink color.
- Multi-stroke accumulation in-memory.
- Undo removes last stroke.
- Tap color in ink strip → set active color.
- Swipe horizontally → next / previous photo (within project).
- Photo strip at bottom (multi-photo projects).

**Scope OUT.** Smart Paper recognition (M29) · Engineering symbols (M36–M40) · Text annotation (M31) · Specific tool variants per category (M20–M28) · Pinch-to-zoom (M45 — stub OK).

**States to show.** Default with photo loaded · During an active stroke · After 3–5 strokes (annotations persist) · Each ink color selected · Multi-photo strip visible · Photo with pre-existing annotations on entry.

**Key interactions.** Tap ink color → set active · Drag on photo → draw with active color · Tap undo → remove last stroke · Swipe horizontally → photo navigation · Tap back → return.

**Decisions to resolve.** **Q04.** Does tap-photo land in Annotate (primed) or Browse (drag-to-annotate)? · Ink strip persistent or appears on first drag? · Tool palette always visible or summoned on demand?

**Reference docs.** Doc 02 Interaction Model (full) · Doc 25 IMP-01 Draw=Enter Annotate · Doc 25 IMP-02 Color as Gesture · Doc 25 IMP-07 Frequency-Sorted Ink Strip.

---

### M06 — Project-internal view

**Goal.** When the user taps a project card, what view opens? What can they do inside?

**Scope IN.**
- Project header (cover, name, kit badge, photo count, status, last activity, location).
- Photo grid within project (3-col like Recents, scoped to project).
- Top bar: back, project name, more menu.
- More menu: Rename · Mark Complete · Export · Share · Archive · Reopen (sealed) · Duplicate · Delete.
- Tap photo → opens in Browse / Annotate (M05).
- Trade Kit indicator in the chrome.
- "Mark Complete" → triggers Completion Ritual (M08).

**Scope OUT.** Completion Ritual (M08) · Real export logic (M46/M47) · Real rename keyboard (visual only).

**States to show.** Default open project (~14 photos) · Sealed project (read-only treatment, padlock indicator) · Synced-to-CMMS project (sync badge) · Empty project (just-created) · More menu open · Rename mode active.

**Key interactions.** Tap photo → opens Browse · Tap back → return to Projects · Tap more menu → action sheet · Tap Mark Complete → Completion Ritual · Tap Rename → inline rename.

**Decisions to resolve.** **Q05.** What goes in the project header vs the body? · Trade Kit surface — chip in header, tinted top bar, side indicator? · Sealed project treatment — full lockdown or banner?

**Reference docs.** Doc 24 §11 Trade Kit Flow · Doc 25 IMP-19 Completion Ritual · ON-10 atom.

---

### M07 — Loose-to-project promotion

**Goal.** Take un-projected photos and turn them into a named project.

**Scope IN.**
- Long-press photo in Recents → enter multi-select.
- Selection visualization (checkmarks, photo dimmed).
- Action bar at bottom: Add to project · Share · Delete · Move.
- Tap Add to project → bottom sheet.
- Sheet contents: existing open projects + "New project" + cancel.
- "New project" → name input with auto-suggested name (date + location).
- Confirm → photos move (visual animation), sheet dismisses, optional toast.
- Auto-suggestion path: app surfaces banner "Group these 6 photos as a project?" when a cluster is detected.

**Scope OUT.** Share / Delete / Move actions (M52) · Real project creation · Real GPS clustering (hardcoded for mockup).

**States to show.** Multi-select 1 photo selected · 3 photos selected · Action bar visible · "Add to project" sheet open · New project name input · After confirmation toast · Auto-suggestion banner.

**Key interactions.** Long-press → multi-select · Tap photos → toggle · Tap Add to project → sheet · Tap existing → confirm · Tap New project → name input → confirm · Tap auto-suggestion banner → accept.

**Decisions to resolve.** **Q07.** Auto-suggestion aggressiveness — always shows / dismissible / opt-in via manual flow · Auto-name format — date · location / date · time / domain-aware.

**Reference docs.** Doc 24 §6 Multi-Photo · Doc 17 Trade Kit adoption.

---

### M08 — Completion ritual

**Goal.** The "mark inspection complete" experience — IMP-19 from Doc 25 / ON-10 atom.

**Scope IN.**
- Triggered from M06 More menu → "Mark Inspection Complete".
- Full-screen summary slides up.
- Header: project name + closing time mark.
- Summary stats: photos · annotations · defect/pass breakdown.
- Time on-site (first photo timestamp → last activity).
- Evidence chain stamp (✓ SHA-256 sealed · GPS verified · timestamp chain intact).
- Primary action: Send Full Report (with destination chip — email / Share / configured CMMS).
- Secondary action: Archive.
- Close / cancel returns to project view unsealed.
- After confirm → sealed visual treatment, return to project view, banner toast.
- Auto-trigger path: 15-min inactivity prompts soft entry to the ritual.

**Scope OUT.** Real cryptographic seal (visual only) · Real report generation · Real CMMS push.

**States to show.** Summary loaded · "Send" tapped (loading → success) · After confirm · Auto-trigger inactivity prompt.

**Key interactions.** Tap Send Full Report → loading → success → return · Tap Archive → seal + return · Tap close → discard.

**Decisions to resolve.** Single screen or stepped (review → confirm → send) · Auto-trigger 15-min inactivity yes/no · Can sealed projects be unsealed (per docs: yes, with explicit "Reopen Inspection" creating a new version).

**Reference docs.** Doc 25 IMP-19 · Doc 02 Completion Arc · ON-10 atom · S-30 session blueprint.

---

### M09 — Search

**Goal.** Cross-app search — projects, photos, annotations, metadata.

**Scope IN.**
- Tap search icon → search view opens.
- Input with auto-focus.
- Result categories: Projects · Photos · Annotations.
- Recent searches when input empty.
- Filter chips (date range, kit type, GPS area, status).
- Tap result → deep-link to that surface.
- Back → close search.

**Scope OUT.** Real search backend (hardcoded results) · OCR / AI (visual only) · Voice search (visible, no action — M77 / M79).

**States to show.** Empty (recent + suggestions) · Typing with live results · One filter applied · No results · Result tapped (deep-link feedback).

**Key interactions.** Type → results update · Tap chip → filter · Tap result → close + navigate · Tap clear → reset.

**Decisions to resolve.** **Q08.** Search scope — metadata only (cheap, fast) / + annotation text / + OCR (Phase 2, expensive) / all-staged · Voice search affordance visible now or wait?

**Reference docs.** Doc 24 §3 Search · ON-06 atom.

---

### M10 — Settings & menu top-level

**Goal.** Settings surface from the brand bar menu icon.

**Scope IN.**
- Tap `≡` → side drawer or full-screen menu (decided here).
- Sections: Account · Trade Kits · Integrations · Annotation Defaults · Evidence Chain · Display & Accessibility · About.
- Each section: navigates to its own pane (M58–M62).
- Back → close menu.

**Scope OUT.** Real persistence (in-memory) · CMMS connection (M60) · Sign-in (M58).

**States to show.** Menu top-level · One section entered · After a setting change.

**Key interactions.** Tap section → enter pane · Toggle → visual feedback · Back → close.

**Decisions to resolve.** Drawer vs full-screen · Section order.

**Reference docs.** Doc 24 §15 Settings · ON-07 atom.

---

### M11 — First-launch experience

**Goal.** What does the user see on a true first install? Is there onboarding at all?

**Scope IN.**
- Cold launch on brand-new install.
- Empty Recents tab.
- Single-line CTA: "Tap the shutter to take your first photo".
- After first photo: optional silent kit detection.
- After first annotation: optional one-line hint about features.
- No multi-step questionnaire (departure from current Doc 24 §2).

**Scope OUT.** Real account creation · Real kit detection (stub) · Onboarding video / tour.

**States to show.** Empty Recents · After tapping shutter (M04 flow) · After first capture · After first annotation.

**Key interactions.** Tap shutter → camera (M04) · Capture → photo lands in Recents · Tap new photo → Browse → first drag draws · Subtle hints fade.

**Decisions to resolve.** Any onboarding at all, or just empty-state hints? · Keep IMP-04 (Situation-First photo-recognition tiles) or drop entirely? · Kit detection silent (auto-applied) or surfaced as one-tap suggestion?

**Reference docs.** Doc 24 §2 First-Time Onboarding (overhauled) · Doc 25 IMP-04 · Doc 17 60-second flow.

---

## Section B — Entry points

---

### M12 — Share-into-TechNotes (Android Share Sheet handler)

**Goal.** Handle photos arriving from other apps via Android Share Sheet.

**Scope IN.**
- App opens with an incoming photo.
- "Add to TechNotes" landing — minimal chrome.
- Two paths: (a) loose in Recents → tap to dismiss / continue, (b) Add to existing project → bottom sheet picker.
- Optional inline rename / immediate annotate.

**Scope OUT.** Real share-intent registration · Project creation (M07).

**States to show.** Photo received, choice presented · After choice (lands in Recents / opens project).

**Key interactions.** Tap "Add to Recents" → photo in Recents, app opens at Recents · Tap "Add to project" → picker · Tap "Annotate now" → opens photo in M05.

**Decisions to resolve.** **Q09.** Default destination — Recents (loose) or always-prompt (picker)? · Should the app auto-open the photo for annotation, or just file it?

**Reference docs.** Doc 24 §1 Share Intent · S-49d hub entry points.

---

### M13 — Deep-link entry (project / photo URL)

**Goal.** Open a specific photo or project from a URL link (sharing, notifications, in-app).

**Phase.** 2.

**Scope.** Tap link → app opens directly to the photo (M05) or project (M06). Authentication prompt if the resource is team-tier. Permission check for sealed projects.

**Builds on.** M05, M06.

**Notes.** Hashable URL scheme TBD. Likely `technotes://photo/{uuid}` and `technotes://project/{uuid}`. Web fallback to a viewer page (post-v1).

**Reference docs.** Doc 14 Integration Layer · Doc 18 Implementation Infrastructure.

---

### M14 — Notification entry

**Goal.** Tapping a TechNotes notification opens the relevant surface.

**Phase.** 3.

**Scope.** Notification types: sync complete, collaboration update, CMMS context arrived, completion ritual reminder (15-min inactivity), report sent. Each opens its appropriate surface with context preserved.

**Builds on.** M01 + relevant target surface.

**Notes.** Notification design itself is OS-level; this mockup covers the in-app landing.

**Reference docs.** Doc 04 Future Vision · Phase 3 atoms.

---

### M15 — CMMS context entry (work order → photo)

**Goal.** From a CMMS work order, jump into TechNotes pre-loaded with context (asset, work order ID, location).

**Phase.** 3.

**Scope.** App opens camera with metadata pre-populated. Save → photo carries work-order linkage in sidecar. Context Whisper banner shows work order details.

**Builds on.** M04, M89.

**Notes.** Per-adapter URL schemes vary. Generic UX is one mockup; per-adapter variants are M94.

**Reference docs.** Doc 14 Integration Layer · Doc 53 CMMS Integration · S-59–S-64 atoms.

---

## Section C — Edit Mode

---

### M16 — Edit Mode entry + toolbar + flip / rotate / crop

**Goal.** Establish how the user enters Edit Mode in the new home, and the toolbar structure for the basic operations.

**Scope IN.**
- Entry from photo view (M05) — long-press photo or tap "Edit" in More menu.
- Edit Mode top bar: ✕ Cancel, ✓ Done.
- 6-slot edit toolbar at bottom: Flip · Rotate · Crop · Adjust · Overlay · More.
- Tap Flip → submenu (H / V) → apply.
- Tap Rotate → submenu (90° CCW / 90° CW / Free) → apply.
- Tap Crop → crop overlay with aspect-lock chips (Free / 1:1 / 4:3 / 16:9) + rule-of-thirds grid + draggable handles.
- Annotations transform with photo (visual feedback).
- Save → photo updated, annotations remapped, Edit Mode closed.

**Scope OUT.** Adjustments sliders (M17) · Shaders (M18) · Overlay (Phase 2 stub).

**States to show.** Edit Mode entered · Flip submenu open · Rotate submenu open · Crop overlay with handles · After crop applied (annotations remapped).

**Key interactions.** Tap toolbar slot → submenu / overlay · Manipulate crop handles · Tap ✓ Done → save · Tap ✕ Cancel → discard.

**Decisions to resolve.** **Q10.** Entry from photo view — long-press? More menu? Both? Dedicated icon? · Annotation remap UX during transform — instant or after-confirm? · Aspect-lock chip set.

**Reference docs.** Doc 02 EDIT mode · Doc 24 §10 Edit Mode · EP-01 / EP-02 atoms · S-22 / S-49b-edit-remap.

---

### M17 — Edit Mode basic adjustments

**Goal.** Brightness / Contrast / Saturation / Exposure inline sliders.

**Scope IN.**
- Tap Adjust in M16 toolbar → inline slider panel.
- Four sliders, named, with center-zero handle.
- Real-time preview as user drags.
- Reset per-slider (long-press handle).
- ✓ to commit, ✕ to revert.
- Non-destructive (original preserved).

**Scope OUT.** Shaders (M18) · LUT (M18).

**States to show.** Slider panel open · Mid-drag preview · After reset · After commit.

**Key interactions.** Drag slider → live preview · Long-press handle → reset · Tap ✓ → commit · Tap ✕ → revert.

**Decisions to resolve.** Slider style (single thumb, center-zero, gradient track) · Tap-to-text-input override for power users.

**Reference docs.** EP-03 atom · S-22 session.

---

### M18 — Edit Mode shaders (vignette / grain / LUT / curves / HSL)

**Goal.** Advanced photo adjustments via AGSL shaders. Your S-41 work, where LUT engine is shipped but UI wire-up is pending.

**Scope IN.**
- From M16 toolbar tap More → expanded toolbar with: Vignette · Grain · LUT · Curves · HSL.
- Each opens its own slider / picker panel.
- Vignette: amount + feather.
- Grain: intensity + size.
- LUT: filter strip (presets) + intensity slider.
- Curves: tone-curve graph (four points draggable).
- HSL: per-channel Hue / Saturation / Lightness sliders.
- Real-time preview.

**Scope OUT.** Custom LUT import (post-v1) · Layer stacking (post-v1).

**States to show.** Each shader panel open · Mid-adjustment preview · Multiple shaders applied · Reset state.

**Key interactions.** Drag sliders · Drag curve points · Tap LUT preset → apply · Tap ✓ → commit · Tap ✕ → revert.

**Decisions to resolve.** Should shader panels stack or be exclusive? · LUT preset library — how many ship in v1? · Curves UI — phone-friendly enough or relegate to Studio Mode?

**Reference docs.** EP-04 / EP-05 / EP-06 atoms · S-41a / S-41b / S-41c sessions · S-41c-wire (current next session).

---

## Section D — Annotation engine

---

### M19 — Annotation tool palette structure

**Goal.** The 5-category + Text bottom palette that Annotate Mode shows. Establishes the entry to every drawing primitive.

**Scope IN.**
- Bottom palette appears in Annotate Mode (M05 entered drawing).
- 5 category slots + Text button.
- Categories: Pen / Shape / Line / Stamp / Symbol (or trade-kit-mapped slots).
- Tap category → variant tray slides up (3–5 variants per category).
- Long-press category → properties sheet (line weight, dash, fill).
- Active tool highlighted.
- Tool persists until explicitly switched (Bluebeam pattern).

**Scope OUT.** Specific tool drawing (M20–M28) · Engineering symbols (M36–M40) · Quick-label dictionary (M31).

**States to show.** Palette with default tools · Variant tray open · Properties sheet open · After tool selected (highlighted).

**Key interactions.** Tap category → variant tray · Tap variant → set active tool · Long-press → properties · Active tool persists.

**Decisions to resolve.** **Q11.** 5 fixed categories or kit-adaptive (palette reconfigures per active Kit)? · Category icon style (line art vs filled) · Variant tray opening direction (up / down / overlay).

**Reference docs.** Doc 02 PROGRESSIVE DISCLOSURE · IC-04 / IC-05 / IC-06 atoms · S-12 session.

---

### M20 — Freehand drawing

**Goal.** Raw freehand stroke with smoothing.

**Scope IN.**
- Pen tool active.
- Drag → smooth Catmull-Rom curve.
- Stroke width determined by tool properties.
- Color from active Semantic Ink.
- Multiple strokes accumulate.

**Scope OUT.** Smart Paper recognition (M29) — freehand stays freehand here.

**States to show.** During stroke · After multiple strokes · With pressure variation (S Pen).

**Key interactions.** Pointer-down → stroke begin · Move → append points · Up → finalize.

**Decisions to resolve.** Stroke smoothing aggressiveness (raw / mild / heavy) · Pressure-sensitivity treatment.

**Reference docs.** DP-30 atom · CatmullRomSmoother spec · S-16 session.

---

### M21 — Lines & line primitives

**Goal.** Every line primitive — straight line, break lines (zigzag / wave), stitch line, zigzag inline, coil/spring, wave/sine, grainline arrow.

**Scope IN.**
- Line tool category opens variant tray with: Straight · Break-zigzag · Break-wave · Stitch · Zigzag · Coil · Wave · Grainline.
- Tap variant → active.
- Tap-tap (two-point) creates the line; for some variants (coil, wave), drag with length.
- Properties: line weight, dash pattern, end caps.

**Scope OUT.** Arrows (M22) · Dimensions (M27, M28).

**States to show.** Each variant rendering · Property changes applied · Multiple lines in a scene.

**Key interactions.** Tap variant → active · Two-tap or drag → place · Long-press placed → properties / delete.

**Decisions to resolve.** Two-tap or drag for each variant (consistency vs natural feel) · Grainline arrow — footwear-kit-only or universal?

**Reference docs.** DP-01 / DP-06 to DP-12 atoms · S-14 / S-31 sessions.

---

### M22 — Arrows & arrowheads

**Goal.** Arrows with selectable arrowheads.

**Scope IN.**
- Arrow tool category with variants: Standard · Open · Closed · Double-head · Leader.
- Tap-tap creates the arrow.
- Arrowhead style is a property (8 styles per Doc 02).
- Color from Semantic Ink.

**Scope OUT.** Flow arrows (M40 stub but build here if simpler) · Dimension arrows (M27).

**States to show.** Each arrowhead style · Single-head, double-head · With leader (label attached) · Color variations.

**Key interactions.** Place arrow (two-tap) · Long-press → arrowhead picker · Long-press → resize handles.

**Decisions to resolve.** Default arrowhead per Trade Kit · Leader auto-label-placement integration with M44.

**Reference docs.** DP-03 atom · S-15 session.

---

### M23 — Circles, rectangles, shapes

**Goal.** Closed-shape primitives with variants.

**Scope IN.**
- Shape category with: Circle · Ellipse · Rectangle · Rounded-rect · Triangle · Diamond · Star (subset per kit).
- Drag-to-size or tap-tap.
- Fill: empty / filled / hatched.
- Properties: line weight, fill, corner radius.

**Scope OUT.** Hatching detail (M26) · Polygon (M24).

**States to show.** Each shape · Fill variations · Color variations · Resized.

**Key interactions.** Drag → size · Long-press → properties / fill picker · Tap to select → resize / move handles.

**Decisions to resolve.** Drag-vs-tap-tap consistency · Fill defaults per Semantic Ink color (red filled = solid defect block, etc.?).

**Reference docs.** DP-18 / DP-20–29 atoms · S-14 / S-32 sessions.

---

### M24 — Polyline & polygon

**Goal.** Multi-point line / closed polygon for irregular shapes (area marking, boundary tracing).

**Scope IN.**
- Polyline tool: tap points, double-tap to end (open).
- Polygon tool: tap points, tap-near-start to close (snap-to-close).
- Visual preview of in-progress line.
- Vertex handles after placement (drag to edit).

**Scope OUT.** Area measurement (M27 may overlap) · Cross-photo polygons (M50).

**States to show.** Mid-creation (3+ points placed) · Snap-to-close indicator · Completed polygon · After edit (vertex moved).

**Key interactions.** Tap → place vertex · Double-tap → end · Tap-near-start → close polygon · Long-press vertex → delete / move.

**Decisions to resolve.** Snap-to-close threshold (px) · Vertex handle size (touch target) · Visual treatment of closing snap.

**Reference docs.** DP-31 / DP-32 atoms · S-16 session.

---

### M25 — Stamps & pins (with auto-numbering)

**Goal.** Single-tap placement of stamps and numbered pins, with auto-numbering across photos.

**Scope IN.**
- Stamp tool with library: ✓ Pass · ✗ Fail · ! Caution · ? Query · Custom (kit-defined).
- Tap on photo → places stamp at point.
- Pin tool with auto-numbering: tap → D-001, next tap → D-002.
- Prefix configurable per kit (D / E / P for electrical / plumbing).
- Renumber on delete with 200 ms slide animation.

**Scope OUT.** Custom stamp creation (post-v1) · Renumber across burst (M41 will handle carry-forward).

**States to show.** Stamp library open · Stamp placed · Pin placed (D-001) · Multiple pins (D-001, D-002, D-003) · After delete (renumbered).

**Key interactions.** Tap library → select · Tap on photo → place · Long-press placed → delete · Delete mid-sequence → renumber animation.

**Decisions to resolve.** Stamp size (fixed in image space or screen space) · Pin prefix default for universal toolbar (no kit) · Numbering behavior across multi-photo (M41 will refine).

**Reference docs.** DP-25–28 atoms · S-17 session.

---

### M26 — Hatching & revision cloud

**Goal.** Hatching (fill pattern within boundary) and revision cloud (scalloped border).

**Scope IN.**
- Hatching tool: tap within a drawn boundary (closed polygon, rectangle, circle) → hatch fills it.
- Hatch angle, spacing, line weight in properties.
- Cloud tool: drag rectangle → scalloped cloud border (architectural revision marking).
- Cloud bulge / scallop count properties.

**Scope OUT.** Hatching across non-closed shapes · Cloud over freehand polygon (M24).

**States to show.** Hatched rectangle · Hatched circle · Hatched polygon · Revision cloud over rectangle · Cloud with text label inside.

**Key interactions.** Tap inside closed shape → hatch fills · Drag rectangle in cloud tool → scalloped border · Long-press → properties.

**Decisions to resolve.** Hatch fill clipping to boundary precision · Cloud scallop count (auto-derived from perimeter vs fixed).

**Reference docs.** DP-33 / DP-34 atoms · S-17 session.

---

### M27 — Linear dimension lines + calibration session

**Goal.** Calibrated linear measurements on photos. Requires a calibration session before measurements are meaningful.

**Scope IN.**
- First-use prompt: "Calibrate scale to make measurements accurate" → calibration UI.
- Calibration: tap two points on a known-dimension reference (ruler in scene, asset of known size), type real-world length.
- Calibration scale stored with project.
- Dimension line tool: tap-tap to place, auto-renders extension lines + measurement label (e.g. 12.5 mm).
- Label position auto-placed (avoids overlap) and draggable after.

**Scope OUT.** Angular / radial (M28) · Cross-photo measurements (M50) · Area measurement (M27 may cover or split into M27b).

**States to show.** Pre-calibration prompt · Calibration in progress (two-tap on reference) · Calibration complete (scale stored) · Linear dimension placed · Multiple dimensions in a scene.

**Key interactions.** Tap calibration prompt → enter session · Tap-tap on reference + enter value → confirm · Place dimensions → tap-tap → label + extension lines.

**Decisions to resolve.** Calibration prompt timing (first dimension use? project start? settings?) · Per-photo vs per-project calibration scope · Area measurement — own tool or derived from polygon (M24)?

**Reference docs.** DP-13 atom · PD-15 calibration model · S-18 / S-33 sessions.

---

### M28 — Angular & radial dimension lines

**Goal.** Angular dimensions (three-tap defining the angle) and radial / diameter dimensions (for circles).

**Scope IN.**
- Angular tool: three-tap (vertex + two rays). Renders arc + degree label.
- Radial tool: tap on placed circle → renders radius arrow with R= label.
- Diameter tool: tap on placed circle → renders diameter line with Ø= label.
- Handles acute, right, and obtuse angles.

**Scope OUT.** GD&T frames (M37).

**States to show.** Acute angle · Right angle · Obtuse angle · Radial · Diameter · Combined with linear (M27).

**Key interactions.** Three-tap → angular · Tap on circle → radial / diameter via picker.

**Decisions to resolve.** Default for tap-on-circle — radial or diameter? · Arc rendering side (inside / outside the angle).

**Reference docs.** DP-14 / DP-15 / DP-17 atoms · S-18 / S-33 sessions.

---

### M29 — Smart Paper shape recognition

**Goal.** Freehand stroke → recognized geometric shape (circle, arrow, X, rectangle, triangle, line). 200 ms snap preview + haptic confirm + tap-to-correct picker.

**Scope IN.**
- Freehand stroke completes → ShapeRecognizer runs (< 50 ms).
- If confidence > 0.7 → 200 ms snap preview (semi-transparent fitted shape) → haptic CONFIRM → replace freehand with fitted element.
- If confidence ≤ 0.7 → keep freehand.
- After snap, tap shape → 4-alternative picker (top 4 matches + Freehand) → tap alt to replace · auto-dismiss 1.5 s.

**Scope OUT.** Engineering symbol recognition (out — those need explicit tool selection).

**States to show.** Stroke being drawn · Snap preview · After snap (recognized shape) · 4-alternative picker open · After correction.

**Key interactions.** Draw → snap auto · Tap snapped → picker · Tap alt → replace · Wait → picker auto-dismiss.

**Decisions to resolve.** **Q12.** Smart Paper on by default? · Confidence threshold for snap (0.7 per docs — keep or tune?) · Picker dismiss timing (1.5 s appropriate?) · Visual treatment of snap preview.

**Reference docs.** IC-14 atom · S-19 session (already shipped in Phase 1).

---

### M30 — Color as Gesture (semantic ink)

**Goal.** Set the active Semantic Ink color via gesture, not just by tapping a chip.

**Scope IN.**
- Option A — Slide-from-strip: touch begins on color strip → slide to canvas in one unbroken motion → stroke begins with that color.
- Option B — S Pen pressure: light = blue (info), medium = yellow (caution), firm = red (defect).
- Color strip remains as tap-to-select fallback and as current-color indicator.
- Frequency-sorted strip (IMP-07): after 5 annotations, most-used color slides to most thumb-accessible position.

**Scope OUT.** Custom color picker (post-v1) · Trade-kit-specific color libraries (M33 / M59).

**States to show.** Color strip default order · After 5 strokes (resorted) · Slide-from-strip gesture in progress · S Pen pressure visualization (only on stylus).

**Key interactions.** Slide-from-strip → color applied to next stroke · Tap chip → set active · S Pen pressure during stroke → live color change (advanced — possibly defer).

**Decisions to resolve.** S Pen pressure mapping — keep as Option B or simplify to slide-only? · Frequency-sort trigger threshold (5 strokes? per session?).

**Reference docs.** IC-06 / IC-21 atoms · Doc 25 IMP-02 / IMP-07 · S-12 / S-19 sessions.

---

### M31 — Text annotation + Quick-label dictionary

**Goal.** Text annotation with the Quick-label dictionary surfacing before the keyboard (IMP-05).

**Scope IN.**
- Tap Text button or double-tap blank canvas → text cursor opens at tap location.
- Quick-label strip slides up BEFORE keyboard appears — horizontally scrollable, 12 most-used labels (frequency-sorted, recency-boosted, kit-seeded).
- Tap label → label inserted, keyboard never opens.
- Or tap keyboard area → keyboard opens for typing.
- Photo-aware label placement: text auto-placed avoiding annotations + photo saliency (IMP-09).
- Live preview bubble at annotation point as user types (IMP-08).
- Photo compresses to top 50 % when keyboard opens.

**Scope OUT.** OCR / scene-text suggestion (M77) · Multi-language autocomplete (Phase 2).

**States to show.** Text cursor placed · Quick-label strip visible (pre-keyboard) · Keyboard up + photo compressed · Live preview bubble during typing · Multi-tap from strip rapid-entry.

**Key interactions.** Tap text spot → cursor + quick-label strip · Tap label → insert · Tap keyboard → type · Type → live preview bubble.

**Decisions to resolve.** Quick-label strip count (12 from docs — keep?) · Frequency vs recency weighting · Strip dismiss when keyboard used.

**Reference docs.** IMP-05 / IMP-08 / IMP-09 · IC-13 / IC-31 atoms · S-20 session.

---

### M32 — Annotation selection & manipulation handles

**Goal.** Select a placed annotation, see its bounding box + handles, move / resize / rotate / delete.

**Scope IN.**
- Tap on placed annotation → selection state (blue outline + 8 resize handles + rotation handle).
- Drag body → move.
- Drag handle → resize (with aspect lock on shape-specific tools).
- Drag rotation handle → rotate.
- Delete button on selection (and / or shake / swipe-up).
- Properties button → property panel (color, line weight, fill, etc.).
- Tap elsewhere → deselect.
- Selection is always in screen-space for handle rendering, but element coords are image-space.

**Scope OUT.** Multi-select annotations (M52 mainly) · Cross-photo selection.

**States to show.** Default (no selection) · Single annotation selected · Resizing in progress · Rotating in progress · Properties panel open · After delete.

**Key interactions.** Tap → select · Drag body / handle / rotation · Tap delete → remove · Tap properties → panel · Tap elsewhere → deselect.

**Decisions to resolve.** Handle size at various zoom levels (always screen-space — keep?) · Rotation snap angles (15° / 45° / 90°) · Delete confirmation for important annotations.

**Reference docs.** CE-07 / CE-08 atoms · S-08 / S-15 / S-49b sessions.

---

## Section E — Trade Kits

---

### M33 — Trade Kit catalog & discovery

**Goal.** How does the user find Kits? Browse, search, see what they do.

**Scope IN.**
- Entered from Settings → Trade Kits, or from ⋯ More tray in toolbar.
- Catalog layout — decision in this mockup.
- Each kit tile: cover, name, description, # tools added, sample symbols, kit author / source.
- Tap tile → preview screen (M34).
- Categories: Industrial · Construction · Inspection · Manufacturing · Design · Community.

**Scope OUT.** Activation flow (M34) · Customization (M35) · Community kits (Phase 2).

**States to show.** Catalog grid (default) · Each category active · Search active · Kit tile pressed (preview hint).

**Key interactions.** Tap tile → preview · Tap category filter · Search → filter list.

**Decisions to resolve.** **Q13.** Layout — grid (visual), list (info-dense), or carousel (one big at a time)? · Kit count per category at v1 ship · How are community kits surfaced (separate section / mixed in)?

**Reference docs.** Doc 17 Trade Kit adoption · TK-01 / TK-02 / TK-03 atoms · S-23 session.

---

### M34 — Trade Kit activation, preview, one-tap apply

**Goal.** Tap a kit tile → see what it adds → activate with one tap.

**Scope IN.**
- Preview screen: kit name, description, list of tools added, sample workflow templates, ink color overrides.
- Big "Activate Kit" button.
- "Activate" → toolbar updates immediately (no restart / loading) + 5 s undo toast.
- After activation, kit indicator appears in chrome (M06 header, etc.).

**Scope OUT.** Customization within kit (M35).

**States to show.** Preview default · Activate pressed (transition) · Toast active · After dismiss (kit active in toolbar).

**Key interactions.** Read preview · Tap Activate → toolbar updates · Tap "Undo" in toast → revert.

**Decisions to resolve.** Multi-kit activation rules — additive or exclusive? · Default kit on first launch — universal (no domain symbols) or auto-detected.

**Reference docs.** TK-04 atom · S-23 session.

---

### M35 — Trade Kit customization + .tnkit import / export

**Goal.** Customize an active kit (reorder tools, override defaults), export as .tnkit, import from file / QR.

**Scope IN.**
- "Customize" button on an active kit.
- Reorder tool slots (drag handles).
- Override defaults (ink color per slot, line weight, stamp set).
- Export → generates .tnkit file → Share Sheet.
- Import → file picker → .tnkit → confirm + activate.
- QR code generation (post-v1 stub).

**Scope OUT.** Community publishing (Phase 2).

**States to show.** Customize mode (drag handles visible) · After reorder · Export confirmation · Import file picker · Import preview · After import (kit activated).

**Key interactions.** Tap Customize → enter mode · Drag tool slot → reorder · Tap slot → override defaults · Export / Import via standard sheets.

**Decisions to resolve.** Reorder slot constraints (lock universal tools in fixed positions, only kit slots reorderable?) · .tnkit version compatibility messaging.

**Reference docs.** TK-04 / TK-06 atoms · S-23 session.

---

## Section F — Engineering symbols

---

### M36 — Welding symbol picker (AWS A2.4)

**Goal.** Pick a welding symbol when the Welding Kit is active.

**Scope IN.**
- From M19 palette, an extra "Welding" category.
- Tap → picker: weld joint types, supplementary symbols, finish symbols, etc.
- Hierarchical OR flat search.
- Search box for symbol name / code.
- Tap symbol → tap on photo → places symbol with reference line + tail.
- Symbol parameters (size, angle, finish) editable post-placement.

**Scope OUT.** Other engineering symbols (M37–M39).

**States to show.** Picker default (hierarchical view) · Search active · Symbol selected, ready to place · Placed on photo · After parameter edit.

**Key interactions.** Open picker · Browse / search · Select symbol · Place on photo · Long-press placed → edit parameters.

**Decisions to resolve.** **Q14.** Picker structure — hierarchical (faster scanning if you know the symbol family) or flat search-first (faster if you know the name) or both? · Reference-line auto vs manual.

**Reference docs.** DP-39 / DP-40 / DP-41 / DP-42 atoms · S-35 / S-36 sessions.

---

### M37 — GD&T feature control frames

**Goal.** Place geometric dimensioning & tolerancing frames (per ASME Y14.5).

**Scope IN.**
- GD&T category in palette.
- Tap → frame builder: tolerance symbol selector (concentricity, parallelism, perpendicularity, position, etc.) + value entry + datum references.
- Renders standard feature control frame (e.g. ⏥ 0.05 ).
- Frame attaches to feature via leader.

**Scope OUT.** Auto-detection of GD&T from existing dimensions.

**States to show.** Frame builder open · Symbol selected · Value entered · Datums added · Frame placed · Leader attached.

**Key interactions.** Build frame step-by-step · Place via leader / tap.

**Decisions to resolve.** Frame builder layout (single screen with all fields vs stepped) · Symbol library coverage (full ASME Y14.5 vs subset).

**Reference docs.** DP-43 / DP-44 atom · S-35 session.

---

### M38 — NDT symbols

**Goal.** Non-destructive testing notations (UT thickness readings, RT shots, PT/MT indications, defect markers).

**Scope IN.**
- NDT category in palette.
- Sub-tools: UT thickness reading · RT shot location · PT indication · MT indication · Defect type marker.
- Each places a specialized annotation with metadata fields (depth, value, reference standard).

**Scope OUT.** Real NDT instrument linkage.

**States to show.** Each sub-tool active · Each annotation placed · Multiple readings on a photo.

**Key interactions.** Pick sub-tool · Place + enter metadata.

**Decisions to resolve.** Metadata field set per tool · Default reference standard per kit.

**Reference docs.** DP-45 / DP-46 atoms · S-36 session.

---

### M39 — EU material labels

**Goal.** EU labeling grids — material composition, care symbols, country of origin (for footwear / garment / consumer goods).

**Scope IN.**
- Material label tool in Footwear & Garment Kit.
- Picker: composition grid, care symbol set, origin selector.
- Renders standard EU label format on photo.

**Scope OUT.** Real material database (manual entry).

**States to show.** Composition grid built · Care symbol set selected · Origin selected · Label rendered.

**Key interactions.** Build label step-by-step.

**Decisions to resolve.** Label format presets · Localization of label content.

**Reference docs.** DP-47 / DP-48 atoms · S-37 session.

---

### M40 — ISO 7010 safety symbol picker

**Goal.** ISO 7010 safety symbols — ~200 SVGs covering Prohibition (P), Warning (W), Mandatory (M), Safe condition (E), Fire (F).

**Scope IN.**
- ISO 7010 category in palette (or in Universal toolbar since safety is cross-kit).
- Hierarchical picker by category color (P/W/M/E/F).
- Each symbol: ID, localized name, SVG, category.
- Searchable in all locales (per L10N-04).
- Tap → place on photo.

**Scope OUT.** Non-ISO custom safety symbols.

**States to show.** Picker by category · Search active · Localized names (current locale) · Symbol placed · Resized.

**Key interactions.** Navigate categories · Search · Tap to place.

**Decisions to resolve.** Default place size · Localization of symbol metadata.

**Reference docs.** L10N-04 atom · DP-49 atom · S-25 session.

---

## Section G — Photo rhythm & capture modes

---

### M41 — Photo strip & swipe-between-photos

**Goal.** The photo strip at the bottom of Browse Mode, and the swipe-between-photos gesture (IMP-06).

**Scope IN.**
- Photo strip below current photo in M05 — horizontal scroll of thumbnails.
- Active photo highlighted.
- + Add photo at end (opens camera).
- Tap thumbnail → switches displayed photo.
- Swipe-between-photos: fast edge-initiated horizontal swipe (> 600 dp/s, starting from edge 15 %, spanning > 60 %) navigates next / previous photo without leaving Annotate Mode.
- Carry-forward applies: tool, color, size, equipment ID, defect numbering (D-003 → D-004 on swipe).

**Scope OUT.** Multi-photo selection (M52).

**States to show.** Strip with multiple thumbnails · Mid-swipe transition · After swipe (new photo + carry-forward applied) · Swipe gesture distinguished from drawing (no false-positive demonstration).

**Key interactions.** Tap thumbnail → switch · Swipe edge → next / previous · Long-press thumbnail → photo-level context menu (cover, delete, copy annotations to next).

**Decisions to resolve.** **Q15.** Strip always visible in Browse Mode or summoned via gesture? · Swipe threshold and zone — tuneable per user, or fixed? · Visual cue for swipe (subtle edge hint?).

**Reference docs.** IMP-06 · Doc 02 Multi-Photo · S-09 / S-19 sessions.

---

### M42 — Burst mode capture

**Goal.** Burst mode in M04 — shutter stays in viewfinder, photos accumulate, carry-forward applies.

**Scope IN.**
- Burst mode active (selected from M04 mode row).
- Tap shutter → photo captured, stays in viewfinder, thumbnail strip accumulates.
- Equipment ID + defect numbering carry across burst.
- "Done" button to exit burst → review captured.

**Scope OUT.** Pre-capture buffer (M71).

**States to show.** Burst mode active · After 3 captures · After 8 captures · Review at exit.

**Key interactions.** Tap shutter repeatedly · Tap Done → review · Tap thumbnail → preview / exclude.

**Decisions to resolve.** Burst storage strategy (auto-project or stay loose) · Max burst count.

**Reference docs.** Doc 02 Annotation Rhythm · S-21 session.

---

### M43 — Ghost overlay capture

**Goal.** When recapturing the same scene (before/after), show a faint overlay of the previous photo for alignment.

**Scope IN.**
- In M04, if previous photo exists, "Ghost overlay" toggle in top bar.
- Toggle ON → semi-transparent previous photo over viewfinder.
- Adjust opacity slider.
- Tap shutter → captures aligned photo.

**Scope OUT.** Auto-alignment detection (Phase 2/3 AI).

**States to show.** Toggle off (normal viewfinder) · Toggle on (overlay visible) · Opacity adjusted · After capture (aligned).

**Key interactions.** Tap toggle · Drag opacity slider · Tap shutter → capture.

**Decisions to resolve.** Default opacity · Which "previous photo" — last in project / last in burst / user-pick.

**Reference docs.** Doc 04 Future Vision (Ghost overlay) · CC-04 atom.

---

### M44 — Soft placement & alignment guides

**Goal.** Soft placement (annotation draggable for 1.5 s after placement, IMP-10) and alignment guides (Figma-style 1px red guides at 12px proximity, IMP-09).

**Scope IN.**
- Place an annotation → pulse animation for 1.5 s indicating it's still draggable.
- Drag during pulse → moves easily; after 1.5 s, drag requires deliberate selection (M32).
- Approaching another annotation (12 dp proximity) → 1 px red guide line + haptic CLOCK_TICK.
- Equal-spacing indicators when three annotations align.
- Speed-based escape: fast drag (> 400 dp/s) bypasses snap.
- Two-finger drag bypasses all snapping.

**Scope OUT.** Smart label placement (covered in M31).

**States to show.** Just-placed annotation (pulsing) · Drag during soft window · Soft window expired · Alignment guide visible · Equal-spacing indicator · Fast-drag bypass.

**Key interactions.** Place → soft drag · Approach another → snap → release · Fast drag → no snap.

**Decisions to resolve.** Soft placement duration tuneable in settings (0.5–3 s per Doc 02) · Guide thickness / opacity · Haptic intensity (configurable per accessibility).

**Reference docs.** CE-10 / IC-30 atoms · IMP-09 / IMP-10 · S-20 session.

---

### M45 — Pinch-to-zoom & two-finger gestures

**Goal.** The cross-cutting two-finger contract: pinch = zoom, drag = pan, double-tap = undo (IMP universal).

**Scope IN.**
- In any mode, two-finger pinch zooms photo (with annotation layer scaled correctly via shared viewMatrix).
- Two-finger drag pans.
- Two-finger double-tap undoes (universal shortcut).
- Two-finger gesture priority — always wins over drawing (per gesture router).
- Pinch limits (min / max zoom).

**Scope OUT.** Single-finger drawing (covered in M20+).

**States to show.** Default zoom · Mid-pinch · Maximum zoom · Two-finger pan · Two-finger double-tap undo confirmation.

**Key interactions.** Pinch · Two-finger drag · Two-finger double-tap.

**Decisions to resolve.** Zoom limits · Inertia on pan release.

**Reference docs.** CE-04 / CE-05 atoms · Doc 02 TWO FINGERS = CANVAS · S-09 session.

---

## Section H — Export, share, collage, PDF

---

### M46 — Share / Export Waterfall

**Goal.** The progressive export sheet — from quick Share Sheet to full PDF report.

**Scope IN.**
- Triggered from photo More menu, project More menu, or multi-select action.
- Sheet shows tiers:
  - **L1** Quick Share — burned-in JPEG via Share Sheet
  - **L1+** With sidecar JSON — JPEG + metadata file
  - **L2** Email / messaging — burned-in + metadata footer
  - **L2+** Zapier webhook (configured) — JSON envelope
  - **L3** Direct CMMS (Phase 3) — native push
- Each tier shows what gets sent.
- Tap tier → executes (mock).

**Scope OUT.** Real backend send · Per-CMMS configuration (M86).

**States to show.** Sheet open · Each tier highlighted · Loading on tap · Success toast.

**Key interactions.** Tap tier → execute.

**Decisions to resolve.** **Q16.** Tier visibility — show all, or hide tiers based on configured integrations? · Kit-aware tier options.

**Reference docs.** EX-01 to EX-05 atoms · IL-01 / IL-02 / IL-03 · Doc 14 Integration Layer · Doc 24 §12 Export Waterfall · S-27 session.

---

### M47 — PDF report builder

**Goal.** Generate a multi-page PDF report from a project's photos + annotations.

**Scope IN.**
- Triggered from M46 "PDF Report" tier or M06 More → Export → PDF.
- Preview pane: pages of the report with photos, annotations, metadata, evidence chain footer.
- Customize: cover page on/off, page-per-photo vs grid, metadata visibility, kit-aware layout template.
- "Render" button → generates → Share Sheet.

**Scope OUT.** Real PDF rendering (visual mock with sample data).

**States to show.** Preview default · After customize toggle · During render (loader) · After render (success → Share Sheet ready).

**Key interactions.** Toggle customize options · Tap Render.

**Decisions to resolve.** **Q17.** Auto-layout (one photo per page, metadata footer) vs template-based (kit-defined layouts) · Cover page format.

**Reference docs.** EX-02 atom · S-28 session.

---

### M48 — Collage creation flow

**Goal.** Build a collage of multiple photos into a structured layout.

**Scope IN.**
- Entry from project More menu → "Create Collage" or from multi-select → "Combine into collage".
- Photo selection (with order).
- Layout suggestion based on photo count + orientation.
- Layout chips: Before/After (2-up) · Quad (2×2) · Hero+Details (1+3) · Process Sequence (3–6) · Forensic Three-Shot (1+2).
- Photo placement preview.
- Confirm → enters M50 cell editing.

**Scope OUT.** Cell editing detail (M50) · Layout templates per kit (M49).

**States to show.** Photo selection · Layout suggestion displayed · Different layout chosen · Confirmation.

**Key interactions.** Select photos · Tap layout chip · Tap Confirm.

**Decisions to resolve.** **Q18.** Photos-first (auto-suggest layout per Instagram Layout) or layout-first (pick template then assign photos) · Kit-template surfacing.

**Reference docs.** Doc 05 Collage Concept · CL-01 / CL-02 atoms · S-29 / S-29b sessions.

---

### M49 — Collage layout templates

**Goal.** The library of layout templates, including kit-specific ones.

**Scope IN.**
- Template browser per kit:
  - Universal: Before/After · Quad · Hero+Details · Process Sequence · Forensic Three-Shot.
  - Welding Kit: Joint Documentation · Weld Defect Report · Procedure Qualification.
  - Electrical Kit: Panel Inspection · Violation Documentation · Before/After Repair.
  - Pipe & Valve Kit: Valve Assembly · Corrosion Documentation · Flow System.
  - Maintenance Kit: Before/After · Condition Assessment · Equipment Survey.
  - Footwear Kit: Construction Breakdown · Material Board · Design Iteration.
- Tap template → preview with placeholder photos → confirm to assign actual photos.

**Scope OUT.** Custom template editor (Phase 2).

**States to show.** Universal templates · Each kit's templates · Template preview · Assignment in progress.

**Key interactions.** Browse · Tap template → preview · Confirm.

**Decisions to resolve.** How prominent kit templates are when a kit is active (top of list / dedicated section).

**Reference docs.** Doc 05 Collage Concept · CL-03 to CL-09 atoms · S-29b session.

---

### M50 — Collage cell editing & cross-photo annotations

**Goal.** Per-cell zoom, pan, swap, plus cross-photo annotations (connecting lines from a detail cell to its location on overview).

**Scope IN.**
- Tap a cell in collage → enters cell-edit mode (blue highlight border).
- Pinch / drag within cell.
- Long-press + drag → swap with another cell.
- Tap outside → return to collage-level.
- Cross-photo annotation: drag from one cell across the gutter to another → creates connection line + annotation.
- Border drag-resize: drag inner borders to resize cell proportions (PicMonkey pattern), adjacent cells reflow.

**Scope OUT.** Collage export (M46 covers).

**States to show.** Cell-edit active · Cell zoomed / panned · Long-press swap in progress · Cross-photo annotation drawn · Border drag in progress.

**Key interactions.** Tap cell → edit · Pinch / drag within · Long-press swap · Drag across gutter · Drag border.

**Decisions to resolve.** Cross-photo annotation rendering when single photos are exported (orphan a line vs convert to caption) · Border resize minimums per cell.

**Reference docs.** CL-07 / CL-15 / CL-16 atoms · S-29b session.

---

### M51 — Photo metadata & Evidence Chain view

**Goal.** View all the metadata behind a photo — GPS, time, heading, SHA-256, EXIF, audit log, evidence chain status.

**Scope IN.**
- Triggered from photo More menu → Info / Metadata.
- View format — inline panel (slides up from bottom) OR dedicated screen.
- Sections: Capture (GPS, time, heading, altitude, technician ID) · Integrity (SHA-256 hash, C2PA seal if present, audit log entries) · Project context (which project, which kit, sync status) · Storage (file size, format, dimensions).
- "Export evidence pack" button (signed PDF with chain manifest).

**Scope OUT.** Real C2PA verification · Real audit log content (mock).

**States to show.** Panel open · Each section expanded · Evidence pack export.

**Key interactions.** Expand section · Tap export.

**Decisions to resolve.** **Q19.** Inline panel (slides up from photo, photo still visible) or dedicated screen? · Audit log depth shown (last 10 entries or full).

**Reference docs.** EX-06 / EX-07 / EX-09 / EX-11 atoms · PD-09 metadata · S-40b / S-40c sessions.

---

## Section I — Multi-select & batch

---

### M52 — Multi-select in Recents

**Goal.** Long-press-driven multi-select in the photostream with batch actions.

**Scope IN.**
- Long-press a photo → enter multi-select mode.
- Action bar slides up from bottom: Add to project · Share · Move · Delete · Cancel.
- Photos in select mode show checkmark; tap toggles selection.
- Top bar changes: "N selected" with × cancel.
- "Select all in section" via section header tap in multi-select.

**Scope OUT.** Specific destinations (M07 for Add to project).

**States to show.** Multi-select with 1 selected · Multi-select with 5 selected · Section-tap-all · Action bar visible · After delete confirmation.

**Key interactions.** Long-press → enter mode · Tap photos → toggle · Tap section header → select all in section · Tap action → execute.

**Decisions to resolve.** **Q20.** Long-press only, or also a "Select" toggle in the brand bar / overflow? · Delete confirmation (single tap vs confirm dialog).

**Reference docs.** Doc 02 Multi-Photo · S-49b session.

---

### M53 — Multi-select in Projects (batch operations)

**Goal.** Long-press project cards for batch operations: merge, archive, delete, export.

**Scope IN.**
- Long-press project card → multi-select mode.
- Action bar: Merge · Archive · Delete · Export Combined Report.
- Merge: pick a target project (or create new) → all photos move, source projects deleted.
- Archive / Delete with confirmation.
- Combined report: PDF spanning multiple projects.

**Scope OUT.** Real merge logic.

**States to show.** 2 projects selected · 4 projects selected · Merge target picker · After merge animation.

**Key interactions.** Long-press → mode · Tap → toggle · Tap action → execute / picker.

**Decisions to resolve.** Merge confirmation depth (need to verify Kits compatible) · Whether to support project splitting (single project → two new).

**Reference docs.** Doc 24 §3 Project context menu · S-49d session.

---

## Section J — System states

---

### M54 — Permission flows (camera, GPS, storage, notification)

**Goal.** Handle Android permission requests gracefully.

**Scope IN.**
- Camera permission: requested when shutter first tapped (M04).
- GPS: requested when shutter first tapped or in settings — Evidence Chain.
- Storage: requested when first import / first export.
- Notification: requested on completion ritual / share moment.
- Each: rationale screen → system dialog → granted / denied state.
- Denied path: clear messaging, "Open Settings" deep link.

**Scope OUT.** Real Android permissions API (mock dialogs).

**States to show.** Rationale screen (each permission) · System dialog (mocked) · Granted state (feature works) · Denied state (feature with degraded mode + Settings link).

**Key interactions.** Tap "Allow" → system dialog · After grant → feature works · After deny → degraded mode shown.

**Decisions to resolve.** **Q21.** Just-in-time (per feature on first use) or upfront (M11 onboarding) · Rationale tone — clinical, friendly, persuasive.

**Reference docs.** CC-01 atom · Android permission best practices · S-21 session.

---

### M55 — Error states & recovery

**Goal.** Catalog every error state and its recovery path.

**Scope IN.**
- Network errors (offline) — Recents still works, sync queue shown.
- Storage full — modal with cleanup suggestions.
- Crash recovery — restore unsaved work toast on next launch.
- Photo corrupted — placeholder + report option.
- Export failure — retry with backoff.
- Camera busy / unavailable — fallback to gallery.
- Sync failure (CMMS, Phase 3) — queue with retry.

**Scope OUT.** Per-CMMS error variations (M93).

**States to show.** Each error type with recovery affordance.

**Key interactions.** Tap retry · Tap fallback · Dismiss · Open settings.

**Decisions to resolve.** Toast vs modal vs banner per error type · "Report this" telemetry option.

**Reference docs.** Doc 18 Implementation Infrastructure · Crash recovery atoms.

---

### M56 — Empty states catalog

**Goal.** Every "empty" surface gets a deliberate empty state.

**Scope IN.**
- Empty Recents (true first-launch).
- Empty Projects (no projects yet).
- Empty project (just-created, zero photos).
- Empty search results.
- Empty kit catalog (no kits installed beyond universal).
- Empty audit log.
- Empty annotations on photo (Browse default).

**Scope OUT.** First-time hints (M11).

**States to show.** Each empty state with appropriate CTA / hint.

**Key interactions.** Tap CTA → drives next action.

**Decisions to resolve.** Tone — encouraging vs neutral · Illustration style (icon, line art, none).

**Reference docs.** Doc 24 empty state mentions · Doc 25 empty arc.

---

### M57 — Loading & processing states

**Goal.** Loading indicators across surfaces.

**Scope IN.**
- App cold-launch splash (300 ms max per ON-01).
- Photo import (SHA-256 computing).
- Export rendering.
- PDF generation.
- Sync upload (Phase 3 — generic mock).
- Image loading in Recents (placeholder skeletons).
- Background pre-warming feedback (camera readiness).

**Scope OUT.** Per-feature loading detail.

**States to show.** Each loader · Determinate (progress bar) vs indeterminate (spinner) · Background pre-warm indicator.

**Key interactions.** Cancel where applicable.

**Decisions to resolve.** Skeleton style (shimmer, fade, pulse) · Progress bar style.

**Reference docs.** ON-01 atom · S-24 session.

---

## Section K — Settings deep-dives

---

### M58 — Account & subscription

**Phase.** 2.

**Goal.** Account sign-in (optional), subscription tier display (Free / Pro / Team), upgrade flow.

**Scope.** Sign-in screen · Tier badges in settings · Upgrade CTA · Team management overview (Team tier — defer real flow to M62/M81).

**Builds on.** M10.

**Reference docs.** Doc 17 Subscription model · Phase 2 atoms.

---

### M59 — Trade Kit management settings

**Goal.** Inside Settings, the Kit management surface — active kit selector, installed kits, browse catalog, import .tnkit.

**Scope IN.**
- Active kit selector (single select among installed).
- Installed kits list (with toggle to deactivate per session vs uninstall).
- "Browse catalog" → M33.
- "Customize active kit" → M35.
- "Import .tnkit" → M35.

**Scope OUT.** Catalog / customize / import (M33–M35).

**States to show.** Default · Multiple kits installed · Mid-switch.

**Key interactions.** Tap active selector · Tap browse · Tap customize.

**Decisions to resolve.** Single-active-kit or multi-active-additive policy.

**Reference docs.** TK-04 atom · S-23 session.

---

### M60 — Integrations / CMMS settings

**Phase.** 3.

**Goal.** Connect CMMS providers, configure webhook destinations, manage sync queue.

**Scope.** Connection list (Procore, MaintainX, etc.) · OAuth flows (M86) · Webhook config (M95) · Sync queue (M92) · Disconnect / reauthorize.

**Builds on.** M10, M86, M95.

**Reference docs.** Doc 14 Integration Layer · Doc 53 CMMS Integration · S-59–S-64 sessions.

---

### M61 — Evidence Chain settings

**Goal.** Configure technician ID, SHA-256 toggle, C2PA settings, audit log export, legal-export format.

**Scope IN.**
- Technician ID field (name + credential).
- SHA-256 toggle (always on by default).
- C2PA settings (on supported devices).
- Audit log: view full, export CSV / signed PDF.
- Legal export format (signed PDF with chain-of-custody manifest).

**Scope OUT.** Real cryptography.

**States to show.** Default · Editing technician ID · Audit log expanded.

**Key interactions.** Edit fields · Toggle SHA-256 / C2PA · Tap export.

**Decisions to resolve.** Credential format — free text vs structured (CWI #, PE state, etc.).

**Reference docs.** Doc 01 Evidence Chain · EX-06 / EX-07 atoms · S-40b session.

---

### M62 — Display, accessibility, language, units

**Goal.** Display + accessibility settings — theme (locked dark for field-grade), haptic intensity, touch target size, language, units (metric / imperial), font scale.

**Scope IN.**
- Theme — field-grade dark only at v1 (locked) but with statement of why.
- Haptic intensity: None · Light · Default · Strong.
- Touch target size: Standard · Large · Glove-friendly.
- Language: dropdown with installed locales (6 launch + expansion).
- Units: Metric / Imperial / Mixed (per measurement type).
- Font scale: 100 % / 125 % / 150 %.

**Scope OUT.** Real locale switching (M72 — Phase 2).

**States to show.** Each setting at each variant.

**Key interactions.** Tap → set.

**Decisions to resolve.** Locked-dark policy v1 vs allow light theme later · Glove-friendly default per accessibility user-research.

**Reference docs.** Doc 24 §15 Settings · ON-07 atom.

---

## Section L — Phase 2 advanced features

---

### M63 — Studio Mode

**Phase.** 2.

**Goal.** Reveal the full engineering toolkit (GD&T, welding, NDT, dimensions, advanced editing) for desk-mode work — not a separate UI mode but a contextual reveal.

**Builds on.** M05, M19, M36–M40.

**Scope.** Triggered from photo More menu → "Open in Studio". Toolbar expands, more space for canvas, sliders for fine-tuning, ruler / grid overlay. Mouse / stylus optimized.

**Decisions to resolve.** **Q22.** Separate UI mode or contextual reveal within Browse / Annotate? · Desktop expansion target.

**Reference docs.** S-42 session · Doc 04 Future Vision · Doc 12 Unified Concept Architecture.

---

### M64 — Annotation layers

**Phase.** 2.

**Goal.** Show / hide annotation layers (already implemented in data via PD-11).

**Builds on.** M05, M32.

**Scope.** Layer panel (left edge or overflow). Each layer: visibility toggle, opacity, lock, rename, delete, reorder. Default layers per kit. Cross-layer selection.

**Reference docs.** PD-11 atom · S-40a session.

---

### M65 — Voice annotations

**Phase.** 2.

**Goal.** Tap-hold-speak annotation. Audio clip as primary artifact, transcription in background.

**Builds on.** M05.

**Scope.** Tap-and-hold annotation point → record (with waveform feedback) → release → mini player + transcription. Works with gloves (large touch target, no precise gestures).

**Decisions to resolve.** **Q23.** Hold-anywhere on photo or dedicated voice tool in palette? · Recording duration cap (per voice memo).

**Reference docs.** Doc 04 Voice as Annotation · S-51–S-53 sessions.

---

### M66 — Voice transcription & linked evidence

**Phase.** 2.

**Goal.** Display transcribed voice annotations with audio link per word (Abridge model).

**Builds on.** M65.

**Scope.** Transcript view of all voice annotations · Tap any phrase → plays linked audio · Supervisor audit trail.

**Reference docs.** Doc 04 Linked Evidence · S-52 session.

---

### M67 — Sound diagnostic recording

**Phase.** 2.

**Goal.** Attach ambient sound clip to an annotation as diagnostic evidence (bearing defects, valve hiss, etc.).

**Builds on.** M65.

**Scope.** Separate "Diagnostic Sound" tool · 5-second clip attached to annotation point · Spectrogram visualization.

**Reference docs.** Doc 04 Sound as Diagnostic · S-53 session.

---

## Section M — Phase 2 video & GIF

---

### M68 — Video capture

**Phase.** 2.

**Goal.** Record short 3-15 s clips for diagnostic video.

**Builds on.** M04.

**Scope.** Video mode in camera (already shown in M04 mode row but no-op until this mockup). Tap-hold shutter to record. Stop. Lands as video item in Recents.

**Reference docs.** CC-06 atom · S-39 session.

---

### M69 — Video frame annotation

**Phase.** 2.

**Goal.** Tap thumbnails in filmstrip → annotate per-frame. Annotations appear/disappear at timestamps.

**Builds on.** M05, M68.

**Scope.** Filmstrip below video. Tap frame → annotation mode (same tools as photo). Drag handles to set temporal range. Play back with annotations animating.

**Reference docs.** Doc 04 Video Annotation · S-55 session.

---

### M70 — Video-to-GIF export

**Phase.** 2.

**Goal.** Export annotated video as GIF for messaging.

**Builds on.** M68, M69.

**Scope.** Export option from video More menu. Duration, FPS, resolution presets. Renders → Share Sheet.

**Reference docs.** Doc 04 GIF Constraints · S-57 session.

---

### M71 — Pre-capture buffer / split-second rewind

**Phase.** 2.

**Goal.** Circular buffer of last 3–5 s of viewfinder frames → "rewind" UI to pick earlier frame.

**Builds on.** M04.

**Scope.** Rewind affordance in camera (right-edge swipe or dedicated button) → reveal recent frames → tap to "capture" retroactively.

**Reference docs.** CC-05 atom · S-39a session.

---

## Section N — Phase 2 localization

---

### M72 — Locale switching (live UI update)

**Phase.** 2.

**Goal.** Switch app language with immediate UI update — no restart.

**Builds on.** M62.

**Scope.** Settings → Language picker. Apply → entire UI re-renders in target locale (including ISO 7010 symbol names, quick-labels, tool names).

**Reference docs.** L10N-02 atom · S-25 / S-44 sessions.

---

### M73 — ML-Kit translation overlay

**Phase.** 2.

**Goal.** Translate annotation text on the fly via Google ML Kit on-device translation.

**Builds on.** M31, M72.

**Scope.** Long-press a text annotation → "Translate to..." menu. Auto-detect source language. Overlay translation. Source preserved underneath.

**Reference docs.** L10N-03 atom · S-46 session.

---

### M74 — IATE / TBX terminology import

**Phase.** 2.

**Goal.** Import official terminology databases (EU IATE, TBX glossaries) for kit-specific autocomplete.

**Builds on.** M31, M59.

**Scope.** Import flow in Trade Kit settings → IATE / TBX file picker. Loaded terms surface in quick-label autocomplete.

**Reference docs.** L10N-05 atom · S-45 session.

---

## Section O — Phase 2 / 3 AI features

---

### M75 — Anomaly detection (PatchCore)

**Phase.** 2.

**Goal.** Show "what normal looks like" — AI compares photo to learned-normal corpus, flags anomalies.

**Builds on.** M05.

**Scope.** Auto-runs after capture (opt-in). Anomaly heatmap overlay. Threshold slider. Convert anomaly to annotation (one tap).

**Reference docs.** "Fifty Breakthrough Concepts" doc · Phase 2 AI atoms.

---

### M76 — Object & material detection

**Phase.** 3.

**Goal.** Auto-tag objects in photos (pipe, valve, panel, etc.).

**Builds on.** M05.

**Scope.** Background detection → photo gets tags · Tap tag → relevant kit suggested.

**Reference docs.** Doc 04 AI features · "Fifty Breakthrough Concepts".

---

### M77 — OCR & scene text extraction

**Phase.** 2.

**Goal.** Extract text from nameplate / equipment ID / label in photo → searchable + auto-fills metadata.

**Builds on.** M05, M09.

**Scope.** Tap "Detect text" in photo More → OCR runs → text appears as overlay · Tap to copy / search.

**Reference docs.** Phase 2 OCR atoms.

---

### M78 — Auto-cropping (saliency)

**Phase.** 2.

**Goal.** When placing photo in collage cell, auto-crop centers the salient subject.

**Builds on.** M50.

**Scope.** Saliency-based centering on cell placement. Face detection priority. Manual override via tap-to-reposition.

**Reference docs.** CL-16 atom · S-29b session.

---

### M79 — Voice-to-structured-annotation

**Phase.** 3.

**Goal.** Speak a structured phrase → auto-create annotation with equipment ID + condition + recommendation.

**Builds on.** M65.

**Scope.** "Bearing 4B showing excessive play, recommend replacement" → parses → places annotation with metadata.

**Reference docs.** Doc 04 Speech-to-Structured · Phase 3 atoms.

---

### M80 — AI-suggested annotations

**Phase.** 3.

**Goal.** AI proposes annotations based on detected anomalies + scene type.

**Builds on.** M75, M76.

**Scope.** Subtle banner after capture: "3 potential defects detected. Review?" → tap → AI proposals shown as semi-transparent annotations → accept / reject each.

**Reference docs.** Phase 3 AI atoms.

---

## Section P — Phase 3 collaboration

---

### M81 — Share-for-review flow

**Phase.** 3.

**Goal.** Share an annotated photo / project to an expert for review. Expert adds annotations in different color. Originator gets push back.

**Builds on.** M46.

**Scope.** "Share for review" tier in Export Waterfall → recipient picker → confirmation. Expert opens via link → adds annotations → originator notified.

**Reference docs.** Doc 04 Collaboration · S-54 session.

---

### M82 — Async expert annotation overlay

**Phase.** 3.

**Goal.** Expert annotations appear in a distinct color/layer over originator's photo.

**Builds on.** M64, M81.

**Scope.** Layer panel includes "Expert: [name]" layers · Different visual treatment (e.g. dashed strokes, distinct color slot).

**Reference docs.** S-54 session.

---

### M83 — Real-time sync (CRDT)

**Phase.** 3.

**Goal.** Live collaborative annotation — two users see each other's strokes in real time.

**Builds on.** M81.

**Scope.** Connection indicator. Live cursor of collaborator. Strokes appear as drawn. Conflict-free per CRDT.

**Reference docs.** Phase 3 CRDT atoms.

---

### M84 — Push notifications for collaboration

**Phase.** 3.

**Goal.** Push notification when expert review comes back, or new collaborator joins.

**Builds on.** M14, M81.

**Scope.** Notification copy templates · Deep-link to relevant photo / project.

**Reference docs.** Phase 3 notification atoms.

---

### M85 — Conflict resolution UI

**Phase.** 3.

**Goal.** When offline edits conflict with cloud, present a clear resolution choice.

**Builds on.** M83.

**Scope.** Conflict modal: "Local version" vs "Cloud version" preview side-by-side · Keep local / keep cloud / merge.

**Reference docs.** Phase 3 sync atoms.

---

## Section Q — Phase 3 CMMS integration

---

### M86 — CMMS connection setup & OAuth

**Phase.** 3.

**Goal.** Connect to a CMMS provider via OAuth.

**Builds on.** M60.

**Scope.** Provider picker · OAuth web flow · Permission scope screen · Success / re-auth states.

**Reference docs.** Doc 53 CMMS Integration · IL-04 atom · S-59 onward.

---

### M87 — Work order linking

**Phase.** 3.

**Goal.** Link a project or photo to a CMMS work order.

**Builds on.** M86.

**Scope.** "Link to Work Order" → WO picker (search by ID, asset, status) · After link, project carries WO badge · Export pushes to WO record.

**Reference docs.** IL-05 atom · S-60 session.

---

### M88 — Asset selection & equipment context

**Phase.** 3.

**Goal.** Tag photos to a specific asset (equipment ID) for asset-centric history.

**Builds on.** M87.

**Scope.** Asset picker · Auto-suggest based on GPS + last-asset · Equipment history banner (Context Whisper) appears for known assets.

**Reference docs.** IL-06 atom · S-61 session.

---

### M89 — Context Whisper banner

**Phase.** 3.

**Goal.** When camera points at a known asset (GPS / OCR), surface its history.

**Builds on.** M04, M88.

**Scope.** Subtle banner in viewfinder: "Pump P-201 · last inspected Mar 12 · 1 open WO" · Tap → asset detail.

**Reference docs.** Doc 14 Network Intelligence · IL-07 atom.

---

### M90 — Priority Tint signal

**Phase.** 3.

**Goal.** Visual tint of UI chrome when current work links to high-priority WO (red border, urgency cue).

**Builds on.** M87.

**Scope.** Project header tint variations per priority tier · Subtle, never alarmist.

**Reference docs.** IL-08 atom.

---

### M91 — Return Signal updates

**Phase.** 3.

**Goal.** When a synced WO updates externally, surface the change.

**Builds on.** M87.

**Scope.** Banner / badge in project: "WO closed externally · PM rescheduled +14d" · Tap → CMMS detail.

**Reference docs.** Doc 14 Return Signal · IL-09 atom.

---

### M92 — Sync queue & status

**Phase.** 3.

**Goal.** View all pending / completed / failed syncs.

**Builds on.** M60.

**Scope.** Queue list with per-item status, retry, view payload, force push.

**Reference docs.** IL-10 atom.

---

### M93 — Sync conflict resolution

**Phase.** 3.

**Goal.** When push fails due to CMMS-side conflict (WO closed, asset moved), present resolution.

**Builds on.** M85, M92.

**Scope.** Conflict modal with options to overwrite / merge / save locally only.

**Reference docs.** IL-11 atom.

---

### M94 — Per-adapter UX variations

**Phase.** 3.

**Goal.** Each CMMS adapter (Procore, MaintainX, UpKeep, ServiceTitan, Maximo, SAP PM) has its own field set / vocabulary.

**Builds on.** M87, M88.

**Scope.** Per-adapter field mappings · Per-adapter terminology overrides · One mockup with variants for each.

**Reference docs.** Doc 53 CMMS Integration · S-59–S-64.

---

### M95 — Zapier / Make webhook configuration

**Phase.** 2.

**Goal.** Configure a generic webhook destination for users without a native CMMS adapter.

**Builds on.** M60.

**Scope.** Webhook URL field · Test ping · Payload preview · Per-event toggle.

**Reference docs.** IL-03 atom · S-50 session.

---

## Cross-cutting appendices

These are specs, not mockups. They codify patterns that every mockup must respect. Each appendix is a separate section of this plan, not a separate file.

---

### A1 — Localization & i18n conventions

- Six launch languages, expansion to 13+ in Phase 2.
- All UI strings via `strings.xml` resources.
- ISO 7010 symbol names localized.
- Quick-label seeded vocabulary per locale.
- RTL support (Arabic, Hebrew) — applies to layout direction, not annotation direction.
- Date / number / unit formats per locale.

---

### A2 — Accessibility (WCAG 2.2 AA)

- Touch targets minimum 44 × 44 dp (large mode: 60 × 60).
- Color contrast: ink colors must pass AA against photo backgrounds (semantic ink chosen for visibility).
- Screen-reader labels on every interactive element.
- Reduce-motion respect: disable transitions when system flag set.
- Sufficient haptic alternatives for visual cues.

---

### A3 — Haptic feedback patterns

- **CONFIRM** (medium tick) — successful action, snap-to-shape, tool change.
- **CLOCK_TICK** (light tick) — guide snap, alignment hit.
- **WARNING** (double tick) — error, unsafe action.
- Intensity respects user setting (None / Light / Default / Strong).
- Never haptic on idle / passive feedback.

---

### A4 — Auto-save behaviors

- Annotations auto-save on every modification.
- Photo metadata auto-saved on capture.
- Project auto-saves on mode switch + idle (per Doc 02 PRIORITY 6).
- Indicator: subtle "Saved" pill that fades after 1 s · never blocks UI.

---

### A5 — Touch target sizing

- Standard: 44 dp minimum.
- Large mode: 60 dp.
- Glove-friendly: 72 dp (and increased gesture forgiveness — slower velocity thresholds).
- Toolbar slot size scales with target setting.

---

### A6 — Animation timing tokens

- **MICRO** 100 ms — tap-down state, hover.
- **QUICK** 150 ms — toolbar updates, mode transitions.
- **STANDARD** 200 ms — view transitions, slide-ups.
- **SOFT** 300 ms — modal slides, sheet openings.
- **DELIBERATE** 500 ms — completion ritual reveal.
- Easing: cubic-bezier(0.32, 0.72, 0, 1) for slides; ease-out for fades.

---

### A7 — Gesture vocabulary & gesture priority

- Two fingers always win over one finger.
- Pinch = zoom.
- Two-finger drag = pan.
- Two-finger double-tap = undo.
- One-finger slow drag (< 300 dp/s, > 80 ms) = draw / annotate.
- One-finger fast edge swipe (> 600 dp/s, edge 15 %, span > 60 %) = photo navigation.
- Long-press (> 500 ms) = context / multi-select.
- StatelessGestureRouter (Doc 02) implements priority.

---

### A8 — Mode transition patterns

- Mode transitions are explicit (chrome reconfigures, colored accent bar signals context) or implicit (Draw=Enter Annotate, Camera→Annotate primed).
- Each mode has independent undo stack.
- Mode transition animations: 150–200 ms cross-fade with accent-bar slide.

---

### A9 — Modal patterns

- **Bottom sheet** — for pickers, actions, brief info (default modal style).
- **Full-screen sheet** — for complex flows (completion ritual, kit catalog, search).
- **Dialog** — only for destructive confirmations.
- **Toast** — for non-blocking confirmations (5 s auto-dismiss).
- **Banner** — for persistent context (sync status, GPS denied, kit suggestion).
- Bottom sheets dismiss via drag-down or backdrop tap.

---

### A10 — Undo / redo system

- Per Doc 02: **Three independent stacks** — ANNOTATE / EDIT / project-level.
- ANNOTATE stack: add / delete / move / property-change of annotations.
- EDIT stack: photo edit operations (crop, rotate, adjust).
- Stack resets on Edit Mode cancel/revert.
- Switching modes preserves each stack.
- Two-finger double-tap = undo (universal).
- Back-tap (Android 14+) = undo fallback (glove-safe).
- Max 50 operations per stack.

---

## When all mockups are settled

Once Phase 1 mockups (M01–M62 where Phase 1) are individually approved, the next phase is **integration** — producing a single end-to-end clickable prototype that stitches them together. That integration becomes the visual + behavioral target for the implementation rewrite.

Implementation routing (which module receives which mockup):

- `:feature-home` ← M01 / M02 / M03 / M07 / M09 / M10 / M11 / M52 / M53 / M59 / M61 / M62
- `:feature-camera` ← M04 / M42 / M43 / M71
- `:feature-canvas` ← M05 / M16–M18 / M19–M32 / M36–M45 / M51 / M63 / M64
- `:feature-collage` ← M48 / M49 / M50
- `:feature-export` ← M08 / M46 / M47
- `:feature-settings` ← M58 / M60
- (new) `:feature-voice` ← M65 / M66 / M67 / M79
- (new) `:feature-ai-vision` ← M75 / M76 / M77 / M78 / M80
- (new) `:feature-collab` ← M81 / M82 / M83 / M84 / M85
- (new) `:feature-cmms` ← M86–M95
- `:app/navigation` ← M01 plus deep-link routing for M12–M15

Existing session blueprints S-24 (Onboarding + Project Hub), S-49 (Nav + entry points), and S-49d (hub entry points) will need to be rewritten against this redesigned target. That rewrite is out of scope for this plan and will be handled as a separate set of session blueprints once the mockups are settled.

---

*Doc 57 · UX Redesign Mockup Plan · comprehensive · authored during the Q01–Q02b interaction redesign conversation*
