package co.hondaya.kotlin_wasm_sample.visualizer

data class Mark(val t: Long, val label: String)

data class Session(
    val version: Int = 1,
    val startedAt: Long,
    val intervalMs: Int,
    val series: Map<String, List<Pair<Long, Double>>>,
    val marks: List<Mark> = emptyList(),
)

object SeriesKeys {
    const val WasmMemoryBytes: String = "wasmMemoryBytes"
    const val JsHeapUsedBytes: String = "jsHeapUsedBytes"
    const val LongTaskMs: String = "longTaskMs"
    const val FpsEstimate: String = "fpsEstimate"
}

