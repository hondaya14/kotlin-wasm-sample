package co.hondaya.kotlin_wasm_sample.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VisualizerPanel() {
    if (!VisualizerConfig.enabled) return
    val collectors = remember { VisualizerCollectors() }
    DisposableEffect(Unit) {
        collectors.start()
        onDispose { collectors.stop() }
    }
    var windowSec by remember { mutableStateOf(30) }
    var interval by remember { mutableStateOf(500) }
    var offsetSec by remember { mutableStateOf(0) } // simple pan offset from right edge
    var showJs by remember { mutableStateOf(true) }
    var markText by remember { mutableStateOf("") }
    var hasLocal by remember { mutableStateOf(hasLocalSession()) }
    Surface(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
        Column(Modifier.fillMaxWidth()) {
            // Controls
            Row(Modifier.fillMaxWidth()) {
                TextField(
                    value = windowSec.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { windowSec = it.coerceIn(5, 600) } },
                    label = { Text("Window (s)") },
                )
                TextField(
                    value = interval.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { interval = it.coerceIn(100, 2000); collectors.setInterval(interval.toLong()) } },
                    label = { Text("Interval (ms)") },
                )
                TextField(
                    value = offsetSec.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { offsetSec = it.coerceAtLeast(0) } },
                    label = { Text("Pan offset (s)") },
                )
                Spacer(Modifier.height(8.dp))
                ElevatedButton(onClick = {
                    // Export
                    val json = JsonCodec.export(
                        startedAt = kotlin.system.getTimeMillis(),
                        intervalMs = 500,
                        wasm = collectors.wasmMemorySnapshot,
                        jsHeap = collectors.jsHeapSnapshot,
                        longTasks = collectors.longTaskSnapshot,
                        fps = collectors.fpsSnapshot,
                        marks = collectors.marksSnapshot,
                    )
                    triggerDownload(json, "visualizer-session.json")
                }) { Text("Export") }
                ElevatedButton(onClick = {
                    val json = JsonCodec.export(
                        startedAt = kotlin.system.getTimeMillis(),
                        intervalMs = 500,
                        wasm = collectors.wasmMemorySnapshot,
                        jsHeap = collectors.jsHeapSnapshot,
                        longTasks = collectors.longTaskSnapshot,
                        fps = collectors.fpsSnapshot,
                        marks = collectors.marksSnapshot,
                    )
                    saveLocal(json); hasLocal = true
                }) { Text("Save Local") }
                if (hasLocal) {
                    ElevatedButton(onClick = {
                        loadLocal()?.let { s -> JsonCodec.import(s)?.let { collectors.loadSession(it) } }
                    }) { Text("Restore Local") }
                    ElevatedButton(onClick = { clearLocal(); hasLocal = false }) { Text("Clear Local") }
                }
                ElevatedButton(onClick = { openFileAndImport(collectors) }) { Text("Import") }
                ElevatedButton(onClick = { /* clear */
                    // Simple clear via stopping/starting
                    collectors.stop(); collectors.start()
                }) { Text("Clear") }
                ElevatedButton(onClick = { showJs = !showJs }) { Text(if (showJs) "Hide JS" else "Show JS") }
                ElevatedButton(onClick = { collectors.start() }) { Text("Start") }
                ElevatedButton(onClick = { collectors.stop() }) { Text("Stop") }
            }

            val windowMs = windowSec * 1000L
            val panMs = offsetSec * 1000L
            val wasmData = windowSlice(collectors.wasmMemorySnapshot, windowMs, panMs).map { it.first to (it.second / (1024.0 * 1024.0)) }
            val jsData = windowSlice(collectors.jsHeapSnapshot, windowMs, panMs).map { it.first to (it.second / (1024.0 * 1024.0)) }

            val upward = TrendDetector.upwardTrend(collectors.wasmMemorySnapshot, windowMs = windowMs, slopeThresholdPerSec = 200_000.0)
            val spike = TrendDetector.suddenSpike(collectors.wasmMemorySnapshot, windowMs = windowMs, spikeFraction = 0.2)
            Card { LineChart(
                title = if (showJs && jsData.isNotEmpty()) "Memory (MB) — Wasm + JS" else "Wasm Memory (MB)",
                data = if (showJs && jsData.isNotEmpty()) mergeSeries(wasmData, jsData) else wasmData,
                unitLabel = { String.format("%1$.2f", it) }
            ) }
            if (upward) Text("Alert: upward memory trend detected")
            if (spike) Text("Alert: sudden memory spike detected")
            Spacer(Modifier.height(8.dp))
            Card { LineChart(
                title = "FPS", data = windowSlice(collectors.fpsSnapshot, windowMs, panMs),
                unitLabel = { String.format("%1$.1f", it) }
            ) }
            Spacer(Modifier.height(8.dp))
            Card {
                val longTasks = windowSlice(collectors.longTaskSnapshot, windowMs, panMs)
                Text("Long Tasks: ${longTasks.size} events in ${windowSec}s window")
            }

            // Add mark UI (simple)
            Row(Modifier.fillMaxWidth()) {
                TextField(value = markText, onValueChange = { markText = it }, label = { Text("Add mark") })
                ElevatedButton(onClick = { if (markText.isNotBlank()) { collectors.addMark(markText); markText = "" } }) { Text("Add") }
            }
        }
    }
}

