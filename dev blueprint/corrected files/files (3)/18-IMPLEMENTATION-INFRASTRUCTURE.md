# 18 — Implementation Infrastructure
## Dependencies, Tooling, Testing, Claude Code Workflow, and Quality Enforcement

> **This document defines every tool, library, testing strategy, and process rule needed to build TechNotes with Claude Code without the project breaking as complexity grows.** It is the engineering-of-the-engineering — not what to build, but how to build it safely.

---

## Part I: Dependency Stack

### Core Libraries (pinned versions for targetSdk 36, minSdk 29, JVM 21)

| Library | Version | Coordinate | Notes |
|---------|---------|-----------|-------|
| Kotlin | 2.3.20 | `org.jetbrains.kotlin:kotlin-stdlib` | Compose compiler merged in; no separate compiler extension needed |
| Compose BOM | 2026.03.00 | `androidx.compose:compose-bom` | Maps to Compose UI ~1.10.x, Material3 ~1.4.x |
| Material3 | ~1.4.0 | `androidx.compose.material3:material3` | Version managed by BOM |
| CameraX | 1.5.1–1.6.0 | `androidx.camera:camera-*` | `camera-compose` artifact enables Compose-native camera |
| Media3 | 1.9.2–1.10.0 | `androidx.media3:media3-*` | ExoPlayer + Transformer + FrameExtractor |
| Room | 2.8.4 | `androidx.room:room-*` | Stay on 2.x; Room 3.0 alpha is not production-ready |
| Hilt | 2.57.1–2.59.2 | `com.google.dagger:hilt-android` | KSP required; KAPT is in maintenance mode |
| Navigation Compose | 2.9.7 | `androidx.navigation:navigation-compose` | Navigation3 1.0.x also available |
| kotlinx.serialization | 1.8.x–1.10.0 | `org.jetbrains.kotlinx:kotlinx-serialization-json` | Type-safe nav routes + data persistence |
| Coil | 3.4.0 | `io.coil-kt.coil3:coil-compose` | Requires explicit `coil-network-okhttp` artifact |
| AGP | 9.1.0 | `com.android.application` | Gradle 9.1+ required; bundles KGP 2.2.10 |
| KSP | 2.3.20-x.x.x | `com.google.devtools.ksp` | KSP version prefix must match Kotlin version (e.g., `2.3.20-1.0.31`) |

*Exact version numbers shift — verify against Maven Central before pinning.*

---

### Build vs. Buy Decision

| Component | Decision | Rationale |
|-----------|----------|-----------|
| Canvas annotation layer | **Build custom** | No Compose-native annotation library exists; PhotoEditor is View-based |
| Collage grid engine | **Build custom** | No maintained Android collage library exists |
| GIF encoder | **Build custom** | Fork AnimatedGifEncoder (~500 lines) or use animated WebP |
| Webhook queue | **Build custom** | Simple OkHttp + WorkManager pattern |
| Image zoom/pan | **Use library** | Zoomable (`net.engawapg.lib:zoomable`) — drop-in Modifier |
| Video processing | **Use library** | Media3 Transformer — overlay burn-in, composition, hardware-accelerated |
| PDF generation | **Use built-in** | `PrintedPdfDocument` uses same Canvas API as annotation rendering |
| SHA-256 hashing | **Use built-in** | `java.security.MessageDigest.getInstance("SHA-256")` |
| C2PA signing | **Use library** | `c2pa-android` (contentauth) — official SDK, Apache 2.0 + MIT |

---

### Libraries Deprecated Since Early 2025 — Do Not Use

| Deprecated | Replacement |
|-----------|-------------|
| Accompanist Pager, FlowLayout, SwipeRefresh, Insets, SystemUIController, Navigation Animation | Built-in Compose/AndroidX equivalents |
| ExoPlayer (`com.google.android.exoplayer2`) | `androidx.media3` |
| KAPT | KSP for all annotation processors |
| iText for PDF | `PrintedPdfDocument` (built-in) or PdfBox-Android (Apache 2.0) |
| FFmpeg for video | Media3 Transformer |
| Room 3.0 alpha | Stay on Room 2.8.4 |

