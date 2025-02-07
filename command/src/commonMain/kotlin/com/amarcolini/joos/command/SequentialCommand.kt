package com.amarcolini.joos.command

/**
 * A command that runs commands in sequence.
 */
class SequentialCommand(
    isInterruptable: Boolean,
    commands: List<Command>
) : CommandGroup(commands) {
    constructor(isInterruptable: Boolean, vararg commands: Command) : this(isInterruptable, commands.toList())
    constructor(vararg commands: Command) : this(!commands.any { !it.isInterruptable }, *commands)

    init {
        this.isInterruptable = isInterruptable
    }

    override fun add(command: Command) = SequentialCommand(isInterruptable, commands + command)
    override val requirements: Set<Component> = commands.flatMap { it.requirements }.toSet()

    var index = -1
        private set
    override fun internalInit() {
        index = 0
        if (commands.isNotEmpty()) scheduleQueue += commands[index]
    }

    override fun isFinished() =
        if (scheduledCommands.isEmpty() && scheduleQueue.isEmpty()) {
            index++
            if (index > commands.lastIndex) true
            else {
                scheduleQueue += commands[index]
                false
            }
        } else false
}