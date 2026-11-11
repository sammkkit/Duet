package com.samkit.duet

import com.samkit.duet.core.camera.cameraModule
import com.samkit.duet.core.settings.SettingsRepository
import com.samkit.duet.feature.camera.CameraViewModel
import com.samkit.duet.feature.settings.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule =
    module {
        // single: one shared instance, so the camera sheet and Settings see the same values.
        single { SettingsRepository() }
        viewModel { CameraViewModel(settingsRepository = get(), camera = get()) }
        viewModelOf(::SettingsViewModel)
    }

/** Called once at app start by each platform (Android: Application.onCreate). */
fun initKoin(platform: KoinAppDeclaration = {}) {
    startKoin {
        platform()
        modules(appModule, cameraModule)
    }
}
