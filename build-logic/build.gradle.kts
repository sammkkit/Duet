plugins {
    `kotlin-dsl`
}

// compileOnly: the real plugin jars come from the root build's classpath, so versions stay in one place.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "duet.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("composeLibrary") {
            id = "duet.compose.library"
            implementationClass = "ComposeLibraryConventionPlugin"
        }
    }
}