---

### Known Compatibility Notes

- **Media3 Transformer + AGSL RuntimeShader**: Both use OpenGL internally but in separate contexts — no conflict. Implement custom video effects as `GlShaderProgram`, not AGSL `RenderEffect`.
- **Coil 3.x + OkHttp**: Share OkHttp dependency; align versions.
- **Hilt + AGP 9.x**: Requires Hilt 2.57.1+ for compatibility.
- **c2pa-android**: Adds JNA (~3MB) with no known conflicts.
- **Kotlin 2.3.x + Compose**: Compose compiler is merged into Kotlin — remove old `composeOptions { kotlinCompilerExtensionVersion }` block; apply `org.jetbrains.kotlin.plugin.compose` instead.

---

### targetSdk 36 Breaking Changes (Play Store deadline: August 2026)

1. **Edge-to-edge is mandatory** — `windowOptOutEdgeToEdgeEnforcement` is ignored; use `enableEdgeToEdge()` and handle WindowInsets in all screens.
2. **`onBackPressed()` is dead** — use Compose's `BackHandler` or `OnBackInvokedCallback`.
3. **Orientation/resizability is ignored on screens ≥600dp** — must support adaptive layouts on tablets/foldables.
4. **16KB page size** — audit all `.so` files including transitive dependencies; rebuild with NDK 27+.

---

## Part II: Quality Tooling Stack

### The Essential Eight (P0 — non-negotiable)

#### 1. detekt + compose-rules (Kotlin static analysis)

**Version:** detekt 1.23.8 stable / 2.0.0-alpha.2 for Kotlin 2.3.x; compose-rules 0.4.22 (for detekt 1.x) or 0.5.6 (for detekt 2.x)

**What it catches:** Unsafe casts, swallowed exceptions, coroutine anti-patterns, performance pitfalls, high cyclomatic complexity. Compose-specific: `ModifierMissing`, `MutableParams`, `MultipleEmitters`, `ViewModelForwarding`, `RememberMissing`.

**Setup:** Single `detekt.yml` at project root, referenced by all modules via convention plugin. Add compose-rules as `detektPlugins("io.nlopez.compose.rules:detekt:0.4.22")`. Configure `ignoreAnnotated: ['Preview']` under `UnusedPrivateMember`.

**Build impact:** ~10–30 seconds for all 12 modules.

---

#### 2. Android Lint (built-in with AGP 9.x)

**What it catches:** 400+ checks — API level misuse, missing permissions, wrong thread, accessibility, performance, security.

**Critical config for 12 modules:**

```kotlin
// app/build.gradle.kts
android {
    lint {
        checkDependencies = true  // single invocation across all modules
        baseline = file("lint-baseline.xml")
        abortOnError = true
        lintConfig = file("${rootDir}/lint.xml")
    }
}
```

**Build impact:** 2–8 minutes for full multi-module analysis.

---

#### 3. Slack compose-lints 1.4.2

**What it catches:** 20+ Compose architectural checks — modifier misuse (5 rules), unstable parameters, ViewModel injection anti-patterns, Material migration, preview conventions.

**Setup:** Single dependency in app module:

```kotlin
dependencies {
    lintChecks("com.slack.lint.compose:compose-lint-checks:1.4.2")
}
```

**Complementary to** built-in Compose runtime checks (which cover state and coroutine bugs).

---

#### 4. Google android-security-lints 1.0.4

**What it catches:** 24 security checks — weak cryptography, cleartext traffic, tapjacking, Strandhogg attacks, exported component issues, PII in external storage.

**Setup:** `lintChecks("com.android.security.lint:lint:1.0.4")` in app module.

---

#### 5. ktlint 1.8.0 (formatting)

**What it does:** Consistent formatting with auto-fix. Eliminates formatting noise in AI-generated code.

**Gradle plugin:** kotlinter 5.4.2 (`org.jmailen.kotlinter`) or JLLeitschuh ktlint-gradle 14.2.0.

