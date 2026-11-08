buildscript {
    dependencies {
        // AGP 9 compiles Kotlin itself (built-in Kotlin); this pins the Kotlin
        // version so it matches the Compose compiler plugin below.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
