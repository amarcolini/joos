package com.amarcolini.joos.command

/**
 * A command that runs commands in sequence.
 */
class SequentialCommand @JvmOverloads constructor(
    override val isInterruptable: Boolean = true,
    vararg val commands: Command
) : CommandGroup(*commands) {
    private var index = -1

    override fun init() {
        index = 0
        commands[index].init()
    }

    override fun execute() {
        if (index < 0 || index >= commands.size) return

        commands[index].execute()

        if (commands[index].isFinished()) {
            commands[index].end(false)
            index++
            if (index < commands.size) commands[index].init()
        }
    }

    override fun isFinished() = index >= commands.size

    override fun end(interrupted: Boolean) {
        if (index < 0) return
        if (interrupted) commands[index].end(interrupted)
        super.end(interrupted)
    }
}