**`.editorconfig` at project root:**

```ini
[*.{kt,kts}]
indent_size = 4
max_line_length = 100
ktlint_code_style = android_studio
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
```

**Build impact:** ~5–15 seconds.

---

#### 6. Module Graph Assert 2.9.0

**What it catches:** Architecture violations — feature-to-feature dependencies, domain depending on features, circular dependencies.

**Setup:**

```kotlin
moduleGraphAssert {
    maxHeight = 4
    allowed = listOf(
        ":app -> :feature.*",
        ":feature.* -> :core.*",
    )
    restricted = listOf(
        ":feature.* -X-> :feature.*",
        ":core:model -X-> :feature.*",
        ":core:data -X-> :feature.*"
    )
}
```

**Build impact:** Negligible.

---

#### 7. Gradle Doctor 0.12.0

**What it catches:** Dagger/Hilt annotation processor timing regressions, GC overhead, build cache misses, empty source directories, JAVA_HOME issues.

**Setup:** Apply plugin. Zero configuration needed.

---

#### 8. Kover 0.9.8 (code coverage)

**Why not JaCoCo:** JaCoCo 0.8.13 still has issues with Kotlin inline functions, coroutines, and reified generics. Kover uses IntelliJ's engine which understands Kotlin bytecode natively.

**Multi-module aggregation:**

```kotlin
dependencies {
    kover(project(":core"))
    kover(project(":data"))
    kover(project(":feature-canvas"))
    // ... all modules
}
```

**Set minimum threshold:** `verify { rule { bound { minValue = 60 } } }`

---

### Essential Architecture Testing (P1)

#### 9. Konsist 0.17.3

**What it catches:** Naming violations, package structure violations, dependency direction violations — written as unit tests.

**Key tests for TechNotes:**

```kotlin
@Test
fun `core model module has no Android imports`() {
    Konsist.scopeFromModule("core:model").files
        .assertFalse { it.hasImport { import -> import.name.startsWith("android") } }
}

@Test
fun `all ViewModels extend ViewModel`() {
    Konsist.scopeFromProject().classes()
        .withNameEndingWith("ViewModel")
        .assertTrue { it.hasParentClassOf(ViewModel::class) }
}

@Test
fun `repositories reside in repository package`() {
    Konsist.scopeFromProject().classes()
        .withNameEndingWith("Repository")
        .assertTrue { it.resideInPackage("..repository..") }
}
```

**Setup:** `testImplementation("com.lemonappdev:konsist:0.17.3")` in a dedicated `:konsist-test` module. Runs as standard unit tests.

---

### Dependency Health (P1)

#### 10. dependency-analysis-gradle-plugin 3.5.1

**What it catches:** Unused dependencies, misused `api` vs `implementation` (causes unnecessary recompilation cascades), unused annotation processors, undeclared transitive dependencies.

**Setup:**

```kotlin
plugins {
    id("com.autonomousapps.dependency-analysis") version "3.5.1"
}
dependencyAnalysis {
    structure {
        bundle("hilt") { includeGroup("com.google.dagger") }
    }
}
```

**Run:** `./gradlew buildHealth`

---

### Nice-to-Have (P2)

| Tool | Version | What | When to run |
|------|---------|------|-------------|
| Dependency Guard (Dropbox) | 0.5.0 | Catches unexpected transitive dependency changes | CI |
| Renovate | Service | Automated dependency update PRs with `libs.versions.toml` support | GitHub App |
| Roborazzi | 1.59.0 | Screenshot regression testing for Canvas composables | CI |
| Gradle Build Scans | Built-in | Build performance diagnostics | As needed |
| OWASP Dependency-Check | 12.2.0 | Known CVEs in dependencies | Monthly/pre-release |

### Skip These

| Tool | Why skip |
|------|----------|
| diktat | Redundant with detekt + ktlint |
| JaCoCo | Kover is superior for Kotlin |
| bye-bye-dead-code | Use R8 `-printusage` directly |
| ArchUnit | Konsist is superior for Kotlin |
| MobSF / QARK | Not Gradle-integrated |

