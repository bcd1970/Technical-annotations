# 30 — Atoms: Systems
## Export & Evidence · Trade Kits · Localization

> **Cross-cutting systems** that serve every feature. Export turns annotations into deliverables. Trade Kits configure per profession. Localization makes it work in 13 languages.

---

## Area 10 — Export & Evidence (EX)

### EX-01 · Burned-In JPEG Export
**Phase:** 1 · **Module:** `:feature-export` · **Size:** M · **Depends:** CE-11, PD-04
Photo + annotations → permanent render via CE-11. Full original resolution, JPEG quality configurable (default 95%). Watermark on free tier, clean on Pro. <3s for 12MP + 50 annotations. <5MB typical output.

### EX-02 · Sidecar JSON Export (Annotation Envelope)
**Phase:** 1 · **Size:** M · **Depends:** PD-04, PD-05
JSON alongside JPEG: full geometry (image-space), semantic tags, metadata (GPS, timestamp, technician, heading), evidence (SHA-256, provenance per PD-14), project context, schema version. `{photo}_annotations.json`. Machine-readable. Round-trip: import sidecar → identical annotations.

### EX-03 · Metadata Bar Burn-In
**Phase:** 1 · **Size:** S · **Depends:** EX-01, PD-09
Bottom strip on export: GPS, timestamp, tech ID, project, heading. Configurable fields. Dark semi-transparent bg. Survives screenshot/WhatsApp/email.

### EX-04 · PDF Report Generation
**Phase:** 1 · **Size:** L · **Depends:** EX-01, PD-02
Single-project PDF: cover page (name, dates, tech, stats) → per-photo page (annotated photo + metadata + annotation table) → summary page. Android `PdfDocument` (uses same Canvas API as annotation rendering). A4/Letter configurable. 20 photos <10s. 300dpi for printing.

### EX-05 · Android Share Sheet
**Phase:** 1 · **Size:** S · **Depends:** EX-01, EX-02
Single photo: JPEG + optional sidecar. Multi: ZIP. Report: PDF. `Intent.ACTION_SEND` / `SEND_MULTIPLE`. `FileProvider` content URIs.

### EX-06 · C2PA Content Credentials
**Phase:** 2 · **Size:** L · **Depends:** EX-01, PD-08
C2PA manifest on exported photos. Device-generated key pair signing. Chain: capture→edits→export. c2pa-rs via JNI. Verifiable on contentcredentials.org. Works offline.

### EX-07 · SHA-256 Audit Log Export — 2/S/PD-08. CSV/JSON of all photo hashes + timestamps.

### EX-08 · Video Annotation Export — **Phase 3** · L · Depends: CC-06, CE-06
Media3 Transformer. Time-stamped annotations baked into MP4 frames.

### EX-09 · Batch Export — 2/M/EX-01. Multi-select → ZIP of JPEGs, single PDF, or folder of JPEG+sidecar pairs. Progress indicator.

### EX-10 · Collage Export
**Phase:** 1 · **Size:** M · **Depends:** CL-01, EX-01
Collage as single JPEG at combined resolution. Metadata bars preserved. Sidecar includes per-cell references.

### EX-11 · Version Timeline Per Photo
**Phase:** 2 · **Size:** M · **Depends:** PD-04, PD-14
Per-photo version history: V1 initial → V2 follow-up → V3 repair → V4 verification. Timeline scrubber to see annotation state at any version. Layer per version. Any version independently exportable. Comparison view in PDF.

**Acceptance:** History preserved indefinitely. Scrubber smooth 60fps. Built on PD-14 provenance.

---

## Area 11 — Trade Kit System (TK)

### TK-01 · Trade Kit Data Model + Registry
**Phase:** 1 · **Module:** `:core:data` · **Size:** M · **Depends:** PD-01, PD-10
`TradeKit`: id, name, displayName (localized), icon, toolSet (List<ElementType>), defaultToolbarSlots, defaultSemanticColors, defaultNumberingPrefix, collageTemplates, reportTemplate. `TradeKitRegistry`: built-in + imported kits. Room entity for active kit + customizations. Kit switch <100ms.

### TK-02 · Kit Activation Engine
**Phase:** 1 · **Size:** M · **Depends:** TK-01, IC-03
`KitActivationManager`: applies kit config to toolbar. Adds kit tools to ⋯More. Suggests slot replacements (non-destructive). Kit→kit switching clean. 5s undo toast.

### TK-03 · Five Launch Kits
**Phase:** 1 · **Size:** L · **Depends:** TK-01, TK-02

| Kit | Key Additions |
|-----|--------------|
| Electrical Inspector | Op-Amp, Logic Gate, Parallel Plates, Zigzag, Coil |
| Pipe & Valve | Bowtie, Triangle+Bar, Diamond Inline, Break Lines, Flow Arrows |
| Welding Inspector | GD&T FCF, Weld Symbol, Surface Finish, Tolerance, Crosshairs |
| General Maintenance | Condition stamps, Numbered Pins, Before/After |
| Footwear & Garment | Stitch, Grainline, Hardware, Notch, EU Material Grid |

Each: tool set, toolbar slots, numbering prefix, report template, sample collage template.

### TK-04 · Kit ↔ Tool Resolver — 1/S/TK-01, PD-05
Resolves visible tools from active kit(s). Multiple kits = union. Priority: user custom > kit default > universal.

### TK-05 · .tnkit File Format — 2/M/TK-01
JSON-based. Export current config → shareable file. Import → custom kit. Versioned schema. Credit attribution (creator name + credential).

