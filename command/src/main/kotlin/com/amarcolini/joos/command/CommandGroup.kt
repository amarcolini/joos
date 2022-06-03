package com.amarcolini.joos.command

/**
 * A type of command that uses multiple commands, joining all their requirements.
 *
 * @param requireJoined whether or not the requirements of the commands should not intersect.
 *
 * @see ParallelCommand
 * @see RaceCommand
 * @see SequentialCommand
 */
abstract class CommandGroup @JvmOverloads constructor(
    requireJoined: Boolean = true,
    vararg commands: Command
) : Command() {
    final override val requirements: MutableSet<Component> = HashSet()

    init {
        for (command in commands) {
            if (requireJoined && (requirements intersect command.requirements).isNotEmpty()) throw IllegalArgumentException(
                "Multiple commands in a command group cannot require the same components."
            )
            requirements += command.requirements
        }
    }
}