---

## Part III: Dead Code Detection Strategy

No single tool catches everything. Use a three-layer approach:

**Layer 1 — Per-commit (fast):** detekt `UnusedPrivateMember` catches unused private functions, properties, parameters. Configure `ignoreAnnotated: ['Preview']`.

**Layer 2 — CI (thorough):** Android Lint `UnusedResources` catches unused drawables, strings, colors, dimensions. Enable `checkGeneratedSources = true`.

**Layer 3 — Monthly (comprehensive):** R8 `-printusage build/outputs/r8/usage.txt` in ProGuard rules. Requires release build. This is the only true whole-program analysis — the only way to find unused public composables across modules.

---

## Part IV: Testing Strategy

### Testing Pyramid for a Solo Developer

| Layer | Tool | What it tests | Frequency |
|-------|------|---------------|-----------|
| Unit tests | JUnit + Robolectric Native Graphics | Coordinate math, undo/redo, annotation timing, Canvas pixels | Every commit |
| Architecture tests | Konsist | Module boundaries, naming, dependency direction | Every commit |
| Screenshot tests | Roborazzi | Full UI, Canvas composables, collage layouts | Every PR / session |
| Integration tests | Instrumented (device) | Video export, real PDF output, camera pipeline | Weekly / pre-release |

### Canvas Drawing: Robolectric Native Graphics

Since Robolectric 4.10+, `@GraphicsMode(GraphicsMode.Mode.NATIVE)` enables real Skia-backed rendering of Canvas, Paint, Path, Matrix, and Bitmap on JVM — no emulator required:

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CanvasDrawingTest {
    @Test
    fun `red circle drawn at center`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawCircle(100f, 100f, 50f,
            Paint().apply { color = Color.RED; style = Paint.Style.FILL })
        assertEquals(Color.RED, bitmap.getPixel(100, 100))
    }
}
```

### Screenshot Testing: Roborazzi

Best fit for TechNotes because it uses real Skia rendering, supports interaction testing (touch simulation), integrates with Hilt, and has first-class GitHub Actions support.

- **Record baselines:** `./gradlew recordRoborazziDebug`
- **Verify in CI:** `./gradlew verifyRoborazziDebug`

### Coordinate Transforms: Pure-Kotlin Testable Classes

Extract a `CoordinateMapper` class that encapsulates screen → image → collage space transforms. Android's `Matrix` class works in Robolectric Native Graphics mode. Key tests: round-trip transforms preserve coordinates, `setRectToRect` maps correctly, boundary conditions at edges.

### Undo/Redo: Command Pattern with Pure JUnit

`UndoRedoManager` is pure Kotlin with no Android dependencies. Test with standard JUnit: state consistency after arbitrary undo/redo sequences, redo stack invalidation on new actions, compound commands, stress testing with 100+ operations.

### Video Annotation Timing: Separate Data from Rendering

`VideoAnnotationTimeline` class: takes timed annotations, returns visible annotations for any timestamp. Pure Kotlin — test with JUnit. Integration testing of burned-in export uses Media3 FrameExtractor.

### Export Pipeline

- **Image export:** Render annotations onto bitmap, verify pixels with Robolectric Native Graphics.
- **PDF export:** Write to `ByteArrayOutputStream`, parse with PdfBox (JVM test dependency), verify page count and dimensions.
- **Video export:** Instrumented tests only (weekly).

### CI/CD: GitHub Actions, No Emulator

GitHub Actions free tier (2,000 min/month). Run lint, unit tests, Roborazzi on `ubuntu-latest` — no emulator. Expected CI time: 5–8 minutes per run (~250 builds/month within free tier).

---

## Part V: Claude Code Workflow

### The Cardinal Rule: One Feature, One Session

Claude Code performs dramatically better with small, focused sessions. Apply the **5-file rule** — break large refactors into phases touching a maximum of 5 files per session. Between tasks, use `/clear` + fresh prompt rather than letting auto-compaction happen.

---

### STATUS.md: The Session Handoff Document

Maintain at project root, updated at the end of every session:

```markdown
# Project Status — TechNotes
Last updated: [date] by Session [N]

