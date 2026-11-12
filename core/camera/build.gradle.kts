plugins {
    alias(libs.plugins.duet.composeLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(libs.kotlinx.coroutines.core)
        api(libs.koin.core)
        implementation(libs.koin.compose)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.camerax.core)
        implementation(libs.camerax.camera2)
        implementation(libs.camerax.lifecycle)
        implementation(libs.camerax.video)
        implementation(libs.camerax.view)
        implementation(libs.media3.transformer)
        implementation(libs.media3.effect)
        implementation(libs.androidx.lifecycle.runtimeCompose)
    }
}
