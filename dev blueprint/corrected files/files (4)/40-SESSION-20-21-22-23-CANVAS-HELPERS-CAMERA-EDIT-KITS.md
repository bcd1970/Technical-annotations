# Session 20 — Canvas Helpers (Labels + Guides + Placement + Metadata)

## GOAL
Implement smart label placement, alignment guides, soft placement window, photo-aware saliency, and auto-metadata overlay. These are the quality-of-life systems that make annotations look professional.

## DEPENDS ON
- **S-10** (CE-06) — Rendering pipeline
- **S-05** (PD-09) — Auto-metadata capture service (GPS, timestamp, heading)

## ATOMS
- **CE-09** · Smart Label Placement — 8-position greedy (S)
- **CE-10** · Alignment Guides — Figma-style snapping (S)
- **IC-13** · Soft Placement — 1.5s window (S)
- **IC-30** · Photo-Aware Label Placement (S)
- **DP-56** · Auto-Metadata Overlay (M)

## SPEC REFERENCES (inlined)

### CE-09 · Smart Label Placement
`LabelPlacer.findBestPosition()`: 8-position greedy (N, NE, E, SE, S, SW, W, NW). Avoids overlap with existing annotations. <5ms. Labels draggable after placement. Falls back to least-overlapping if all occupied.

### CE-10 · Alignment Guides
1px red guide at 12px proximity. Haptic CLOCK_TICK on snap. Equal-spacing indicators for 3+ aligned annotations. Speed-based escape: fast drag (>400dp/s) bypasses all snapping. Two-finger drag bypasses. <2ms per frame.

### IC-13 · Soft Placement (1.5s Window)
Element draggable for 1.5s after placement. Tap elsewhere confirms early. Subtle pulse animation. Configurable 0.5–3.0s.

### IC-30 · Photo-Aware Label Placement
Extends CE-09 with Canny edge detection (<8ms) for photo saliency. Labels pushed away from saliency hotspots AND 15% exclusion radius around annotation's own point.

### DP-56 · Auto-Metadata Overlay
Semi-transparent bar: GPS, timestamp, technicianId, heading, project name. Auto-generated, configurable fields. Renders in export, toggleable in live view.

## DELIVERABLES

### 1. LabelPlacer.kt
```kotlin
object LabelPlacer {
    // fun findBestPosition(
    //     anchorPoint: ImagePoint,
    //     labelSize: Size,
    //     existingAnnotations: List<BoundingBox>,
    //     saliencyMap: SaliencyMap?,  // from IC-30
    //     imageBounds: Rect
    // ): ImagePoint
    
    // 8 candidates: N, NE, E, SE, S, SW, W, NW at fixed offset
    // Score each: overlap penalty + saliency penalty + out-of-bounds penalty
    // Return lowest penalty position
    // If all bad: return least-overlapping
}
```

### 2. AlignmentGuideEngine.kt
```kotlin
class AlignmentGuideEngine {
    // fun checkAlignment(
    //     movingElement: AnnotationElement,
    //     allElements: List<AnnotationElement>,
    //     viewMatrix: Matrix
    // ): List<AlignmentGuide>
    
    // Check: horizontal center, vertical center, top, bottom, left, right edges
    // Proximity threshold: 12dp in screen-space
    // Snap: adjust position to align
    // Equal-spacing: if 3+ elements aligned, show equal-spacing indicators
    // Speed escape: if drag velocity > 400dp/s, return empty list
}

data class AlignmentGuide(
    val type: GuideType,  // HORIZONTAL, VERTICAL
    val position: Float,  // in image-space
    val snapDelta: Float, // how much to adjust
)
```

### 3. SoftPlacementController.kt
```kotlin
class SoftPlacementController {
    // Manages the 1.5s window after element placement:
    // - Element is in "soft" state: highlighted, draggable without selection
    // - Subtle pulse animation (scale 1.0 → 1.02 → 1.0, repeating)
    // - Tap elsewhere → confirm (exit soft state)
    // - Timer expires → auto-confirm
    // - During soft: alignment guides active
    // - Window duration configurable (0.5-3.0s via settings PD-10)
}
```

### 4. MetadataOverlayRenderer.kt
```kotlin
class MetadataOverlayRenderer : AnnotationRenderer {
    // Renders DP-56 auto-metadata overlay:
    // - Semi-transparent dark bar at bottom of image
    // - Fields: GPS coordinates, timestamp, technician ID, compass heading, project name
    // - Configurable: which fields to show (via settings)
    // - Toggleable in live view (but always available for export)
    // - IBM Plex Mono font for data readability
    // - Uses PD-09 MetadataCaptureService for data
}
```

