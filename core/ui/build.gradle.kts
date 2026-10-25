plugins {
    alias(libs.plugins.duet.composeLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        // api: core:ui's public signatures expose LENS, CaptureLayout and LensColors.
        api(project(":core:designsystem"))
        api(project(":core:model"))
    }
}
