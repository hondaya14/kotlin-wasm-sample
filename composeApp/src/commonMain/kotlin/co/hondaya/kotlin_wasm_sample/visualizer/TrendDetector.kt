package co.hondaya.kotlin_wasm_sample.visualizer

import kotlin.math.abs

object TrendDetector {
    // Returns true if slope exceeds threshold over the time window
    fun upwardTrend(
        series: List<Pair<Long, Double>>,
        windowMs: Long,
        minPoints: Int = 5,
        slopeThresholdPerSec: Double = 10_000.0, // bytes/sec default
    ): Boolean {
        if (series.size < minPoints) return false
        val tail = tailWindow(series, windowMs)
        if (tail.size < minPoints) return false
        val (slope, _) = linearFit(tail)
        return slope * 1000.0 >= slopeThresholdPerSec
    }

    // Returns true if the latest value spikes above mean by given fraction
    fun suddenSpike(
        series: List<Pair<Long, Double>>,
        windowMs: Long,
        minPoints: Int = 5,
        spikeFraction: Double = 0.2,
    ): Boolean {
        if (series.size < minPoints) return false
        val tail = tailWindow(series, windowMs)
        if (tail.size < minPoints) return false
        val values = tail.map { it.second }
        val mean = values.average()
        val last = values.last()
        return last >= mean * (1.0 + spikeFraction)
    }

    private fun tailWindow(series: List<Pair<Long, Double>>, windowMs: Long): List<Pair<Long, Double>> {
        val end = series.last().first
        val start = end - windowMs
        val idx = series.indexOfFirst { it.first >= start }.let { if (it < 0) 0 else it }
        return series.subList(idx, series.size)
    }

    private fun linearFit(series: List<Pair<Long, Double>>): Pair<Double, Double> {
        if (series.isEmpty()) return 0.0 to 0.0
        val xs = series.map { it.first.toDouble() }
        val ys = series.map { it.second }
        val n = xs.size
        val meanX = xs.average()
        val meanY = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val dx = xs[i] - meanX
            num += dx * (ys[i] - meanY)
            den += dx * dx
        }
        val slope = if (den == 0.0) 0.0 else num / den
        val intercept = meanY - slope * meanX
        return slope to intercept
    }
}

