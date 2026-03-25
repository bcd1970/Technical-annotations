# Port Sandbox to Main App

Execute the full sandbox-to-app porting workflow. This is a strict, sequential process — do not skip steps.

## Step 1: Identify file pairs

Read all Kotlin source files in both modules and build the mapping:

| Sandbox | App |
|---------|-----|
| `sandbox/src/main/kotlin/com/bcd/technotes/sandbox/experiments/CanvasExperiment.kt` | `app/src/main/kotlin/com/bcd/technotes/ui/canvas/CanvasScreen.kt` |
| `sandbox/src/main/kotlin/com/bcd/technotes/sandbox/experiments/PhotoPickerScreen.kt` | `app/src/main/kotlin/com/bcd/technotes/ui/photo/PhotoPickerScreen.kt` |

Also check `sandbox/build.gradle.kts` vs `app/build.gradle.kts` for dependency differences.

If new sandbox files exist that have no app counterpart, flag them and create the app version.

## Step 2: Side-by-side diff

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

Report the differences to the user before proceeding.

## Step 3: Apply changes

For each difference found:
1. Update the app file to match the sandbox behavior
2. Adapt package names and imports appropriately (e.g., `com.bcd.technotes.sandbox.experiments` → `com.bcd.technotes.ui.photo`)
3. Do NOT change the sandbox files

## Step 4: Verify port

After all changes are applied:
1. Read BOTH versions of each file pair again
2. Confirm every behavior matches — check edge cases, clamping, animations, gesture handling, callbacks
3. If any mismatch remains, fix it before proceeding

## Step 5: Compliance & security audit

Review all app source files that were changed for:
- Google Play Store compliance (permissions, targetSdk, no hardcoded secrets)
- Security (no SQL injection, no PII logging, no cleartext traffic, input validation)
- For each finding: fix immediately if safe, or log as DEFERRED in AUDIT.md

## Step 6: Build both apps

```bash
./gradlew :sandbox:assembleDebug :app:assembleDebug
```

Both must build successfully. If build fails, fix and retry.

## Step 7: Install both on device

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
$ADB install -r sandbox/build/outputs/apk/debug/sandbox-debug.apk
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch both for the user to verify:
```bash
$ADB shell am start -n com.bcd.technotes.sandbox/.SandboxActivity
$ADB shell am start -n com.bcd.technotes/.MainActivity
```

## Step 8: Update DECISIONS.md

Add a new entry under "Port to Main App" with:
- Date
- What was ported
- Verification status (side-by-side diff confirmed)
- Any audit findings

## Step 9: Commit and push

Stage all changed files (not screenshots or temp files), create a commit with a descriptive message, and push to origin/main.

---

**IMPORTANT**: Do not mark any step as complete until it is actually done. If a step fails, fix the issue and retry that step. The user should be able to run `/port` and have confidence that the app is fully in sync with the sandbox.
