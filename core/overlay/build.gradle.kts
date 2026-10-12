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
