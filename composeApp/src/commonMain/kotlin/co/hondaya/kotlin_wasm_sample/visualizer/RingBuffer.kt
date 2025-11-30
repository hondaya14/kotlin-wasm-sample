package co.hondaya.kotlin_wasm_sample.visualizer

class RingBuffer(capacity: Int) {
    private val times = LongArray(capacity)
    private val values = DoubleArray(capacity)
    private var start = 0
    private var size = 0

    val capacity: Int = capacity

    fun add(timeMillis: Long, value: Double) {
        val idx = (start + size) % capacity
        if (size < capacity) {
            size++
        } else {
            start = (start + 1) % capacity
        }
        times[idx] = timeMillis
        values[idx] = value
    }

    fun snapshot(): List<Pair<Long, Double>> {
        val result = ArrayList<Pair<Long, Double>>(size)
        for (i in 0 until size) {
            val idx = (start + i) % capacity
            result.add(times[idx] to values[idx])
        }
        return result
    }

    fun clear() {
        start = 0
        size = 0
    }
}