## Current state
- What's working, what module is in progress, current branch

## Completed features (don't redo these)
- [x] :feature-canvas — drawing engine functional
- [x] :feature-camera — capture + picker with permissions

## In progress
- [ ] :feature-collage — 60% complete, grid done, freeform pending

## Next steps (priority order)
1. Complete freeform collage layout
2. Wire collage preview to :feature-export

## Decisions made (with rationale)
- Coil over Glide: better Compose integration
- Canvas export uses software rendering for fidelity

## Known issues
- Room schema migration needed if UserProject changes
```

Create a custom slash command (`.claude/commands/save-status.md`) that prompts Claude to update this file at session end.

---

### CLAUDE.md: Keep Under 150 Lines

Structure around five sections: build commands, architecture rules, code style, forbidden patterns, key file references. Use `@path/to/file.md` imports to keep it lean.

Use **path-scoped rules** in `.claude/rules/` instead of bloating root file. A `canvas-module.md` rule with `paths: ["feature-canvas/**"]` loads only when Claude edits Canvas module files.

**Critical rules for TechNotes CLAUDE.md:**

```markdown
# Build & Verify
- After ANY code change: `./gradlew assembleDebug`
- Before declaring done: `./gradlew testDebugUnitTest lintDebug`
- Format before commit: `./gradlew ktlintFormat`

# Architecture
- All cross-module dependencies go through interfaces in :core
- ViewModels ONLY in :feature-* modules
- No Android imports in :core:model
- No Room imports in :feature-* modules

# Forbidden Patterns
- NEVER modify test assertions — fix the code, not the tests
- NEVER use GlobalScope
- NEVER hardcode colors — use design system tokens
- NEVER create parallel implementations — delete old code before writing new
- NEVER add dependencies without adding to libs.versions.toml

# Compose Patterns
- All public composables must accept Modifier parameter
- Use collectAsStateWithLifecycle() not collectAsState()
- Wrap expensive objects in remember {}
- Use stable/immutable types for compose parameters

# Session Discipline
- Read STATUS.md at session start
- Update STATUS.md at session end
- Commit at clean checkpoints with descriptive messages
- Run detekt on modified files before committing
```

---

### Claude Code Hooks: Automated Enforcement

#### PostToolUse: ktlint format on every file write

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "FILE=$(cat | jq -r '.tool_input.file_path // empty') && [ -n \"$FILE\" ] && [[ \"$FILE\" == *.kt ]] && ktlint --format \"$FILE\" 2>/dev/null || true"
          }
        ]
      }
    ]
  }
}
```

#### PreToolUse: Protect critical files

Block modifications to `build-logic/`, `libs.versions.toml`, CI workflows, and contract interfaces without explicit confirmation.

---

### Preventing Common Claude Code Failures

| Failure Mode | Mitigation |
|-------------|------------|
| Declares "Done!" without compiling | CLAUDE.md rule: always run `assembleDebug` before declaring done |
| Edits wrong module's files | Path-scoped `.claude/rules/` enforce module boundaries |
| Modifies tests to match broken code | CLAUDE.md: "NEVER modify test assertions without approval" |
| Creates parallel implementations | CLAUDE.md: "Delete old implementation before writing new one" |
| Band-aid patches symptoms | CLAUDE.md: "Fix root causes. Investigate before patching." |
| Infinite retry loops | Press Esc after 2 failed attempts; redirect strategy |
| Context rot after compaction | Use `/clear` + fresh prompt; maintain STATUS.md |
| Stale dependency versions | Pin versions in libs.versions.toml; CLAUDE.md references it |

---

### Git Workflow for Claude Code Sessions

**Git worktrees** for experiments: `claude --worktree experiment-name` creates isolated branch and working directory. Changes don't affect main. Auto-removes on exit with no changes.

**Writer/Reviewer pattern:** Session A implements a feature; Session B reviews it from fresh context, catching dead code, hallucinated APIs, and style violations.

