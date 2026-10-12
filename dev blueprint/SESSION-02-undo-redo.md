# Session 02 — Undo/Redo Engine

## GOAL

Implement the complete undo/redo system: three per-mode stacks, three access points (floating pill, top bar icons, two-finger gesture), snapshot/revert for Edit Mode cancel, coalescing for rapid changes, and history popup.

## DEPENDS ON

- **S01 (Mode System)** — `AppMode` enum, `CanvasViewModel.currentMode`, mode transition functions, cancel dialog wiring

## SPEC REFERENCES

Read before coding:
- `INTERACTION-SPEC.md` §4 (Undo / Redo System)
- `IMPLEMENTATION-BLUEPRINT.md` Part 2: P-06 (Undo Everywhere)
- Resolution R-07 (Browse Mode Undo Access)
- Resolution R-08 (Edit Mode Cancel + Undo Stack)
- Resolution R-15 (Symmetry Guide Undo Scope — note for future, not implemented here)

## EXISTING CODE TO INTEGRATE WITH

| File | Module | Integration Notes |
|------|--------|-------------------|
| `CanvasViewModel.kt` | `:feature:canvas` | Add UndoManager injection, wire undo/redo to mode transitions |
| `CanvasScreen.kt` | `:feature:canvas` | Add floating pill composable, wire top bar undo buttons (Browse), add two-finger gesture detection |
| `EditableTouchImageView.kt` | `:feature:canvas` | Add two-finger double-tap detection to existing touch handler |
| S01's cancel dialog | `:feature:canvas` | Wire `confirmCancelEdit()` to actually revert the edit stack snapshot |

## DELIVERABLES

### Core Engine (`:core:undo`)

1. **`UndoAction` sealed interface** — base type for all undoable actions
2. **`UndoStack` class** — single stack with undo/redo lists, push/undo/redo/clear operations
3. **`UndoManager` class** — holds three `UndoStack` instances (browse, annotate, edit), routes operations by current mode
4. **Snapshot mechanism** — `UndoManager.snapshotEditStack()` and `UndoManager.revertEditStack()` for Edit Mode cancel
5. **Coalescing utility** — `UndoCoalescer` that groups rapid changes (e.g., slider drags) into a single undo entry on finger-up

### UI — Floating Pill (`:feature:canvas`)

6. **`UndoPill` composable** — two stacked buttons (↩ ↪) in a pill shape
   - Position: left edge, 8dp from edge, vertically centered on canvas
   - Size: 36×80dp pill (two 36×36dp buttons)
   - Background: semi-transparent surface (80% opacity), rounded pill, subtle border
   - Disabled state: 30% opacity when stack empty
   - Visible in ANNOTATE + EDIT modes only (hidden in BROWSE)
   - Auto-hide: fades to 20% during active draw gesture, restores on gesture end
7. **Long-press ↩ → history popup** — last 10 undo actions as a vertical list
   - Each item shows action description and timestamp
   - Tap any item → undo everything back to that point (batch undo)
   - Popup dismisses on tap outside or back gesture
8. **Left-hand mode stub** — pill reads a `isLeftHanded: Boolean` from ViewModel, positions on right edge when true. Setting UI deferred to S31.

### UI — Top Bar Icons (`:feature:canvas`)

9. **↩ ↪ in Browse Mode top bar** — wire the placeholder buttons from S01
   - Enabled/disabled state based on browse stack
   - Haptic tick on press

### UI — Two-Finger Double-Tap Gesture

10. **Gesture detection** — detect two-finger double-tap anywhere on canvas
    - Two pointers down within 300ms, both taps within 15dp of each other
    - Works in ALL three modes
    - No redo gesture (pill and top bar cover it)
    - Implementation: detect in `EditableTouchImageView.onTouchEvent()` alongside existing two-finger zoom/pan

### Haptic & Animation

11. **Haptic feedback** — `HapticFeedbackConstants.CLOCK_TICK` on every undo/redo
12. **Element animation** — undo: element fades out (150ms), redo: element fades in (150ms). For now, just the fade — actual element rendering comes in later sessions.

## DATA MODELS

```kotlin
// In :core:undo — new file UndoAction.kt

/**
 * Base type for all undoable actions. Every user-visible state change
 * creates an UndoAction and pushes it to the appropriate mode stack.
 */
interface UndoAction {
    val id: String              // Unique ID (UUID)
    val timestamp: Long         // System.currentTimeMillis()
    val description: String     // Human-readable: "Draw line", "Flip photo", "Remove photo"
    fun execute()               // Apply this action (for redo)
    fun reverse()               // Reverse this action (for undo)
}
```

