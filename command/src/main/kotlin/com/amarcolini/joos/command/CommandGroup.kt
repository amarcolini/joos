package com.amarcolini.joos.command

/**
 * A type of command that uses multiple commands, joining all their requirements.
 *
 * @see ParallelCommand
 * @see RaceCommand
 * @see SequentialCommand
 */
abstract class CommandGroup(private vararg val commands: Command) : Command() {
    final override val requirements = HashSet<Component>()

    init {
        for (command in commands) {
            if ((requirements intersect command.requirements).isNotEmpty()) throw IllegalArgumentException(
                "Multiple commands in a command group cannot require the same components."
            )
            command.scheduler = scheduler
            requirements += command.requirements
        }
    }

    /**
     * Resets the schedulers of each command. Should be called at the end of any overrides.
     */
    override fun end(interrupted: Boolean) {
        commands.forEach { it.scheduler = null }
    }
}