package com.samkit.duet

import androidx.compose.runtime.Composable
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.navigation.DuetNavigation

@Composable
fun App() {
    DuetTheme {
        DuetNavigation()
    }
}
