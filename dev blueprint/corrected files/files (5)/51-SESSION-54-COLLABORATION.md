# 51 — Session 54: Collaboration System

> **Async shared annotation, comments, multi-user layers, user accounts.** The first multi-user features — async-first, no real-time sync (that's Phase 3 CRDT).

---

## S-54 · Collaboration System

### ATOMS
| ID | Name | Size | Depends |
|----|------|------|---------|
| CO-01 | Async shared annotation | L | EX-01, PD-04 |
| CO-02 | Comment threads | M | PD-04 |
| CO-03 | Multi-user layers | M | PD-11, IC-18 |
| CO-06 | User accounts (optional) | M | ON-07 |

### NEW MODULE: `:feature-collab`

```kotlin
// feature-collab/build.gradle.kts
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.google.dagger.hilt.android")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature-export"))
    // Firebase Auth for user accounts
    implementation(platform("com.google.firebase:firebase-bom:x.y.z"))
    implementation("com.google.firebase:firebase-auth-ktx")
    // Ktor for REST upload/download
    implementation("io.ktor:ktor-client-android:x.y.z")
}
```

### USER ACCOUNTS (CO-06)

Optional accounts — TechNotes works fully without an account (Phase 1 behavior). Account adds: collaboration, cloud backup, cross-device sync (Phase 3).

```kotlin
class UserAccountManager(
    private val auth: FirebaseAuth,
    private val localPrefs: UserPreferencesDataStore,
) {
    // Sign-in options: Email/Password, Google Sign-In
    // Local data PRESERVED after account creation
    //   → existing projects/annotations stay intact
    //   → account just wraps existing local identity
    
    suspend fun signInWithGoogle(): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun createAccount(email: String, password: String): AuthResult
    suspend fun signOut()
    fun isSignedIn(): Boolean
    fun getCurrentUser(): UserProfile?
    
    // Profile: display name, email, avatar (from Google)
    // No mandatory fields beyond email
}

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String,
    val avatarUrl: String?,
)
```

**UX:** Account creation prompt appears when user first tries to share (not on first launch). "Sign in to share annotations with your team." Skip option always available.

### ASYNC SHARED ANNOTATION (CO-01)

Share a project or photo with another user → they add annotations in a different color/layer → original user gets a push notification.

**Flow:**
1. User A taps "Share" on a project → generates a share link
2. Server creates a shared project record, uploads current state
3. User B opens link → TechNotes opens → downloads project
4. User B adds annotations (auto-assigned to their layer, in their color)
5. User B taps "Send back" → uploads their annotations
6. User A receives push notification → downloads User B's annotations
7. User B's annotations appear on a new layer in User A's project

```kotlin
class AsyncShareManager(
    private val httpClient: HttpClient,
    private val projectRepo: ProjectRepository,
    private val auth: UserAccountManager,
) {
    // Upload
    suspend fun shareProject(projectId: String): ShareLink {
        val envelope = exportManager.createEnvelope(projectId)
        val shareId = httpClient.post("/api/shares", envelope)
        return ShareLink("technotes://share/$shareId")
    }
    
    // Download
    suspend fun receiveShare(shareId: String): ReceivedProject {
        val envelope = httpClient.get("/api/shares/$shareId")
        return importManager.importAsSharedProject(envelope)
    }
    
    // Send back
    suspend fun sendAnnotations(projectId: String, shareId: String) {
        val myAnnotations = annotationRepo.getByLayer(projectId, myLayerId)
        httpClient.post("/api/shares/$shareId/reply", myAnnotations)
    }
}
```

**Web viewer:** Read-only web viewer for recipients who don't have TechNotes installed. Shows annotated photos with zoom/pan. "Install TechNotes to add your own annotations" CTA.

### COMMENT THREADS (CO-02)

Per-annotation comment threads. Each annotation can have a thread of text comments — speech bubble icon appears on annotations with comments.

```kotlin
@Entity
data class CommentEntity(
    @PrimaryKey val commentId: String = UUID.randomUUID().toString(),
    val annotationId: String,
    val userId: String?,
    val displayName: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = false,
)

@Composable
fun CommentThread(
    annotationId: String,
    comments: List<CommentEntity>,
    onAddComment: (String) -> Unit,
) {
    // Bottom sheet triggered by tapping speech bubble on annotation
    // Chronological list of comments
    // Each: avatar + name + text + timestamp
    // Text input at bottom
    // "Mark all read" on open
}
```

**Visual indicator:** Small speech bubble badge on annotations that have comments. Badge shows unread count.

### MULTI-USER LAYERS (CO-03)

Each user's annotations live on their own layer with a distinct color. Uses PD-11 (annotation layers from S-40) and IC-18 (layer management from S-42).

```kotlin
class MultiUserLayerController(
    private val layerDao: AnnotationLayerDao,
    private val auth: UserAccountManager,
) {
    fun getOrCreateUserLayer(
        projectId: String,
        photoId: String,
    ): AnnotationLayerEntity {
        val user = auth.getCurrentUser()
        val existing = layerDao.findByUser(projectId, photoId, user?.uid)
        if (existing != null) return existing
        
        return layerDao.insert(AnnotationLayerEntity(
            layerId = UUID.randomUUID().toString(),
            projectId = projectId,
            photoId = photoId,
            userId = user?.uid,
            displayName = user?.displayName ?: "Anonymous",
            color = assignUniqueColor(projectId, photoId),
            isVisible = true,
            isLocked = false,
            sortOrder = layerDao.getMaxOrder(projectId, photoId) + 1,
            createdAt = System.currentTimeMillis(),
        ))
    }
    
    private fun assignUniqueColor(projectId: String, photoId: String): Long {
        // Assign from a palette of 8 distinct colors
        // Skip colors already used by other users on this photo
        val usedColors = layerDao.getUsedColors(projectId, photoId)
        return LAYER_PALETTE.first { it !in usedColors }
    }
}

val LAYER_PALETTE = listOf(
    0xFF4488FF, // Blue
    0xFFFF4444, // Red
    0xFF44BB44, // Green
    0xFFFF8800, // Orange
    0xFF8844FF, // Purple
    0xFF00BBBB, // Teal
    0xFFFF44BB, // Pink
    0xFFBBBB00, // Yellow
)
```

### BACKEND REQUIREMENTS

Phase 2 collaboration requires a minimal backend:
- **Share endpoint:** POST/GET share envelopes
- **Comment sync:** REST for comment CRUD
- **Push notifications:** Firebase Cloud Messaging
- **Auth:** Firebase Auth (handled client-side, no custom auth server)

**Recommended:** Simple REST API (Ktor Server or Firebase Functions) + Cloud Storage for envelope files + Firestore for comments.

### DELIVERABLES
1. `feature-collab/build.gradle.kts` — new module
2. `AsyncShareManager.kt` — share link generation + upload/download
3. `CommentThread.kt` — per-annotation Compose comments UI
4. `UserAccountManager.kt` — Firebase Auth wrapper
5. `MultiUserLayerController.kt` — per-user layer assignment

### ACCEPTANCE
- Share link generates and opens on recipient device
- Recipient's annotations appear on separate layer with distinct color
- Comments persist per annotation
- Google Sign-In works on first attempt
- Local data preserved after account creation
- Push notification arrives when collaborator sends annotations back
- Works offline (queues for sync when online)

---

*April 2026 · TechNotes Phase 2 Session 54 v1.0*
