# Technical Annotations — Project Rules

## Plan-First Workflow (MANDATORY)

Before implementing ANY new feature or making significant code changes:

1. **Enter plan mode** — call `EnterPlanMode` yourself before writing any code
2. **Create a plan** — write it to the plan file, covering what will change and why
3. **Get user approval** — call `ExitPlanMode` and wait for approval
4. **Only then implement** — write code only after the plan is approved

This applies to any work that touches source files in `app/src/`, `sandbox/src/`, or `core/src/`. It does NOT apply to:
- Bug fixes (single-line or obvious fixes)
- Config/build file changes
- Updating DECISIONS.md or documentation

## Decision Logging (MANDATORY)

Every feature implementation must be logged in `DECISIONS.md` at the project root:
- Add a new section header with the feature name and date
- Log each decision/attempt with SUCCESS or FAILED outcome
- Include the reasoning (why it worked or didn't)
- Failed attempts stay in the log — they are part of the history

## Sandbox-First Workflow

Every feature follows this strict pipeline:
1. `:core` — data model + business logic (pure Kotlin, unit tested)
2. `:sandbox` — build and test the feature on the phone
3. `:app` — port only after user confirms it works in sandbox

The sandbox tests ONE feature at a time. Only add UI controls for the feature being tested.

## Port Verification (MANDATORY)

When porting features from sandbox to main app:
1. After writing the app version, **read BOTH files side-by-side** and verify every behavior is present
2. Check edge cases, clamping, animations, gesture handling — don't skip anything
3. **Build and install BOTH apps** and confirm they behave identically
4. Only then mark the task complete and update DECISIONS.md

Never mark a port as complete without this verification.

## Deploy After Changes

After modifying sandbox or app code:
- Build the APK: `./gradlew :sandbox:assembleDebug` or `:app:assembleDebug`
- Install on device: `adb install -r <path-to-apk>`
- Launch it: `adb shell am start -n <package>/<activity>`
