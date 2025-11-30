package co.hondaya.kotlin_wasm_sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import co.hondaya.kotlin_wasm_sample.visualizer.VisualizerPanel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        Box(Modifier.fillMaxSize()) {
            App()
            // Overlay the visualizer on top-right if enabled
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                VisualizerPanel()
            }
        }
    }
}
