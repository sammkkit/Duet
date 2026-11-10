package com.samkit.duet

import com.samkit.duet.feature.camera.CameraViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule =
    module {
        factory { Greeting() }
        viewModelOf(::CameraViewModel)
    }
