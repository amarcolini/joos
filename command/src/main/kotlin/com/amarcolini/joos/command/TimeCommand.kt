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
    private val clock: NanoClock = NanoClock.system()
) : Command() {
    constructor(execute: BiFunction<Double, Double, Boolean>) : this(execute, NanoClock.system())

    private var start: Double? = null
    private var lastUpdate: Double? = null

    override fun init() {
        start = null
        lastUpdate = null
    }

    private var isFinished = false
    override fun execute() {
        val start = start ?: clock.seconds().also { start = it }
        val t = clock.seconds() - start
        isFinished = execute.apply(t, t - (lastUpdate ?: t))
        lastUpdate = t
    }

    override fun isFinished(): Boolean = isFinished
}