package com.amarcolini.joos.command

import com.amarcolini.joos.util.NanoClock
import java.util.function.BiConsumer
import java.util.function.BiFunction

/**
 * A time-based command.
 *
 * @param execute the execute function, accepting the time elapsed since initialization and
 * the time since last update, and returning whether the command is finished.
 */
class TimeCommand constructor(
    private val execute: BiFunction<Double, Double, Boolean>,
    private val clock: NanoClock = NanoClock.system
) : Command() {
    constructor(execute: BiFunction<Double, Double, Boolean>) : this(execute, NanoClock.system)

    private var start: Double = 0.0
    private var lastUpdate: Double = 0.0

    override fun init() {
        lastUpdate = clock.seconds()
        start = clock.seconds()
    }

    private var isFinished = false
    override fun execute() {
        val t = clock.seconds() - start
        isFinished = execute.apply(t, t - lastUpdate)
        lastUpdate = t
    }

    override fun isFinished(): Boolean = isFinished
}