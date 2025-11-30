package co.hondaya.kotlin_wasm_sample.visualizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JsonCodecTest {
    @Test
    fun roundTrip() {
        val wasm = listOf(1L to 10.0, 2L to 20.0)
        val js = listOf(1L to 5.0)
        val lt = listOf(2L to 80.0)
        val fps = listOf(2L to 60.0)
        val marks = listOf(Mark(3, "hello"))
        val json = JsonCodec.export(0, 500, wasm, js, lt, fps, marks)
        val session = JsonCodec.import(json)
        assertNotNull(session)
        assertEquals(wasm, session!!.series[SeriesKeys.WasmMemoryBytes])
        assertEquals(js, session.series[SeriesKeys.JsHeapUsedBytes])
        assertEquals(1, session.marks.size)
        assertEquals("hello", session.marks.first().label)
    }
}

