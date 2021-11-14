package com.amarcolini.joos.command

import java.util.function.Supplier

/**
 * A command that runs the specified command every time it is scheduled.
 */
class SelectCommand @JvmOverloads constructor(
    private val command: Supplier<Command>,
    override val requirements: Set<Component> = emptySet()
) : Command() {

    constructor(
        command: Supplier<Command>,
        vararg requirements: Component
    ) : this(command, requirements.toSet())

    private var selected: Command = emptyCommand()

    override fun init() {
        selected = command.get()
    }

    override fun execute() {
        selected.execute()
    }

    override fun end(interrupted: Boolean) {
        selected.end(interrupted)
    }

    override val isInterruptable: Boolean
        get() = selected.isInterruptable

    override fun isFinished(): Boolean = selected.isInterruptable
}