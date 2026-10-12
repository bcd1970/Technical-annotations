# Claude Code — Development Environment Setup

> **PURPOSE:** Execute this document top-to-bottom in a single Claude Code session.
> It restructures the Technical Annotation App project from the current flat 3-module layout
> to the new multi-module architecture defined in the Implementation Blueprint.
>
> **OUTCOME:** A fully compilable, empty-shell project ready for SESSION 01+ implementation.
>
> **IMPORTANT:** Read this entire document BEFORE making any changes. Execute in order.

---

## 0. PRE-FLIGHT CHECKS

Before starting, verify the current state:

```bash
# 1. Confirm we're in the right project
cd ~/Projects/TechnicalAnnotations  # adjust to actual project root
git status

# 2. Create a fresh branch for the restructure
git checkout -b restructure/multi-module-setup

# 3. Verify current build works (baseline)
./gradlew :app:assembleDebug
```

If the current build fails, fix it first. Do NOT proceed with a broken baseline.

---

## 1. TARGET MODULE STRUCTURE

Transform the project FROM this:

```
TechnicalAnnotations/
├── :app          (production app — everything lives here)
├── :core         (pure Kotlin — flat single module)
└── :sandbox      (experimentation app)
```

TO this:

```
TechnicalAnnotations/
├── :app                  — MainActivity, navigation host, Hilt setup
├── :feature:canvas       — CanvasScreen, CanvasViewModel, drawing engine, mode system
├── :feature:picker       — PhotoPickerScreen, PhotoPickerViewModel
├── :feature:editor       — WholeImageEditScreen, adjustment UI
├── :feature:export       — Export sheet, PDF/JPG generation
├── :core:model           — All shared data classes (Project, DrawingElement, PhotoTransform, etc.)
├── :core:ui              — Monochrome design system: theme, tokens, fonts, shared composables
├── :core:drawing         — Primitives, ArrowheadRenderer, SnapEngine, CatmullRomSmoother
├── :core:image           — BitmapService, GuidedFilter, transform pipeline
├── :core:project         — ProjectRepository, auto-save, serialization
├── :core:overlay         — Overlay engine, mask shapes, blend modes, shaders
├── :core:undo            — UndoManager (per-mode stacks)
└── :sandbox              — Experimentation app (depends on all :core:* modules)
```

### Dependency Rules (ENFORCED — no exceptions)

```
:app             → :feature:canvas, :feature:picker, :feature:editor, :feature:export, all :core:*
:feature:canvas  → :core:model, :core:ui, :core:drawing, :core:image, :core:overlay, :core:undo
:feature:picker  → :core:model, :core:ui, :core:image
:feature:editor  → :core:model, :core:ui, :core:image, :core:overlay
:feature:export  → :core:model, :core:ui, :core:image, :core:project
:core:ui         → :core:model
:core:drawing    → :core:model
:core:image      → :core:model
:core:project    → :core:model
:core:overlay    → :core:model
:core:undo       → :core:model
:core:model      → (nothing — leaf module)
:sandbox         → all :core:* modules (NOT :feature:* or :app)
```

**CRITICAL:** `:feature:*` modules NEVER depend on each other. `:core:*` modules depend ONLY on `:core:model` (or other `:core:*` modules if strictly necessary). `:sandbox` depends on `:core:*` only — it has its own throwaway UI.

---

## 2. BUILD CONFIGURATION

### 2.1 Version Catalog (`gradle/libs.versions.toml`)

Create or update the version catalog. This is the SINGLE SOURCE OF TRUTH for all dependency versions.

```toml
[versions]
agp = "8.8.2"
kotlin = "2.1.10"
compose-bom = "2025.03.00"
hilt = "2.56.2"
ksp = "2.1.10-1.0.31"
coil = "3.1.0"
gpuimage = "2.1.0"
telephoto = "0.14.0"
kotlinx-serialization = "1.7.3"
kotlinx-collections = "0.3.8"
timber = "5.0.1"
leakcanary = "2.14"
touchimageview = "3.7.1"
activity-compose = "1.10.1"
lifecycle = "2.9.0"
navigation-compose = "2.9.0"
core-ktx = "1.16.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }

# AndroidX
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Image
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
gpuimage = { group = "jp.co.cyberagent.android", name = "gpuimage", version.ref = "gpuimage" }
telephoto-zoomable = { group = "me.saket.telephoto", name = "zoomable", version.ref = "telephoto" }
touchimageview = { group = "com.github.MikeOrtiz", name = "TouchImageView", version.ref = "touchimageview" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "kotlinx-collections" }

# Logging & Debug
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
leakcanary = { group = "com.squareup.leakcanary", name = "leakcanary-android", version.ref = "leakcanary" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**NOTE on fonts:** The monochrome design system requires two UI typefaces — **Plus Jakarta Sans** (weights 400, 500, 600) and **IBM Plex Mono** (weights 400, 500, 600). These are bundled as `.ttf` files in `:core:ui/src/main/res/font/`, NOT fetched at runtime. No library dependency needed — they are raw resource files. Download them from Google Fonts before running this setup. See Section 3.7 for the font file listing.

### 2.2 Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 2.3 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TechnicalAnnotations"

include(":app")
include(":sandbox")
include(":core:model")
include(":core:ui")
include(":core:drawing")
include(":core:image")
include(":core:project")
include(":core:overlay")
include(":core:undo")
include(":feature:canvas")
include(":feature:picker")
include(":feature:editor")
include(":feature:export")
```

### 2.4 Shared Build Logic — Convention Plugins

Create `buildSrc/` or use an `includeBuild` to reduce duplication. At minimum, create shared config constants:

