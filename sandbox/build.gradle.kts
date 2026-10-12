plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = AppConfig.JVM_TARGET
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
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

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Image
    implementation(libs.touchimageview)
    implementation(libs.coil.compose)
    implementation(libs.gpuimage)

    // Immutable Collections
    implementation(libs.kotlinx.collections.immutable)

    // Logging
    implementation(libs.timber)

    // Debug
    debugImplementation(libs.leakcanary)
}