### TK-06 · Kit Catalog Browse UI — 1/M/TK-01, DS-01
Via ⋯More. Domain sections. Kit card: icon, name, tool count, preview. One-tap activate. Search.

### TK-07 · Kit Customization — 2/S/TK-02, IC-03. Long-press-drag from ⋯More into 5 slots.
### TK-08 · Contextual Kit Suggestion — 2/S/TK-01. Detect usage patterns → suggest kit. Max 1/session.
### TK-09 · Community Kits — **Phase 3** · M · TK-05. Import from link/QR. Community section.
### TK-10 · Kit Collage Templates — 2/S/TK-01, CL-10
### TK-11 · Kit Semantic Colors — 1/S/TK-01, IC-06. Per-kit color meanings. Strip labels update.

---

## Area 12 — Localization (L10N)

### L10N-01 · String Resource Infrastructure
**Phase:** 1 · **Module:** `:core:ui` + all features · **Size:** M · **Depends:** —
Base `strings.xml` (en) + 5 EU: de, fr, es, it, pt. Naming: `feature_screen_element`. All via `R.string.*`. `stringResource()` in Composables only (never ViewModel). ~500 strings Phase 1.

**Acceptance:** Fully usable in 6 languages. No untranslated strings visible. IDs stable after initial translation.

### L10N-02 · Locale Switching
**Phase:** 1 · **Size:** S · **Depends:** L10N-01, ON-07
Settings → picker. `AppCompatDelegate.setApplicationLocales()` (API 33+), fallback API 29-32. Immediate UI update. Per-app override works (TechNotes German, phone English).

### L10N-03 · donottranslate.xml + xliff:g — 1/S/L10N-01
Standard codes, equipment IDs, extensions in donottranslate. `<xliff:g>` for non-translatable fragments.

### L10N-04 · ISO 7010 Safety Symbol Picker
**Phase:** 1 · **Size:** M · **Depends:** DS-01, L10N-01
~200 SVGs from Wikimedia Commons. Hierarchical grid: P (prohibition), W (warning), M (mandatory), E (safe condition), F (fire). Each: ID + localized name + SVG + category. Searchable in all locales.

### L10N-05 · Plurals + RTL Testing — 1/S/L10N-01
Polish `few`/`many` categories. Test 0, 1, 2, 5, 21. Layout doesn't break on RTL (no hardcoded start/end).

### L10N-06 · Full 13-Language Strings — 2/L/L10N-01
Add nl, pl, sv, cs, ro, zh-CN, zh-TW. Chinese: min 14sp body, test shorter text. `resConfigs`.

### L10N-07 · Terminology Database
**Phase:** 2 · **Size:** M · **Depends:** PD-01, L10N-01
Room tables: ConceptEntity (concept_id, domain, standard_refs) + TermEntity (concept_id, locale, term, source). ~2000 terms × 5 languages. Bidirectional lookup: concept→terms, term→concept→all languages. <5MB, <5ms lookup.

### L10N-08 · IATE TBX Import — 2/M/L10N-07
Parse IATE TBX XML. Filter industry/technology, reliability ≥3. Supplement IEC Electropedia. Manual cross-standard mapping. Build-time import → bundled SQLite.

### L10N-09 · Structured Annotation i18n (ISO 14224) — 2/M/L10N-07, PD-04
Coded defect inputs: CRA=crack, location, severity. Renders as localized terms. Eliminates translation for structured annotations. ~38 problem codes, ~20 cause codes, ~10 detection methods.

### L10N-10 · ML Kit Offline Translation — 2/M/L10N-07
On-device, ~30MB per pack, on-demand download. 13 languages. Labeled "machine-assisted". Tier priority: cache→glossary→ML Kit→cloud.

### L10N-11 · Pre-Translated Phrase Cache — 2/S/L10N-07. 200–500 phrases, human-translated. Zero latency.
### L10N-12 · Terminology Glossary Overlay — 2/S/L10N-07, L10N-10
Identify terms → placeholders → translate → substitute. Catches dangerous mistranslations (crane, lead, seal).

### L10N-13 · DeepL API Cloud — **Phase 3** · M · L10N-10. $5.49/mo + $25/M chars. Glossary adaptation. Cache locally.
### L10N-14 · Date/Time/Number Formatting — 1/S/L10N-01
`DateTimeFormatter` with locale. Decimal/thousands separators. Metric/imperial per PD-10.

### L10N-15 · Chinese Market Variant — **Phase 3** · L · L10N-06
Huawei ML Kit, JPush, Alibaba Cloud. Separate build variant. GB standards. DingTalk option. Red=positive consideration.

### L10N-16 · Dual-Standard Reference Fields — 2/S/L10N-07
ISO 2553 ↔ AWS A2.4 (arrow side flips). ASME Y14.5 vs ISO 1101 interpretation differences flagged.

### L10N-17 · locales_config.xml Auto-Gen — 1/S/L10N-01
AGP 8.1+ `generateLocaleConfig=true`. `res/resources.properties` with `unqualifiedResLocale=en-US`.

### L10N-18 · Disable AAB Language Splits — 1/S/L10N-01
`android.bundle.language.enableSplit=false`. Essential for offline language switching.

---

*April 2026 · TechNotes Atoms: Systems v2.1*
*Changes from v2.0: EX-04 — removed "or iText" (deprecated per Doc 18). Android PdfDocument is the endorsed approach.*
