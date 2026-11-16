package io.github.stardust824

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "plant_app",
    ) {
        App()
    }
}