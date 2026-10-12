# 49 — Sessions 47–50: Kits + Collage + Navigation + Integration

> **Four independent extension tracks.** Trade kit customization, collage intelligence, search/deep-linking, and the Zapier integration bridge.

---

## S-47 · Trade Kit Extensions

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| TK-05 | .tnkit file format | S | TK-01 |
| TK-07 | Kit customization (drag into slots) | S | TK-02, IC-03 |
| TK-08 | Contextual kit suggestion | S | TK-01 |
| TK-10 | Kit-specific collage templates | S | TK-01, CL-10 |

### .TNKIT FORMAT (TK-05)

Export/import format for custom Trade Kits. JSON-based, zipped with SVG assets.

```kotlin
// .tnkit = ZIP containing:
//   manifest.json — kit metadata + tool list + defaults
//   symbols/     — custom SVG assets
//   colors.json  — semantic color definitions

@Serializable
data class TnKitManifest(
    val formatVersion: Int = 1,
    val kitId: String,
    val name: String,
    val toolSet: List<String>,
    val defaultToolbarSlots: List<String>,
    val semanticColors: Map<String, String>,
    val numberingPrefix: String,
    val symbolAssets: List<String>,       // filenames in symbols/
)
```

### KIT CUSTOMIZATION (TK-07)

Long-press-drag any tool from the ⋯More catalog into one of the 5 main toolbar category slots, replacing the default. Customization persists per kit in PD-10 (user preferences).

**Gesture:** Long-press on tool in ⋯More grid (500ms) → tool lifts with shadow → drag to toolbar slot → slot highlights on hover → release to replace → displaced tool returns to ⋯More.

### CONTEXTUAL KIT SUGGESTION (TK-08)

Detects annotation patterns and suggests a kit switch. Max 1 suggestion per session to avoid annoyance.

```kotlin
class ContextualKitSuggester(
    private val annotationRepo: AnnotationRepository,
    private val kitRegistry: TradeKitRegistry,
) {
    // Triggers:
    // - User draws 3+ P&ID-style symbols without P&ID kit → "Switch to Pipe & Valve?"
    // - User places welding symbols without Welding kit → "Switch to Welding?"
    // - User draws stitch lines without Footwear kit → "Switch to Footwear?"
    
    fun shouldSuggest(recentAnnotations: List<AnnotationElement>): KitSuggestion?
    
    // Toast-style suggestion with "Switch" + "Dismiss" buttons
    // Max 1 per session
    // Tracks dismissed suggestions to avoid repeating
}
```

### KIT COLLAGE TEMPLATES (TK-10)

Pre-configured collage layouts per Trade Kit (see Doc 05 for full template definitions):

| Kit | Templates |
|-----|-----------|
| Welding | Joint Documentation (4-up), Weld Defect Report (3-up), Procedure Qualification (3-up) |
| Electrical | Panel Inspection (4-up), Violation Documentation (3-up), Before/After Repair (2-up) |
| P&ID | Valve Assembly (4-up), Corrosion Documentation (4-up), Flow System (4-6 strip) |
| Maintenance | Before/After (2-up), Condition Assessment (4-up), Equipment Survey (1+3) |
| Footwear | Construction Breakdown (4-up), Material Board (4-6), Design Iteration (3-4) |

Templates appear in the collage layout picker when the corresponding kit is active.

### DELIVERABLES
1. `TnKitFormat.kt` — .tnkit ZIP serialization/deserialization
2. `KitCustomizationDragHandler.kt` — long-press-drag gesture
3. `ContextualKitSuggestion.kt` — pattern detection + suggestion UI
4. `KitCollageTemplates.kt` — per-kit template definitions

### ACCEPTANCE
- .tnkit round-trip: export → import → identical kit
- Toolbar customization persists across sessions
- Kit suggestion fires max 1/session
- Correct templates appear per active kit in collage picker

---

## S-48 · Collage Extensions

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| CL-09 | Smart layout suggestion | M | CL-01, PD-03 |
| CL-10 | Domain templates | S | CL-01, TK-01 |
| CL-11 | Freeform layout | M | CL-01 |
| CL-13 | Synchronized zoom/pan | S | CL-07 |
| CL-16 | Content-aware auto-crop | S | CL-07, AI-15 |
| CL-17 | Auto-match exposure | M | CL-01, EP-03 |

### SMART LAYOUT SUGGESTION (CL-09)

Analyzes the selected photo set and pre-applies the most likely layout:

