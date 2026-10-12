# Session 04 — Browse Mode Chrome

## GOAL

Implement all Browse Mode UI elements: top bar with contextual buttons, primary "+" FAB with import popup, secondary "✏" Annotate FAB, edit icon for single photo, project name with inline rename, save state indicator, layout selector visibility rules, and the empty canvas state. This session makes Browse Mode fully functional as the app's home state.

## DEPENDS ON

- **S01 (Mode System)** — `AppMode.BROWSE`, mode transition functions, top bar shell
- **S02 (Undo/Redo)** — `UndoManager`, ↩ ↪ top bar buttons wired to browse stack
- Photo picker, collage canvas, layout engine are already implemented (existing code)

## SPEC REFERENCES

Read before coding:
- `INTERACTION-SPEC.md` §3 (Canvas Screen Layout), §9 (Project Save/Load — title, menu), §10 (Import System — "+" FAB), §15 (Top Bar — Browse Mode), §20 (Complete Button Map)
- Resolution R-03 (Single Photo Edit Entry — ✎ icon)
- Resolution R-07 (Browse Mode Undo Access — confirmation dialogs)
- Resolution R-09 (Empty Canvas State)
- Resolution R-10 (Layout Selector & Thumbnail Bar Visibility)
- Resolution R-11 (Video/GIF — third FAB popup item, stub)
- Resolution R-13 (Collage → Single Photo Fallback)

## EXISTING CODE TO INTEGRATE WITH

| File | Module | What It Does | Integration Notes |
|------|--------|-------------|-------------------|
| `CanvasScreen.kt` | `:feature:canvas` | Main composable — already has mode-dependent top bar shell from S01 | Replace Browse mode shell with full chrome |
| `CanvasViewModel.kt` | `:feature:canvas` | State management — has mode, undo wiring from S01/S02 | Add project title, photo count, canvas empty state, FAB popup state |
| `PhotoPickerScreen.kt` | `:feature:picker` | Existing photo picker with albums/selection/preview | "+" FAB → Photo navigates here |
| `CollageLayout.kt` | `:core:model` | Layout definitions, 20+ layouts | Layout selector shows these as thumbnails |
| `EditableTouchImageView.kt` | `:feature:canvas` | Existing photo display with zoom/pan | Already has long-press → edit mode from S01 |

## DELIVERABLES

### Top Bar — Browse Mode

1. **Full Browse top bar layout:**
   ```
   ┌────────────────────────────────────────────────────────────┐
   │  ☰   My Project ●                 [✎]  ↩  ↪   ⬆   ⋮    │
   └────────────────────────────────────────────────────────────┘
   ```
   - **☰ Menu** (left) — opens hamburger menu. For now: stub with "Save", "Open Project", "Settings" items that show "Coming Soon" toast. Real implementation in S23.
   - **Project Name** (center-left) — displays current project title, truncated with ellipsis if long
   - **● Save Indicator** — colored dot next to project name. Static green for now (auto-save logic in S24)
   - **✎ Edit Icon** — visible ONLY when single photo on canvas (per R-03). Tap → `enterEditMode(0)`
   - **↩ ↪ Undo/Redo** — wired to browse stack from S02. Enabled/disabled per stack state.
   - **⬆ Export** — placeholder, shows "Coming Soon" toast. Real implementation in S25.
   - **⋮ Overflow** — stub menu placeholder

2. **Project name inline edit:**
   - Tap project name → transforms to `TextField` with cursor
   - Keyboard appears, user types new name
   - Tap outside or press Enter → confirm rename, keyboard dismisses
   - Default: "Untitled Project" with auto-increment for new projects
   - Name stored in ViewModel (persisted in S23)

3. **Top bar semi-transparency:**
   - Background: `Surface` color at 90% opacity
   - Respects system bar insets (padding top for status bar)

### FABs — Primary "+" and Secondary "✏"

4. **Primary FAB "+"** (bottom-right corner):
   - Size: 56dp, accent color (from theme)
   - Position: 16dp from right edge, 16dp from bottom edge (above gesture bar)
   - Tap → shows import popup above FAB

