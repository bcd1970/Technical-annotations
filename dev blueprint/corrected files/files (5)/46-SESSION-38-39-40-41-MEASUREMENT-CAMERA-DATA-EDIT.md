# 46 — Sessions 38–41: Measurement + Camera + Data + Edit

> **Four independent sessions** extending measurement, camera, data, and edit subsystems. All roots depend only on Phase 1.

---

## S-38 · Area Measurement + Cross-Photo Annotations

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| DP-16 | Area measurement | M | DP-64 (polygon), PD-06 |
| DP-65 | Cross-photo annotations | M | CL-01, CE-06 |

### AREA MEASUREMENT (DP-16)

Extends polygon (DP-64 from Phase 1 S-16) with area calculation. User draws a closed polygon → area displayed at centroid. Uses calibration from DP-17 (S-33) if available.

```kotlin
@Serializable
data class AreaMeasurementElement(
    override val id: String = UUID.randomUUID().toString(),
    val points: List<ImagePoint>,     // closed polygon vertices
    val precision: Int = 1,
    val unit: String = "px²",         // or "mm²", "m²" if calibrated
    val strokeWidth: Float = 1.5f,
    val strokeColor: Long,
    val fillColor: Long? = null,
    val fillAlpha: Float = 0.15f,
) : AnnotationElement()
```

**Area calculation:** Shoelace formula for simple polygons. Self-intersecting polygons handled by splitting into simple sub-polygons or displaying warning.

```kotlin
fun calculateArea(points: List<ImagePoint>): Float {
    var area = 0f
    val n = points.size
    for (i in 0 until n) {
        val j = (i + 1) % n
        area += points[i].x * points[j].y
        area -= points[j].x * points[i].y
    }
    return abs(area) / 2f
}
```

### CROSS-PHOTO ANNOTATIONS (DP-65)

Arrows and lines that span across collage cell gutters. An arrow from a detail photo to the corresponding area in an overview photo. Only available in collage mode.

```kotlin
@Serializable
data class CrossPhotoAnnotationElement(
    override val id: String = UUID.randomUUID().toString(),
    val startCellIndex: Int,
    val startPoint: ImagePoint,       // relative to start cell
    val endCellIndex: Int,
    val endPoint: ImagePoint,         // relative to end cell
    val annotationType: CrossPhotoType,  // ARROW, LINE, CALLOUT
    val strokeWidth: Float = 2f,
    val strokeColor: Long,
    val arrowheadStyle: ArrowheadStyle? = null,
    val label: String? = null,
) : AnnotationElement()
```

**Rendering:** The arrow is drawn on the collage-level canvas (above all cells), using cell-to-collage coordinate transforms. The line crosses the gutter naturally.

### DELIVERABLES
1. `AreaMeasurementRenderer.kt` — polygon + centroid label
2. `CrossPhotoAnnotationRenderer.kt` — inter-cell arrows
3. Data model classes
4. Shoelace area calculation tests

### ACCEPTANCE
- Area calculation accurate for convex + concave polygons
- Calibrated units display correctly
- Cross-photo arrows render across cell gutters
- Cross-photo elements survive cell reordering (re-map indices)

---

## S-39 · Camera Extensions

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| CC-05 | Split-second buffer | M | CC-01 |
| CC-06 | Video capture + frame extraction | M | CC-01 |

### SPLIT-SECOND BUFFER (CC-05)

Ring buffer that continuously captures frames 1–2 seconds before the shutter press. When the user takes a photo, they can "rewind" slightly to pick the perfect moment — critical for capturing fleeting defects (spark, drip, vibration peak).

```kotlin
class SplitSecondBuffer(
    private val bufferDuration: Long = 2000L,  // ms
    private val frameRate: Int = 10,            // fps in buffer
) {
    private val ringBuffer = ArrayDeque<BufferedFrame>(maxFrames)
    
    fun onPreviewFrame(frame: ImageProxy) // called from CameraX analysis
    fun captureWithRewind(): List<BufferedFrame>  // returns buffer + shutter frame
    fun getFrameAt(timestampMs: Long): BufferedFrame?
}

data class BufferedFrame(
    val bitmap: Bitmap,        // downscaled preview (720p max)
    val timestampMs: Long,
    val metadata: CaptureMetadata,
)
```

**Implementation:** Uses CameraX ImageAnalysis with a ring buffer. Frames are stored at reduced resolution (720p) to limit memory (~20 frames × ~1.5MB = ~30MB). On shutter press, the buffer is frozen and a horizontal filmstrip appears allowing the user to pick the best frame.

### VIDEO CAPTURE (CC-06)

CameraX video recording (3–15 seconds) with frame extraction via Media3 ExperimentalFrameExtractor. This is the foundation for the Video Notepad (VD-01, S-55).

```kotlin
class VideoCapture(
    private val maxDuration: Duration = 15.seconds,
) {
    fun startRecording(outputFile: File)
    fun stopRecording(): VideoResult
    fun isRecording(): Boolean
}

class FrameExtractor {
    suspend fun extractThumbnails(
        videoUri: Uri,
        count: Int = 10,
        targetSize: Size = Size(240, 180),
    ): List<VideoFrame>
    
    suspend fun extractFrameAt(
        videoUri: Uri,
        timestampUs: Long,
        fullResolution: Boolean = false,
    ): Bitmap
}
```

### DELIVERABLES
1. `SplitSecondBuffer.kt` — ring buffer + filmstrip picker
2. `VideoCapture.kt` — CameraX video recording
3. `FrameExtractor.kt` — Media3 thumbnail extraction

