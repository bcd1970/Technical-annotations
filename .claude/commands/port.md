# Session Wrap-Up: Port & Finalize

Complete session wrap-up workflow. Run this when done working in the sandbox, or at the start of a new session to sync everything up. This is a strict, sequential process — do not skip steps.

## Architecture Mapping

The sandbox uses monolithic composables. The app uses MVVM + Repository + Hilt DI. When porting, sandbox code must be routed to the correct architectural layer:

| Sandbox Source | App Target(s) |
|---|---|
| `CanvasExperiment.kt` — data classes | `data/model/` (PhotoTransform, CollageResult, etc.) |
| `CanvasExperiment.kt` — decode/transform/stitch functions | `data/service/BitmapService.kt` |
| `CanvasExperiment.kt` — state variables + LaunchedEffects | `viewmodel/CanvasViewModel.kt` |
| `CanvasExperiment.kt` — UI composables (thumbbar, layout selector, command bars) | `ui/canvas/` (CanvasScreen.kt + sub-composable files) |
| `PhotoPickerScreen.kt` — data classes | `data/model/PickerModels.kt` |
| `PhotoPickerScreen.kt` — MediaStore query functions | `data/repository/MediaRepositoryImpl.kt` |
| `PhotoPickerScreen.kt` — selection/loading state | `viewmodel/PhotoPickerViewModel.kt` |
| `PhotoPickerScreen.kt` — UI composables (grids, preview, strip) | `ui/photo/` (individual files: AlbumGrid, PhotoGrid, PhotoPreview, SelectedPhotosStrip) |
| `EditableTouchImageView.kt` | `ui/util/EditableTouchImageView.kt` (1:1 copy) |
| `TouchImageViewExt.kt` | `ui/util/TouchImageViewExt.kt` (1:1 copy) |
| `sandbox/build.gradle.kts` — new dependencies | `app/build.gradle.kts` |

## Step 1: Assess session state

1. Run `git status` and `git diff` to see what changed
2. Read DECISIONS.md to understand what features were worked on
3. Summarize to the user: what files changed, what features are in play

If there are no changes (clean working tree, sandbox and app already in sync), tell the user and stop.

## Step 2: Identify what changed in sandbox

Read all changed sandbox Kotlin files. For each change, classify it using the architecture mapping above:

- **New data classes** → route to `data/model/`
- **New/changed image processing functions** → route to `data/service/BitmapService.kt` (add as new methods)
- **New/changed state variables or reactive logic** → route to `viewmodel/CanvasViewModel.kt` or `PhotoPickerViewModel.kt` (add StateFlows + action methods)
- **New/changed UI composables** → route to the appropriate `ui/` file (create new file if it's a new composable)
- **New/changed data queries** → route to `data/repository/MediaRepositoryImpl.kt` (add to interface + impl)
- **EditableTouchImageView changes** → 1:1 copy with package rename
- **New sandbox files with no app counterpart** → flag and create following the architecture pattern

Also check for new dependencies in `sandbox/build.gradle.kts` that need adding to `app/build.gradle.kts`.

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

For each difference found, apply it to the correct app layer:

1. **Data models**: Add/update classes in `data/model/`
2. **Services**: Add/update methods in `BitmapService.kt` (keep granular — individual ops should be callable independently)
3. **Repository**: Add/update methods in `MediaRepository.kt` (interface) + `MediaRepositoryImpl.kt`
4. **ViewModels**: Add StateFlows for new state, add action methods for new behaviors. Convert `LaunchedEffect` triggers to `viewModelScope.launch` with `combine`/`collect`.
5. **UI composables**: Update existing composable files or create new ones. Composables should read ViewModel state via `collectAsState()` and call ViewModel actions. Permission handling and navigation state stay in composables.
6. **Hilt**: If new services/repos were added, update `di/AppModule.kt`
7. **1:1 files** (EditableTouchImageView, TouchImageViewExt): Copy with package rename

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
