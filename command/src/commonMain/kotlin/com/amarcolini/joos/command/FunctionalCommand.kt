package com.amarcolini.joos.command

import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * A command whose properties can be defined externally. Useful for making simple inline commands or combining commands together.
 */
class FunctionalCommand @JvmOverloads constructor(
    @JvmField var initFunction: () -> Unit = {},
    @JvmField var executeFunction: () -> Unit = {},
    @JvmField var endFunction: (Boolean) -> Unit = {},
    @JvmField var isFinishedFunction: () -> Boolean = { false },
    override var isInterruptable: Boolean = true,
    override var requirements: Set<Component> = emptySet()
) : Command() {
    constructor(
        init: () -> Unit = {},
        execute: () -> Unit = {},
        end: (Boolean) -> Unit = {},
        isFinished: () -> Boolean = { false },
        isInterruptable: Boolean = true,
        vararg requirements: Component
    ) : this(init, execute, end, isFinished, isInterruptable, requirements.toSet())

    override fun isFinished(): Boolean = isFinishedFunction()
    override fun init() = initFunction()
    override fun execute() = executeFunction()
    override fun end(interrupted: Boolean) = endFunction(interrupted)
}