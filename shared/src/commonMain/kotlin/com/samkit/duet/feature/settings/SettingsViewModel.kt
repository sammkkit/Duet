package com.samkit.duet.feature.settings

import androidx.lifecycle.ViewModel
import com.samkit.duet.core.settings.SettingsRepository
import com.samkit.duet.core.settings.UserSettings
import kotlinx.coroutines.flow.StateFlow

sealed interface SettingsAction {
    data object ToggleKeepOriginals : SettingsAction

    data object ToggleMirrorFront : SettingsAction
}

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.ToggleKeepOriginals -> repository.update { it.copy(keepOriginals = !it.keepOriginals) }
            SettingsAction.ToggleMirrorFront -> repository.update { it.copy(mirrorFrontCamera = !it.mirrorFrontCamera) }
        }
    }
}
