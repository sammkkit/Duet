package com.samkit.duet

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
