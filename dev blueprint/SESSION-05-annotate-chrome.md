# Session 05 — Annotate Mode Chrome + Auto-Hide

## GOAL

Implement the Annotate Mode UI shell: top bar with ✕ Done / tool name / layers / properties icons, the 2dp blue accent bar, the bottom toolbar placeholder (56dp empty shell ready for S07), the chrome auto-hide system that fades UI during drawing, and the context bar that appears when an element is selected.

## DEPENDS ON

- **S01 (Mode System)** — `AppMode.ANNOTATE`, `exitAnnotateMode()`, accent bar, top bar shell
- **S02 (Undo/Redo)** — `UndoPill` composable with `isDrawing` auto-hide
- **S03 (Drawing Surface)** — `isDrawing` callback from `EditableTouchImageView`, `ActiveTool` state, `selectedElement` state

## SPEC REFERENCES

Read before coding:
- `INTERACTION-SPEC.md` §5 (Bottom Tool Palette Architecture — toolbar shape/size)
- `INTERACTION-SPEC.md` §15 (Top Bar — Annotate Mode)
- `INTERACTION-SPEC.md` §18 (Chrome Auto-Hide Behavior)
- `INTERACTION-SPEC.md` §20 (Complete Button Map — Annotate column)
- Context Bar rules from `editing-tools-interaction.html` §12
- `IMPLEMENTATION-BLUEPRINT.md` Part 2: P-03 (Canvas First — >95% canvas during draw)

## EXISTING CODE TO INTEGRATE WITH

| File | Module | Integration Notes |
|------|--------|-------------------|
| `CanvasScreen.kt` | `:feature:canvas` | S01 has mode-dependent top bar shell; S04 added Browse chrome. Add Annotate-specific chrome here. |
| `CanvasViewModel.kt` | `:feature:canvas` | Has `currentMode`, `activeTool`, `selectedElement`, `isDrawing` from previous sessions |
| `UndoPill.kt` | `:feature:canvas` | Already takes `isDrawing` for auto-hide from S02 |
| `EditableTouchImageView.kt` | `:feature:canvas` | Fires `onDrawingStateChanged` callback from S03 |

## DELIVERABLES

### Annotate Mode Top Bar

1. **Full Annotate top bar layout:**
   ```
   ┌────────────────────────────────────────────────────────────┐
   │  ✕ Done         Linear Dim · Snap 45°           🔲    ⚙  │
   └────────────────────────────────────────────────────────────┘
   ```
   - **✕ Done** (left) — exits to Browse via `exitAnnotateMode()`
   - **Tool name** (center) — displays active tool name from `ActiveTool` state. "Select" when `ActiveTool.Select`, "Text" when `ActiveTool.Text`, tool name when `ActiveTool.Draw`. "No Tool" when `ActiveTool.None`.
   - **Snap info** (center, after dot separator) — "Snap 45°" / "Snap OFF". Stub text for now — real snap toggle in S08.
   - **🔲 Layers** (right) — stub `onClick` showing toast. Real panel in S19.
   - **⚙ Properties** (right) — stub `onClick` showing toast. Real sheet in S18.

2. **2dp blue accent bar** — `#2196F3`, full width, below status bar, above top bar content. Already stubbed in S01 — ensure it renders correctly.

### Bottom Toolbar Placeholder

3. **Toolbar shell** (Annotate mode):
   - Pill-shaped container, 56dp height, 8dp from bottom edge, centered horizontally
   - Background: `Surface` color, rounded corners (28dp radius), subtle elevation
   - Width: spans ~90% of screen width (or wraps content with padding)
   - Contains 6 placeholder slots with icons and labels:
     ```
     ╱ Lines  │  📏 Measure  │  ○ Shapes  │  T Text  │  ◇ Marks  │  ⋯ More
     ```
   - All slots show icons but are non-functional (tap shows toast "Tool coming in next session"). Real tool palette logic in S07.
   - Active slot highlight: filled accent background at 15% opacity (stub — first slot "Lines" highlighted by default)

### Chrome Auto-Hide System