**`buildSrc/src/main/kotlin/AppConfig.kt`**

```kotlin
object AppConfig {
    const val APPLICATION_ID = "com.bcd.technotes"
    const val MIN_SDK = 29
    const val TARGET_SDK = 36
    const val COMPILE_SDK = 36
    const val VERSION_CODE = 1
    const val VERSION_NAME = "2.0.0"
    const val JVM_TARGET = "21"
}
```

**`buildSrc/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}
```

---

## 3. MODULE BUILD FILES

### 3.1 `:core:model` (Pure Kotlin — NO Android)

```
core/model/build.gradle.kts
core/model/src/main/kotlin/com/bcd/technotes/core/model/
```

```kotlin
// core/model/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
}
```

**Contents — migrate from current `:core`:**
- `AppMode.kt` — `enum class AppMode { BROWSE, ANNOTATE, EDIT }`
- `Project.kt` — `AnnotationProject`, `ProjectMetadata`
- `DrawingElement.kt` — sealed class skeleton
- `PhotoTransform.kt` — per-photo transform (pure Kotlin version, no `android.graphics.RectF`)
- `PhotoOverlay.kt` — overlay data class
- `CollageLayout.kt` — layouts and cells
- `LayerInfo.kt` — layer model
- `UndoAction.kt` — sealed class skeleton
- `PhotoAdjustments.kt` — adjustment parameters
- `Geometry.kt` — serializable `PointF`, `SizeF`, `RectF` (pure Kotlin replacements)
- `AnnotationStyle.kt` — stroke/fill properties (including user-chosen annotation colors)

**CRITICAL:** All geometry types in `:core:model` must be **pure Kotlin** — no `android.graphics.*` imports. Use the custom serializable `PointF`/`RectF` already in `:core`.

### 3.2 `:core:drawing` (Pure Kotlin — NO Android)

```kotlin
// core/drawing/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":core:model"))
}
```

**Contents — empty stubs for now:**
- `SnapEngine.kt` — stub class
- `ArrowheadRenderer.kt` — stub class
- `CatmullRomSmoother.kt` — stub class
- `PrimitiveFactory.kt` — stub class

### 3.3 `:core:image` (Android Library)

```kotlin
// core/image/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.bcd.technotes.core.image"
    compileSdk = AppConfig.COMPILE_SDK
    defaultConfig { minSdk = AppConfig.MIN_SDK }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.gpuimage)
    implementation(libs.timber)
}
```

**Contents — migrate from current `:core` and `:app`:**
- `processing/GuidedFilter.kt` — existing from `:core`
- `processing/ColorSpace.kt` — existing from `:core`
- `BitmapService.kt` — migrate from `:app/data/service/`
- `TransformPipeline.kt` — stub

**NOTE:** `BitmapService` uses Android APIs (`Bitmap`, `Canvas`, `Context`), so it belongs in an Android library module, not pure JVM. However, `GuidedFilter` and `ColorSpace` are pure Kotlin — consider keeping them as-is or moving to `:core:model` if they have no image-specific logic. Decision: keep them in `:core:image` for cohesion since they serve the image pipeline.

### 3.4 `:core:project` (Android Library)

```kotlin
// core/project/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bcd.technotes.core.project"
    compileSdk = AppConfig.COMPILE_SDK
    defaultConfig { minSdk = AppConfig.MIN_SDK }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
```

**Contents:**
- `ProjectRepository.kt` — interface
- `ProjectRepositoryImpl.kt` — JSON file persistence
- `AutoSaveManager.kt` — stub
- `ProjectSerializer.kt` — migrate from current `:core/serialization/`

### 3.5 `:core:overlay` (Android Library)

```kotlin
// core/overlay/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.bcd.technotes.core.overlay"
    compileSdk = AppConfig.COMPILE_SDK
    defaultConfig { minSdk = AppConfig.MIN_SDK }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.timber)
}
```

**Contents — all stubs:**
- `OverlayEngine.kt` — stub
- `MaskRenderer.kt` — stub
- `BlendModeProcessor.kt` — stub

### 3.6 `:core:undo` (Pure Kotlin — NO Android)

```kotlin
// core/undo/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":core:model"))
}
```

**Contents:**
- `UndoManager.kt` — stub with three stacks (browse, annotate, edit)

### 3.7 `:core:ui` (Android Library with Compose) — DESIGN SYSTEM MODULE

> **AUTHORITATIVE SOURCE:** `ui-monochrome-design-system.html`
> This module implements the monochrome design system. Every color, font, spacing, radius,
> and shared component in the app originates here. Feature modules NEVER hardcode design values.

```kotlin
// core/ui/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bcd.technotes.core.ui"
    compileSdk = AppConfig.COMPILE_SDK
    defaultConfig { minSdk = AppConfig.MIN_SDK }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:model"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)
}
```

**Font files** — bundle these `.ttf` files in `core/ui/src/main/res/font/`:

```
res/font/
├── plus_jakarta_sans_regular.ttf      (400)
├── plus_jakarta_sans_medium.ttf       (500)
├── plus_jakarta_sans_semibold.ttf     (600)
├── ibm_plex_mono_regular.ttf          (400)
├── ibm_plex_mono_medium.ttf           (500)
└── ibm_plex_mono_semibold.ttf         (600)
```

Download from Google Fonts before running this setup. These are UI chrome fonts — distinct from annotation fonts (see Section 3.7.1).

**Contents — skeleton files to create:**

**`theme/TechNotesTheme.kt`** — the root theme composable:

