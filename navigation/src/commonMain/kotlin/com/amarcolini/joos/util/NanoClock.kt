package com.amarcolini.joos.util

import kotlin.jvm.JvmStatic

/**
 * Clock interface with millisecond precision and no guarantee about its origin (that is, this is only suited for
 * measuring relative/elapsed time).
 */
// interface breaks companion object JVM static modifier
expect abstract class NanoClock {

    companion object {
        /**
         * Returns a [NanoClock] backed by the target platform.
         */
        @JvmStatic
        fun system(): NanoClock
    }

    /**
     * Returns the number of seconds since an arbitrary (yet consistent) origin.
     */
    abstract fun seconds(): Double
}