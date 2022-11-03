package com.amarcolini.joos.util

/**
 * Clock interface with nanosecond precision and no guarantee about its origin (that is, this is only suited for
 * measuring relative/elapsed time).
 */
// interface breaks companion object JVM static modifier
actual abstract class NanoClock {
    actual companion object {
        /**
         * Returns a [NanoClock] backed by [System.nanoTime].
         */
        @JvmStatic
        actual fun system(): NanoClock = object : NanoClock() {
            override fun seconds(): Double = System.nanoTime() / 1e9
        }
    }

    /**
     * Returns the number of seconds since an arbitrary (yet consistent) origin.
     */
    actual abstract fun seconds(): Double
}