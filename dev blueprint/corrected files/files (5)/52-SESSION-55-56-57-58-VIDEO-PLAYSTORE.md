# 52 — Sessions 55–58: Video + Play Store

> **Video Notepad, multi-video playback, and Play Store compliance.** The video system is TechNotes's biggest Phase 2 feature — diagnostic clips annotated with the same tools as photos.

---

## S-55 · Video Notepad

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| VD-01 | Video notepad | L | CC-06, CE-06 |
| VD-09 | Progressive reveal | S | CL-01 |

### NEW MODULE: `:feature-video`

```kotlin
// feature-video/build.gradle.kts
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.google.dagger.hilt.android")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature-canvas"))  // reuse drawing tools
    // Media3 for playback + frame extraction
    implementation("androidx.media3:media3-exoplayer:x.y.z")
    implementation("androidx.media3:media3-transformer:x.y.z")
    implementation("androidx.media3:media3-effect:x.y.z")
}
```

### VIDEO NOTEPAD (VD-01)

Not a video editor — a "Video Notepad" for diagnostic clips. Record 3–15 second clips, browse frames via filmstrip, annotate individual frames with the same tools as photo annotation, set temporal ranges for annotations, and play back with timed annotations appearing/disappearing.

**Screen layout:**
```
┌─────────────────────────────────────┐
│                                     │
│           VIDEO PLAYBACK            │
│        (or frozen frame for         │
│         annotation mode)            │
│                                     │
├─────────────────────────────────────┤
│ [thumb][thumb][thumb][thumb][thumb]  │  ← filmstrip
├─────────────────────────────────────┤
│ ◀ ──●─────────────────── ▶  0:03   │  ← timeline + scrubber
│   [▶]    [📝 View/Draw]    [⋯]     │  ← controls
└─────────────────────────────────────┘
```

**Mode toggle:** "View" mode (pinch = zoom video) vs "Draw" mode (single-finger = annotation). Same separation as photo annotation.

### DATA MODELS

```kotlin
@Serializable
data class VideoProject(
    val videoId: String = UUID.randomUUID().toString(),
    val videoFile: String,           // relative path
    val durationMs: Long,
    val frameRate: Float,
    val resolution: Size,
    val thumbnails: List<String>,    // pre-extracted frame paths
    val annotations: List<VideoAnnotation>,
)

@Serializable
data class VideoAnnotation(
    val element: AnnotationElement,   // reuses ALL Phase 1 element types
    val frameTimestampMs: Long,       // which frame it was drawn on
    val temporalRange: TemporalRange, // when it appears/disappears
)

@Serializable
data class TemporalRange(
    val startMs: Long,    // annotation appears
    val endMs: Long,      // annotation disappears (-1 = until end)
)
```

### FILMSTRIP

Horizontal scrollable strip of extracted thumbnails (from CC-06 FrameExtractor, S-39). Tap any thumbnail → video freezes at that frame → Draw mode available.

```kotlin
@Composable
fun VideoFilmstrip(
    thumbnails: List<VideoFrame>,
    currentPosition: Long,
    onFrameSelected: (Long) -> Unit,
) {
    LazyRow {
        items(thumbnails) { frame ->
            FilmstripThumbnail(
                frame = frame,
                isSelected = frame.timestampMs == currentPosition,
                onClick = { onFrameSelected(frame.timestampMs) },
            )
        }
    }
    // Current position indicator syncs with playback
    // Selected frame highlighted with border
}
```

### TEMPORAL RANGE HANDLES

After annotating a frame, the user can drag handles on the timeline to set when the annotation appears and disappears during playback:

```kotlin
@Composable
fun TemporalRangeEditor(
    annotation: VideoAnnotation,
    videoDuration: Long,
    onRangeChanged: (TemporalRange) -> Unit,
) {
    // Two draggable handles on the timeline:
    //   [start handle] ──────── [end handle]
    // Default: start = frame timestamp, end = video end
    // Minimum range: 500ms
    // Snap to frame boundaries
}
```

### EXPORT

Two export modes:
1. **MP4 with burned-in annotations:** Media3 Transformer renders annotations onto video frames at their temporal positions. Hardware-accelerated.
2. **Sidecar JSON:** Video file + JSON with all annotation data including temporal ranges.

```kotlin
class VideoExporter {
    suspend fun exportBurnedIn(
        project: VideoProject,
        outputFile: File,
        quality: ExportQuality = ExportQuality.ORIGINAL,
    ): ExportResult {
        // Media3 Transformer pipeline:
        // 1. For each frame, check active annotations at that timestamp
        // 2. Render annotations onto frame using existing renderers
        // 3. Encode output
    }
    
    suspend fun exportSidecar(
        project: VideoProject,
        outputDir: File,
    ): ExportResult {
        // Copy original video + write JSON sidecar
    }
}
```

### PROGRESSIVE REVEAL (VD-09)

Collage cells start blacked out and reveal one by one. Timer-based or tap-to-advance. For narrative evidence presentation: initial fault → disassembly → root cause → repair.

