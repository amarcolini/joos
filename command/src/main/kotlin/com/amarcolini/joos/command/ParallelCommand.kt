package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel (Runs them all simultaneously until they finish).
 */
class ParallelCommand @JvmOverloads constructor(
    override val isInterruptable: Boolean = true,
    vararg commands: Command
) : CommandGroup(commands = commands) {
    val commands = LinkedHashMap<Command, Boolean>()

    init {
        for (command in commands) {
            this.commands[command] = false
        }
    }

    override fun init() {
        commands.mapValues { false }
        commands.forEach { it.key.init() }
    }

    override fun execute() {
        commands.filterValues { !it }.forEach { it.key.execute() }
        commands.filterKeys { it.isFinished() }.forEach {
            it.key.end(false)
            commands[it.key] = true
        }
    }

    override fun end(interrupted: Boolean) {
        if (interrupted) commands.filterKeys { !it.isFinished() }.forEach { it.key.end(true) }
        super.end(interrupted)
    }

    override fun isFinished() = commands.values.all { it }
}
