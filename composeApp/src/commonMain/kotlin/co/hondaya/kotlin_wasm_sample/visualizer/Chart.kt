package co.hondaya.kotlin_wasm_sample.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    title: String,
    data: List<Pair<Long, Double>>,
    color: Color = MaterialTheme.colorScheme.primary,
    unitLabel: (Double) -> String = { it.toString() },
    minHeight: Int = 120,
) {
    var hoverX by remember { mutableStateOf<Float?>(null) }
    var hoverText by remember { mutableStateOf<String?>(null) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(minHeight.dp)
            .onPointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.firstOrNull()?.position
                hoverX = pos?.x
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoverX = null; hoverText = null
            }
    ) {
        if (data.isEmpty()) {
            Text("$title: no data")
            return@Box
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(minHeight.dp)
        ) {
            val times = data.map { it.first }
            val values = data.map { it.second }
            val tMin = times.first()
            val tMax = times.last().coerceAtLeast(tMin + 1)
            val vMin = values.minOrNull() ?: 0.0
            val vMax = values.maxOrNull()?.coerceAtLeast(vMin + 1e-9) ?: (vMin + 1.0)

            val w = size.width
            val h = size.height

            fun x(t: Long): Float = ((t - tMin).toFloat() / (tMax - tMin).toFloat()) * w
            fun y(v: Double): Float {
                val norm = ((v - vMin) / (vMax - vMin))
                return (h - (norm * h)).toFloat()
            }

            // Title
            drawContext.canvas.nativeCanvas.apply {
                // very basic title placement via skip; keep chart minimal
            }

            val path = Path()
            path.moveTo(x(times.first()), y(values.first()))
            for (i in 1 until data.size) {
                path.lineTo(x(times[i]), y(values[i]))
            }
            drawPath(path = path, color = color)

            // Draw last value label in corner
            val label = "$title: ${unitLabel(values.last())}"
            drawContext.canvas.nativeCanvas.apply {
                // Let Compose Text outside for simplicity
            }

            // Hover marker
            hoverX?.let { hx ->
                // Map hover x back to time and find nearest index
                val tHover = (hx / w) * (tMax - tMin).toFloat() + tMin.toFloat()
                // linear scan (data limited by ring window); okay for small window
                var nearestIdx = 0
                var best = Float.MAX_VALUE
                for (i in times.indices) {
                    val dx = kotlin.math.abs(x(times[i]) - hx)
                    if (dx < best) { best = dx; nearestIdx = i }
                }
                val px = x(times[nearestIdx])
                val py = y(values[nearestIdx])
                // Vertical guide
                drawLine(Color.LightGray, Offset(px, 0f), Offset(px, h), strokeWidth = 1f)
                // Point marker
                drawCircle(color = color, radius = 3f, center = Offset(px, py))
                hoverText = unitLabel(values[nearestIdx])
            }
        }
        // Compose text overlay for title/value
        val labelValue = hoverText ?: unitLabel(data.last().second)
        Text("$title: $labelValue")
    }
}
