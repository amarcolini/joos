package com.amarcolini.joos.command

/**
 * A type of command that uses multiple commands, joining all their requirements.
 *
 * @param requireJoined whether the requirements of the commands should not intersect.
 *
 * @see ParallelCommand
 * @see RaceCommand
 * @see SequentialCommand
 */
abstract class CommandGroup @JvmOverloads constructor(
    private val requireJoined: Boolean = true,
    private val commands: Array<out Command>,
    override var isInterruptable: Boolean = commands.all { it.isInterruptable },
) : Command() {
    final override val requirements: Set<Component>
        get() {
            val temp = HashSet<Component>()
            for (command in commands) {
                if (requireJoined && (temp intersect command.requirements).isNotEmpty()) throw IllegalArgumentException(
                    "Multiple commands in a command group cannot require the same components."
                )
                temp += command.requirements
            }
            return temp
        }

    /**
     * Adds a command to this group.
     */
    abstract fun add(command: Command)

    /**
     * Adds a runnable to this group.
     */
    fun add(runnable: Runnable): Unit = add(of(runnable))
}