5. **Import popup** (above "+" FAB):
   ```
   ┌─────────────────┐
   │  📷  Photo      │  → navigate to PhotoPickerScreen
   ├─────────────────┤
   │  📄  Document   │  → system file picker (PDF) — stub toast for now
   ├─────────────────┤
   │  🎬  Video      │  → "Coming Soon" toast (per R-11)
   └─────────────────┘
   ```
   - 3-item vertical list, 48dp per item, icon + label
   - Background: elevated surface with shadow
   - Dismiss: tap outside or back gesture
   - 📷 Photo: navigates to existing `PhotoPickerScreen`
   - 📄 Document: shows "PDF import coming soon" toast (real implementation deferred — existing photo import is sufficient for now)
   - 🎬 Video: shows "Coming Soon" toast

6. **Secondary FAB "✏"** (left of primary):
   - Size: 44dp, secondary accent color
   - Position: 12dp left of "+" FAB, same vertical
   - Tap → `enterAnnotateMode()`
   - Visible only when canvas has content (hidden when empty per R-09)

### Edit Icon for Single Photo (R-03)

7. **✎ icon in top bar:**
   - Visible ONLY when `photoCount == 1` (single photo, not collage)
   - Position: right side of top bar, before ↩ ↪ buttons
   - Tap → `enterEditMode(0)`
   - Hidden when: canvas empty, collage (2+ photos), or non-Browse mode

### Empty Canvas State (R-09)

8. **When canvas has no photos:**
   - "+" FAB → ✅ VISIBLE (primary action)
   - "✏" Annotate FAB → HIDDEN
   - "⬆" Export → HIDDEN
   - "✎" Edit icon → HIDDEN
   - ↩ ↪ Undo/Redo → HIDDEN (nothing to undo)
   - ☰ Menu → ✅ VISIBLE
   - Project name → ✅ EDITABLE
   - Center hint text: "Tap + to add a photo or document" (dimmed, 14sp, center of canvas)

### Layout Selector & Thumbnail Bar Visibility (R-10)

9. **Layout selector** (existing horizontal scroll of 48×48 layout previews):
   - Visible ONLY in Browse mode AND when collage (2+ photos)
   - Animate out: 200ms slide-down on Browse → Annotate/Edit transition
   - Animate in: 250ms spring slide-up on return to Browse

10. **Thumbnail bar** (existing bottom-left collage minimap):
    - Same visibility rules as layout selector
    - Same animations

11. **Collage → Single Photo Fallback (R-13):**
    - When photo removal reduces collage to 1 photo:
      - Layout selector: 150ms slide-down animation out
      - Thumbnail bar: 150ms slide-down animation out
      - Remaining photo: 300ms scale animation to fill canvas
    - Undo of removal: reverse animations restore collage state

### FAB Visibility Per Mode (R-10)

12. **FAB visibility rules:**
    - BROWSE: both FABs visible (with empty canvas exception for ✏)
    - ANNOTATE: both FABs hidden (200ms fade-out on mode entry)
    - EDIT: both FABs hidden (200ms fade-out on mode entry)
    - On return to BROWSE: 200ms fade-in

### Confirmation Dialogs for Destructive Browse Actions (R-07)

13. **Remove Photo confirmation:**
    ```
    "Remove this photo?"
    [Cancel]  [Remove]
    (small hint: "Undo available after removal")
    ```
    - Shown before removing a photo from collage
    - On Remove: photo removed, action pushed to browse undo stack

14. **Change Layout confirmation** (optional — only if layout change would crop content):
    - Stub for now, real logic when layout switching adds photo loss risk

## DATA MODELS

```kotlin
// Add to CanvasViewModel state
data class CanvasUiState(
    // ... existing from S01 ...
    val projectTitle: String = "Untitled Project",
    val isRenaming: Boolean = false,
    val saveState: SaveState = SaveState.SAVED,
    val photoCount: Int = 0,           // drives empty state + ✎ visibility
    val isCollage: Boolean = false,    // drives layout selector + thumbnail visibility
    val showImportPopup: Boolean = false,
    val showRemovePhotoDialog: Boolean = false,
    val removePhotoIndex: Int = -1,
)

enum class SaveState {
    SAVED,      // ● green
    UNSAVED,    // ● amber
    SAVING,     // ● gray (animated pulse)
    ERROR       // ● red
}
```

## KEY IMPLEMENTATION DETAILS

### Browse Mode Top Bar Composable

