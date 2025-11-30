package co.hondaya.kotlin_wasm_sample.visualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class VisualizerCollectors(
    private var intervalMs: Long = 500L,
    capacity: Int = 5_000,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var job: Job? = null

    private val wasmMemory = RingBuffer(capacity)
    private val fpsSeries = RingBuffer(capacity)
    private val longTaskSeries = RingBuffer(capacity)
    private val jsHeapSeries = RingBuffer(capacity)

    private val marks = mutableListOf<Mark>()
    var marksSnapshot by mutableStateOf<List<Mark>>(emptyList())
        private set

    private var startedAt: Long = nowMillis()

    var wasmMemorySnapshot by mutableStateOf<List<Pair<Long, Double>>>(emptyList())
        private set
    var fpsSnapshot by mutableStateOf<List<Pair<Long, Double>>>(emptyList())
        private set
    var longTaskSnapshot by mutableStateOf<List<Pair<Long, Double>>>(emptyList())
        private set
    var jsHeapSnapshot by mutableStateOf<List<Pair<Long, Double>>>(emptyList())
        private set

    fun start() {
        if (job != null) return
        job = scope.launch {
            // FPS via requestAnimationFrame deltas
            launch { trackFps() }
            // Long task observer / stall fallback
            launch { observeLongTasks() }
            // Poll memory and publish snapshots periodically
            while (isActive) {
                sampleWasmMemory()
                sampleJsHeap()
                publish()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun setInterval(ms: Long) {
        intervalMs = ms.coerceIn(100L, 2000L)
    }

    fun clear() {
        wasmMemory.clear(); fpsSeries.clear(); longTaskSeries.clear(); jsHeapSeries.clear();
        marks.clear()
        publish()
    }

    fun loadSession(session: Session) {
        clear()
        session.series[SeriesKeys.WasmMemoryBytes]?.forEach { wasmMemory.add(it.first, it.second) }
        session.series[SeriesKeys.JsHeapUsedBytes]?.forEach { jsHeapSeries.add(it.first, it.second) }
        session.series[SeriesKeys.LongTaskMs]?.forEach { longTaskSeries.add(it.first, it.second) }
        session.series[SeriesKeys.FpsEstimate]?.forEach { fpsSeries.add(it.first, it.second) }
        marks.clear(); marks.addAll(session.marks)
        publish()
    }

    private fun publish() {
        wasmMemorySnapshot = wasmMemory.snapshot()
        fpsSnapshot = fpsSeries.snapshot()
        longTaskSnapshot = longTaskSeries.snapshot()
        jsHeapSnapshot = jsHeapSeries.snapshot()
        marksSnapshot = marks.toList()
    }

    private fun sampleWasmMemory() {
        val now = nowMillis()
        val bytes = currentWasmMemoryBytes()
        if (bytes != null) {
            wasmMemory.add(now, bytes.toDouble())
        }
    }

    private fun sampleJsHeap() {
        val now = nowMillis()
        val bytes = currentJsHeapBytes()
        if (bytes != null) {
            jsHeapSeries.add(now, bytes.toDouble())
        }
    }

    private suspend fun trackFps() {
        var lastTs = rafNowMillis()
        while (isActive) {
            val t = nextAnimationFrame()
            val dt = max(1.0, t - lastTs)
            lastTs = t
            val fps = 1000.0 / dt
            fpsSeries.add(nowMillis(), fps)
        }
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun currentWasmMemoryBytes(): Long? {
    return try {
        val has = js("typeof wasmMemory !== 'undefined' && wasmMemory && wasmMemory.buffer && wasmMemory.buffer.byteLength")
        if (has == undefined) null else (has as Double).toLong()
    } catch (_: dynamic) {
        null
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun currentJsHeapBytes(): Long? {
    return try {
        val mem = js("(typeof performance !== 'undefined' && performance && performance.memory && performance.memory.usedJSHeapSize) || undefined")
        if (mem == undefined) null else (mem as Double).toLong()
    } catch (_: dynamic) {
        null
    }
}

@Suppress("UnsafeCastFromDynamic")
private suspend fun nextAnimationFrame(): Double = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
    val cb = { t: dynamic -> if (cont.isActive) cont.resume(t as Double, null) }
    js("requestAnimationFrame")(cb)
}

@Suppress("UnsafeCastFromDynamic")
private fun rafNowMillis(): Double {
    return try {
        js("performance.now()") as Double
    } catch (_: dynamic) {
        0.0
    }
}

private fun nowMillis(): Long = kotlin.system.getTimeMillis()

@Suppress("UnsafeCastFromDynamic")
private suspend fun VisualizerCollectors.observeLongTasks() {
    // Try PerformanceObserver longtask; fallback to RAF based stall detection
    val hasPO = try {
        js("typeof PerformanceObserver !== 'undefined'") as Boolean
    } catch (_: dynamic) { false }
    if (hasPO) {
        try {
            val self = this
            val obs = js("new PerformanceObserver(function(list){ try { var entries = list.getEntries(); for (var i=0;i<entries.length;i++){ var e = entries[i]; if(e.duration){ self._recordLongTask(e.duration); } } } catch(e){} });")
            obs.observe(js("{type:'longtask', buffered:true}"))
        } catch (_: dynamic) {
            // ignore
        }
    } else {
        // Fallback: detect stalls via RAF delta outliers (>80ms)
        var lastTs = rafNowMillis()
        while (kotlinx.coroutines.isActive) {
            val t = nextAnimationFrame()
            val dt = t - lastTs
            lastTs = t
            if (dt > 80.0) {
                longTaskSeries.add(nowMillis(), dt)
            }
        }
    }
}

// Called from JS observer above via dynamic bridge
private fun VisualizerCollectors._recordLongTask(duration: dynamic) {
    val dur = try { (duration as Double) } catch (_: dynamic) { return }
    longTaskSeries.add(nowMillis(), dur)
}

fun VisualizerCollectors.addMark(label: String) {
    marks.add(Mark(nowMillis(), label))
    publish()
}
