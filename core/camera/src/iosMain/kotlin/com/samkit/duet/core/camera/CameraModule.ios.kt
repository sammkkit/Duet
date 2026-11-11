package com.samkit.duet.core.camera

import org.koin.dsl.module

actual val cameraModule =
    module {
        single { AVFoundationController() }
        single<CameraController> { get<AVFoundationController>() }
    }