```kotlin
package com.bcd.technotes.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Monochrome design system theme.
 *
 * DARK-FIRST: The app defaults to dark theme. The canvas is nearly black (#090909).
 * UI chrome floats barely above it. Light theme is available as a user toggle.
 *
 * ZERO CHROMA: No accent hue. Active states = brightness (bright on dark, solid on light).
 * The only color in the app comes from user photos and chosen annotation colors.
 *
 * AUTHORITATIVE SOURCE: ui-monochrome-design-system.html
 */
@Composable
fun TechNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalSpacing provides TechNotesSpacing,
        LocalRadius provides TechNotesRadius,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TechNotesTypography,
            shapes = TechNotesShapes,
            content = content
        )
    }
}

/** Access extended tokens: TechNotesTheme.extendedColors.canvas */
object TechNotesTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
    val radius: Radius
        @Composable get() = LocalRadius.current
}
```

**`theme/Color.kt`** — all gray ramp tokens:

```kotlin
package com.bcd.technotes.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Gray ramp ────────────────────────────────────────────
val Gray090909 = Color(0xFF090909)
val Gray0e0e0e = Color(0xFF0e0e0e)
val Gray171717 = Color(0xFF171717)
val Gray1f1f1f = Color(0xFF1f1f1f)
val Gray282828 = Color(0xFF282828)
val Gray313131 = Color(0xFF313131)
val Gray4e4e4e = Color(0xFF4e4e4e)
val Gray727272 = Color(0xFF727272)
val Grayb0b0b0 = Color(0xFFb0b0b0)
val Graye0e0e0 = Color(0xFFe0e0e0)
val Grayf0f0f0 = Color(0xFFf0f0f0)
val Grayf8f8f8 = Color(0xFFf8f8f8)
val Graye4e4e4 = Color(0xFFe4e4e4)
val Grayeaeaea = Color(0xFFeaeaea)
val Graydddddd = Color(0xFFdddddd)
val Grayc4c4c4 = Color(0xFFc4c4c4)
val Gray999999 = Color(0xFF999999)
val Gray666666 = Color(0xFF666666)
val Gray222222 = Color(0xFF222222)
val Gray111111 = Color(0xFF111111)

// ── Material 3 color scheme mapping ─────────────────────
val DarkColorScheme = darkColorScheme(
    primary = Graye0e0e0,                // active/interactive highlight
    onPrimary = Gray090909,
    primaryContainer = Color(0x12E0E0E0), // rgba(224,224,224,0.07)
    background = Gray0e0e0e,
    onBackground = Grayb0b0b0,           // primary body text
    surface = Gray171717,                 // toolbars, sheets, cards
    onSurface = Gray727272,              // secondary text, labels
    surfaceVariant = Gray1f1f1f,         // elevated surfaces
    onSurfaceVariant = Gray4e4e4e,       // tertiary text, disabled
    surfaceBright = Gray282828,          // highest elevation
    outline = Gray313131,                // borders, dividers
    scrim = Color(0xE00E0E0E),           // rgba(14,14,14,0.88)
)

val LightColorScheme = lightColorScheme(
    primary = Gray111111,
    onPrimary = Grayf8f8f8,
    primaryContainer = Color(0x0D111111), // rgba(17,17,17,0.05)
    background = Grayf0f0f0,
    onBackground = Gray222222,
    surface = Grayf8f8f8,
    onSurface = Gray666666,
    surfaceVariant = Grayeaeaea,
    onSurfaceVariant = Gray999999,
    surfaceBright = Graydddddd,
    outline = Grayc4c4c4,
    scrim = Color(0xE0F0F0F0),
)

// ── Extended colors (not in Material 3 standard) ────────
@Immutable
data class ExtendedColors(
    val canvas: Color,                    // drawing surface — deepest/lightest
    val modeBrowse: Color,               // mode indicator bar — dim
    val modeAnnotate: Color,             // mode indicator bar — bright
    val modeEdit: Color,                 // mode indicator bar — mid
)

val DarkExtendedColors = ExtendedColors(
    canvas = Gray090909,
    modeBrowse = Color(0xFF606060),
    modeAnnotate = Color(0xFFd0d0d0),
    modeEdit = Color(0xFF909090),
)

val LightExtendedColors = ExtendedColors(
    canvas = Graye4e4e4,
    modeBrowse = Color(0xFFaaaaaa),
    modeAnnotate = Color(0xFF333333),
    modeEdit = Color(0xFF777777),
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
```

**`theme/Typography.kt`** — custom typography using bundled fonts:

```kotlin
package com.bcd.technotes.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bcd.technotes.core.ui.R

// ── Font families ────────────────────────────────────────
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
)

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

// ── Material 3 Typography mapping ───────────────────────
// UI text  = Plus Jakarta Sans (buttons, body, captions, sheet headers) 11-14sp
// Tool labels = IBM Plex Mono 600 (toolbar labels, section headers, chips) uppercase, tracked
// Data readouts = IBM Plex Mono 500 (dimension values, measurement text, slider values)
val TechNotesTypography = Typography(
    // Body text — Plus Jakarta Sans
    bodyLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 12.sp),

    // Labels — IBM Plex Mono (tool labels, section headers)
    labelLarge = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.3.sp),

    // Titles — Plus Jakarta Sans
    titleLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),

    // Headlines — Plus Jakarta Sans
    headlineLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    headlineSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 18.sp),
)
```

**`theme/Spacing.kt`** — spacing and corner radius tokens:

