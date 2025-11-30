package co.hondaya.kotlin_wasm_sample.visualizer

@Suppress("UnsafeCastFromDynamic")
actual object VisualizerConfig {
    actual val enabled: Boolean = try {
        js("typeof __VISUALIZER__ !== 'undefined' && __VISUALIZER__ === true") as Boolean
    } catch (_: dynamic) {
        false
    }
}

