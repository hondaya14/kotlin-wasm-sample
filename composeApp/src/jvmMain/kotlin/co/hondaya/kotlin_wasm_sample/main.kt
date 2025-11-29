package co.hondaya.kotlin_wasm_sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "kotlin_wasm_sample",
    ) {
        App()
    }
}