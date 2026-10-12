# Technical Annotations — Development Environment & Workflow

## Platform

- **OS:** Fedora Linux
- **IDE:** Android Studio + Claude Code CLI
- **Language:** Kotlin 2.1.10, Java 21
- **UI:** Jetpack Compose (BOM 2025.03.00), Material 3
- **Build:** Gradle with version catalog, AGP 8.8.2
- **Target:** Android 10+ (minSdk 29, targetSdk 36)
- **Device:** Samsung Galaxy S25 Ultra (wireless ADB)
- **Repository:** github.com/bcd1970/Technical-annotations

---

## Module Structure

```
TechnicalAnnotations/
├── :app          Production Android app (Compose, Hilt, MVVM)
├── :sandbox      Throwaway app for prototyping one feature at a time
└── :core         Pure Kotlin/JVM library (zero Android dependencies)
```

`:app` and `:sandbox` both depend on `:core`. They share no Android code — sandbox is ephemeral by design.

---

## Architecture Pattern

MVVM + Repository + Hilt DI (refactored from MVI on 2026-03-29):

| Layer | Location | Role |
|-------|----------|------|
| Data models | `data/model/` (app) or `core/model/` (pure Kotlin) | State and domain objects |
| Repositories | `data/repository/` (interface + impl) | Data access (MediaStore), Hilt-injected |
| Services | `data/service/` (e.g., BitmapService) | Business logic, `@Inject constructor` |
| ViewModels | `viewmodel/` | `@HiltViewModel`, StateFlow-based state |
| UI | `ui/<feature>/` | Compose screens, single-responsibility files |
| DI | `di/AppModule.kt` | Hilt bindings |

No domain/use-case layer — ViewModels call repos and services directly.

---

## Claude Code Context Management

### Global Configuration (`~/.claude/`)

**CLAUDE.md** — Developer profile and cross-project preferences:
- Solo developer, Fedora Linux
- Android: Kotlin, Jetpack Compose, data/sealed classes
- Web: Next.js, TypeScript, Tailwind (separate projects)
- Minimal comments, no `.env` commits, test after changes

**settings.json** — Tool permissions and hooks:
- Allows: npm, npx, git, node, cat, ls, mkdir, Read, Edit, Write
- Denies: `rm -rf /`, piping curl/wget to bash
- Hook: blocks `compileDebugKotlin` without `installDebug`

### Project Configuration (`.claude/`)

**CLAUDE.md** — Project-specific rules enforced every session:
1. **Plan-First Workflow** — `EnterPlanMode` before any source file edits; get approval before coding
2. **Decision Logging** — every feature logged in `DECISIONS.md` with SUCCESS/FAILED outcomes
3. **Sandbox-First Pipeline** — `:core` -> `:sandbox` -> confirm on device -> `:app`
4. **Port Verification** — side-by-side comparison, build both APKs, verify identical behavior
5. **Deploy After Changes** — build, install via ADB, launch on device

**settings.json** — Three PreToolUse hooks that trigger on source file edits:
1. **Plan-First Hook** — reminds that an approved plan is required before editing `app/src/`, `sandbox/src/`, or `core/src/`
2. **Port Parity Hook** — when editing `app/src/`, reminds to compare sandbox and app side-by-side and verify all behaviors
3. **Compliance & Security Audit Hook** — when editing `app/src/`, prompts review for Play Store compliance, security (SQL injection, PII logging, cleartext traffic, etc.), and logs findings in `AUDIT.md`

### Memory System (`~/.claude/projects/.../memory/`)

Persistent cross-session memory with 13 files indexed in `MEMORY.md`:

