plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