```kotlin
@Composable
fun BrowseTopBar(
    projectTitle: String,
    isRenaming: Boolean,
    saveState: SaveState,
    photoCount: Int,
    undoCount: Int,
    redoCount: Int,
    onMenuClick: () -> Unit,
    onTitleTap: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onTitleConfirmed: () -> Unit,
    onEditPhotoClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExportClick: () -> Unit,
    onOverflowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ☰ Menu
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, "Menu")
        }

        // Project name + save dot (takes remaining space)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRenaming) {
                // Inline TextField
                BasicTextField(
                    value = projectTitle,
                    onValueChange = onTitleChanged,
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = { onTitleConfirmed() }),
                    // ... styling
                )
            } else {
                Text(
                    text = projectTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onTitleTap() }
                )
            }
            // ● Save indicator
            SaveStateDot(saveState)
        }

        // ✎ Edit (single photo only)
        if (photoCount == 1) {
            IconButton(onClick = onEditPhotoClick) {
                Icon(Icons.Default.Edit, "Edit photo")
            }
        }

        // ↩ Undo
        IconButton(onClick = onUndo, enabled = undoCount > 0) {
            Icon(Icons.Default.Undo, "Undo", alpha = if (undoCount > 0) 1f else 0.3f)
        }
        // ↪ Redo
        IconButton(onClick = onRedo, enabled = redoCount > 0) {
            Icon(Icons.Default.Redo, "Redo", alpha = if (redoCount > 0) 1f else 0.3f)
        }

        // ⬆ Export
        IconButton(onClick = onExportClick) {
            Icon(Icons.Default.Upload, "Export")
        }

        // ⋮ Overflow
        IconButton(onClick = onOverflowClick) {
            Icon(Icons.Default.MoreVert, "More options")
        }
    }
}
```

### Import Popup Composable

```kotlin
@Composable
fun ImportPopup(
    onDismiss: () -> Unit,
    onPhotoClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onVideoClick: () -> Unit,
) {
    // Popup positioned above "+" FAB
    // Background: elevated surface, rounded corners, shadow
    Popup(
        alignment = Alignment.BottomEnd,
        offset = IntOffset(x = -16.dp.roundToPx(), y = -(56 + 8).dp.roundToPx())
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                ImportPopupItem(icon = Icons.Default.PhotoCamera, label = "Photo", onClick = {
                    onDismiss()
                    onPhotoClick()
                })
                ImportPopupItem(icon = Icons.Default.Description, label = "Document", onClick = {
                    onDismiss()
                    onDocumentClick()
                })
                ImportPopupItem(icon = Icons.Default.Videocam, label = "Video", onClick = {
                    onDismiss()
                    onVideoClick()
                })
            }
        }
    }
}

@Composable
private fun ImportPopupItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
```

### Empty Canvas Hint

```kotlin
@Composable
fun EmptyCanvasHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tap + to add a photo or document",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
```

### Save State Dot

```kotlin
@Composable
fun SaveStateDot(state: SaveState) {
    val color = when (state) {
        SaveState.SAVED -> Color(0xFF4CAF50)    // green
        SaveState.UNSAVED -> Color(0xFFFF9800)  // amber
        SaveState.SAVING -> Color(0xFF9E9E9E)   // gray
        SaveState.ERROR -> Color(0xFFF44336)    // red
    }
    // Pulse animation for SAVING state
    val alpha = if (state == SaveState.SAVING) {
        val infiniteTransition = rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
        ).value
    } else 1f

    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .size(8.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}
```

### FAB Visibility Animation

```kotlin
// In CanvasScreen, FABs animate based on mode
val fabVisible = uiState.mode == AppMode.BROWSE

AnimatedVisibility(
    visible = fabVisible,
    enter = fadeIn(tween(200)),
    exit = fadeOut(tween(200))
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 16.dp, bottom = 16.dp)
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.weight(1f))
        
        // Secondary FAB "✏" (only if canvas has content)
        if (uiState.photoCount > 0) {
            SmallFloatingActionButton(
                onClick = { viewModel.enterAnnotateMode() },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Create, "Annotate")
            }
        }
        
        // Primary FAB "+"
        FloatingActionButton(
            onClick = { viewModel.showImportPopup() }
        ) {
            Icon(Icons.Default.Add, "Add photo or document")
        }
    }
}
```

### Layout Selector / Thumbnail Bar Animation

```kotlin
// Animate visibility based on mode + collage state
val showCollageControls = uiState.mode == AppMode.BROWSE && uiState.isCollage

AnimatedVisibility(
    visible = showCollageControls,
    enter = slideInVertically(
        initialOffsetY = { it },  // from below
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
    ),
    exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(200)
    )
) {
    // Existing layout selector and thumbnail bar composables
    LayoutSelectorBar(...)
    CollageThumbnailBar(...)
}
```