```kotlin
package com.bcd.technotes.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,     // icon-to-label gap
    val sm: Dp = 8.dp,     // chip gaps, tight padding
    val md: Dp = 12.dp,    // component internal
    val lg: Dp = 16.dp,    // section padding
    val xl: Dp = 24.dp,    // card padding, sheet sections
)

@Immutable
data class Radius(
    val sm: Dp = 6.dp,     // chips, badges
    val md: Dp = 8.dp,     // buttons, icon buttons
    val lg: Dp = 14.dp,    // cards, sheets
    val xl: Dp = 16.dp,    // main toolbar
    val pill: Dp = 999.dp, // FAB, undo pill
)

val TechNotesSpacing = Spacing()
val TechNotesRadius = Radius()

val LocalSpacing = staticCompositionLocalOf { TechNotesSpacing }
val LocalRadius = staticCompositionLocalOf { TechNotesRadius }

// Material 3 Shapes mapped to our radius tokens
val TechNotesShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)
```

**`components/`** — shared composable stubs (implement during SESSION 03-05):

- `ModeIndicatorBar.kt` — 2dp bar at screen top, brightness per mode
- `TechNotesButton.kt` — monochrome button variants (filled, tonal, outlined, ghost, destructive)
- `TechNotesChip.kt` — monochrome filter/action chips
- `TechNotesSlider.kt` — monochrome slider
- `TechNotesToggle.kt` — monochrome toggle switch
- `ToolIcon.kt` — icon + label composable for toolbar items (active=bright, inactive=dim)

Create all as empty stub composables. Actual implementation happens during Blueprint sessions.

#### 3.7.1 Annotation Fonts (separate from UI fonts)

The GRAPHIC-ELEMENT-EDITING-SPEC defines **6 annotation fonts** for text annotations that users place on photos. These are NOT UI fonts — they belong in `:core:drawing` as asset references (or in `:core:model` as enum definitions with paths).

Bundle these in `core/drawing/src/main/assets/fonts/`:

```
assets/fonts/
├── sans.ttf          — system sans-serif (default, clean)
├── mono.ttf          — monospaced (tabular data, tag IDs)
├── condensed.ttf     — narrow sans (tight spaces)
├── serif.ttf         — serif (formal notes)
├── handwritten.ttf   — casual script (sketch-style)
└── stencil.ttf       — industrial stencil (marking callouts)
```

**Wait:** `:core:drawing` is pure JVM (no Android). Annotation font asset loading requires Android APIs. Move the font assets to `:core:image` (which is an Android library) or create a dedicated `AnnotationFontProvider` in `:feature:canvas`. Decision for now: put the font `.ttf` files in `:core:ui/src/main/assets/fonts/` alongside the UI fonts, and let `:feature:canvas` access them via the `AssetManager`. The `FontFamily` enum stays in `:core:model` as a pure data definition.

### 3.8 `:feature:canvas` (Android Library with Compose)

```kotlin
// feature/canvas/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bcd.technotes.feature.canvas"
    compileSdk = AppConfig.COMPILE_SDK
    defaultConfig { minSdk = AppConfig.MIN_SDK }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:drawing"))
    implementation(project(":core:image"))
    implementation(project(":core:overlay"))
    implementation(project(":core:undo"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.touchimageview)
    implementation(libs.coil.compose)
    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}
```

**Contents — migrate from current `:app`:**
- `CanvasScreen.kt` — root composable (migrate from `ui/canvas/`)
- `CanvasViewModel.kt` — state management (migrate from `viewmodel/`)
- `CollageThumbBar.kt` — migrate
- `LayoutSelector.kt` — migrate
- `EditableTouchImageView.kt` — migrate from `ui/util/`
- `TouchImageViewExt.kt` — migrate

### 3.9 `:feature:picker` (Android Library with Compose)

```kotlin
// feature/picker/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bcd.technotes.feature.picker"
    compileSdk = AppConfig.COMPILE_SDK
    defaultConfig { minSdk = AppConfig.MIN_SDK }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:image"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.runtime)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.telephoto.zoomable)
    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}
```

**Contents — migrate from current `:app`:**
- `PhotoPickerScreen.kt` — migrate from `ui/photo/`
- `PhotoPickerViewModel.kt` — migrate from `viewmodel/`
- `PhotoGrid.kt`, `AlbumGrid.kt`, `PhotoPreview.kt`, `SelectedPhotosStrip.kt`
- `MediaRepository.kt` + `MediaRepositoryImpl.kt` — migrate from `data/repository/`
- `PickerModels.kt` — migrate from `data/model/`

### 3.10 `:feature:editor` (Android Library with Compose)

```kotlin
// feature/editor/build.gradle.kts — same pattern as canvas
// Dependencies: :core:model, :core:ui, :core:image, :core:overlay, Compose, Hilt
```

**Contents — migrate from current `:app`:**
- `WholeImageEditScreen.kt` — migrate from `ui/editor/`
- `AdjustmentPanel.kt` — migrate
- `WholeImageEditView.kt` — migrate from `ui/util/`
- `ImageEditState.kt` — migrate from `data/model/`

### 3.11 `:feature:export` (Android Library with Compose)

```kotlin
// feature/export/build.gradle.kts — same pattern
// Dependencies: :core:model, :core:ui, :core:image, :core:project, Compose, Hilt
```

**Contents — stubs only:**
- `ExportSheet.kt` — stub composable
- `ExportViewModel.kt` — stub

### 3.12 `:app` (Application Module — Slim Shell)

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bcd.technotes"
    compileSdk = AppConfig.COMPILE_SDK

    defaultConfig {
        applicationId = AppConfig.APPLICATION_ID
        minSdk = AppConfig.MIN_SDK
        targetSdk = AppConfig.TARGET_SDK
        versionCode = AppConfig.VERSION_CODE
        versionName = AppConfig.VERSION_NAME
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
    buildFeatures { compose = true }
}

