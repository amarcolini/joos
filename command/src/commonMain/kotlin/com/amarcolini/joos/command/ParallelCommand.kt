package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel until they all finish.
 */
class ParallelCommand(
    isInterruptable: Boolean,
    commands: List<Command>
) : CommandGroup(commands) {
    constructor(isInterruptable: Boolean, vararg commands: Command) : this(isInterruptable, commands.toList())
    constructor(vararg commands: Command) : this(!commands.any { !it.isInterruptable }, *commands)

    init {
        this.isInterruptable = isInterruptable
    }

    override fun add(command: Command) = ParallelCommand(isInterruptable, commands + command)
    override val requirements: Set<Component> = intersectRequirements(commands)

    override fun internalInit() {
        scheduleQueue += commands
    }

    override fun isFinished() = scheduledCommands.isEmpty()
}