private fun tailWindow(series: List<Pair<Long, Double>>, windowMs: Long): List<Pair<Long, Double>> {
    if (series.isEmpty()) return series
    val end = series.last().first
    val start = end - windowMs
    val idx = series.indexOfFirst { it.first >= start }.let { if (it < 0) 0 else it }
    return series.subList(idx, series.size)
}

private fun windowSlice(series: List<Pair<Long, Double>>, windowMs: Long, panOffsetMs: Long): List<Pair<Long, Double>> {
    if (series.isEmpty()) return series
    val endAll = series.last().first
    val end = (endAll - panOffsetMs).coerceAtLeast(series.first().first)
    val start = (end - windowMs).coerceAtLeast(series.first().first)
    val startIdx = series.indexOfFirst { it.first >= start }.let { if (it < 0) 0 else it }
    val endIdx = series.indexOfLast { it.first <= end }.let { if (it < 0) series.lastIndex else it }
    return series.subList(startIdx, endIdx + 1)
}

// Very simple merge: interleave two series by time
private fun mergeSeries(a: List<Pair<Long, Double>>, b: List<Pair<Long, Double>>): List<Pair<Long, Double>> {
    return (a + b).sortedBy { it.first }
}

@Suppress("UnsafeCastFromDynamic")
private fun triggerDownload(content: String, filename: String) {
    try {
        val blob = js("new Blob([content], {type:'application/json'})")
        val url = js("URL.createObjectURL(blob)")
        val a = js("document.createElement('a')")
        a.href = url
        a.download = filename
        js("document.body.appendChild(a)")
        a.click()
        js("document.body.removeChild(a); URL.revokeObjectURL(url)")
    } catch (_: dynamic) {
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun openFileAndImport(collectors: VisualizerCollectors) {
    try {
        val input = js("document.createElement('input')")
        input.type = "file"
        input.accept = ".json"
        input.onchange = { _: dynamic ->
            val file = input.files[0]
            val reader = js("new FileReader()")
            reader.onload = { _: dynamic ->
                val text = reader.result as String
                val session = JsonCodec.import(text)
                if (session != null) collectors.loadSession(session)
            }
            reader.readAsText(file)
        }
        js("document.body.appendChild(input)")
        input.click()
        js("document.body.removeChild(input)")
    } catch (_: dynamic) {}
}

@Suppress("UnsafeCastFromDynamic")
private fun saveLocal(content: String) {
    try { js("localStorage.setItem('visualizer_session', content)") } catch (_: dynamic) {}
}

@Suppress("UnsafeCastFromDynamic")
private fun loadLocal(): String? = try {
    val v = js("localStorage.getItem('visualizer_session')")
    if (v == undefined) null else (v as String?)
} catch (_: dynamic) { null }

@Suppress("UnsafeCastFromDynamic")
private fun hasLocalSession(): Boolean = try {
    val v = js("localStorage.getItem('visualizer_session')")
    v != undefined && v != null
} catch (_: dynamic) { false }

@Suppress("UnsafeCastFromDynamic")
private fun clearLocal() {
    try { js("localStorage.removeItem('visualizer_session')") } catch (_: dynamic) {}
}
