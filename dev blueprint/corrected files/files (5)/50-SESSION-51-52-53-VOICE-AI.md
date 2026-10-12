# 50 — Sessions 51–53: Voice + AI

> **Voice as input, intelligence as augmentation.** Voicemail annotations, speech transcription, diagnostic audio, OCR, QR scanning, and content-aware cropping.

---

## S-51 · Voice Annotations + Transcription

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| VO-01 | Voicemail annotations | M | CE-06, PD-04 |
| VO-02 | On-device speech transcription | M | VO-01 |

### NEW MODULE: `:feature-voice`

```kotlin
// feature-voice/build.gradle.kts
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.google.dagger.hilt.android")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    // sherpa-onnx for offline transcription
    implementation("com.k2fsa.sherpa:onnx-android:x.y.z")
}
```

### VOICEMAIL ANNOTATIONS (VO-01)

WhatsApp-style tap-hold-speak-release. Audio clip is the PRIMARY artifact — transcription is secondary. Mini waveform player renders at the annotation location on the canvas.

**Gesture flow:**
1. Select voice annotation tool
2. Tap-hold on canvas location (threshold: 300ms)
3. Recording starts with haptic CONFIRM
4. Speak (max 30s, countdown visible)
5. Release finger → recording stops
6. Audio clip saved as WAV in app-internal storage
7. Waveform player appears at tap location
8. Background transcription starts automatically

```kotlin
@Serializable
data class VoicemailElement(
    override val id: String = UUID.randomUUID().toString(),
    val position: ImagePoint,
    val audioFile: String,           // relative path in app storage
    val durationMs: Long,
    val transcription: String? = null,  // filled async by VO-02
    val transcriptionStatus: TranscriptionStatus = PENDING,
    val waveformData: FloatArray? = null,  // pre-computed amplitude samples
    val strokeColor: Long,
) : AnnotationElement()

enum class TranscriptionStatus { PENDING, PROCESSING, COMPLETE, FAILED }
```

**Audio recording:**
```kotlin
class VoicemailRecorder {
    private var recorder: AudioRecord? = null
    private val sampleRate = 16000  // 16kHz for speech recognition
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    fun startRecording(outputFile: File)
    fun stopRecording(): RecordingResult
    fun getAmplitude(): Float  // for live waveform
    fun isRecording(): Boolean
}
```

**Waveform view:** Renders as a mini player at the annotation position on the canvas. Tap to play/pause. The waveform shows amplitude samples extracted during recording. Player duration shown as "0:05 / 0:12".

```kotlin
@Composable
fun WaveformPlayer(
    element: VoicemailElement,
    viewMatrix: Matrix,
    modifier: Modifier,
) {
    // Positioned at element.position in image coordinates
    // Transformed by viewMatrix for correct zoom/pan
    // Mini pill shape: [▶ ▃▅▇▅▃▇▅▃ 0:05]
    // Tap → toggle play/pause
    // Below waveform: transcription text (if available)
}
```

### ON-DEVICE SPEECH TRANSCRIPTION (VO-02)

Fully offline using sherpa-onnx (or whisper.cpp). ~50MB model, downloaded on first use. Background processing — doesn't block the UI.

```kotlin
class SpeechTranscriber(
    private val context: Context,
) {
    private var recognizer: OfflineRecognizer? = null
    
    suspend fun ensureModel(): Boolean {
        // Download ~50MB model on Wi-Fi
        // Store in app-internal storage
        // Return false if download fails
    }
    
    suspend fun transcribe(audioFile: File): TranscriptionResult {
        // Process in background
        // Language detection from device locale
        // >85% accuracy target on clear speech
        // Complete within 2× recording duration
    }
    
    fun isModelAvailable(): Boolean
}

data class TranscriptionResult(
    val text: String,
    val words: List<WordTimestamp>,  // for VO-03 linked evidence
    val language: String,
    val confidence: Float,
)

data class WordTimestamp(
    val word: String,
    val startMs: Long,
    val endMs: Long,
)
```

**Post-transcription:** Transcription text appears below the waveform player. Editable by the technician (tap text → keyboard). Edited text is marked as "verified" for higher confidence in exports.

### DELIVERABLES
1. `VoicemailRecorder.kt` — AudioRecord + WAV output
2. `WaveformPlayer.kt` — Compose mini player
3. `SpeechTranscriber.kt` — sherpa-onnx offline transcription
4. `VoicemailElement.kt` — data model
5. Module setup: `feature-voice/build.gradle.kts`

### ACCEPTANCE
- Recording starts <100ms of hold threshold
- Waveform renders at 60fps during recording and playback
- Works with gloves (large touch targets)
- Transcription >85% accuracy on clear speech
- Transcription completes within 2× recording duration
- Fully offline — no network required after model download
- Audio files <500KB for 10s clip (16kHz mono WAV)

---

## S-52 · Voice Extensions

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| VO-03 | Linked evidence (audio ↔ transcript) | S | VO-01, VO-02 |
| VO-04 | Sound as diagnostic data | S | VO-01 |

### LINKED EVIDENCE (VO-03)

Word-level timestamps link every transcribed word back to its audio. Tap any word in the transcription → audio plays from that exact moment. Creates an auditable chain: raw audio → transcription → structured data.

