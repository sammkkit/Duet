plugins {
    alias(libs.plugins.duet.kmpLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(libs.kotlinx.coroutines.core)
    }
}
