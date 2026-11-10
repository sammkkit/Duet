package com.samkit.duet

import com.samkit.duet.core.settings.SettingsRepository
import com.samkit.duet.feature.camera.CameraViewModel
import com.samkit.duet.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule =
    module {
        // single: one shared instance, so the camera sheet and Settings see the same values.
        single { SettingsRepository() }
        viewModel { CameraViewModel(settingsRepository = get()) }
        viewModelOf(::SettingsViewModel)
    }