```kotlin
// In :core:undo — new file UndoStack.kt

/**
 * A single undo/redo stack. One per mode.
 * Thread-safe: all operations synchronized.
 */
class UndoStack(
    private val maxDepth: Int = 50
) {
    // Exposed as read-only for UI observation
    val undoCount: StateFlow<Int>    // drives pill enabled/disabled
    val redoCount: StateFlow<Int>
    val recentActions: StateFlow<List<UndoActionInfo>>  // last 10, for history popup

    fun push(action: UndoAction)     // Push new action, clears redo stack
    fun undo(): Boolean              // Pop undo → push redo, returns false if empty
    fun redo(): Boolean              // Pop redo → push undo, returns false if empty
    fun undoTo(actionId: String)     // Batch undo back to specific action (history popup)
    fun clear()                      // Clear both stacks
    fun snapshot(): StackSnapshot    // Capture current state (for Edit Mode entry)
    fun restore(snapshot: StackSnapshot)  // Revert to snapshot (for Edit Mode cancel)
}

/**
 * Lightweight info for UI display (history popup).
 * Does NOT hold execute/reverse lambdas.
 */
data class UndoActionInfo(
    val id: String,
    val description: String,
    val timestamp: Long
)

/**
 * Opaque snapshot of stack state for Edit Mode cancel/revert.
 */
class StackSnapshot internal constructor(
    internal val undoActions: List<UndoAction>,
    internal val redoActions: List<UndoAction>
)
```

```kotlin
// In :core:undo — new file UndoManager.kt

/**
 * Routes undo/redo operations to the correct per-mode stack.
 * Injected via Hilt as @Singleton.
 */
@Singleton
class UndoManager @Inject constructor() {
    private val browseStack = UndoStack()
    private val annotateStack = UndoStack()
    private val editStack = UndoStack()
    private var editSnapshot: StackSnapshot? = null

    fun stackFor(mode: AppMode): UndoStack = when (mode) {
        AppMode.BROWSE -> browseStack
        AppMode.ANNOTATE -> annotateStack
        AppMode.EDIT -> editStack
    }

    fun push(mode: AppMode, action: UndoAction) = stackFor(mode).push(action)
    fun undo(mode: AppMode): Boolean = stackFor(mode).undo()
    fun redo(mode: AppMode): Boolean = stackFor(mode).redo()

    // Called on BROWSE → EDIT transition
    fun snapshotEditStack() {
        editSnapshot = editStack.snapshot()
    }

    // Called on Edit Cancel (Discard)
    fun revertEditStack() {
        editSnapshot?.let { editStack.restore(it) }
        editSnapshot = null
    }

    // Called on Edit Done
    fun commitEditStack() {
        editSnapshot = null
        editStack.clearRedo()  // intermediate redo states no longer meaningful
    }
}
```

```kotlin
// In :core:undo — new file UndoCoalescer.kt

/**
 * Groups rapid changes into a single undo entry.
 * Usage: call `track()` on every change, `commit()` on finger-up.
 * If only `track()` was called (no commit), auto-commits after timeout.
 *
 * Example: slider drag produces 30 changes/sec. Each calls track().
 * On finger-up, commit() merges all tracked changes into one UndoAction.
 */
class UndoCoalescer(
    private val undoManager: UndoManager,
    private val timeoutMs: Long = 500L
) {
    private var pendingDescription: String? = null
    private var firstState: Any? = null     // State before first change
    private var latestState: Any? = null    // State after last change

    fun track(description: String, beforeState: Any, afterState: Any) { ... }
    fun commit(mode: AppMode) { ... }       // Creates single UndoAction from first→latest
    fun cancel() { ... }                     // Discard tracked changes
}
```

## KEY IMPLEMENTATION DETAILS

### UndoStack Internal Structure

```
undoList: ArrayDeque<UndoAction>  (max 50 entries)
redoList: ArrayDeque<UndoAction>  (max 50 entries)

push(action):
    action.execute()           // Apply immediately
    undoList.addLast(action)
    redoList.clear()           // New action invalidates redo
    if (undoList.size > maxDepth) undoList.removeFirst()
    emit new counts

undo():
    if (undoList.isEmpty()) return false
    val action = undoList.removeLast()
    action.reverse()
    redoList.addLast(action)
    emit new counts
    return true

redo():
    if (redoList.isEmpty()) return false
    val action = redoList.removeLast()
    action.execute()
    undoList.addLast(action)
    emit new counts
    return true

undoTo(actionId):
    // Batch undo: keep calling undo() until we've undone the action with matching id
    while (undoList.isNotEmpty()) {
        val top = undoList.last()
        undo()
        if (top.id == actionId) break
    }
```