```kotlin
@Composable
fun ProgressiveRevealCollage(
    cells: List<CollageCell>,
    mode: RevealMode,  // TIMER (auto 3s each), TAP (tap to advance)
) {
    // Each cell starts with black overlay at 100% opacity
    // On reveal: overlay fades out (300ms)
    // Revealed cells stay visible
    // Counter: "2 / 6 revealed"
    // "Reveal all" button
}
```

### DELIVERABLES
1. `feature-video/build.gradle.kts` — new module
2. `VideoNotepadScreen.kt` — filmstrip + playback + annotation
3. `FrameAnnotationSurface.kt` — reuses Phase 1 drawing tools on frozen frame
4. `TemporalRangeController.kt` — appear/disappear timeline handles
5. `VideoExporter.kt` — burned-in MP4 + sidecar JSON

### ACCEPTANCE
- Filmstrip <3s for 15s clip
- Frame annotation pixel-accurate (same tools as photo)
- Temporal playback correct — annotations appear/disappear at set times
- Export burned-in MP4 <30s for 15s clip with 10 annotations
- Mode toggle (View/Draw) clear and reliable
- Progressive reveal works in both timer and tap modes

---

## S-56 · Video Bursts + Before/After

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| VD-02 | Annotation bursts | S | VD-01 |
| VD-03 | Before/after video comparison | M | VD-01 |

### ANNOTATION BURSTS (VD-02)

During video playback, tap anywhere to mark a moment of interest. Each tap captures `player.currentPosition` as a burst marker on the timeline. On second pass, tap any marker to jump to that frame and annotate. Fast "flag then annotate" workflow.

```kotlin
class BurstMarkerController(
    private val player: ExoPlayer,
) {
    private val markers = mutableStateListOf<BurstMarker>()
    
    fun markMoment() {
        markers.add(BurstMarker(
            timestampMs = player.currentPosition,
            annotated = false,
        ))
    }
    
    fun jumpToMarker(index: Int) {
        player.seekTo(markers[index].timestampMs)
    }
}

data class BurstMarker(
    val timestampMs: Long,
    val annotated: Boolean,
    val label: String? = null,
)
```

**Timeline UI:** Markers appear as colored dots on the scrubber timeline. Unannotated markers pulse. Annotated markers show checkmark.

### BEFORE/AFTER VIDEO COMPARISON (VD-03)

Side-by-side synced playback of two clips. Shared transport controls (play/pause/scrub). Both videos play at the same speed and position.

```kotlin
@Composable
fun BeforeAfterVideoComparison(
    beforeVideo: VideoProject,
    afterVideo: VideoProject,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        VideoPlayer(
            video = beforeVideo,
            modifier = Modifier.weight(1f),
            controller = sharedController,
        )
        VideoPlayer(
            video = afterVideo,
            modifier = Modifier.weight(1f),
            controller = sharedController,
        )
    }
    SharedTransportControls(controller = sharedController)
    // Labels: "BEFORE" / "AFTER" with timestamps
}
```

**Sync:** Both ExoPlayer instances share a single `SharedTransportController` that dispatches play/pause/seek to both simultaneously. Max drift tolerance: 100ms.

### DELIVERABLES
1. `BurstMarkerController.kt` — mark + timeline markers
2. `BurstAnnotationUI.kt` — second-pass annotation on markers
3. `BeforeAfterVideoComparison.kt` — side-by-side layout
4. `SharedTransportController.kt` — synchronized dual playback

### ACCEPTANCE
- Burst markers appear on timeline within 50ms of tap
- Jump-to-marker seeks accurately
- Synced playback maintains <100ms drift
- Both videos render at 30fps simultaneously

---

## S-57 · Multi-Video Parallel Playback

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| VD-04 | Multi-video parallel playback | L | VD-01, CL-01 |
| VD-05 | Event-based sync points | S | VD-04 |
| VD-06 | Tap-to-focus audio | S | VD-04 |

### MULTI-VIDEO PLAYBACK (VD-04)

2–4 simultaneous ExoPlayer/Media3 instances in grid layouts. SurfaceView rendering for hardware acceleration.

```kotlin
class VideoPlayerPool(
    private val context: Context,
) {
    private val players = mutableListOf<ExoPlayer>()
    
    fun createPool(count: Int): List<ExoPlayer> {
        // Check hardware support
        val maxInstances = MediaCodecList(REGULAR_CODECS)
            .codecInfos
            .filter { it.isDecoder && it.supportedTypes.contains("video/avc") }
            .maxOf { it.getCapabilitiesForType("video/avc")
                .videoCapabilities.supportedPerformancePoints
                ?.maxOf { pp -> pp.maxInstanceCount } ?: 1 }
        
        val actualCount = minOf(count, maxInstances)
        // Create players at appropriate resolution:
        //   4 players → 480p each
        //   2 players → 720p each
        return (0 until actualCount).map { createPlayer() }
    }
}
```

**Grid layouts:**
```
1+1:  [  A  ][  B  ]         (side by side)
2×2:  [ A ][ B ]             (quad)
      [ C ][ D ]
1+3:  [    A    ][ B ]       (hero + details)
                 [ C ]
                 [ D ]
```