4. **`ChromeAutoHideController`** — manages the hide/show state for all chrome elements during drawing:

   **Trigger:** `isDrawing = true` from `EditableTouchImageView` (ACTION_DOWN + 10dp movement with a draw tool active in Annotate mode)

   **On draw start (200ms animation):**
   - Top bar → fade to 30% opacity
   - Bottom toolbar → slide down, leaving only 4dp blue accent line visible
   - Undo pill → fade to 20% opacity (already handled in S02's `UndoPill`)
   - Context bar → fade out completely

   **On draw end (500ms delay, then 200ms fade-in):**
   - All chrome restores to full visibility
   - The 500ms delay prevents flickering during brief pauses between strokes

   **Exceptions (chrome stays visible at full opacity):**
   - Two-finger zoom/pan gesture (not a draw gesture)
   - Category tray is open (future — S07)
   - Properties sheet is open (future — S18)
   - System keyboard is showing
   - Text cursor is active (`ActiveTool.Text` with active input)

5. **Space reclaimed:** When chrome hides, canvas gains ~104dp of vertical space:
   - Top bar: 48dp → 0dp (faded, still in layout but transparent)
   - Bottom toolbar: 56dp → 4dp accent line
   - Total reclaimed: ~100dp

### Context Bar (Element Selection)

6. **`AnnotationContextBar` composable** — floating bar above selected element:
   ```
   ┌──────────────────────────────────────────────┐
   │  ● color  │  1px  2px  [3px]  │  ⧉  🗑  │  ⚙  │
   └──────────────────────────────────────────────┘
   ```
   - **Position:** 16dp above selected element's bounding box (in screen-space). Auto-repositions if element is near top of screen.
   - **Appears on:** single tap to select an element (200ms fade-in)
   - **Disappears on:** tap empty canvas, select different element (bar updates), draw gesture starts (chrome auto-hide), 150ms fade-out
   - **Contents (stubs for now — real interactions in S18):**
     - Color dot: shows current element color, tap opens color picker (stub)
     - Stroke width presets: 1px, 2px, 3px quick-select (stub)
     - ⧉ Duplicate: duplicate selected element (stub)
     - 🗑 Delete: delete selected element, push to undo stack (functional — this is simple enough to wire now)
     - ⚙ Full properties: opens properties sheet (stub)
   - **Delete is functional now:** removes element from annotations list, pushes `DeleteElementUndoAction` to annotate stack.

## DATA MODELS

```kotlin
// Add to CanvasViewModel state
data class CanvasUiState(
    // ... existing from S01, S04 ...
    
    // Annotate chrome state
    val toolDisplayName: String = "No Tool",
    val snapDisplayText: String = "Snap OFF",
    val isDrawing: Boolean = false,         // drives auto-hide
    val chromeHidden: Boolean = false,      // derived: true when isDrawing after 10dp threshold
    val showContextBar: Boolean = false,    // true when element selected
    val contextBarPosition: Offset = Offset.Zero,  // screen-space position above element
)
```

```kotlin
// In :core:undo — concrete UndoAction for element deletion
class DeleteElementUndoAction(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val description: String,
    private val element: DrawableElement,
    private val elementList: MutableList<DrawableElement>
) : UndoAction {
    override fun execute() { elementList.remove(element) }
    override fun reverse() { elementList.add(element) }
}
```

## KEY IMPLEMENTATION DETAILS

### Annotate Top Bar Composable

```kotlin
@Composable
fun AnnotateTopBar(
    toolName: String,
    snapText: String,
    chromeAlpha: Float,        // 1.0 normal, 0.3 during draw
    onDoneClick: () -> Unit,
    onLayersClick: () -> Unit,
    onPropertiesClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .alpha(chromeAlpha)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✕ Done
        TextButton(onClick = onDoneClick) {
            Icon(Icons.Default.Close, "Done", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Done")
        }

        // Tool name + snap info (center, takes remaining space)
        Text(
            text = buildAnnotatedString {
                append(toolName)
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))) {
                    append(" · $snapText")
                }
            },
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 🔲 Layers
        IconButton(onClick = onLayersClick) {
            Icon(Icons.Default.Layers, "Layers")
        }
        // ⚙ Properties
        IconButton(onClick = onPropertiesClick) {
            Icon(Icons.Default.Settings, "Properties")
        }
    }
}
```

### Chrome Auto-Hide State Machine

```kotlin
// In CanvasViewModel or a dedicated controller

class ChromeAutoHideController {
    private val _chromeAlpha = MutableStateFlow(1f)
    val chromeAlpha: StateFlow<Float> = _chromeAlpha.asStateFlow()

    private var restoreJob: Job? = null
    private val scope: CoroutineScope  // injected or viewModelScope

    // Track exception conditions
    var isTrayOpen: Boolean = false
    var isSheetOpen: Boolean = false
    var isKeyboardVisible: Boolean = false
    var isTextInputActive: Boolean = false

    private val hasException: Boolean
        get() = isTrayOpen || isSheetOpen || isKeyboardVisible || isTextInputActive

    fun onDrawingStarted() {
        if (hasException) return  // Don't hide if exception active
        restoreJob?.cancel()
        scope.launch {
            // Animate to hidden (200ms)
            animate(1f, 0.3f, animationSpec = tween(200)) { value, _ ->
                _chromeAlpha.value = value
            }
        }
    }

    fun onDrawingEnded() {
        restoreJob?.cancel()
        restoreJob = scope.launch {
            delay(500)  // Wait 500ms before restoring
            // Animate to visible (200ms)
            animate(0.3f, 1f, animationSpec = tween(200)) { value, _ ->
                _chromeAlpha.value = value
            }
        }
    }
}
```

### Toolbar Auto-Hide: Slide to Accent Line

The bottom toolbar has a special auto-hide behavior — instead of just fading, it slides down to leave only a 4dp blue accent line visible:

```kotlin
@Composable
fun AnnotateToolbarArea(
    chromeAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit  // The actual toolbar content (placeholder now, S07 later)
) {
    val isHidden = chromeAlpha < 0.5f

    Box(modifier = modifier.navigationBarsPadding()) {
        // 4dp accent line — always visible
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0xFF2196F3))  // blue
        )

        // Toolbar content — slides down when hidden
        val toolbarOffset by animateDpAsState(
            targetValue = if (isHidden) 56.dp else 0.dp,
            animationSpec = tween(200)
        )

        Box(
            modifier = Modifier
                .offset(y = toolbarOffset)
                .alpha(chromeAlpha)
        ) {
            content()
        }
    }
}
```

### Context Bar Positioning

```kotlin
@Composable
fun AnnotationContextBar(
    selectedElement: DrawableElement?,
    viewMatrix: Matrix,
    chromeAlpha: Float,
    onColorClick: () -> Unit,
    onStrokeClick: (Float) -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPropertiesClick: () -> Unit,
) {
    if (selectedElement == null) return
    if (chromeAlpha < 0.5f) return  // Hidden during draw

    val bounds = selectedElement.getBounds()
    // Convert top-center of bounds to screen-space
    val screenTopCenter = imageToScreen(
        bounds.centerX(), bounds.top, viewMatrix
    )
    
    // Position 16dp above element, clamped to screen
    val barY = (screenTopCenter.y - 16.dp.toPx() - barHeight).coerceAtLeast(statusBarHeight)
    val barX = screenTopCenter.x  // centered on element

    Box(
        modifier = Modifier
            .offset { IntOffset(barX.roundToInt(), barY.roundToInt()) }
            .alpha(chromeAlpha)
    ) {
        // Context bar content
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Color dot
                Box(Modifier.size(18.dp).background(Color.Blue, CircleShape).clickable { onColorClick() })
                VerticalDivider()
                // Stroke presets
                listOf(1f, 2f, 3f).forEach { width ->
                    Text("${width.toInt()}px", modifier = Modifier.clickable { onStrokeClick(width) })
                }
                VerticalDivider()
                // Duplicate
                IconButton(onClick = onDuplicateClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, "Duplicate", modifier = Modifier.size(16.dp))
                }
                // Delete
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
                VerticalDivider()
                // Properties
                IconButton(onClick = onPropertiesClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Settings, "Properties", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
```

### Keyboard Visibility Detection

For the auto-hide exception when keyboard is showing:

```kotlin
// In CanvasScreen — detect keyboard visibility
val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
LaunchedEffect(imeVisible) {
    chromeController.isKeyboardVisible = imeVisible
}
```

## RESOLVED CONFLICTS

No new resolutions for this session. The auto-hide behavior is unambiguous in the spec (§18). The context bar positioning uses the editing-tools-interaction spec which doesn't conflict with INTERACTION-SPEC.

## STUB REQUIREMENTS

| Feature | What to stub | Session that implements |
|---------|-------------|----------------------|
| Tool palette (6 slots) | Visual shell with icons + labels, tap shows toast | S07 (Tool Palette Architecture) |
| Snap info text | Static "Snap OFF" text | S08 (Snap Engine) |
| 🔲 Layers button | Shows toast "Layers panel coming soon" | S19 (Layer Panel) |
| ⚙ Properties button | Shows toast "Properties coming soon" | S18 (Properties Sheet) |
| Context bar color picker | Color dot visible, tap shows toast | S18 |
| Context bar stroke presets | Text visible, tap shows toast | S18 |
| Context bar duplicate | Shows toast | S18 |
| Context bar ⚙ properties | Shows toast | S18 |
| Tray open exception | `isTrayOpen` flag exists but never set true | S07 |
| Sheet open exception | `isSheetOpen` flag exists but never set true | S18 |

## TEST CRITERIA

1. **Annotate top bar:** Enter Annotate → ✕ Done, tool name, 🔲, ⚙ all visible in correct positions
2. **✕ Done exits:** Tap ✕ Done → returns to Browse mode
3. **Tool name updates:** When `activeTool` changes (manually via test), top bar center text updates
4. **Blue accent bar:** 2dp blue bar visible at top when in Annotate mode
5. **Toolbar shell visible:** 56dp pill-shaped toolbar at bottom with 6 placeholder icons
6. **Chrome auto-hide — draw start:** Start draw gesture (10dp+ movement with draw tool) → top bar fades to 30%, toolbar slides down to 4dp accent line
7. **Chrome auto-hide — draw end:** Release finger → 500ms delay → chrome fades back in (200ms)
8. **Chrome auto-hide — rapid strokes:** Multiple quick strokes don't cause flickering (500ms delay resets on each new stroke)
9. **Chrome stays for two-finger:** Two-finger zoom/pan does NOT trigger auto-hide
10. **Chrome stays for keyboard:** When keyboard is visible, chrome stays at full opacity during any gesture
11. **Context bar appears:** Tap an annotation element → context bar fades in 16dp above it
12. **Context bar repositions:** Select element near top of screen → bar appears below element instead
13. **Context bar delete:** Tap 🗑 on context bar → element deleted, pushed to undo stack, context bar disappears
14. **Context bar hides during draw:** Start drawing → context bar fades out with other chrome
15. **Context bar updates on reselect:** Tap different element → bar moves to new position
16. **Context bar dismisses:** Tap empty canvas → bar disappears
17. **Space reclaimed:** During draw, visually confirm >95% canvas visible
18. **Rotate device:** Chrome state survives rotation. Auto-hide animations still work.

## FILES TO CREATE / MODIFY

### Create

| Module | Package | File | Purpose |
|--------|---------|------|---------|
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `AnnotateTopBar.kt` | Annotate mode top bar composable |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `AnnotateToolbarPlaceholder.kt` | 56dp pill-shaped toolbar shell with 6 placeholder slots |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `AnnotationContextBar.kt` | Floating context bar above selected element |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `ChromeAutoHideController.kt` | State machine for auto-hide with exceptions |
| `:core:undo` | `com.bcd.technotes.core.undo` | `DeleteElementUndoAction.kt` | Undo action for element deletion |

### Modify

| Module | File | Changes |
|--------|------|---------|
| `:feature:canvas` | `CanvasViewModel.kt` | Add `toolDisplayName`, `chromeAlpha` via `ChromeAutoHideController`, wire `isDrawing` callback to controller, add `deleteSelectedElement()` function |
| `:feature:canvas` | `CanvasScreen.kt` | Add `AnnotateTopBar` for Annotate mode, `AnnotateToolbarPlaceholder` at bottom, `AnnotationContextBar` overlay, wire `chromeAlpha` to all chrome elements, detect keyboard visibility |
| `:feature:canvas` | `UndoPill.kt` | Verify `isDrawing` auto-hide still works alongside new chrome controller (should already work from S02) |

---

*Session 05 Blueprint — v1.0*
