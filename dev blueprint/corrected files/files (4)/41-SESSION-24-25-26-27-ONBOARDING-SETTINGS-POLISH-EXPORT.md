# Session 24 — Onboarding + Project Hub

## GOAL
Implement splash screen, first-launch onboarding (3 steps: work question → first photo → kit suggestion), project hub home screen, project creation flow, and warm launch state restoration.

## DEPENDS ON
- **S-01** (DS-01) — Design tokens/theme
- **S-04** (PD-01, PD-02) — Room DB, project model
- **S-11** (IC-01) — Mode state machine (warm launch restores to Browse)
- **S-23** (TK-01) — Trade kits (onboarding suggests kit based on work question)

## ATOMS
- **ON-01** · Splash + Cold Launch (S)
- **ON-02** · First-Launch Onboarding (M)
- **ON-03** · Work Question UI (S)
- **ON-04** · Project Hub — Home Screen (M)
- **ON-05** · Project Creation Flow (S)
- **ON-08** · Warm Launch State Restoration (S)

## SPEC REFERENCES (inlined)

### ON-01 · Splash + Cold Launch
300ms max. Routes to onboarding (first launch) or hub (returning user).

### ON-02 · First-Launch Onboarding
3 steps (15s+30s+15s = 60s total): work question → first photo (camera or gallery) → kit suggestion. No account required. Auto-creates "My First Project".

### ON-03 · Work Question UI
8 tiles: Electrical, Plumbing, HVAC, Welding, General Maintenance, Construction, Footwear, Other. "More..." expandable. Selection maps to Trade Kit suggestion.

### ON-04 · Project Hub
LazyColumn of project cards (thumbnail, name, photo count, date, completion status). FAB for new project. Top bar: logo + search icon + settings icon. Long-press → context menu (rename, duplicate, archive, delete).

### ON-05 · Project Creation
3 taps max: FAB → name (optional, auto-generated) → confirm → camera opens. Minimal friction.

### ON-08 · Warm Launch Restoration
Restores to Browse Mode with the last-viewed photo and all annotation state visible. Always restores to Browse (safety default — prevents accidental annotation from stray touches). Camera warms in background. Photo strip position, scroll state, project context all restored.

## DELIVERABLES

### 1. SplashScreen.kt
### 2. OnboardingFlow.kt (3 steps)
### 3. ProjectHubScreen.kt + ProjectHubViewModel.kt
### 4. WarmLaunchRestorer.kt

## KEY IMPLEMENTATION DETAILS

### Navigation Graph
```
SplashScreen → (first launch?) → OnboardingFlow → ProjectHub
                  (returning?) → ProjectHub
ProjectHub → CameraScreen / CanvasScreen
Warm launch → Browse Mode on last-viewed project/photo
```

### Warm Launch
Save to DataStore on every navigation:
```kotlin
data class LastSessionState(
    val projectId: String,
    val photoIndex: Int,
    val photoStripScrollPosition: Int,
)
```
On resume: read state → navigate to Browse with correct photo. Force Browse mode regardless of previous mode.

### Project Auto-Naming
```kotlin
fun generateProjectName(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val count = projectDao.countByDate(date) + 1
    return "Inspection $date #$count"
}
```

## TEST CRITERIA
- [ ] Cold launch <300ms to splash
- [ ] Onboarding completes in ~60 seconds
- [ ] Work question maps to correct kit suggestion
- [ ] "My First Project" auto-created
- [ ] Project hub scrolls 50 projects at 60fps
- [ ] FAB → create project → camera in 3 taps
- [ ] Warm launch restores to Browse with correct photo
- [ ] Long-press project → context menu (rename/delete/archive)

## FILES TO CREATE / MODIFY

### Create
1. `app/src/main/java/.../SplashScreen.kt`
2. `feature-home/src/main/java/.../onboarding/OnboardingFlow.kt`
3. `feature-home/src/main/java/.../hub/ProjectHubScreen.kt`
4. `feature-home/src/main/java/.../hub/ProjectHubViewModel.kt`

### Modify
- `app/src/main/java/.../navigation/NavGraph.kt` — add splash → onboarding → hub routes
- `core/data/src/main/java/.../prefs/UserPreferencesDataStore.kt` — last session state

---

*Session 24 · Size M · 6 atoms · App entry points*

---

# Session 25 — Settings + Locale Switching + ISO Symbols

## GOAL
Implement settings screen, locale picker with immediate UI update, and ISO 7010 safety symbol picker (~200 SVG symbols searchable in all locales).

## DEPENDS ON
- **S-04** (PD-10) — Preferences DataStore
- **S-07** (L10N-01) — String resources for 6 languages

## ATOMS
- **ON-07** · Settings Screen (M)
- **L10N-02** · Locale Switching (S)
- **L10N-04** · ISO 7010 Safety Symbol Picker (M)

