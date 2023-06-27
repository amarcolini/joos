package com.amarcolini.joos.util

import kotlin.js.Date

/**
 * Clock interface with millisecond precision and no guarantee about its origin (that is, this is only suited for
 * measuring relative/elapsed time).
 */
actual abstract class NanoClock {
    actual companion object {
        /**
         * Returns a [NanoClock] backed by [window].
         */
        actual fun system(): NanoClock = object : NanoClock() {
            override fun seconds(): Double = Date.now() / 1000
        }
    }

    /**
     * Returns the number of seconds since an arbitrary (yet consistent) origin.
     */
    actual abstract fun seconds(): Double
}