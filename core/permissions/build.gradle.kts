plugins {
    alias(libs.plugins.duet.composeLibrary)
}
kotlin {
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.activity.compose)
    }
}