## SPEC REFERENCES (inlined)

### ON-07 · Settings
Sections: Account (Phase 2 stub), Active Kit, Integrations (Phase 3 stub), Defaults (line weight, font size, default color), Evidence (SHA-256 toggle, C2PA Phase 2 stub), Display (soft placement duration, haptic intensity).

### L10N-02 · Locale Switching
Settings → picker. `AppCompatDelegate.setApplicationLocales()` (API 33+), fallback API 29-32. Immediate UI update. Per-app override works (TechNotes German, phone English).

### L10N-04 · ISO 7010 Safety Symbol Picker
~200 SVGs from Wikimedia Commons. Hierarchical grid: P (prohibition red), W (warning yellow), M (mandatory blue), E (safe condition green), F (fire red). Each: ID + localized name + SVG + category. Searchable in all locales.

## DELIVERABLES

### 1. SettingsScreen.kt + SettingsViewModel.kt
### 2. LocalePicker.kt
### 3. IsoSymbolPicker.kt + SVG assets

## KEY IMPLEMENTATION DETAILS

### Per-App Locale
```kotlin
// API 33+
val localeList = LocaleListCompat.forLanguageTags(selectedLocale)
AppCompatDelegate.setApplicationLocales(localeList)

// API 29-32 fallback
val config = resources.configuration
config.setLocale(Locale(selectedLocale))
resources.updateConfiguration(config, resources.displayMetrics)
recreate()
```

### ISO 7010 Symbol Data
```kotlin
data class IsoSymbol(
    val id: String,          // "P001", "W012", "M004"
    val category: IsoCategory,
    val svgResId: Int,       // R.raw.iso_p001
    val nameResIds: Map<String, Int>,  // locale → string resource
)
```
SVGs bundled as raw resources (~200 files, ~2MB total). Picker uses `LazyVerticalGrid` with category tabs.

## TEST CRITERIA
- [ ] Settings render all sections
- [ ] Locale switch immediate (no restart required on API 33+)
- [ ] Per-app locale works (TechNotes ≠ system language)
- [ ] ISO symbols: all 5 categories render with correct colors
- [ ] Symbol search works in all 6 locales
- [ ] Phase 2/3 sections show as disabled stubs

## FILES TO CREATE / MODIFY

### Create
1. `feature-settings/src/main/java/.../SettingsScreen.kt`
2. `feature-settings/src/main/java/.../SettingsViewModel.kt`
3. `core/ui/src/main/java/.../symbols/IsoSymbolPicker.kt`
4. `core/ui/src/main/res/raw/iso7010/` (SVG files)

### Modify
- `app/src/main/java/.../navigation/NavGraph.kt` — settings route

---

*Session 25 · Size M · 3 atoms · Settings + localization*

---

# Session 26 — Interaction Polish

## GOAL
Implement quick-label dictionary (autocomplete from usage history), swipe-between-photos in Annotate mode, text keyboard-aware layout (photo compression), and engineering term autocomplete.

## DEPENDS ON
- **S-14** (DP-25) — TextElement, TextRenderer
- **S-12** (IC-03, IC-05) — Toolbar, tool persistence
- **S-09** (CE-04) — Gesture router

## ATOMS
- **IC-26** · Quick-Label Dictionary (M)
- **IC-27** · Swipe-Between-Photos in Annotate (M)
- **IC-29** · Text Without Keyboard Blackout (M)
- **VO-06** · Engineering Autocomplete (M)

## SPEC REFERENCES (inlined)

### IC-26 · Quick-Label Dictionary
After 3+ uses: most-used text labels as suggestion chips above keyboard. Frequency-sorted, recency-boosted. Room DB stores usage count + last-used. Tap chip = instant insert.

### IC-27 · Swipe-Between-Photos in Annotate (CRITICAL)
Fast edge-initiated horizontal swipe (>600dp/s, starting from edge 15%, spanning >60% width) navigates next/previous photo WITHOUT leaving Annotate mode. Distinguished from drawing by zone+velocity+span. All carry-forwards apply: tool, color, size, equipment ID, numbering (D-003 → D-004 on swipe).

**Acceptance:** Zero false positives during drawing. Numbering carries across. 60fps transition.

### IC-29 · Text Without Keyboard Blackout
1. Photo compresses to top 50% when keyboard opens — target always visible.
2. Live preview bubble at annotation position, real-time as user types.
3. Double-tap blank canvas → immediate text cursor (no tool selection needed).

### VO-06 · Engineering Autocomplete
Room DB with engineering terms, standard codes, equipment IDs. Trie prefix matching <5ms. Domain dictionaries per Trade Kit (~5000 terms each). "corr-" → corrosion, corroded, corrective action. Recent/frequent prioritized. Integrated into all text inputs. Fully offline.

