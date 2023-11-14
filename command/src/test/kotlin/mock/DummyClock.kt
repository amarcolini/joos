package mock

import com.amarcolini.joos.util.NanoClock

object DummyClock : NanoClock() {
    private var mockSeconds = 0.0
    private var isReal = false

    fun enableRealtime() {
        isReal = true
    }

    fun disableRealtime() {
        isReal = false
    }

    override fun seconds(): Double = if (isReal) system().seconds() else mockSeconds

    fun step(duration: Double) {
        mockSeconds += duration
    }
}