### Floating Pill Composable

```kotlin
@Composable
fun UndoPill(
    mode: AppMode,
    undoCount: Int,
    redoCount: Int,
    isDrawing: Boolean,        // True during active draw gesture → auto-hide
    isLeftHanded: Boolean,     // Pill on right edge if true
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUndoLongPress: () -> Unit  // Opens history popup
) {
    // Visibility: only in ANNOTATE and EDIT
    if (mode == AppMode.BROWSE) return

    // Auto-hide animation
    val targetAlpha = if (isDrawing) 0.2f else 0.8f
    val alpha by animateFloatAsState(targetAlpha, tween(200))

    // Position: left edge (or right edge for left-handed)
    val alignment = if (isLeftHanded) Alignment.CenterEnd else Alignment.CenterStart
    val padding = 8.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = padding),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    RoundedCornerShape(18.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .width(36.dp)
                .height(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Undo button (↩)
            IconButton(
                onClick = onUndo,
                enabled = undoCount > 0,
                modifier = Modifier
                    .size(36.dp)
                    .alpha(if (undoCount > 0) 1f else 0.3f)
                    .combinedClickable(
                        onClick = onUndo,
                        onLongClick = onUndoLongPress
                    )
            ) {
                Icon(Icons.Default.Undo, "Undo")
            }

            // Redo button (↪)
            IconButton(
                onClick = onRedo,
                enabled = redoCount > 0,
                modifier = Modifier
                    .size(36.dp)
                    .alpha(if (redoCount > 0) 1f else 0.3f)
            ) {
                Icon(Icons.Default.Redo, "Redo")
            }
        }
    }
}
```

### Two-Finger Double-Tap Detection

Add to `EditableTouchImageView.onTouchEvent()`:

```kotlin
// State tracking for two-finger double-tap
private var lastTwoFingerTapTime = 0L
private var lastTwoFingerTapCenter = PointF()
private val TWO_FINGER_DOUBLE_TAP_TIMEOUT = 300L  // ms
private val TWO_FINGER_DOUBLE_TAP_SLOP = 15f       // dp

// In onTouchEvent, when ACTION_POINTER_DOWN detected (2 fingers):
// 1. Record center of two fingers
// 2. On ACTION_POINTER_UP (back to 1 finger) within <200ms and <10dp movement:
//    → This is a "two-finger tap"
//    → If within 300ms of previous two-finger tap AND within 15dp:
//      → TWO-FINGER DOUBLE-TAP DETECTED → trigger undo callback
//      → Reset timer
//    → Else: record as new "last two-finger tap"
```

The gesture must NOT conflict with existing two-finger zoom/pan. Key distinction: zoom/pan involves continuous movement; double-tap is two quick stationary touches.

### Edit Mode Snapshot Integration (wire S01 stubs)

In `CanvasViewModel`, update the mode transition functions from S01:

```kotlin
fun enterEditMode(photoIndex: Int) {
    undoManager.snapshotEditStack()  // ← NEW: capture snapshot before edits
    _uiState.update { it.copy(mode = AppMode.EDIT, editingPhotoIndex = photoIndex) }
}

fun confirmCancelEdit() {
    undoManager.revertEditStack()    // ← NEW: revert to snapshot
    _uiState.update { it.copy(showCancelDialog = false, mode = AppMode.BROWSE) }
}

fun confirmDoneEdit() {
    undoManager.commitEditStack()    // ← NEW: clear snapshot, clear redo
    _uiState.update { it.copy(mode = AppMode.BROWSE) }
}
```

### Undo History Popup

```kotlin
@Composable
fun UndoHistoryPopup(
    actions: List<UndoActionInfo>,  // Last 10
    onUndoTo: (actionId: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Popup positioned next to the pill (offset right of pill)
    // Each item: Row with description + relative time ("2s ago", "15s ago")
    // Tap item → onUndoTo(id) → dismisses
    // Tap outside → onDismiss
}
```

### Coalescing Rules (from spec)

| Action | Undo Entries | Coalesce Rule |
|--------|-------------|---------------|
| Flip / Rotate tap | 1 per tap | None — each tap is discrete |
| Crop confirm | 1 total | Entire crop = 1 entry |
| Adjust slider | 1 per finger-up | Rapid movement coalesced on release |
| Property change (sheet) | 1 per finger-up | Same as adjust |
| Toggle (bold/italic) | 1 per toggle | None — discrete boolean |
| Color swatch tap | 1 per tap | None — discrete selection |
| HSB wheel drag | 1 per finger-up | Coalesced on dismissal |

The `UndoCoalescer` handles the "1 per finger-up" cases. Other cases are simple `push()` calls.

