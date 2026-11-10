package com.samkit.duet

import androidx.compose.runtime.Composable
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.navigation.DuetNavigation
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModule) }) {
        DuetTheme {
            DuetNavigation()
        }
    }
}
