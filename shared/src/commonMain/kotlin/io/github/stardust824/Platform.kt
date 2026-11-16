package io.github.stardust824

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform