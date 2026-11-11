package com.samkit.duet.core.camera

import org.koin.dsl.module

actual val cameraModule =
    module {
        // single: one camera system for the whole app. get() is the application Context, never an Activity.
        single { CameraXController(get()) }
        single<CameraController> { get<CameraXController>() }
    }