**Playback modes:**
- **Synchronized:** All videos play/pause/scrub together. Shared scrub bar at bottom. When shorter video ends → freeze on last frame.
- **Independent:** Each video has its own transport. Tap cell to select active player.

### EVENT-BASED SYNC POINTS (VD-05)

Kinovea-inspired sync. Mark a common moment in each video (e.g., "tool contacts surface") and playback aligns from that point. Accounts for videos recorded at different times.

```kotlin
class SyncPointController {
    data class SyncPoint(
        val playerIndex: Int,
        val timestampMs: Long,
        val label: String?,
    )
    
    fun setSyncPoint(playerIndex: Int, timestampMs: Long, label: String?)
    
    fun alignToSyncPoints() {
        // Calculate offset for each player relative to reference (player 0)
        // On play: each player.seekTo(currentTime + offset)
    }
}
```

### TAP-TO-FOCUS AUDIO (VD-06)

Apple Vision Pro-inspired audio focus. Tap a video cell → that cell's audio plays, others mute. Speaker icon indicator on active cell.

```kotlin
class AudioFocusController(
    private val players: List<ExoPlayer>,
) {
    var focusedIndex: Int = 0
    
    fun setFocus(index: Int) {
        players.forEachIndexed { i, player ->
            player.volume = if (i == index) 1f else 0f
        }
        focusedIndex = index
    }
}
```

### DELIVERABLES
1. `MultiVideoScreen.kt` — grid layouts + mode toggle
2. `VideoPlayerPool.kt` — ExoPlayer instance management
3. `SyncPointController.kt` — event-based temporal alignment
4. `AudioFocusController.kt` — tap-to-focus audio routing
5. `SharedScrubBar.kt` — unified timeline for sync mode

### ACCEPTANCE
- 4× 480p 30fps on Galaxy S25 Ultra
- 2 streams minimum on lower-end devices
- No MediaCodec deadlock or ANR
- Sync points align playback correctly
- Tap-to-focus routes audio instantly
- Freeze-on-last-frame for shorter videos in sync mode

---

## S-58 · Play Store Compliance

### ATOMS
None — infrastructure session.

### CONTEXT

Final compliance pass before Phase 2 Play Store release. Verifies targetSdk 36, Data Safety declarations, AAB bundle, privacy policy, and content rating.

### CHECKLIST

**Build configuration:**
- [ ] `targetSdk = 36` verified and tested
- [ ] `compileSdk = 36`
- [ ] `minSdk = 29` (Android 10+)
- [ ] AAB bundle generated: `./gradlew bundleRelease`
- [ ] ProGuard/R8 rules updated for all Phase 2 dependencies
- [ ] APK size check: <100MB base APK (without on-demand models)

**Data Safety declarations:**
- [ ] Data collected: location (GPS in metadata), photos (user content), audio (voice annotations)
- [ ] Data shared: only via explicit user action (share, export, Zapier webhook)
- [ ] Encryption: in transit (HTTPS) and at rest (EncryptedSharedPreferences for tokens)
- [ ] Data deletion: user can delete all data from Settings
- [ ] Firebase Auth data: email, display name (optional account)

**Privacy policy:**
- [ ] Updated for Phase 2 features: voice recording, video capture, ML Kit models, Firebase Auth
- [ ] Hosted at accessible URL
- [ ] Linked from Play Store listing and in-app Settings

**Content rating:**
- [ ] IARC questionnaire completed
- [ ] Expected rating: Everyone (no user-generated content sharing in Phase 2 basic tier)

**App signing:**
- [ ] Play App Signing enrolled
- [ ] Upload key stored securely
- [ ] Signing config in `app/build.gradle.kts`

**Permissions audit:**
- [ ] `CAMERA` — capture photos/video
- [ ] `RECORD_AUDIO` — voice annotations
- [ ] `ACCESS_FINE_LOCATION` — GPS metadata
- [ ] `INTERNET` — Zapier webhook, Firebase Auth, ML Kit model download
- [ ] `POST_NOTIFICATIONS` — collaboration notifications (Android 13+)
- [ ] `FOREGROUND_SERVICE` — batch export, sync queue
- [ ] All permissions requested at point of use (not on launch)

**Testing:**
- [ ] Pre-launch report clean (no crashes, no ANRs, no policy warnings)
- [ ] Tested on minimum API 29 device
- [ ] Tested on target API 36 device
- [ ] All 13 locales verified

### DELIVERABLES
1. Updated `app/build.gradle.kts` — signing, targetSdk, ProGuard
2. `PLAY-STORE-CHECKLIST.md` — completed checklist
3. Privacy policy document
4. Screenshots for Play Store listing (13 locales × required device types)

### ACCEPTANCE
- `./gradlew bundleRelease` produces valid AAB
- No crashes in pre-launch report
- Data Safety form accepted by Play Console
- Privacy policy accessible at listed URL
- All permissions behave correctly on API 29 and API 36

---

*April 2026 · TechNotes Phase 2 Sessions 55–58 v1.0*
