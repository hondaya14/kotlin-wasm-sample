import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "co.hondaya.kotlin_wasm_sample.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "co.hondaya.kotlin_wasm_sample"
            packageVersion = "1.0.0"
        }
    }
}

// Bridge Gradle property `-Pvisualizer=true` to env `VISUALIZER` for JS/Wasm dev runs
val visualizerEnabled = (findProperty("visualizer")?.toString()?.toBoolean() == true)
tasks.withType<NodeJsExec>().configureEach {
    // Covers dev run tasks that launch Node (webpack dev server)
    environment("VISUALIZER", if (visualizerEnabled) "1" else "0")
}
tasks.withType<KotlinWebpack>().configureEach {
    // Also propagate for webpack bundling tasks to ensure DefinePlugin sees it
    // Note: KotlinWebpack exposes nodejs environment via `environment` in exec spec internally.
    // Using provider on environment variable through Gradle is sufficient when executed under Node.
    doFirst {
        System.getenv("VISUALIZER") ?: System.setProperty("VISUALIZER", if (visualizerEnabled) "1" else "0")
    }
}
