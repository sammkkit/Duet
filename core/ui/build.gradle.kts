plugins {
    alias(libs.plugins.duet.composeLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:designsystem"))
        implementation(project(":core:model"))
    }
}