```kotlin
@Composable
fun LinkedTranscriptionView(
    element: VoicemailElement,
    words: List<WordTimestamp>,
    audioPlayer: AudioPlayer,
) {
    // Display transcription as tappable text
    // Each word is individually tappable
    // Tap word → audioPlayer.seekTo(word.startMs) → play
    // Currently playing word highlighted
    // Supervisor can verify: "Is the transcription accurate?"
}
```

### SOUND AS DIAGNOSTIC DATA (VO-04)

"Record what you hear" mode — 5-second ambient audio clips attached to annotations. Not voice memos, but environmental sound capture. Bearing defects, valve hiss, relay clicking, pump cavitation all carry diagnostic frequency signatures.

```kotlin
class DiagnosticAudioRecorder {
    fun startCapture(duration: Duration = 5.seconds)
    fun stopCapture(): DiagnosticAudioResult
    
    // Optional spectrogram view
    fun computeSpectrogram(audioFile: File): SpectrogramData
}

@Composable
fun SpectrogramView(data: SpectrogramData) {
    // Frequency vs time heatmap
    // Highlights dominant frequencies
    // Useful for: bearing defect (characteristic frequencies),
    //   valve leak (broadband hiss), motor issues (harmonics)
}
```

### DELIVERABLES
1. `LinkedTranscriptionView.kt` — tappable word-level transcript
2. `DiagnosticAudioRecorder.kt` — ambient 5s capture
3. `SpectrogramView.kt` — frequency visualization

### ACCEPTANCE
- Tap any word → plays corresponding audio segment
- Currently playing word highlighted
- Diagnostic capture exactly 5s (configurable)
- Spectrogram renders in <500ms for 5s clip

---

## S-53 · AI Features

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| AI-02 | OCR for equipment nameplates | M | CC-01 |
| AI-10 | QR/barcode scan | M | CC-01, PD-12 |
| AI-15 | Content-aware auto-crop | S | CC-01 |

### OCR NAMEPLATES (AI-02)

ML Kit Text Recognition (offline, bundled) for reading equipment nameplates. Extracts structured information from common nameplate patterns (manufacturer, model, serial, voltage, amperage, date).

```kotlin
class NameplateOCR(
    private val textRecognizer: TextRecognizer,
) {
    suspend fun recognizeNameplate(bitmap: Bitmap): NameplateData {
        val result = textRecognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        return parseNameplateFields(result)
    }
    
    private fun parseNameplateFields(result: Text): NameplateData {
        // Pattern matching for common fields:
        //   "Model: XYZ-123" → model = "XYZ-123"
        //   "S/N: 12345678" → serialNumber = "12345678"
        //   "MFG DATE: 03/2024" → mfgDate = "2024-03"
        //   "240V 60Hz 15A" → voltage/frequency/amperage
    }
}

data class NameplateData(
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val voltage: String?,
    val frequency: String?,
    val amperage: String?,
    val mfgDate: String?,
    val rawText: String,
    val confidence: Float,
)
```

**UX:** Camera viewfinder shows "Point at nameplate" overlay. On recognition, extracted fields appear in an editable card. User confirms or corrects. Data attached to current annotation/project.

### QR/BARCODE SCAN (AI-10)

ML Kit Barcode Scanning for equipment identification. Supports QR, Code 128, Code 39, EAN, UPC. Scanned value auto-fills equipment reference field.

```kotlin
class BarcodeScanner(
    private val scanner: BarcodeScanner,
) {
    suspend fun scan(bitmap: Bitmap): BarcodeResult? {
        val barcodes = scanner.process(InputImage.fromBitmap(bitmap, 0)).await()
        return barcodes.firstOrNull()?.let {
            BarcodeResult(
                rawValue = it.rawValue,
                format = it.format,
                displayValue = it.displayValue,
            )
        }
    }
}
```

**Note:** PD-12 (asset linkage database) is Phase 4. In Phase 2, QR/barcode scan extracts text only — no asset database lookup. Asset linkage wired in Phase 4.

### CONTENT-AWARE AUTO-CROP (AI-15)

Face detection (~40ms via ML Kit) + saliency-based centering for auto-cropping photos on placement into collage cells or during capture. Manual tap-to-reposition override.

```kotlin
class ContentAwareCrop {
    suspend fun computeCropRegion(
        bitmap: Bitmap,
        targetAspectRatio: Float,
    ): RectF {
        // 1. Face detection (highest priority)
        val faces = faceDetector.process(bitmap).await()
        if (faces.isNotEmpty()) return centerOnFaces(faces, targetAspectRatio)
        
        // 2. Saliency estimation (contrast + edge density)
        val saliencyMap = computeSaliency(bitmap)
        return centerOnSaliency(saliencyMap, targetAspectRatio)
    }
    
    private fun computeSaliency(bitmap: Bitmap): FloatArray {
        // Divide into 8×8 grid
        // Score each cell: contrast + edge density (Sobel)
        // Return highest-scoring region
    }
}
```

### DELIVERABLES
1. `NameplateOCR.kt` — ML Kit text → structured nameplate data
2. `BarcodeScanner.kt` — ML Kit barcode → equipment reference
3. `ContentAwareCrop.kt` — face detection + saliency

### ACCEPTANCE
- OCR >90% on clean, well-lit nameplates
- Barcode scan <500ms
- Auto-crop centers subject correctly
- Face detection <40ms
- All three features work offline (ML Kit bundled models)

---

*April 2026 · TechNotes Phase 2 Sessions 51–53 v1.0*