```kotlin
class SmartLayoutSuggester(
    private val photoRepository: PhotoRepository,
) {
    fun suggest(photos: List<PhotoEntity>): LayoutSuggestion {
        return when {
            photos.size == 2 && areTimestampClose(photos, 30_000) ->
                LayoutSuggestion(BEFORE_AFTER, confidence = 0.9f)
            photos.size in 3..6 && areSequentialPins(photos) ->
                LayoutSuggestion(PROCESS_SEQUENCE, confidence = 0.85f)
            photos.size == 4 && areDifferentAngles(photos) ->
                LayoutSuggestion(QUAD_VIEW, confidence = 0.8f)
            photos.size == 3 && hasContextDetailPair(photos) ->
                LayoutSuggestion(FORENSIC_THREE_SHOT, confidence = 0.7f)
            else ->
                LayoutSuggestion(QUAD_VIEW, confidence = 0.5f)
        }
    }
    
    private fun areTimestampClose(photos: List<PhotoEntity>, maxDeltaMs: Long): Boolean
    private fun areSequentialPins(photos: List<PhotoEntity>): Boolean
    private fun areDifferentAngles(photos: List<PhotoEntity>): Boolean
}
```

Layout pre-applied → user sees collage immediately → confirms or changes.

### FREEFORM LAYOUT (CL-11)

Unconstrained canvas where photos can be freely positioned, resized, overlapped, and rotated. Drop shadows distinguish overlapping layers. No grid constraints.

```kotlin
@Serializable
data class FreeformCellState(
    val photoId: String,
    val x: Float,           // position (dp)
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,    // degrees
    val zIndex: Int,
    val dropShadow: Boolean = true,
)
```

### SYNCHRONIZED ZOOM/PAN (CL-13)

Chain-link toggle per cell pair. When linked, zoom/pan in one cell mirrors identically in the other — critical for objective before/after comparison at identical scale. Two-finger drag on linked cell is an escape hatch (temporary independent control).

### AUTO-CROP (CL-16) + EXPOSURE MATCH (CL-17)

**Auto-crop:** On cell placement, face detection + saliency analysis centers the key subject. Tap to manually reposition.

**Exposure match:** One-tap "Match" button normalizes white balance and exposure across all photos in the collage. Uses AGSL shaders (EP-03) to compute and apply per-photo adjustments.

### DELIVERABLES
1. `SmartLayoutSuggester.kt` — photo analysis → layout recommendation
2. `FreeformLayout.kt` — free positioning canvas
3. `SynchronizedZoomController.kt` — linked cell pairs
4. `AutoCropEngine.kt` — saliency + face detection centering
5. `ExposureMatchEngine.kt` — cross-photo normalization

### ACCEPTANCE
- Smart suggestion >70% accuracy for clear cases
- Freeform drag/resize/rotate/overlap at 60fps
- Linked cells zoom/pan in perfect sync
- Auto-crop centers subject correctly
- Exposure match produces visually consistent results

---

## S-49 · Navigation Phase 2

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| ON-06 | Search | M | PD-13 (→ Room FTS fallback) |
| ON-09 | Situation-first onboarding | M | ON-02 |
| ON-11 | Deep linking | M | ON-06, PD-04 |

### SEARCH (ON-06)

Phase 2 scope: Room FTS (Full-Text Search) or LIKE queries. Full search index (PD-13) is Phase 4.

```kotlin
@Dao
interface SearchDao {
    @Query("""
        SELECT * FROM projects WHERE name LIKE '%' || :query || '%'
        UNION
        SELECT * FROM photos WHERE notes LIKE '%' || :query || '%'
        UNION
        SELECT * FROM annotations WHERE textContent LIKE '%' || :query || '%'
    """)
    fun search(query: String): List<SearchResult>
}

@Composable
fun SearchScreen(onNavigate: (SearchResult) -> Unit) {
    // Top: search bar with real-time results
    // Grouped by: Projects, Photos, Annotations
    // Each result: thumbnail + title + snippet + timestamp
    // Tap → navigate to result (deep link)
}
```

### SITUATION-FIRST ONBOARDING (ON-09)

Replaces text-based "What kind of work?" tiles with photo-recognition tiles. The user sees photos representing different work types and taps the one that looks like their work.

```kotlin
@Composable
fun SituationFirstOnboarding() {
    // Grid of photo tiles:
    // - Electrical panel (→ Electrical kit)
    // - Pipe system (→ P&ID kit)
    // - Weld joint (→ Welding kit)
    // - Building/facility (→ General Maintenance kit)
    // - Shoes/garments (→ Footwear kit)
    // - "Something else" (→ no kit, universal)
    
    // Tap → kit auto-activates → "My First Project" created → camera opens
}
```

### DEEP LINKING (ON-11)

