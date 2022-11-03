package com.amarcolini.joos.util

import kotlinx.browser.window

/**
 * Clock interface with microsecond precision and no guarantee about its origin (that is, this is only suited for
 * measuring relative/elapsed time).
 */
actual abstract class NanoClock {
    actual companion object {
        /**
         * Returns a [NanoClock] backed by [window].
         */
        actual fun system(): NanoClock = object : NanoClock() {
            override fun seconds(): Double = window.performance.now() / 1000
        }
    }

    /**
     * Returns the number of seconds since an arbitrary (yet consistent) origin.
     */
    actual abstract fun seconds(): Double
}