## DELIVERABLES

### 1. SwipeBetweenPhotos.kt
```kotlin
class SwipeBetweenPhotos {
    // Detect: edge start (within 15% of screen edge)
    //       + high velocity (>600dp/s)
    //       + long span (>60% screen width)
    // MUST NOT conflict with drawing gestures
    // On detect: navigate photo, preserve all tool state
    // Carry-forward: tool, color, size, equipment ID, numbering sequence
}
```

### 2. KeyboardAwareLayout.kt
```kotlin
class KeyboardAwareLayout {
    // On keyboard open: compress photo to top 50%
    // Annotation position remains visible (auto-scroll if needed)
    // Live preview bubble at annotation point
    // On keyboard close: restore full layout <200ms
}
```

### 3. EngineeringAutocomplete.kt
```kotlin
class EngineeringAutocomplete @Inject constructor(db: TechNotesDatabase) {
    // Trie-based prefix matching
    // Sources: built-in engineering terms + user history
    // Per-kit dictionaries loaded on kit activation
    // fun getSuggestions(prefix: String, limit: Int = 5): List<String>
}
```

### 4. QuickLabelDictionary.kt
```kotlin
class QuickLabelDictionary @Inject constructor(dao: LabelUsageDao) {
    // Track: label text, usage count, last used timestamp
    // Threshold: show after 3+ uses
    // Display: chips above keyboard, frequency-sorted, recency-boosted
    // Tap chip: insert text at cursor
}
```

## KEY IMPLEMENTATION DETAILS

### Swipe Detection (Zero False Positives)
Three conditions ALL required:
1. Touch starts within 15% of left or right edge
2. Velocity >600dp/s horizontal
3. Span >60% of screen width
Any drawing gesture starts in the canvas center and has lower velocity → never triggers.

### Keyboard Compression
```kotlin
// Detect keyboard via WindowInsets.ime
val imeVisible = WindowInsets.ime.getBottom(density) > 0
if (imeVisible) {
    // Animate canvas to top 50% (spring animation, 200ms)
    // Scale photo to fit
    // Ensure annotation point is visible
}
```

### Engineering Term Database
Pre-seeded with ~5000 general terms + per-kit additions. Schema:
```kotlin
@Entity
data class EngineeringTerm(
    @PrimaryKey val id: Int,
    val term: String,
    val domain: String,     // general, electrical, piping, welding, footwear
    val frequency: Int = 0, // user usage count
)
```

## TEST CRITERIA
- [ ] Swipe: zero false positives during drawing (test 100 drawing strokes)
- [ ] Swipe: photo navigation works from both edges
- [ ] Swipe: numbering carries across (D-003 → D-004)
- [ ] Swipe: 60fps transition animation
- [ ] Keyboard: photo compresses to top 50%, annotation visible
- [ ] Keyboard: compression <200ms
- [ ] Keyboard: double-tap canvas → text cursor (no tool selection)
- [ ] Autocomplete: suggestions <50ms
- [ ] Autocomplete: domain terms load with kit
- [ ] Quick-label: chips appear after 3+ uses of same text
- [ ] Quick-label: tap chip inserts text

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../input/SwipeBetweenPhotos.kt`
2. `feature-canvas/src/main/java/.../text/KeyboardAwareLayout.kt`
3. `core/data/src/main/java/.../autocomplete/EngineeringAutocomplete.kt`
4. `core/data/src/main/java/.../autocomplete/QuickLabelDictionary.kt`

### Modify
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt` — swipe detection
- `feature-canvas/src/main/java/.../render/TextRenderer.kt` — autocomplete integration
- `core/data/src/main/java/.../db/TechNotesDatabase.kt` — EngineeringTerm + LabelUsage entities

---

*Session 26 · Size M · 4 atoms · Interaction polish*

---

# Session 27 — Export Pipeline

## GOAL
Implement canvas-to-bitmap export, burned-in JPEG with annotations, sidecar JSON (Annotation Envelope), metadata bar burn-in, Android Share Sheet integration, and Annotation Envelope format specification.

## DEPENDS ON
- **S-10** (CE-06) — Rendering pipeline (reused for software rendering to bitmap)
- **S-06** (PD-04, PD-14) — Annotation data model + provenance

## ATOMS
- **CE-11** · Canvas Export — Render to Bitmap (M)
- **EX-01** · Burned-In JPEG Export (M)
- **EX-02** · Sidecar JSON Export — Annotation Envelope (M)
- **EX-03** · Metadata Bar Burn-In (S)
- **EX-05** · Android Share Sheet (S)
- **IL-02** · Annotation Envelope Format Spec (M)

## SPEC REFERENCES (inlined)

### CE-11 · Canvas Export
`CanvasExporter.exportToBitmap()`: software rendering, full original resolution, proportional annotation scale, optional metadata bar, JPEG quality configurable (default 95%).

