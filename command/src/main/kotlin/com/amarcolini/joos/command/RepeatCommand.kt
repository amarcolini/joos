package com.amarcolini.joos.command

/**
 * A command that runs another command multiple times.
 *
 * @param command the command to run
 * @param times the number of times to run the command. If less than zero, then
 * the command will be run indefinitely.
 */
class RepeatCommand(
    private val command: Command,
    private val times: Int,
) : Command() {
    override val isInterruptable: Boolean
        get() = super.isInterruptable

    override val requirements: Set<Component>
        get() = command.requirements

    private var time = 1

    override fun init() {
        time = 1
        command.init()
    }

    override fun execute() {
        if (isFinished()) return

        command.execute()

        if (command.isFinished()) {
            command.end(false)
            time++
            if (time <= times || times < 0) command.init()
        }
    }

    override fun isFinished(): Boolean = times in 1 until time

    override fun end(interrupted: Boolean) {
        command.end(interrupted)
    }
}