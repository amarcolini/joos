package com.amarcolini.joos.command

import com.amarcolini.joos.util.NanoClock

/**
 * A command that waits the specified duration before finishing.
 *
 * @param duration the duration in seconds
 */
class WaitCommand @JvmOverloads constructor(var duration: Double, private val clock: NanoClock = NanoClock.system) :
    Command() {
    private var start = clock.seconds()

    override fun init() {
        start = clock.seconds()
    }

    override fun isFinished() = clock.seconds() - start >= duration
}