dependencies {
    // Feature modules
    implementation(project(":feature:canvas"))
    implementation(project(":feature:picker"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:export"))

    // Core modules
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:drawing"))
    implementation(project(":core:image"))
    implementation(project(":core:project"))
    implementation(project(":core:overlay"))
    implementation(project(":core:undo"))

    // Android + Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.leakcanary)
}
```

**Contents — after migration, `:app` should contain ONLY:**
- `TechNotesApplication.kt` — `@HiltAndroidApp`, Timber init
- `MainActivity.kt` — `@AndroidEntryPoint`, single Activity, `enableEdgeToEdge()`, wraps content in `TechNotesTheme { }`
- `navigation/AppNavigation.kt` — Compose Navigation host with routes
- `di/AppModule.kt` — Hilt bindings that wire feature modules together

**NOTE:** `Theme.kt` is GONE from `:app`. The theme now lives in `:core:ui` as `TechNotesTheme`. `MainActivity` wraps its content with `TechNotesTheme { AppNavigation() }`.

### 3.13 `:sandbox` (Application Module)

```kotlin
// sandbox/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bcd.technotes.sandbox"
    compileSdk = AppConfig.COMPILE_SDK

    defaultConfig {
        applicationId = "com.bcd.technotes.sandbox"
        minSdk = AppConfig.MIN_SDK
        targetSdk = AppConfig.TARGET_SDK
        versionCode = 1
        versionName = "1.0-sandbox"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = AppConfig.JVM_TARGET }
    buildFeatures { compose = true }
}

dependencies {
    // Core modules ONLY — sandbox has its own throwaway UI
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:drawing"))
    implementation(project(":core:image"))
    implementation(project(":core:project"))
    implementation(project(":core:overlay"))
    implementation(project(":core:undo"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.touchimageview)
    implementation(libs.coil.compose)
    implementation(libs.gpuimage)
    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.leakcanary)
}
```

---

## 4. DIRECTORY CREATION SCRIPT

Run this to create the full directory tree. Adjust the base path if needed.

```bash
#!/bin/bash
# Run from project root

# Core modules (model is pure JVM, others are Android libs)
mkdir -p core/model/src/main/kotlin/com/bcd/technotes/core/model
mkdir -p core/ui/src/main/kotlin/com/bcd/technotes/core/ui/theme
mkdir -p core/ui/src/main/kotlin/com/bcd/technotes/core/ui/components
mkdir -p core/ui/src/main/res/font
mkdir -p core/ui/src/main/assets/fonts
mkdir -p core/drawing/src/main/kotlin/com/bcd/technotes/core/drawing
mkdir -p core/image/src/main/kotlin/com/bcd/technotes/core/image/processing
mkdir -p core/project/src/main/kotlin/com/bcd/technotes/core/project
mkdir -p core/overlay/src/main/kotlin/com/bcd/technotes/core/overlay
mkdir -p core/undo/src/main/kotlin/com/bcd/technotes/core/undo

# Feature modules
mkdir -p feature/canvas/src/main/kotlin/com/bcd/technotes/feature/canvas
mkdir -p feature/canvas/src/main/res
mkdir -p feature/picker/src/main/kotlin/com/bcd/technotes/feature/picker
mkdir -p feature/picker/src/main/res
mkdir -p feature/editor/src/main/kotlin/com/bcd/technotes/feature/editor
mkdir -p feature/editor/src/main/res
mkdir -p feature/export/src/main/kotlin/com/bcd/technotes/feature/export
mkdir -p feature/export/src/main/res

# Android library manifests (Android libs need these)
for mod in core/ui core/image core/project core/overlay feature/canvas feature/picker feature/editor feature/export; do
    mkdir -p "$mod/src/main"
    # AndroidManifest.xml is auto-generated by AGP from namespace, but create empty one for safety
    echo '<manifest />' > "$mod/src/main/AndroidManifest.xml"
done

# App module (restructure)
mkdir -p app/src/main/kotlin/com/bcd/technotes/navigation
mkdir -p app/src/main/kotlin/com/bcd/technotes/di

# Sandbox (keep existing structure, just update dependencies)
mkdir -p sandbox/src/main/kotlin/com/bcd/technotes/sandbox

# buildSrc
mkdir -p buildSrc/src/main/kotlin

echo "Directory tree created."
echo ""
echo "MANUAL STEP REQUIRED: Download font files into core/ui/src/main/res/font/"
echo "  - Plus Jakarta Sans: Regular (400), Medium (500), SemiBold (600)"
echo "  - IBM Plex Mono: Regular (400), Medium (500), SemiBold (600)"
echo "Download from: https://fonts.google.com/specimen/Plus+Jakarta+Sans"
echo "           and: https://fonts.google.com/specimen/IBM+Plex+Mono"
echo ""
echo "MANUAL STEP REQUIRED: Place annotation font files in core/ui/src/main/assets/fonts/"
echo "  - sans.ttf, mono.ttf, condensed.ttf, serif.ttf, handwritten.ttf, stencil.ttf"
```

---

## 5. MIGRATION PLAN — FILE MOVEMENTS

### Phase A: Move `:core` to `:core:model` (pure Kotlin files)

| Current Location | New Location |
|---|---|
| `core/src/.../model/Annotation.kt` | `core/model/src/.../core/model/Annotation.kt` |
| `core/src/.../model/AnnotationStyle.kt` | `core/model/src/.../core/model/AnnotationStyle.kt` |
| `core/src/.../model/CollageLayout.kt` | `core/model/src/.../core/model/CollageLayout.kt` |
| `core/src/.../model/Geometry.kt` | `core/model/src/.../core/model/Geometry.kt` |
| `core/src/.../model/Layer.kt` | `core/model/src/.../core/model/LayerInfo.kt` |
| `core/src/.../model/PhotoAdjustments.kt` | `core/model/src/.../core/model/PhotoAdjustments.kt` |
| `core/src/.../model/Project.kt` | `core/model/src/.../core/model/Project.kt` |

### Phase B: Move `:core` processing to `:core:image`

| Current Location | New Location |
|---|---|
| `core/src/.../processing/GuidedFilter.kt` | `core/image/src/.../core/image/processing/GuidedFilter.kt` |
| `core/src/.../processing/ColorSpace.kt` | `core/image/src/.../core/image/processing/ColorSpace.kt` |

### Phase C: Move `:core` serialization to `:core:project`

| Current Location | New Location |
|---|---|
| `core/src/.../serialization/ProjectSerializer.kt` | `core/project/src/.../core/project/ProjectSerializer.kt` |

### Phase D: Move `:app` code to feature modules

| Current `:app` Location | New Module |
|---|---|
| `viewmodel/CanvasViewModel.kt` | `:feature:canvas` |
| `ui/canvas/CanvasScreen.kt` | `:feature:canvas` |
| `ui/canvas/CollageThumbBar.kt` | `:feature:canvas` |
| `ui/canvas/LayoutSelector.kt` | `:feature:canvas` |
| `ui/util/EditableTouchImageView.kt` | `:feature:canvas` |
| `ui/util/TouchImageViewExt.kt` | `:feature:canvas` |
| `viewmodel/PhotoPickerViewModel.kt` | `:feature:picker` |
| `ui/photo/PhotoPickerScreen.kt` | `:feature:picker` |
| `ui/photo/PhotoGrid.kt` | `:feature:picker` |
| `ui/photo/AlbumGrid.kt` | `:feature:picker` |
| `ui/photo/PhotoPreview.kt` | `:feature:picker` |
| `ui/photo/SelectedPhotosStrip.kt` | `:feature:picker` |
| `data/repository/MediaRepository.kt` | `:feature:picker` |
| `data/repository/MediaRepositoryImpl.kt` | `:feature:picker` |
| `data/model/PickerModels.kt` | `:feature:picker` |
| `ui/editor/WholeImageEditScreen.kt` | `:feature:editor` |
| `ui/editor/AdjustmentPanel.kt` | `:feature:editor` |
| `ui/util/WholeImageEditView.kt` | `:feature:editor` |
| `data/model/ImageEditState.kt` | `:feature:editor` |
| `data/service/BitmapService.kt` | `:core:image` |
| `data/model/PhotoTransform.kt` | Decide: `:core:model` (pure) or `:feature:canvas` (if Android-dependent) |

### Phase D2: Create `:core:ui` theme files (NEW)

Create the following skeleton files as specified in Section 3.7:

| File | Location |
|---|---|
| `TechNotesTheme.kt` | `core/ui/src/.../core/ui/theme/` |
| `Color.kt` | `core/ui/src/.../core/ui/theme/` |
| `Typography.kt` | `core/ui/src/.../core/ui/theme/` |
| `Spacing.kt` | `core/ui/src/.../core/ui/theme/` |
| `ModeIndicatorBar.kt` | `core/ui/src/.../core/ui/components/` (stub) |
| `TechNotesButton.kt` | `core/ui/src/.../core/ui/components/` (stub) |
| `TechNotesChip.kt` | `core/ui/src/.../core/ui/components/` (stub) |
| `TechNotesSlider.kt` | `core/ui/src/.../core/ui/components/` (stub) |
| `TechNotesToggle.kt` | `core/ui/src/.../core/ui/components/` (stub) |
| `ToolIcon.kt` | `core/ui/src/.../core/ui/components/` (stub) |

### Phase E: Slim down `:app`

After migration, `:app` should contain ONLY:
- `TechNotesApplication.kt`
- `MainActivity.kt` — wraps content in `TechNotesTheme { }`
- `navigation/AppNavigation.kt` (NEW — replace manual boolean routing)
- `di/AppModule.kt` (update Hilt bindings)

**Delete** from `:app` after migration: all `viewmodel/`, `data/`, `ui/canvas/`, `ui/photo/`, `ui/editor/`, `ui/util/`, `ui/theme/` directories. Theme now lives in `:core:ui`.

---

## 6. PACKAGE NAMING

After migration, update all `package` declarations:

| Module | Package Root |
|---|---|
| `:core:model` | `com.bcd.technotes.core.model` |
| `:core:ui` | `com.bcd.technotes.core.ui` |
| `:core:ui` (theme) | `com.bcd.technotes.core.ui.theme` |
| `:core:ui` (components) | `com.bcd.technotes.core.ui.components` |
| `:core:drawing` | `com.bcd.technotes.core.drawing` |
| `:core:image` | `com.bcd.technotes.core.image` |
| `:core:project` | `com.bcd.technotes.core.project` |
| `:core:overlay` | `com.bcd.technotes.core.overlay` |
| `:core:undo` | `com.bcd.technotes.core.undo` |
| `:feature:canvas` | `com.bcd.technotes.feature.canvas` |
| `:feature:picker` | `com.bcd.technotes.feature.picker` |
| `:feature:editor` | `com.bcd.technotes.feature.editor` |
| `:feature:export` | `com.bcd.technotes.feature.export` |
| `:app` | `com.bcd.technotes` |
| `:sandbox` | `com.bcd.technotes.sandbox` |

Update all `import` statements to reflect new package paths.

---

## 7. NAVIGATION — REPLACE MANUAL ROUTING

Replace the current manual `if (showPicker) { ... return }` routing in `CanvasScreen` with Jetpack Compose Navigation:

**`app/src/.../navigation/AppNavigation.kt`:**

```kotlin
package com.bcd.technotes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val CANVAS = "canvas"
    const val PICKER = "picker"
    const val EDITOR = "editor/{photoIndex}"
    const val WHOLE_IMAGE_EDIT = "whole_image_edit"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CANVAS) {
        composable(Routes.CANVAS) {
            // CanvasScreen with navigation callbacks
        }
        composable(Routes.PICKER) {
            // PhotoPickerScreen
        }
        composable(Routes.WHOLE_IMAGE_EDIT) {
            // WholeImageEditScreen
        }
    }
}
```

**Note:** This is a SHELL. The actual screen wiring happens during SESSION 01-07 implementation.

---

## 8. HILT DI UPDATES

### 8.1 `AppModule.kt` — Update Bindings

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
}
```