**Feature flags for experiments:** Simple enum-based `FeatureFlag` in `:core:feature-flags`. Kotlin's exhaustive `when` catches forgotten cases. Use `@RequiresOptIn` for experimental APIs.

---

### Managing Experiments Without Dead Code

1. Use Git worktrees for experiments — not branches.
2. All experimental code behind feature flags.
3. When resolving a flag: remove the enum entry AND delete the losing code path.
4. End each session with: "Search for unused functions, classes, or imports in modified files. Remove them."
5. Run `./gradlew detekt lintDebug` before session ends.
6. Weekly cleanup session: Claude scans for dead code, duplicate implementations, abandoned experiments.
7. Monthly: Run R8 `-printusage` to find truly unreachable code.

---

## Part VI: Dependency Management Across 13 Modules

### Version Catalogs: Single Source of Truth

`gradle/libs.versions.toml` organized into four sections: `[versions]`, `[libraries]`, `[bundles]`, `[plugins]`. Group related dependencies into bundles (`compose-core`, `camerax`, `media3`). Use Compose BOM via `platform()` for automatic Compose version alignment.

### Convention Plugins: Eliminate Boilerplate

`build-logic/` included build with shared Android config:
- `AndroidLibraryConventionPlugin` — shared compileSdk, minSdk, JVM target
- `ComposeConventionPlugin` — Compose compiler plugin, BOM platform
- `HiltConventionPlugin` — KSP + Hilt dependencies

This is the pattern used by Google's "Now in Android" reference app.

### AGP 9.x: Kotlin Is Built In

AGP 9.0+ bundles Kotlin support directly — `org.jetbrains.kotlin.android` plugin is no longer required. Override to Kotlin 2.3.20 with `android.builtInKotlin=false` if needed.

### Configuration Cache

Fully supported in AGP 9.x. Enable immediately — Google reported 3.75× reduction in configuration cache size for AndroidX builds.

---

## Part VII: Git Hooks

### Pre-commit: Under 10 Seconds

Run **ktlint format on staged `.kt` files only** using the CLI binary (not Gradle):

```bash
#!/bin/bash
STAGED=$(git diff --cached --name-only --diff-filter=d | grep '\.kt$')
[ -z "$STAGED" ] && exit 0
echo "$STAGED" | xargs ktlint --format
echo "$STAGED" | xargs git add
```

Completes in 3–5 seconds. Everything else runs CI-only.

---

## Part VIII: CI Pipeline

All comprehensive checks run here:

```yaml
# .github/workflows/check.yml
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'zulu' }
      - name: Lint
        run: ./gradlew :app:lintRelease
      - name: Detekt
        run: ./gradlew detekt
      - name: Unit Tests + Konsist
        run: ./gradlew testDebugUnitTest
      - name: Screenshot Tests
        run: ./gradlew verifyRoborazziDebug
      - name: Coverage
        run: ./gradlew koverHtmlReportDebug
      - name: Dependency Health
        run: ./gradlew buildHealth
      - name: Module Graph
        run: ./gradlew assertModuleGraph
      - name: Dependency Guard
        run: ./gradlew dependencyGuard
```

---

## Part IX: Three Enforcement Layers Summary

| Layer | Speed | Tools | When |
|-------|-------|-------|------|
| **Claude Code hooks** | Instant (<1s) | ktlint format, detekt single-file | Every file write |
| **CLAUDE.md rules** | Proactive | Architecture rules, naming, patterns, build commands | Every session |
| **CI pipeline** | 5–8 min | Full lint, detekt, tests, Konsist, coverage, dependency analysis, module graph | Every push |

---

*April 2026 · TechNotes Implementation Infrastructure · v1.1*
*Changes from v1.0: :domain module references → :core:model (no :domain module exists). Module graph assert patterns fixed (:data.* → :core:data). Module count corrected to 12. STATUS.md example :feature-gallery → :feature-camera. KSP version format clarified. EX-04 iText reference remains as iText was only deprecated for PDF creation — Doc 30 updated to remove ambiguity.*