## RESOLVED CONFLICTS

**R-07 (Browse Mode Undo Access):**
- Undo pill HIDDEN in Browse (clean viewing)
- Undo/redo accessible via top bar icons in Browse
- Two-finger double-tap gesture works in ALL modes including Browse
- Destructive Browse actions (remove photo, change layout) get confirmation dialogs — implemented in S04 (Browse Chrome)

**R-08 (Edit Mode Cancel + Undo Stack):**
- Snapshot captured on BROWSE → EDIT entry via `snapshotEditStack()`
- Cancel (Discard) reverts via `revertEditStack()`
- Done commits via `commitEditStack()` (clears snapshot + redo)

## STUB REQUIREMENTS

| Feature | What to stub | Session that implements |
|---------|-------------|----------------------|
| Actual UndoAction implementations | Define `TestUndoAction` for sandbox testing — real actions come per-feature | S09+ |
| Auto-hide during draw gesture | `isDrawing` flag passed to pill — actual gesture detection in S05 | S05 |
| Left-hand mode setting | `isLeftHanded = false` hardcoded, setting UI in S31 | S31 |
| Element fade animation on undo/redo | Alpha animation stub — actual element rendering in later sessions | S09+ |

## TEST CRITERIA

1. **Pill visibility:** BROWSE → pill hidden. ANNOTATE → pill visible. EDIT → pill visible.
2. **Push + undo:** Push 3 test actions. Undo button enabled. Tap ↩ → last action reversed. Repeat 2 more → all reversed. ↩ now disabled (30% opacity).
3. **Redo:** After undoing, ↪ enabled. Tap ↪ → action re-applied. ↪ disabled after all redone.
4. **Clear redo on new action:** Undo 2 actions, push new action → ↪ disabled (redo stack cleared).
5. **Top bar undo (Browse):** In Browse mode, ↩ ↪ in top bar work same as pill.
6. **Two-finger double-tap:** Two-finger double-tap anywhere → undo fires. Works in all 3 modes.
7. **History popup:** Long-press ↩ → popup shows last 10 actions. Tap action → batch undo to that point.
8. **Edit snapshot/revert:** Enter Edit, push 3 actions, tap Cancel → Discard → all 3 reverted. Stack returns to entry state.
9. **Edit Done/commit:** Enter Edit, push 3 actions, tap Done → actions committed. Redo cleared.
10. **Haptic feedback:** Feel CLOCK_TICK on every undo/redo operation.
11. **Max depth:** Push 55 actions → oldest 5 silently dropped. Undo 50 → all undone. Stack empty.
12. **Coalescing (manual test):** Push coalescer.track() 10 times, then commit() → only 1 undo entry appears.
13. **Rotate device:** Undo stacks survive configuration change.

## FILES TO CREATE / MODIFY

### Create

| Module | Package | File | Purpose |
|--------|---------|------|---------|
| `:core:undo` | `com.bcd.technotes.core.undo` | `UndoAction.kt` | Interface for undoable actions |
| `:core:undo` | `com.bcd.technotes.core.undo` | `UndoStack.kt` | Single undo/redo stack with snapshot support |
| `:core:undo` | `com.bcd.technotes.core.undo` | `UndoManager.kt` | Three-stack manager routed by AppMode |
| `:core:undo` | `com.bcd.technotes.core.undo` | `UndoCoalescer.kt` | Groups rapid changes into single undo entry |
| `:core:undo` | `com.bcd.technotes.core.undo` | `TestUndoAction.kt` | Simple test implementation for sandbox |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `UndoPill.kt` | Floating pill composable |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `UndoHistoryPopup.kt` | Long-press history list |

### Modify

| Module | File | Changes |
|--------|------|---------|
| `:feature:canvas` | `CanvasViewModel.kt` | Inject `UndoManager`, expose stack counts as StateFlow, wire snapshot/revert/commit to mode transitions |
| `:feature:canvas` | `CanvasScreen.kt` | Add `UndoPill` in overlay, wire top bar ↩ ↪ in Browse mode, pass `isDrawing` state |
| `:feature:canvas` | `EditableTouchImageView.kt` | Add two-finger double-tap detection, callback to ViewModel |
| `:core:undo` | `build.gradle.kts` | Add dependencies (kotlinx-coroutines for StateFlow) |

### Unit Tests (`:core:undo`)

| Test File | What It Tests |
|-----------|--------------|
| `UndoStackTest.kt` | push/undo/redo, max depth, clear redo on push, snapshot/restore, undoTo |
| `UndoCoalescerTest.kt` | track multiple → commit → single action, cancel discards |

---

*Session 02 Blueprint — v1.0*