## RESOLVED CONFLICTS

**R-03:** ✎ edit icon in top bar, visible only for single photo. Position: before ↩ ↪ buttons.

**R-07:** Destructive Browse actions (remove photo) get confirmation dialog with "Undo available" hint. Two-finger double-tap undo still works in Browse.

**R-09:** Empty canvas shows only "+" FAB, ☰ Menu, project name. All other elements hidden. Center hint text.

**R-10:** Layout selector and thumbnail bar visible only in Browse + collage state. Animate out on mode change.

**R-11:** "+" FAB popup has 3 items (Photo, Document, Video). Video shows "Coming Soon" toast.

**R-13:** Collage → single photo: layout selector/thumbnail animate out, photo scales to fill canvas.

## STUB REQUIREMENTS

| Feature | What to stub | Session that implements |
|---------|-------------|----------------------|
| ☰ Menu full content | Show drawer/sheet with placeholder items, toast on tap | S23 (Project Save/Load) |
| ⬆ Export | Show toast "Export coming soon" | S25 (Export) |
| 📄 Document import | Show toast "PDF import coming soon" | Future (already have photo import) |
| 🎬 Video import | Show toast "Coming Soon" | S30 (Video-to-GIF) |
| Auto-save state changes | SaveState stays SAVED (green) | S24 (Auto-Save) |
| ⋮ Overflow menu | Empty menu or toast | S23+ |

## TEST CRITERIA

1. **Top bar layout:** All elements visible in correct positions — ☰, project name, ●, ↩, ↪, ⬆, ⋮
2. **Project rename:** Tap project name → TextField appears → type new name → tap outside → name updated
3. **Save indicator:** Green dot visible next to project name (static for now)
4. **Single photo ✎ icon:** Load one photo → ✎ visible in top bar. Tap → enters Edit mode.
5. **Collage no ✎:** Load 2+ photos → ✎ hidden (collage, not single photo)
6. **"+" FAB visible:** Always visible in Browse (even on empty canvas)
7. **Import popup:** Tap "+" → popup with 3 items. Tap Photo → navigates to picker. Tap Document/Video → toast. Tap outside → dismisses.
8. **"✏" FAB visible:** Load photo → ✏ visible. Tap → enters Annotate mode.
9. **"✏" FAB hidden on empty:** No photos → ✏ hidden
10. **FABs hide on mode change:** Enter Annotate → both FABs fade out. Return to Browse → fade in.
11. **Empty canvas state:** No photos → only "+", ☰, project name visible. Center hint text shown. ⬆ hidden. ✏ hidden. ↩ ↪ hidden.
12. **Layout selector visible (collage):** Load 3 photos → layout selector and thumbnail bar visible
13. **Layout selector hidden (single):** Load 1 photo → no layout selector, no thumbnail bar
14. **Layout controls hide on mode switch:** In collage Browse mode, enter Annotate → layout selector/thumbnail slide down. Return → slide up.
15. **Remove photo confirmation (R-07):** Trigger remove photo → dialog appears → Cancel keeps photo → Remove deletes + pushes to undo
16. **Undo/redo in top bar:** In Browse, push a browse action → ↩ enabled. Tap → undone. ↪ enabled.
17. **Rotate device:** All chrome elements survive configuration change. Positions correct.

## FILES TO CREATE / MODIFY

### Create

| Module | Package | File | Purpose |
|--------|---------|------|---------|
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `BrowseTopBar.kt` | Full Browse mode top bar composable |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `ImportPopup.kt` | "+" FAB import options popup |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `EmptyCanvasHint.kt` | Center hint for empty canvas |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas.ui` | `SaveStateDot.kt` | Animated save indicator dot |

### Modify

| Module | File | Changes |
|--------|------|---------|
| `:feature:canvas` | `CanvasViewModel.kt` | Add `projectTitle`, `saveState`, `photoCount`, `isCollage`, `showImportPopup`, `showRemovePhotoDialog` state fields. Add rename functions, import popup toggle, remove photo with confirmation. |
| `:feature:canvas` | `CanvasScreen.kt` | Replace Browse mode top bar shell with `BrowseTopBar`. Add FABs with visibility animation. Add `ImportPopup`. Add `EmptyCanvasHint`. Add layout selector/thumbnail visibility animations. Add remove photo confirmation dialog. |

---

*Session 04 Blueprint — v1.0*
