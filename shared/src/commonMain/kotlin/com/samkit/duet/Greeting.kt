package com.samkit.duet

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
