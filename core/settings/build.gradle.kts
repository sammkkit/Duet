plugins {
    alias(libs.plugins.duet.kmpLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // api: SettingsRepository exposes StateFlow in its public API.
        api(libs.kotlinx.coroutines.core)
    }
}