| Memory | Type | Key Content |
|--------|------|-------------|
| `project_overview.md` | project | App purpose, domains, module structure, current phase |
| `feedback_architecture.md` | feedback | MVVM + Repository + Hilt DI pattern is mandatory |
| `feedback_sandbox_first.md` | feedback | Strict sandbox-first pipeline, never skip |
| `feedback_sandbox_minimal.md` | feedback | Sandbox tests ONE feature, minimal UI only |
| `feedback_decision_log.md` | feedback | DECISIONS.md logging is mandatory |
| `feedback_plan_concise.md` | feedback | Plans must be short, current task only |
| `feedback_plan_scope.md` | feedback | Approach changes require new plan approval |
| `feedback_port_verify.md` | feedback | Side-by-side verification mandatory when porting |
| `feedback_port_new_features.md` | feedback | /port creates new features in app, doesn't skip them |
| `feedback_no_compose_zoom.md` | feedback | Use native View zoom (OverScroller), not Compose gestures |
| `feedback_system_bars.md` | feedback | All screens must respect system bar insets |
| `feedback_wait_for_instruction.md` | feedback | Answer questions only; wait for explicit go-ahead to code |

These memories shape behavior across conversations — corrections don't need repeating, validated approaches are preserved.

---

## Skills & Commands

### /port (Custom Command)

Defined in `.claude/commands/port.md`. 9-step workflow for porting sandbox features to the main app:

1. Assess session state (git status, diff, DECISIONS.md)
2. Identify and classify sandbox changes using architecture mapping:
   - Data classes -> `data/model/`
   - Image functions -> `data/service/BitmapService.kt`
   - State/reactive logic -> ViewModel files
   - UI composables -> `ui/<feature>/`
   - EditableTouchImageView -> 1:1 copy with package rename
   - New dependencies -> update both gradle files
3. Side-by-side diff of sandbox and app targets
4. Apply changes in correct app layers
5. Verify port (read both, confirm every behavior matches)
6. Build both APKs
7. Install and launch both on device
8. Update DECISIONS.md
9. Commit and push

### /cn (Custom Skill)

Auto-discover and connect Samsung Galaxy S25 Ultra via wireless ADB.

### /simplify (Built-in Skill)

Review changed code for reuse, quality, and efficiency, then fix issues found.

### /commit, /review-pr (Built-in Skills)

Standard git commit and PR review workflows.

---

## MCP Servers

| Server | Purpose |
|--------|---------|
| GitHub MCP | Repos, PRs, issues, code search |
| Supabase MCP | Database operations (for web projects) |
| Context7 MCP | Up-to-date library/framework documentation |
| Playwright MCP | Browser testing and automation |

---

## Project Tracking Files

| File | Purpose |
|------|---------|
| `DECISIONS.md` | Chronological decision log with SUCCESS/FAILED outcomes per feature |
| `AUDIT.md` | Play Store compliance and security findings (template ready, awaiting entries) |
| `APP-ARCHITECTURE.md` | Full code architecture reference (modules, packages, models, threading) |
| `APP-FEATURES.md` | Feature catalog and interaction flow documentation |

---

## Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Hilt | 2.56.2 | Dependency injection |
| TouchImageView | 3.7.1 | Zoom/pan/fling base class (native View) |
| Coil 3 | 3.1.0 | Async image loading with thumbnails |
| GPUImage | 2.1.0 | Image filter fallback for Android < 13 |
| Telephoto | 0.14.0 | Zoomable image support in Compose |
| kotlinx.serialization | 1.7.3 | JSON serialization for core models |
| kotlinx.collections.immutable | 0.3.8 | Stable collections for Compose |
| Timber | 5.0.1 | Debug logging |
| LeakCanary | 2.14 | Memory leak detection (debug only) |

---

## Development Cycle

```
Idea / Feature Request
  |
  v
Enter Plan Mode -> Write plan -> Get approval
  |
  v
:core — Pure Kotlin data models + logic (unit tested)
  |
  v
:sandbox — Build feature, deploy to phone, user confirms
  |
  v
/port — Classify changes, apply to :app layers, verify parity
  |
  v
Build both APKs -> Install both -> Confirm identical behavior
  |
  v
Update DECISIONS.md -> Audit compliance -> Commit & push
```

Every step has enforcement: hooks remind about plans, memory preserves learned preferences, DECISIONS.md captures the reasoning trail, and AUDIT.md tracks compliance.
