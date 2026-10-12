# Technical Annotations — Project Rules

## Module Architecture

Multi-module project. Strict dependency rules:

- :core:model (pure Kotlin) — leaf module, no dependencies
- :core:ui (Android + Compose) — monochrome design system, depends on :core:model only
- :core:drawing, :core:image, :core:project, :core:overlay, :core:undo → depend on :core:model only
- :feature:canvas, :feature:picker, :feature:editor, :feature:export → depend on :core:* modules (including :core:ui), NEVER on each other
- :app → depends on everything, contains only MainActivity, navigation, DI (NO theme — theme is in :core:ui)
- :sandbox → depends on :core:* only, has its own throwaway UI

## Design System

Monochrome. Zero chroma in UI chrome. See ui-monochrome-design-system.html.
- All colors are gray stops from #090909 to #f8f8f8
- Active = brightness (bright on dark, dark on light). No accent hue.
- Custom theme: TechNotesTheme in :core:ui — use it, never hardcode colors
- Typography: Plus Jakarta Sans (UI text) + IBM Plex Mono (labels, readouts)
- Spacing: xs=4dp, sm=8dp, md=12dp, lg=16dp, xl=24dp
- Radii: sm=6dp, md=8dp, lg=14dp, xl=16dp, pill=999dp

## Plan-First Workflow (MANDATORY)

Before implementing ANY new feature or making significant code changes:

1. **Enter plan mode** — call `EnterPlanMode` yourself before writing any code
2. **Create a plan** — write it to the plan file, covering what will change and why
3. **Get user approval** — call `ExitPlanMode` and wait for approval
4. **Only then implement** — write code only after the plan is approved

This applies to any work that touches source files in `app/src/`, `feature/*/src/`, `core/*/src/`, or `sandbox/src/`. It does NOT apply to:
- Bug fixes (single-line or obvious fixes)
- Config/build file changes
- Updating DECISIONS.md or documentation

## Decision Logging (MANDATORY)

Every feature implementation must be logged in `DECISIONS.md` at the project root:
- Add a new section header with the feature name and date
- Log each decision/attempt with SUCCESS or FAILED outcome
- Include the reasoning (why it worked or didn't)
- Failed attempts stay in the log — they are part of the history

## Sandbox-First Pipeline

1. :core:* — Pure Kotlin data models + logic (unit tested, no emulator)
2. :sandbox — Build feature UI, deploy to phone, user confirms
3. /port — Classify changes, rewrite cleanly into :feature:* and :core:*, verify parity
4. Build both :app and :sandbox APKs → Install both → Confirm identical behavior
5. Update DECISIONS.md → Audit compliance → Commit & push

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
