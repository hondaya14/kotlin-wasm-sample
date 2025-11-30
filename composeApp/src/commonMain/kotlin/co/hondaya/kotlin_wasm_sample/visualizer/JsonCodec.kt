package co.hondaya.kotlin_wasm_sample.visualizer

import kotlin.math.roundToInt

object JsonCodec {
    fun export(
        startedAt: Long,
        intervalMs: Long,
        wasm: List<Pair<Long, Double>>,
        jsHeap: List<Pair<Long, Double>>,
        longTasks: List<Pair<Long, Double>>,
        fps: List<Pair<Long, Double>>,
        marks: List<Mark>,
    ): String {
        // Minimal JSON construction to avoid extra deps
        fun pairs(arr: List<Pair<Long, Double>>): String = arr.joinToString(prefix = "[", postfix = "]") {
            "[${it.first},${it.second}]"
        }
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"version\":1,")
        sb.append("\"startedAt\":$startedAt,")
        sb.append("\"intervalMs\":${intervalMs.roundToInt()},")
        sb.append("\"series\":{")
        sb.append("\"${SeriesKeys.WasmMemoryBytes}\":${pairs(wasm)},")
        sb.append("\"${SeriesKeys.JsHeapUsedBytes}\":${pairs(jsHeap)},")
        sb.append("\"${SeriesKeys.LongTaskMs}\":${pairs(longTasks)},")
        sb.append("\"${SeriesKeys.FpsEstimate}\":${pairs(fps)}")
        sb.append("},")
        val marksJson = marks.joinToString(prefix = "[", postfix = "]") {
            "{\"t\":${it.t},\"label\":\"${escape(it.label)}\"}"
        }
        sb.append("\"marks\":$marksJson")
        sb.append("}")
        return sb.toString()
    }

    fun import(json: String): Session? {
        // Very small, permissive parser using Kotlin stdlib; not robust but sufficient for dev tool
        return try {
            val cleaned = json.trim()
            val version = cleaned.findNumberAfter("\"version\"")?.toInt() ?: 1
            val startedAt = cleaned.findNumberAfter("\"startedAt\"")?.toLong() ?: 0L
            val intervalMs = cleaned.findNumberAfter("\"intervalMs\"")?.toDouble()?.roundToInt() ?: 500
            fun parsePairs(key: String): List<Pair<Long, Double>> =
                cleaned.findArrayPairsAfter("\"$key\"")

            val series = mapOf(
                SeriesKeys.WasmMemoryBytes to parsePairs(SeriesKeys.WasmMemoryBytes),
                SeriesKeys.JsHeapUsedBytes to parsePairs(SeriesKeys.JsHeapUsedBytes),
                SeriesKeys.LongTaskMs to parsePairs(SeriesKeys.LongTaskMs),
                SeriesKeys.FpsEstimate to parsePairs(SeriesKeys.FpsEstimate),
            )
            val marks = cleaned.findMarksArray()
            Session(version, startedAt, intervalMs, series, marks)
        } catch (_: Throwable) {
            null
        }
    }

    private fun String.findNumberAfter(key: String): Double? {
        val idx = indexOf(key)
        if (idx < 0) return null
        val colon = indexOf(':', idx)
        if (colon < 0) return null
        val tail = substring(colon + 1)
        val m = Regex("-?[0-9]+(\\.[0-9]+)?").find(tail) ?: return null
        return m.value.toDouble()
    }

    private fun String.findArrayPairsAfter(key: String): List<Pair<Long, Double>> {
        val idx = indexOf(key)
        if (idx < 0) return emptyList()
        val start = indexOf('[', idx)
        var depth = 0
        var i = start
        while (i < length) {
            val c = this[i]
            if (c == '[') depth++
            if (c == ']') {
                depth--
                if (depth == 0) break
            }
            i++
        }
        if (start < 0 || i >= length) return emptyList()
        val content = substring(start + 1, i)
        if (content.isBlank()) return emptyList()
        return content.split("],").mapNotNull { seg ->
            val nums = Regex("-?[0-9]+(\\.[0-9]+)?").findAll(seg).map { it.value }.toList()
            if (nums.size >= 2) (nums[0].toLong() to nums[1].toDouble()) else null
        }
    }

    private fun String.findMarksArray(): List<Mark> {
        val key = "\"marks\""
        val idx = indexOf(key)
        if (idx < 0) return emptyList()
        val start = indexOf('[', idx)
        val end = indexOf(']', start + 1)
        if (start < 0 || end < 0) return emptyList()
        val content = substring(start + 1, end)
        if (content.isBlank()) return emptyList()
        return content.split("},").mapNotNull { seg ->
            val t = Regex("\\\"t\\\"\\s*:\\s*([0-9]+)").find(seg)?.groupValues?.getOrNull(1)?.toLongOrNull()
            val label = Regex("\\\"label\\\"\\s*:\\s*\\\"(.*?)\\\"").find(seg)?.groupValues?.getOrNull(1)?.let { unescape(it) }
            if (t != null && label != null) Mark(t, label) else null
        }
    }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun unescape(s: String): String = s.replace("\\\"", "\"").replace("\\\\", "\\")
}

