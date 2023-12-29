package com.amarcolini.joos.util

import kotlin.jvm.JvmStatic
import kotlin.time.TimeSource

/**
 * Clock interface with nanosecond precision and no guarantee about its origin (that is, this is only suited for
 * measuring relative/elapsed time).
 */
// interface breaks companion object JVM static modifier
interface NanoClock {
    companion object {
        /**
         * Returns a [NanoClock] backed by the target platform.
         */
        @JvmStatic
        val system: NanoClock = object : NanoClock {
            private val start = TimeSource.Monotonic.markNow()

            override fun seconds(): Double =
                (TimeSource.Monotonic.markNow() - start).inWholeNanoseconds / 1e9
        }
    }

    /**
     * Returns the number of seconds since an arbitrary (yet consistent) origin.
     */
    fun seconds(): Double
}