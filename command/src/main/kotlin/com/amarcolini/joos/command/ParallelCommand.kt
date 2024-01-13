package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel until they all finish.
 */
class ParallelCommand(
    isInterruptable: Boolean = true,
    vararg commands: Command
) : CommandGroup(true, commands, isInterruptable) {
    constructor(vararg commands: Command) : this(!commands.any { !it.isInterruptable }, *commands)

    private val commands = LinkedHashMap<Command, Boolean>()
    override fun add(command: Command) {
        commands += command to false
        isInterruptable = isInterruptable && command.isInterruptable
    }

    init {
        for (command in commands) {
            this.commands[command] = false
        }
    }

    override fun init() {
        for (command in commands) {
            command.setValue(false)
            command.key.init()
        }
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