### 8.2 Feature Module Hilt

Each `:feature:*` module uses `@HiltViewModel` for its ViewModels. Hilt discovers these automatically — no additional module bindings needed as long as `:app` includes the feature modules as dependencies.

---

## 9. CLAUDE CODE CONFIGURATION UPDATES

### 9.1 `.claude/CLAUDE.md` — Update Project Rules

Replace the module architecture section with:

```markdown
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

## Sandbox-First Pipeline

1. :core:* — Pure Kotlin data models + logic (unit tested, no emulator)
2. :sandbox — Build feature UI, deploy to phone, user confirms
3. /port — Classify changes, rewrite cleanly into :feature:* and :core:*, verify parity
4. Build both :app and :sandbox APKs → Install both → Confirm identical behavior
5. Update DECISIONS.md → Audit compliance → Commit & push
```

### 9.2 `.claude/commands/port.md` — Update Architecture Mapping

Update step 2 (classify changes) to reflect the new module structure:

```markdown
## Architecture Mapping (for classifying sandbox changes)

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
```

### Resource Externalization — Porting Rule

During porting, all UI values MUST reference the design system:

```kotlin
// WRONG (sandbox-style — hardcoded values)
Text("Select a photo")
Modifier.padding(16.dp)
Color(0xFF6200EE)

// RIGHT (production-style — design system tokens)
Text(stringResource(R.string.select_photo))
Modifier.padding(TechNotesTheme.spacing.lg)
MaterialTheme.colorScheme.primary

// For extended tokens:
TechNotesTheme.extendedColors.canvas
TechNotesTheme.spacing.md
TechNotesTheme.radius.lg
```

