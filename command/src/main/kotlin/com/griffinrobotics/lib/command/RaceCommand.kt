package com.griffinrobotics.lib.command

/**
 * A command that runs commands in parallel (Runs them all simultaneously until one of them finishes).
 */
class RaceCommand(
    override val isInterruptable: Boolean = true,
    private vararg val commands: Command
) : Command() {
    override val requirements = HashSet<Component>()

    init {
        for (command in commands) {
            if ((requirements intersect command.requirements).isNotEmpty()) throw IllegalArgumentException(
                "Multiple commands in a parallel group cannot require the same components"
            )
            requirements.addAll(command.requirements)
        }
    }

    override fun init() {
        commands.forEach { it.init() }
    }

    override fun execute() {
        commands.forEach { it.execute() }
        commands.filter { it.isFinished() }.forEach { it.end(false) }
    }

    override fun end(interrupted: Boolean) {
        if (!interrupted) commands.filter { !it.isFinished() }.forEach { it.end(true) }
        else commands.forEach { it.end(true) }
    }

    override fun isFinished() = commands.any { it.isFinished() }
}