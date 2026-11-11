package com.samkit.duet

import androidx.compose.ui.window.ComposeUIViewController

@Suppress("ktlint:standard:function-naming") // the name Swift calls; template convention
fun MainViewController() = ComposeUIViewController { App() }

/** Called once from iOSApp.init (Swift sees it as MainViewControllerKt.setUpKoin()). */
fun setUpKoin() = initKoin()
