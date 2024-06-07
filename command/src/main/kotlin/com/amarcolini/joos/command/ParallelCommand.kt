package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel until they all finish.
 */
class ParallelCommand(
    override var isInterruptable: Boolean,
    commands: List<Command>
) : CommandGroup(commands) {
    constructor(isInterruptable: Boolean, vararg commands: Command) : this(isInterruptable, commands.toList())
    constructor(vararg commands: Command) : this(!commands.any { !it.isInterruptable }, *commands)

    override fun add(command: Command) = ParallelCommand(isInterruptable, commands + command)
    override val requirements: Set<Component> = intersectRequirements(commands)

    override fun internalInit() {
        scheduleQueue += commands
    }

    override fun isFinished() = scheduledCommands.isEmpty()
}