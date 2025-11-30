package co.hondaya.kotlin_wasm_sample.visualizer

import kotlin.test.Test
import kotlin.test.assertEquals

class RingBufferTest {
    @Test
    fun ringBufferEvictsOldest() {
        val rb = RingBuffer(3)
        rb.add(1, 1.0)
        rb.add(2, 2.0)
        rb.add(3, 3.0)
        rb.add(4, 4.0) // evict first

        val snap = rb.snapshot()
        assertEquals(listOf(2L to 2.0, 3L to 3.0, 4L to 4.0), snap)
    }

    @Test
    fun ringBufferClear() {
        val rb = RingBuffer(2)
        rb.add(1, 1.0)
        rb.add(2, 2.0)
        rb.clear()
        assertEquals(emptyList(), rb.snapshot())
    }
}

