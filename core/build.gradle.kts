import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The game engine (dictionary, level generator, backup codes) in plain Kotlin,
// shared by the Android app (JVM target) and the web app (JS target, web/).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    js {
        outputModuleName.set("wordlink-core")
        // Node runs the JS tests; the library itself has no Node dependencies
        // and is bundled for the browser by Vite.
        nodejs {
            testTask {
                // The parity test generates ~140 levels; Mocha's 2 s default is too
                // tight for that on shared CI runners.
                useMocha { timeout = "60s" }
            }
        }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Use the Node.js on PATH (CI installs it for web/ anyway) instead of letting
// Kotlin download one: settings' FAIL_ON_PROJECT_REPOS refuses that download.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    extensions.getByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download.set(false)
}
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download.set(false)
}
