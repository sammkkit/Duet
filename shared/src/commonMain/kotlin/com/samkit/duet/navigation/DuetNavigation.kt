package com.samkit.duet.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.samkit.duet.feature.camera.CameraPermissionGate
import com.samkit.duet.feature.camera.CameraRoute
import com.samkit.duet.feature.review.ReviewScreen
import com.samkit.duet.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

// Screens are keys. @Serializable so the back stack survives rotation and process death.
@Serializable
data object CameraKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object ReviewKey : NavKey

// Non-JVM targets (iOS) can't find NavKey subclasses by reflection, so list them explicitly.
private val navConfig =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(CameraKey::class)
                    subclass(SettingsKey::class)
                    subclass(ReviewKey::class)
                }
            }
    }

/**
 * Navigation 3: the back stack is a plain list we own. Navigate = add a key, back = remove the last one.
 * NavDisplay shows the top key and handles system back.
 */
@Composable
fun DuetNavigation() {
    val backStack = rememberNavBackStack(navConfig, CameraKey)
    val back: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        // Push: new screen slides in from the right, old one out to the left. Back (incl. predictive swipe): the reverse.
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        entryProvider =
            entryProvider {
                entry<CameraKey> {
                    CameraPermissionGate {
                        CameraRoute(
                            onNavigateToSettings = { backStack.add(SettingsKey) },
                            onNavigateToReview = { backStack.add(ReviewKey) },
                        )
                    }
                }
                entry<SettingsKey> { SettingsRoute(onBack = back) }
                entry<ReviewKey> { ReviewScreen(onBack = back) }
            },
    )
}
