package com.samkit.duet

import org.koin.dsl.module

val appModule =
    module {
        factory { Greeting() }
    }