### 9.3 Memory Updates (`~/.claude/projects/.../memory/`)

Update `project_overview.md`:

```markdown
# Project Overview

Technical Annotation App — Android app for annotating photos/documents with engineering symbols.

## Module Structure (as of April 2026)
Multi-module: :app, :feature:canvas, :feature:picker, :feature:editor, :feature:export,
:core:model, :core:ui, :core:drawing, :core:image, :core:project, :core:overlay, :core:undo, :sandbox

## Design System
Monochrome. Zero chroma. Dark-first. Theme in :core:ui (TechNotesTheme).
Fonts: Plus Jakarta Sans (UI) + IBM Plex Mono (labels). Bundled as .ttf resources.
See: ui-monochrome-design-system.html

## Current Phase
Restructured to multi-module. Ready for SESSION 01+ of Implementation Blueprint.

## Key Files
- IMPLEMENTATION-BLUEPRINT.md — what to build (35 sessions)
- CLAUDE-CODE-WORKFLOW.md — how to build (4 phases per session)
- APP-ARCHITECTURE.md — code architecture reference (UPDATE AFTER RESTRUCTURE)
- DEV-ENVIRONMENT.md — tooling and config reference
- ui-monochrome-design-system.html — visual design authority
```

### 9.4 Hook Updates (`.claude/settings.json`)

