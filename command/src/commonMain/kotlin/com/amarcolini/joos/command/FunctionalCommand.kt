package com.amarcolini.joos.command

import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * A command whose properties can be defined externally. Useful for making simple inline commands or combining commands together.
 */
class FunctionalCommand @JvmOverloads constructor(
    @JvmField var initFunction: Runnable = Runnable {},
    @JvmField var executeFunction: Runnable = Runnable {},
    @JvmField var endFunction: CommandEnd = CommandEnd {},
    @JvmField var isFinishedFunction: () -> Boolean = { false },
    override var isInterruptable: Boolean = true,
    override var requirements: Set<Component> = emptySet()
) : Command() {
    constructor(
        init: Runnable = Runnable {},
        execute: Runnable = Runnable {},
        end: CommandEnd = CommandEnd {},
        isFinished: () -> Boolean = { false },
        isInterruptable: Boolean = true,
        vararg requirements: Component
    ) : this(init, execute, end, isFinished, isInterruptable, requirements.toSet())

    override fun isFinished(): Boolean = isFinishedFunction()
    override fun init() = initFunction.run()
    override fun execute() = executeFunction.run()
    override fun end(interrupted: Boolean) = endFunction.end(interrupted)
}