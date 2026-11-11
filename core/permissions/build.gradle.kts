plugins {
    alias(libs.plugins.duet.composeLibrary)
}
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.androidx.lifecycle.runtimeCompose)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.activity.compose)
    }
}
