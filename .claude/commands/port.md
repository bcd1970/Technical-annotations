# Session Wrap-Up: Port & Finalize

Complete session wrap-up workflow. Run this when done working in the sandbox, or at the start of a new session to sync everything up. This is a strict, sequential process — do not skip steps.

## Step 1: Assess session state

1. Run `git status` and `git diff` to see what changed
2. Read DECISIONS.md to understand what features were worked on
3. Summarize to the user: what files changed, what features are in play

If there are no changes (clean working tree, sandbox and app already in sync), tell the user and stop.

## Step 2: Identify file pairs

Read all Kotlin source files in both modules and build the mapping:

| Sandbox | App |
|---------|-----|
| `sandbox/src/.../experiments/CanvasExperiment.kt` | `app/src/.../ui/canvas/CanvasScreen.kt` |
| `sandbox/src/.../experiments/PhotoPickerScreen.kt` | `app/src/.../ui/photo/PhotoPickerScreen.kt` |
| `sandbox/src/.../util/EditableTouchImageView.kt` | `app/src/.../ui/util/EditableTouchImageView.kt` |
| `sandbox/src/.../util/TouchImageViewExt.kt` | `app/src/.../ui/util/TouchImageViewExt.kt` |
| `sandbox/build.gradle.kts` | `app/build.gradle.kts` |

Also check for new sandbox files that have no app counterpart — flag them and create the app version.

## Step 3: Side-by-side diff

For EACH file pair:
1. Read both files in full
2. Compare all functional code (ignore package names, import paths with package differences)
3. List every behavioral difference found — be exhaustive:
   - Missing functions or composables
   - Different parameter signatures
   - Different logic in function bodies
   - Missing state variables
   - Different UI behavior (gestures, callbacks, navigation)
   - Different image loading approaches
   - Missing or different animations
   - Dependency differences in build.gradle.kts

If both files are already identical (modulo package names), say so and skip to Step 6.

Report the differences to the user before proceeding.

## Step 4: Apply changes

For each difference found:
1. Update the app file to match the sandbox behavior
2. Adapt package names and imports appropriately (e.g., `com.bcd.technotes.sandbox.experiments` → `com.bcd.technotes.ui.photo`)
3. Do NOT change the sandbox files

## Step 5: Verify port

After all changes are applied:
1. Read BOTH versions of each changed file pair again
2. Confirm every behavior matches — check edge cases, clamping, animations, gesture handling, callbacks
3. If any mismatch remains, fix it before proceeding

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
