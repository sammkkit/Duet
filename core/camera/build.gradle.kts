plugins {
    alias(libs.plugins.duet.kmpLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:model"))
    }
}