Update the PreToolUse hooks to match the new paths:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "name": "plan-first",
        "match": ["Edit", "Write"],
        "pathPattern": "(app|feature|core)/src/",
        "message": "REMINDER: An approved plan is required before editing source files. Enter plan mode first."
      },
      {
        "name": "port-parity",
        "match": ["Edit", "Write"],
        "pathPattern": "(app|feature)/src/",
        "message": "REMINDER: When porting, compare sandbox and app side-by-side. Verify ALL behaviors match."
      },
      {
        "name": "compliance-audit",
        "match": ["Edit", "Write"],
        "pathPattern": "(app|feature)/src/",
        "message": "REMINDER: Review for Play Store compliance, security (SQL injection, PII logging, cleartext traffic), and log findings in AUDIT.md."
      },
      {
        "name": "design-system",
        "match": ["Edit", "Write"],
        "pathPattern": "(feature|sandbox)/src/.*\\.(kt|xml)$",
        "message": "REMINDER: Never hardcode colors, spacing, or font values. Use TechNotesTheme tokens from :core:ui. See ui-monochrome-design-system.html."
      }
    ]
  }
}
```

---

## 10. EXECUTION SEQUENCE

Run these steps IN ORDER:

```
STEP 01 — Create git branch: restructure/multi-module-setup
STEP 02 — Create buildSrc/src/main/kotlin/AppConfig.kt
STEP 03 — Create buildSrc/build.gradle.kts
STEP 04 — Update gradle/libs.versions.toml (version catalog)
STEP 05 — Update root build.gradle.kts
STEP 06 — Update settings.gradle.kts (include all new modules including :core:ui)
STEP 07 — Run directory creation script (Section 4)
STEP 08 — MANUAL: Download font .ttf files into core/ui/src/main/res/font/
STEP 09 — Create all module build.gradle.kts files (Section 3)
STEP 10 — Migrate :core files to :core:model, :core:image, :core:project (Phase A-C)
STEP 11 — Create :core:ui theme skeleton files (Phase D2 — Color.kt, Typography.kt, Spacing.kt, TechNotesTheme.kt)
STEP 12 — Create :core:ui component stubs (ModeIndicatorBar, TechNotesButton, etc.)
STEP 13 — Create stub files in :core:drawing, :core:overlay, :core:undo
STEP 14 — Migrate :app code to :feature:* modules (Phase D)
STEP 15 — Update all package declarations and imports
STEP 16 — Slim down :app to shell only (Phase E) — remove ui/theme/, use TechNotesTheme from :core:ui
STEP 17 — Create navigation/AppNavigation.kt shell
STEP 18 — Update di/AppModule.kt with new bindings
STEP 19 — Update :sandbox build.gradle.kts to depend on new :core:* modules (including :core:ui)
STEP 20 — Verify: ./gradlew :core:model:build (pure JVM compiles)
STEP 21 — Verify: ./gradlew :core:ui:assembleDebug (design system compiles)
STEP 22 — Verify: ./gradlew :core:drawing:build
STEP 23 — Verify: ./gradlew :core:image:assembleDebug
STEP 24 — Verify: ./gradlew :feature:canvas:assembleDebug
STEP 25 — Verify: ./gradlew :app:assembleDebug (full app compiles)
STEP 26 — Verify: ./gradlew :sandbox:assembleDebug
STEP 27 — Deploy :app to device — verify it launches, shows empty canvas
STEP 28 — Deploy :sandbox to device — verify it launches
STEP 29 — Update .claude/CLAUDE.md, commands/port.md, settings.json, memory files
STEP 30 — Update APP-ARCHITECTURE.md to reflect new structure
STEP 31 — Commit: "restructure: multi-module architecture per Implementation Blueprint"
STEP 32 — Push branch, verify CI if configured
```

### Verification Checklist

```
□ ./gradlew :core:model:build              PASS
□ ./gradlew :core:ui:assembleDebug         PASS
□ ./gradlew :core:drawing:build            PASS
□ ./gradlew :core:image:assembleDebug      PASS
□ ./gradlew :core:project:assembleDebug    PASS
□ ./gradlew :core:overlay:assembleDebug    PASS
□ ./gradlew :core:undo:build               PASS
□ ./gradlew :feature:canvas:assembleDebug  PASS
□ ./gradlew :feature:picker:assembleDebug  PASS
□ ./gradlew :feature:editor:assembleDebug  PASS
□ ./gradlew :feature:export:assembleDebug  PASS
□ ./gradlew :app:assembleDebug             PASS
□ ./gradlew :sandbox:assembleDebug         PASS
□ :app installs and launches on device     PASS
□ :sandbox installs and launches on device PASS
□ No :feature:* → :feature:* dependencies  PASS
□ No :core:* → :feature:* dependencies     PASS
□ :core:model has zero Android imports     PASS
□ :core:drawing has zero Android imports   PASS
□ :core:undo has zero Android imports      PASS
□ :core:ui has Compose but NO Hilt/nav     PASS
□ Font .ttf files present in core/ui res   PASS
□ TechNotesTheme wraps content in :app     PASS
□ TechNotesTheme wraps content in :sandbox PASS
□ No hardcoded colors in :feature:* code   PASS
```

---

## 11. WHAT NOT TO DO

- **DO NOT refactor any business logic.** This is a structural migration only. Code behavior must be identical before and after.
- **DO NOT add new features.** No new UI, no new models, no new tools. SESSION 01+ handles that.
- **DO NOT delete the old `:core` module until the new `:core:*` modules all compile.** Migrate incrementally.
- **DO NOT change minSdk/targetSdk/compileSdk.** Keep current values (29/36/36).
- **DO NOT introduce Compose Navigation yet if it would break existing manual routing.** Create the shell but wire it up during SESSION 01-03.
- **DO NOT change the `:sandbox` workflow.** It still works the same way — just depends on `:core:*` submodules now instead of flat `:core`.
- **DO NOT implement design system components yet.** Create stubs only. The actual button/chip/slider/toggle implementations happen during Blueprint sessions.
- **DO NOT use Material 3 default theme colors.** Even in stubs, wrap with `TechNotesTheme`. The monochrome gray ramp is the only valid palette.

---

## 12. POST-SETUP — READY STATE

When this setup is complete, the project is in "Blueprint Ready" state:

- **All 13 modules** compile independently (6 core + 4 feature + app + sandbox + buildSrc)
- **Both apps** (:app and :sandbox) install and launch on device
- **Existing features** work exactly as before (photo picker, canvas, collage, editing)
- **Module boundaries** are enforced by Gradle dependency declarations
- **Design system** is in place as `:core:ui` with full color/typography/spacing skeletons
- **TechNotesTheme** wraps content in both `:app` and `:sandbox`
- **Font files** are bundled and ready (Plus Jakarta Sans + IBM Plex Mono)
- **Claude Code config** is updated to understand the new structure, including design system enforcement
- **The /port command** knows the new architecture mapping (including `:core:ui`)
- **SESSION 01** of the Implementation Blueprint can begin immediately

The next step after this setup is to start the granular Implementation Blueprint sessions (01-35), each following the 4-phase workflow: Sandbox → Clean Port → Quality Gate → Done.
