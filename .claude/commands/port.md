# Session Wrap-Up: Port & Finalize

Complete session wrap-up workflow. Run this when done working in the sandbox, or at the start of a new session to sync everything up. This is a strict, sequential process — do not skip steps.

## Architecture Mapping

The sandbox uses monolithic composables. The app uses multi-module MVVM + Repository + Hilt DI. When porting, sandbox code must be routed to the correct module and layer:

| Change Type | Target Module | Target Package |
|---|---|---|
| Data classes, sealed classes, enums | :core:model | com.bcd.technotes.core.model |
| Theme tokens, colors, typography, spacing | :core:ui | com.bcd.technotes.core.ui.theme |
| Shared composables (buttons, chips, sliders, toggles) | :core:ui | com.bcd.technotes.core.ui.components |
| Drawing algorithms, snap, smoothing | :core:drawing | com.bcd.technotes.core.drawing |
| Bitmap operations, image processing | :core:image | com.bcd.technotes.core.image |
| Project persistence, auto-save | :core:project | com.bcd.technotes.core.project |
| Overlay engine, masks, blends | :core:overlay | com.bcd.technotes.core.overlay |
| Undo/redo logic | :core:undo | com.bcd.technotes.core.undo |
| Canvas UI, canvas ViewModel | :feature:canvas | com.bcd.technotes.feature.canvas |
| Photo picker UI, picker ViewModel | :feature:picker | com.bcd.technotes.feature.picker |
| Image editor UI, editor ViewModel | :feature:editor | com.bcd.technotes.feature.editor |
| Export UI, export ViewModel | :feature:export | com.bcd.technotes.feature.export |
| Navigation routes, DI bindings | :app | com.bcd.technotes |
| EditableTouchImageView | :feature:canvas | 1:1 copy with package rename |
| New dependencies | Update version catalog + relevant module build.gradle.kts |

### Resource Externalization — Porting Rule

During porting, all UI values MUST reference the design system:

```kotlin
// WRONG (sandbox-style — hardcoded values)
Modifier.padding(16.dp)
Color(0xFF6200EE)

// RIGHT (production-style — design system tokens)
Modifier.padding(TechNotesTheme.spacing.lg)
MaterialTheme.colorScheme.primary
TechNotesTheme.extendedColors.canvas
```

## Step 1: Assess session state

1. Run `git status` and `git diff` to see what changed
2. Read DECISIONS.md to understand what features were worked on
3. Summarize to the user: what files changed, what features are in play

If there are no changes (clean working tree, sandbox and app already in sync), tell the user and stop.

## Step 2: Identify what changed in sandbox

Read all changed sandbox Kotlin files. For each change, classify it using the architecture mapping above:

- **New data classes** → route to :core:model
- **New/changed image processing functions** → route to :core:image
- **New/changed state variables or reactive logic** → route to the appropriate :feature:* ViewModel
- **New/changed UI composables** → route to the appropriate :feature:* module
- **New/changed data queries** → route to :feature:picker (MediaRepository)
- **EditableTouchImageView changes** → 1:1 copy to :feature:canvas with package rename
- **New sandbox files with no app counterpart** → flag and create following the architecture pattern

Also check for new dependencies in `sandbox/build.gradle.kts` that need adding to the relevant module's `build.gradle.kts`.

## Step 3: Side-by-side diff

For EACH sandbox change identified in Step 2:
1. Read the sandbox source AND the corresponding app target file(s)
2. Compare the specific functions/state/composables — not whole files
3. List every behavioral difference — be exhaustive:
   - Missing functions, state variables, or composables
   - Different parameter signatures or logic
   - Different UI behavior (gestures, callbacks, animations)
   - Missing edge cases, clamping, or error handling

Report the differences to the user before proceeding.

## Step 4: Apply changes

For each difference found, apply it to the correct module and layer:

1. **Core models**: Add/update classes in :core:model
2. **Image services**: Add/update methods in :core:image BitmapService.kt
3. **Repository**: Add/update methods in :feature:picker MediaRepository interface + impl
4. **ViewModels**: Add StateFlows for new state, add action methods for new behaviors. Convert `LaunchedEffect` triggers to `viewModelScope.launch` with `combine`/`collect`.
5. **UI composables**: Update existing composable files or create new ones in :feature:*. Composables should read ViewModel state via `collectAsState()` and call ViewModel actions.
6. **Hilt**: If new services/repos were added, update :app `di/AppModule.kt`
7. **1:1 files** (EditableTouchImageView, TouchImageViewExt): Copy to :feature:canvas with package rename

Do NOT change the sandbox files.

## Step 5: Verify port

After all changes are applied:
1. For each change, read the sandbox source and ALL corresponding app target files
2. Confirm every behavior matches — check edge cases, clamping, animations, gesture handling, callbacks
3. Verify the ViewModel properly exposes all state and actions that the composable needs
4. If any mismatch remains, fix it before proceeding

## Step 6: Build both apps

```bash
./gradlew :sandbox:assembleDebug :app:assembleDebug
```

Both must build successfully. If build fails, fix and retry.

## Step 7: Install both on device

```bash
adb install -r sandbox/build/outputs/apk/debug/sandbox-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch both for the user to verify:
```bash
adb shell am start -n com.bcd.technotes.sandbox/.SandboxActivity
adb shell am start -n com.bcd.technotes/.MainActivity
```

## Step 8: Update DECISIONS.md

For each feature or change made during this session that is not yet logged:
- Add a new section with the feature name and date
- Log decisions with SUCCESS/FAILED outcomes and reasoning
- If this was a port, log it as a port entry with verification status

Do NOT duplicate entries that are already in the log.

## Step 9: Commit and push

1. Run `git status` and `git diff` to see all changes
2. Run `git log --oneline -5` to match commit message style
3. Stage all changed source files (not screenshots, temp files, or .env)
4. Create a descriptive commit message summarizing what was done
5. Push to origin/main

---

**IMPORTANT**: Do not mark any step as complete until it is actually done. If a step fails, fix the issue and retry that step. The user should be able to run `/port` and have confidence that the app is fully in sync with the sandbox and everything is committed.
