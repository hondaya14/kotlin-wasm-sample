package co.hondaya.kotlin_wasm_sample.visualizer

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrendDetectorTest {
    @Test
    fun detectsUpwardTrend() {
        val base = 1_000_000.0
        val series = (0..20).map { i -> (i * 500L) to (base + i * 50_000.0) }
        assertTrue(TrendDetector.upwardTrend(series, windowMs = 10_000, slopeThresholdPerSec = 20_000.0))
    }

    @Test
    fun detectsSpike() {
        val series = (0..20).map { i -> (i * 500L) to (100.0 + i) }.toMutableList()
        series[series.lastIndex] = series.last().first to 1000.0
        assertTrue(TrendDetector.suddenSpike(series, windowMs = 10_000, spikeFraction = 0.2))
    }

    @Test
    fun noFalsePositive() {
        val series = (0..20).map { i -> (i * 500L) to (100.0 + (i % 5)) }
        assertFalse(TrendDetector.upwardTrend(series, windowMs = 10_000))
        assertFalse(TrendDetector.suddenSpike(series, windowMs = 10_000))
    }
}

