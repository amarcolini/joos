package com.griffinrobotics.lib.command

/**
 * A command that runs commands in parallel (Runs them all simultaneously until they finish).
 */
class ParallelCommand @JvmOverloads constructor(
    override val isInterruptable: Boolean = true,
    vararg commands: Command
) : Command() {
    override val requirements = HashSet<Component>()
    val commands = HashMap<Command, Boolean>()

    init {
        for (command in commands) {
            if ((requirements intersect command.requirements).isNotEmpty()) throw IllegalArgumentException(
                "Multiple commands in a parallel group cannot require the same components"
            )
            requirements.addAll(command.requirements)
            this.commands[command] = false
        }
    }

    override fun init() {
        commands.mapValues { false }
        commands.forEach { it.key.init() }
    }

    override fun execute() {
        commands.filterValues { !it }.forEach { it.key.execute() }
        commands.filterKeys { it.isFinished() }.mapValues { true }.forEach { it.key.end(false) }
    }

    override fun end(interrupted: Boolean) {
        if (interrupted) commands.filterKeys { !it.isFinished() }.forEach { it.key.end(true) }
    }

    override fun isFinished() = commands.values.all { it }
}