### EX-01 · Burned-In JPEG Export
Photo + annotations → permanent render via CE-11. Full original resolution. Watermark on free tier, clean on Pro. <3s for 12MP + 50 annotations. <5MB typical output.

### EX-02 · Sidecar JSON Export
JSON alongside JPEG: full geometry (image-space), semantic tags, metadata (GPS, timestamp, technician, heading), evidence (SHA-256, provenance per PD-14), project context, schema version. `{photo}_annotations.json`. Machine-readable. Round-trip: import sidecar → identical annotations.

### EX-03 · Metadata Bar Burn-In
Bottom strip on export: GPS, timestamp, tech ID, project, heading. Configurable fields. Dark semi-transparent bg. Survives screenshot/WhatsApp/email.

### EX-05 · Android Share Sheet
Single photo: JPEG + optional sidecar. Multi: ZIP. Report: PDF. `Intent.ACTION_SEND` / `SEND_MULTIPLE`. `FileProvider` content URIs.

### IL-02 · Annotation Envelope Format Spec
Universal export container with 5 outputs: visual (JPEG), structured (JSON), metadata, evidence (SHA-256/C2PA), summary. Schema versioned, machine-readable, open format.

## DELIVERABLES

### 1. CanvasExporter.kt (in `:feature-canvas`)
```kotlin
class CanvasExporter {
    // fun exportToBitmap(
    //     photo: Bitmap,
    //     annotations: List<AnnotationElement>,
    //     includeMetadataBar: Boolean,
    //     quality: Int = 95
    // ): Bitmap
    
    // Software rendering at full original resolution
    // Same RendererRegistry used for on-screen rendering
    // Annotation scale proportional to image (not screen)
}
```

### 2. BurnedInExporter.kt
### 3. SidecarJsonWriter.kt
### 4. AnnotationEnvelope.kt (format spec as Kotlin data classes)
### 5. ShareSheetIntegration.kt

## KEY IMPLEMENTATION DETAILS

### Software Rendering for Export
Reuse the same `RendererRegistry` and renderers from S-10, but render onto a software `Canvas` at full photo resolution instead of the on-screen Canvas. Annotations scale proportionally — a stroke that appears 3dp on screen should be proportionally 3dp relative to the full image dimensions.

### Sidecar JSON Schema
```json
{
  "schema_version": "1.0.0",
  "technotes_version": "1.0.0",
  "exported_at": "2026-04-11T14:30:00Z",
  "project": { "id": "", "name": "", "created_at": "" },
  "photo": {
    "original_sha256": "",
    "dimensions": { "width": 4032, "height": 3024 },
    "metadata": { "gps": {}, "timestamp": "", "heading": 0, "technician": "" }
  },
  "annotations": [
    {
      "type": "LineElement",
      "id": "",
      "geometry": { "startPoint": {}, "endPoint": {} },
      "style": { "strokeWidth": 3, "strokeColor": "", "dashType": "SOLID" },
      "provenance": { "created_at": "", "created_by": "", "modified_at": "" },
      "semantic_tags": ["defect", "crack"]
    }
  ],
  "evidence": {
    "photo_sha256": "",
    "annotation_hash": "",
    "export_timestamp": ""
  }
}
```

### Share Sheet
```kotlin
fun sharePhoto(context: Context, jpeg: File, sidecar: File?) {
    val uris = ArrayList<Uri>()
    uris.add(FileProvider.getUriForFile(context, "${context.packageName}.provider", jpeg))
    sidecar?.let { uris.add(FileProvider.getUriForFile(context, "${context.packageName}.provider", it)) }
    
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/jpeg"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
```

## TEST CRITERIA
- [ ] Export matches on-screen rendering exactly
- [ ] 12MP + 50 annotations <3s export time
- [ ] JPEG quality configurable, default 95%
- [ ] Sidecar JSON validates against schema
- [ ] JSON round-trip: import → identical annotations
- [ ] Metadata bar renders at bottom of exported image
- [ ] Share Sheet opens with JPEG + sidecar
- [ ] FileProvider content URIs work (no FileUriExposedException)
- [ ] No OOM on 12MP export

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../export/CanvasExporter.kt`
2. `feature-export/src/main/java/.../BurnedInExporter.kt`
3. `feature-export/src/main/java/.../SidecarJsonWriter.kt`
4. `feature-export/src/main/java/.../AnnotationEnvelope.kt`
5. `feature-export/src/main/java/.../ShareSheetIntegration.kt`

### Modify
- `app/src/main/res/xml/file_paths.xml` — FileProvider paths
- `app/src/main/AndroidManifest.xml` — FileProvider declaration

---

*Session 27 · Size L · 6 atoms · Export pipeline — enables S-28, S-29, S-30*