### ACCEPTANCE
- Buffer captures 1–2s pre-shutter at 10fps
- Memory stays under 50MB for buffer
- Video records 3–15s with visible timer
- Frame extraction <3s for 15s clip
- Extracted frames are pixel-accurate

---

## S-40 · Data Layer Extensions

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| PD-11 | Annotation layer model | M | PD-04 |
| EX-06 | C2PA content credentials | M | EX-01, PD-08 |
| EX-07 | SHA-256 audit log | M | PD-08 |
| EX-09 | Batch export | M | EX-01 |
| EX-11 | Version timeline | M | PD-04, PD-14 |

### ANNOTATION LAYERS (PD-11)

Per-user annotation layers for collaboration. Each layer has a unique color, visibility toggle, and lock state.

```kotlin
@Entity
data class AnnotationLayerEntity(
    @PrimaryKey val layerId: String,
    val projectId: String,
    val photoId: String,
    val userId: String?,          // null = default layer
    val displayName: String,
    val color: Long,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
)
```

Each `AnnotationElement` gets a `layerId` field linking it to its layer.

### C2PA CONTENT CREDENTIALS (EX-06)

Content Credentials (C2PA) signing for tamper-evident chain of custody. Uses the c2pa-java library or manual JUMBF box construction.

**Flow:** On export, the annotated image is signed with a C2PA manifest that includes: capture device, software version, annotation actions performed, GPS, timestamp, hash of original photo.

### SHA-256 AUDIT LOG (EX-07)

Independent hash chain for audit trail. Each action (create annotation, modify, delete) appends a hash entry:

```kotlin
data class AuditEntry(
    val hash: String,           // SHA-256
    val previousHash: String,   // chain link
    val action: String,
    val elementId: String?,
    val timestamp: Long,
    val userId: String?,
)
```

### BATCH EXPORT (EX-09)

Export multiple photos at once — all photos in a project or a selected subset. Progress indicator. Background processing via WorkManager.

### VERSION TIMELINE (EX-11)

Visual timeline of all edits to a photo/project. Each annotation action is a node. Tap node → see state at that point. Uses PD-14 (provenance) data from Phase 1.

### DELIVERABLES
1. `AnnotationLayer.kt` — Room entity + DAO
2. `C2PACredentialSigner.kt` — manifest generation
3. `SHA256AuditLog.kt` — hash chain
4. `BatchExporter.kt` — WorkManager multi-photo export
5. `VersionTimeline.kt` — Compose timeline UI

### ACCEPTANCE
- Layers isolate annotations visually
- C2PA manifest validates in Content Authenticity Initiative tools
- Hash chain verifiable externally
- Batch export handles 50+ photos with progress
- Timeline navigable, each node shows correct state

---

## S-41 · Edit Mode Extensions

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| EP-04 | Vignette | S | EP-03 |
| EP-05 | Film grain | S | EP-03 |
| EP-06 | Color LUT presets | M | EP-03 |
| EP-07 | Overlay browser | S | IC-10 |

### CONTEXT

Four edit tools extending the Phase 1 AGSL shader pipeline (EP-03). Vignette and film grain are AGSL shaders. Color LUT loads .cube files and applies via RuntimeShader. Overlay browser is the UI that organizes all edit tools in a scrollable grid.

### AGSL SHADERS

**Vignette (EP-04):**
```agsl
uniform float2 resolution;
uniform float intensity;  // 0.0–1.0
uniform float radius;     // 0.0–1.0

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float2 center = float2(0.5, 0.5);
    float dist = distance(uv, center);
    float vignette = smoothstep(radius, radius - 0.2, dist);
    half4 color = sample(image);
    return half4(color.rgb * mix(1.0 - intensity, 1.0, vignette), color.a);
}
```

**Film grain (EP-05):**
```agsl
uniform float2 resolution;
uniform float intensity;  // 0.0–1.0
uniform float time;       // for animation seed

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float noise = fract(sin(dot(uv + time, float2(12.9898, 78.233))) * 43758.5453);
    half4 color = sample(image);
    return half4(color.rgb + (noise - 0.5) * intensity * 0.3, color.a);
}
```

### COLOR LUT (EP-06)

Loads `.cube` 3D LUT files (industry standard). Bundled presets: Field (contrast boost for outdoor photos), Evidence (neutral, max detail), Thermal (heat-map-style false color for temperature annotations).

```kotlin
class ColorLUTManager {
    fun loadCube(assetPath: String): LUTData
    fun applyLUT(bitmap: Bitmap, lut: LUTData): Bitmap
    fun getPresets(): List<LUTPreset>
}
```

### OVERLAY BROWSER (EP-07)

Scrollable grid organizing all edit tools: Adjustments (existing 8), Vignette, Grain, LUT presets. Each cell shows a thumbnail preview applied to the current photo.

### DELIVERABLES
1. `VignetteShader.kt` — AGSL shader + slider UI
2. `FilmGrainShader.kt` — AGSL shader + slider UI
3. `ColorLUTManager.kt` — .cube loader + preset management
4. `OverlayBrowser.kt` — grid UI for all edit tools

### ACCEPTANCE
- Vignette + grain real-time preview at 60fps
- LUT presets render <100ms
- Overlay browser shows live thumbnails
- All effects non-destructive (stored as parameters, not baked until export)

---

*April 2026 · TechNotes Phase 2 Sessions 38–41 v1.0*
