package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel until one of them finishes.
 */
class RaceCommand(
    override var isInterruptable: Boolean,
    commands: List<Command>
) : CommandGroup(commands) {
    constructor(isInterruptable: Boolean, vararg commands: Command) : this(isInterruptable, commands.toList())
    constructor(vararg commands: Command) : this(!commands.any { !it.isInterruptable }, *commands)

    override fun add(command: Command) = RaceCommand(isInterruptable, commands + command)
    override val requirements: Set<Component> = intersectRequirements(commands)

    override fun internalInit() {
        scheduleQueue += commands
    }

    override fun isFinished() = scheduledCommands.size < commands.size
}