## KEY IMPLEMENTATION DETAILS

### Saliency Map (IC-30)
Simple Canny edge detection on a downscaled version of the photo:
```kotlin
// Downscale to 200px wide for speed
// Apply Gaussian blur
// Canny edge detection → binary edge map
// Convert to heat map: high-edge-density regions = high saliency
// Total computation: <8ms
```
Labels avoid high-saliency regions. The 15% exclusion radius prevents labels from sitting on top of the annotation point they refer to.

### Alignment Guide Rendering
Guides render as 1px red lines that extend across the full canvas width/height. They appear/disappear smoothly (alpha animation 150ms). Equal-spacing indicators are small marks showing the distance between aligned elements.

### Metadata Overlay in Export
The metadata bar is part of the rendered output (burned into JPEG via S-27). It's not an annotation — it's a system-generated overlay. Stored as configuration, not as an element in the annotation list.

## TEST CRITERIA
- [ ] Label placement avoids existing annotations (8 positions tested)
- [ ] Label placement <5ms for 50 annotations
- [ ] Alignment guides appear at 12dp proximity
- [ ] Haptic CLOCK_TICK on snap
- [ ] Fast drag (>400dp/s) bypasses snapping
- [ ] Soft placement: element draggable for 1.5s with pulse animation
- [ ] Tap elsewhere confirms soft placement early
- [ ] Metadata overlay shows GPS/timestamp/heading/tech ID
- [ ] Metadata bar renders correctly in export

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../placement/LabelPlacer.kt`
2. `feature-canvas/src/main/java/.../placement/AlignmentGuideEngine.kt`
3. `feature-canvas/src/main/java/.../placement/SoftPlacementController.kt`
4. `feature-canvas/src/main/java/.../render/MetadataOverlayRenderer.kt`

### Modify
- `feature-canvas/src/main/java/.../gesture/TapPlaceHandler.kt` — integrate soft placement
- `feature-canvas/src/main/java/.../input/StatelessGestureRouter.kt` — alignment guide checks during drag
- `feature-canvas/src/main/java/.../render/LeaderRenderer.kt` — use LabelPlacer for label position

---

*Session 20 · Size M · 5 atoms · Quality-of-life systems*

---

# Session 21 — Camera + Photo Picker

## GOAL
Implement CameraX viewfinder with capture, pre-warming at launch, metadata injection into EXIF + PhotoEntity, and gallery photo picker with multi-select.

## DEPENDS ON
- **S-05** (PD-03, PD-09) — Photo data model, metadata capture service

## ATOMS
- **CC-01** · CameraX Viewfinder + Capture (M)
- **CC-02** · Camera Pre-Warming (S)
- **CC-03** · Photo Metadata Injection (S)
- **CC-04** · Gallery Photo Picker (S)

## SPEC REFERENCES (inlined)

### CC-01 · CameraX Viewfinder + Capture
CameraX Preview + ImageCapture. Compose-native viewfinder (camera-compose 1.5+). Back camera default, front toggle. Flash AUTO/ON/OFF. Shutter with haptic. Photo to app-internal storage. CAMERA permission handling.

### CC-02 · Camera Pre-Warming
Binding starts at app launch (Application.onCreate or LifecycleOwner). Never "loading camera" state. Release on background, re-acquire on foreground.

### CC-03 · Photo Metadata Injection
On capture: metadata written to EXIF + PhotoEntity fields (redundant for survival). Works when GPS denied (null fields). Heading from compass sensor.

### CC-04 · Gallery Photo Picker
Android PickVisualMedia. Copy to app-internal storage. SHA-256 on import. EXIF extraction. Multi-select up to 20.

## DELIVERABLES

### 1. CameraScreen.kt
```kotlin
@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    // Compose CameraX viewfinder
    // Shutter button with haptic CONFIRM
    // Flash toggle (AUTO/ON/OFF cycle)
    // Camera flip (back/front)
    // Gallery button → picker
    // Permission handling with rationale
}
```

### 2. CameraViewModel.kt
```kotlin
@HiltViewModel
class CameraViewModel : ViewModel() {
    // CameraX use cases: Preview + ImageCapture
    // Pre-warm: bind on init, unbind on lifecycle
    // Capture → save to app-internal → create PhotoEntity → inject metadata
    // Flash state, camera facing state
}
```

### 3. GalleryPicker.kt
```kotlin
class GalleryPicker {
    // PickMultipleVisualMedia(maxItems = 20)
    // For each selected: copy to app-internal, compute SHA-256, extract EXIF
    // Create PhotoEntity per image
    // Progress indicator for multi-select
}
```

## KEY IMPLEMENTATION DETAILS

### Pre-Warming
```kotlin
// In Application or main Activity:
class TechNotesApp : Application() {
    override fun onCreate() {
        cameraProvider = ProcessCameraProvider.getInstance(this)
        // Camera binds immediately — viewfinder ready when user opens camera
    }
}
```
Release camera in `ON_STOP`, re-acquire in `ON_START` via `DefaultLifecycleObserver`.

### Metadata Injection
On capture:
1. GPS coordinates from `FusedLocationProviderClient` (if permitted)
2. Compass heading from `SensorManager` (TYPE_ROTATION_VECTOR)
3. Timestamp from `System.currentTimeMillis()`
4. Write to EXIF tags AND `PhotoEntity.metadata` field (redundant storage)
5. If GPS permission denied: fields are null (no crash)

### SHA-256 on Import
```kotlin
fun computeSha256(inputStream: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var read: Int
    while (inputStream.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```
<200ms for 12MP JPEG.

## TEST CRITERIA
- [ ] Viewfinder appears <500ms from camera screen open
- [ ] Capture-to-display <1s
- [ ] Full sensor resolution on S25 Ultra
- [ ] Flash toggle cycles AUTO→ON→OFF→AUTO
- [ ] Camera flip works (back→front→back)
- [ ] Pre-warm: no "loading" state on camera screen
- [ ] GPS/heading/timestamp written to EXIF AND PhotoEntity
- [ ] GPS denied: fields null, no crash
- [ ] Gallery picker: multi-select up to 20 photos
- [ ] SHA-256 computed on import (<200ms for 12MP)
- [ ] Imported photos copied to app-internal storage (not referencing gallery URI)

## FILES TO CREATE / MODIFY

### Create
1. `feature-camera/src/main/java/.../CameraScreen.kt`
2. `feature-camera/src/main/java/.../CameraViewModel.kt`
3. `feature-camera/src/main/java/.../GalleryPicker.kt`

### Modify
- `app/src/main/java/.../TechNotesApp.kt` — camera pre-warming
- `app/src/main/AndroidManifest.xml` — CAMERA, ACCESS_FINE_LOCATION permissions

---

*Session 21 · Size M · 4 atoms · Camera module*

---

# Session 22 — Edit Mode (Flip/Rotate/Crop/Adjust)

## GOAL
Implement Edit mode toolbar, flip/rotate operations, crop tool with aspect ratio presets, and AGSL RuntimeShader image adjustments (brightness, contrast, saturation, exposure). Non-destructive — original photo preserved.

## DEPENDS ON
- **S-11** (IC-01) — Mode state machine (EDIT mode)
- **S-08** (CE-01, CE-02) — Base canvas + viewMatrix

## ATOMS
- **IC-10** · Edit Mode Toolbar — 6 slots (M)
- **EP-01** · Flip & Rotate (S)
- **EP-02** · Crop Tool (M)
- **EP-03** · AGSL RuntimeShader Adjustments (M)

## SPEC REFERENCES (inlined)

### IC-10 · Edit Mode Toolbar (6 slots)
Flip, Rotate, Crop, Adjust (inline sliders), Overlay, More. Overlay and More are Phase 2 stubs.

### EP-01 · Flip & Rotate
Flip H/V, rotate 90° CW/CCW, free rotation. Annotations transform with photo (coordinates remapped).

### EP-02 · Crop Tool
Draggable rectangle, aspect lock (free/1:1/4:3/16:9), grid overlay (rule of thirds). Annotations outside crop deleted, coords remapped to new origin.

### EP-03 · AGSL RuntimeShader Adjustments
AGSL (API 33+): brightness, contrast, saturation, exposure. Inline sliders below toolbar. Real-time preview. Non-destructive (original preserved). `ColorMatrix` fallback for API <33.

## DELIVERABLES

### 1. EditToolbar.kt
```kotlin
@Composable
fun EditToolbar(onAction: (EditAction) -> Unit) {
    // 6 slots: Flip | Rotate | Crop | Adjust | Overlay(stub) | More(stub)
    // Matches design system tokens (dark, monochrome)
}
```

### 2. CropTool.kt
```kotlin
class CropTool {
    // Draggable crop rectangle overlay
    // Aspect ratio presets: Free, 1:1, 4:3, 16:9
    // Rule-of-thirds grid overlay
    // On confirm: remap annotation coordinates, delete out-of-bounds
    // Crop is non-destructive: stored as CropRect in PhotoEntity
}
```

### 3. ShaderAdjustments.kt
```kotlin
class ShaderAdjustments {
    // AGSL RuntimeShader for API 33+:
    //   uniform float brightness;  // -1.0 to 1.0
    //   uniform float contrast;    // 0.0 to 2.0
    //   uniform float saturation;  // 0.0 to 2.0
    //   uniform float exposure;    // -2.0 to 2.0
    // ColorMatrix fallback for API 29-32
    // Inline sliders: one per parameter, horizontal below toolbar
    // Real-time preview <16ms per frame
    // Non-destructive: store adjustment values, apply on render
}
```

## KEY IMPLEMENTATION DETAILS

### Non-Destructive Edits
All edit operations store parameters, not modified pixels:
- Flip/rotate: `PhotoEntity.transform: AffineTransform`
- Crop: `PhotoEntity.cropRect: Rect?`
- Adjustments: `PhotoEntity.adjustments: ShaderParams`

Original photo bytes never modified. Transforms applied at render time and on export.

### Annotation Coordinate Remapping
After crop: every annotation's coordinates must be remapped:
```kotlin
fun remapForCrop(point: ImagePoint, cropRect: Rect): ImagePoint {
    return ImagePoint(point.x - cropRect.left, point.y - cropRect.top)
}
```
Annotations fully outside crop rect → deleted (with undo entry).

### AGSL Shader
```agsl
uniform shader image;
uniform float brightness;
uniform float contrast;
uniform float saturation;
uniform float exposure;

half4 main(float2 coord) {
    half4 color = image.eval(coord);
    // Exposure
    color.rgb *= pow(2.0, exposure);
    // Brightness
    color.rgb += brightness;
    // Contrast
    color.rgb = (color.rgb - 0.5) * contrast + 0.5;
    // Saturation
    half luma = dot(color.rgb, half3(0.299, 0.587, 0.114));
    color.rgb = mix(half3(luma), color.rgb, saturation);
    return color;
}
```

## TEST CRITERIA
- [ ] Edit mode toolbar renders 6 slots (4 active, 2 stubs)
- [ ] Flip H/V works, annotations mirror correctly
- [ ] Rotate 90° CW/CCW, annotations remap
- [ ] Crop with aspect lock works
- [ ] Annotations outside crop deleted
- [ ] AGSL adjustments render in <16ms (real-time preview)
- [ ] `ColorMatrix` fallback works on API 29-32
- [ ] Non-destructive: original photo recoverable
- [ ] Edit mode undo stack independent from Annotate undo stack

## FILES TO CREATE / MODIFY

### Create
1. `feature-canvas/src/main/java/.../edit/EditToolbar.kt`
2. `feature-canvas/src/main/java/.../edit/CropTool.kt`
3. `feature-canvas/src/main/java/.../edit/ShaderAdjustments.kt`
4. `feature-canvas/src/main/res/raw/adjustments.agsl`

### Modify
- `feature-canvas/src/main/java/.../mode/ModeStateMachine.kt` — EDIT mode handling
- `core/data/src/main/java/.../photo/PhotoEntity.kt` — add transform, cropRect, adjustments fields

---

*Session 22 · Size M · 4 atoms · Edit mode*

---

# Session 23 — Trade Kit System

## GOAL
Implement trade kit data model, activation engine, 5 launch kits (Electrical, Pipe & Valve, Welding, General Maintenance, Footwear), kit↔tool resolver, catalog browse UI, and kit-specific semantic colors.

## DEPENDS ON
- **S-03** (PD-05) — AnnotationElement types (for TK-04 tool resolver)
- **S-04** (PD-01, PD-10) — Room database, preferences DataStore
- **S-12** (IC-03) — Annotate toolbar (kits configure toolbar slots)

## ATOMS
- **TK-01** · Trade Kit Data Model + Registry (M)
- **TK-02** · Kit Activation Engine (M)
- **TK-03** · Five Launch Kits (L)
- **TK-04** · Kit ↔ Tool Resolver (S)
- **TK-06** · Kit Catalog Browse UI (M)
- **TK-11** · Kit Semantic Colors (S)

## SPEC REFERENCES (inlined)

### TK-01 · Trade Kit Data Model + Registry
`TradeKit`: id, name, displayName (localized), icon, toolSet (List<ElementType>), defaultToolbarSlots, defaultSemanticColors, defaultNumberingPrefix, collageTemplates, reportTemplate. `TradeKitRegistry`: built-in + imported kits. Room entity for active kit + customizations. Kit switch <100ms.

### TK-02 · Kit Activation Engine
`KitActivationManager`: applies kit config to toolbar. Adds kit tools to ⋯More. Suggests slot replacements (non-destructive). Kit→kit switching clean. 5s undo toast.

### TK-03 · Five Launch Kits
| Kit | Key Additions | Prefix |
|-----|--------------|--------|
| Electrical Inspector | Op-Amp, Logic Gate, Parallel Plates, Zigzag, Coil | E- |
| Pipe & Valve | Bowtie, Triangle+Bar, Diamond Inline, Break Lines, Flow Arrows | P- |
| Welding Inspector | GD&T FCF, Weld Symbol, Surface Finish, Tolerance, Crosshairs | W- |
| General Maintenance | Condition stamps, Numbered Pins, Before/After | D- |
| Footwear & Garment | Stitch, Grainline, Hardware, Notch, EU Material Grid | F- |

Note: Phase 2 tools (Op-Amp, Logic Gate, etc.) are listed in kit definitions but stubbed — they appear in ⋯More as "coming soon" placeholders. Only Phase 1 tools are functional.

### TK-04 · Kit ↔ Tool Resolver
Resolves visible tools from active kit(s). Multiple kits = union. Priority: user custom > kit default > universal.

### TK-06 · Kit Catalog Browse UI
Via ⋯More. Domain sections. Kit card: icon, name, tool count, preview. One-tap activate. Search.

### TK-11 · Kit Semantic Colors
Per-kit color meanings. Welding: red = defect (default). Maintenance: green = pass (default). Strip labels update with kit context. Tracked per kit in PD-10.

## DELIVERABLES

### 1. TradeKit.kt + TradeKitRegistry.kt
```kotlin
@Serializable
data class TradeKit(
    val id: String,
    val name: String,
    val displayNameResId: Int,
    val icon: Int,                      // drawable resource
    val toolSet: List<String>,          // element type IDs
    val defaultToolbarSlots: List<String>,  // 5 category defaults
    val defaultSemanticColors: Map<String, Long>,
    val numberingPrefix: String,
    val isBuiltIn: Boolean = true,
)

object TradeKitRegistry {
    fun getBuiltInKits(): List<TradeKit>
    fun getActiveKit(): TradeKit
    fun getCustomKits(): List<TradeKit>   // Phase 2
}
```

### 2. KitActivationManager.kt
```kotlin
class KitActivationManager @Inject constructor(
    private val registry: TradeKitRegistry,
    private val prefs: UserPreferencesDataStore,
) {
    fun activateKit(kitId: String)      // <100ms
    fun deactivateKit()                 // return to universal
    fun getVisibleTools(): List<ToolDefinition>  // resolved from active kit
}
```

### 3. LaunchKits.kt — 5 kit definitions
### 4. KitCatalogScreen.kt — browse/activate UI
### 5. KitToolResolver.kt — resolves visible tools from active kit + universal

## KEY IMPLEMENTATION DETAILS

### Universal Tools (Always Available)
Regardless of kit: straight line, arrow, circle, rectangle, text, freehand, dimension, stamps, numbered pins. Kit adds domain-specific tools ON TOP of these.

### Kit Switch Performance
Target <100ms. Kit activation updates toolbar slots (5 categories) + semantic color map + numbering prefix. No database migration — kit config is in-memory with persistence.

### Semantic Color Context
```kotlin
// General kit: red = defect, green = pass, yellow = caution
// Welding kit: red = defect, blue = informational, green = pass
// Color strip labels change: "Defect" vs "Fail" etc.
```

## TEST CRITERIA
- [ ] All 5 kits load with correct tool sets
- [ ] Kit activation changes toolbar <100ms
- [ ] Universal tools always available regardless of kit
- [ ] Kit switch with 5s undo toast
- [ ] Catalog UI: all kits browsable, one-tap activate
- [ ] Semantic colors update on kit change
- [ ] Numbering prefix changes on kit change (D-001 → E-001)
- [ ] Kit persists across app restart

## FILES TO CREATE / MODIFY

### Create
1. `core/data/src/main/java/.../kit/TradeKit.kt` + `TradeKitRegistry.kt`
2. `core/data/src/main/java/.../kit/KitActivationManager.kt`
3. `core/data/src/main/java/.../kit/LaunchKits.kt`
4. `feature-canvas/src/main/java/.../kit/KitCatalogScreen.kt`

### Modify
- `feature-canvas/src/main/java/.../ui/AnnotateToolbar.kt` — read active kit for slot config
- `feature-canvas/src/main/java/.../ui/SemanticInkStrip.kt` — kit-specific color labels
- `core/data/src/main/java/.../prefs/UserPreferencesDataStore.kt` — active kit preference

---

*Session 23 · Size L · 6 atoms · Trade kit system*
