package com.samkit.duet.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** User preferences that more than one screen reads (camera sheet + Settings). */
data class UserSettings(
    val keepOriginals: Boolean = true,
)

// ponytail: in-memory only; Phase 7 backs this with DataStore so settings survive a restart.

/** Single source of truth for [UserSettings]. */
class SettingsRepository {
    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    fun update(transform: (UserSettings) -> UserSettings) = _settings.update(transform)
}
