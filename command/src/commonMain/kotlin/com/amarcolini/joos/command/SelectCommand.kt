package com.amarcolini.joos.command

import kotlin.jvm.JvmOverloads

/**
 * A command that runs the specified command every time it is scheduled.
 */
class SelectCommand @JvmOverloads constructor(
    private val command: () -> Command,
    override val requirements: Set<Component> = emptySet()
) : Command() {

    constructor(
        command: () -> Command,
        vararg requirements: Component
    ) : this(command, requirements.toSet())

    private var selected: Command = empty()

    override fun init() {
        selected = command()
        selected.init()
    }

    override fun execute() {
        selected.execute()
    }

    override fun end(interrupted: Boolean) {
        selected.end(interrupted)
    }

    override val isInterruptable: Boolean
        get() = selected.isInterruptable

    override fun isFinished(): Boolean = selected.isFinished()
}