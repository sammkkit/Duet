package com.samkit.duet

import android.app.Application
import org.koin.android.ext.koin.androidContext

class DuetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@DuetApplication) }
    }
}