Navigate directly to a specific annotation within a project from external sources (notifications, share links, search results).

```kotlin
// URI scheme: technotes://project/{projectId}/photo/{photoId}/annotation/{annotationId}
class DeepLinkHandler {
    fun handle(uri: Uri): NavigationTarget {
        return when {
            uri.pathSegments.size >= 6 -> NavigationTarget.Annotation(...)
            uri.pathSegments.size >= 4 -> NavigationTarget.Photo(...)
            uri.pathSegments.size >= 2 -> NavigationTarget.Project(...)
            else -> NavigationTarget.Hub
        }
    }
}
```

### DELIVERABLES
1. `SearchScreen.kt` — search UI with grouped results
2. `SearchRepository.kt` — Room FTS / LIKE queries
3. `SituationFirstOnboarding.kt` — photo-recognition tiles
4. `DeepLinkHandler.kt` — URI routing

### ACCEPTANCE
- Search results <200ms
- Deep links navigate to correct annotation
- Situation-first tiles render photos
- Onboarding flow completes in <60s

---

## S-50 · Integration Layer — Offline Queue + Zapier

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| IL-03 | Offline sync queue (outbox) | M | PD-01 |
| IL-04 | Zapier webhook (Level 2) | M | IL-02, IL-03 |

### OFFLINE SYNC QUEUE (IL-03)

WorkManager-based outbox that queues Annotation Envelopes for delivery. Survives force-kill, reboot, and airplane mode.

```kotlin
@Entity
data class SyncQueueEntity(
    @PrimaryKey val queueId: String = UUID.randomUUID().toString(),
    val payload: String,                    // serialized Envelope JSON
    val destination: String,                // "zapier", "procore", etc.
    val destinationUrl: String?,
    val status: SyncStatus,                 // PENDING, SENDING, SENT, FAILED
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val createdAt: Long,
    val lastAttempt: Long? = null,
    val errorMessage: String? = null,
)

enum class SyncStatus { PENDING, SENDING, SENT, FAILED }
```

**WorkManager worker:**
```kotlin
class SyncQueueWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val pending = syncDao.getPending(limit = 10)
        for (item in pending) {
            try {
                syncDao.updateStatus(item.queueId, SyncStatus.SENDING)
                httpClient.post(item.destinationUrl, item.payload)
                syncDao.updateStatus(item.queueId, SyncStatus.SENT)
            } catch (e: Exception) {
                val newRetry = item.retryCount + 1
                if (newRetry >= item.maxRetries) {
                    syncDao.updateStatus(item.queueId, SyncStatus.FAILED, e.message)
                } else {
                    syncDao.updateRetry(item.queueId, newRetry)
                    // Exponential backoff: 30s, 60s, 2m, 4m, 8m
                }
            }
        }
        return Result.success()
    }
}
```

### ZAPIER WEBHOOK (IL-04)

Level 2 integration: user enters a Zapier webhook URL in Settings → Integrations. On project completion, the Annotation Envelope is POSTed to the webhook. Zapier then routes to any of 5,000+ connected apps.

```kotlin
class ZapierWebhookAdapter(
    private val syncQueue: SyncQueueDao,
) {
    fun configure(webhookUrl: String)
    
    fun sendEnvelope(envelope: AnnotationEnvelope) {
        syncQueue.insert(SyncQueueEntity(
            payload = Json.encodeToString(envelope),
            destination = "zapier",
            destinationUrl = webhookUrl,
            status = SyncStatus.PENDING,
            createdAt = System.currentTimeMillis(),
        ))
        // WorkManager picks up and sends
    }
}
```

**Settings UI:**
```kotlin
@Composable
fun IntegrationSettingsScreen() {
    // Section: Zapier
    //   - Webhook URL text field
    //   - "Test Connection" button → sends test payload → shows success/error
    //   - Queue status: "3 pending · 47 sent · 1 failed"
    //   - "Retry failed" button
    //   - "Clear queue" with confirmation
}
```

### DELIVERABLES
1. `SyncQueueEntity.kt` — Room entity + DAO
2. `SyncQueueWorker.kt` — WorkManager + exponential backoff
3. `ZapierWebhookAdapter.kt` — webhook POST
4. `IntegrationSettingsScreen.kt` — webhook URL + queue status

### ACCEPTANCE
- Queue survives force-kill + device reboot
- Exponential backoff: 30s → 60s → 2m → 4m → 8m
- Max 1000 queued items (oldest removed first)
- Zapier receives valid Annotation Envelope JSON
- Test connection works
- Queue status shows correct counts

---

*April 2026 · TechNotes Phase 2 Sessions 47–50 v1.0*
