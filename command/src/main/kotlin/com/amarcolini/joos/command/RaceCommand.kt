package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel (Runs them all simultaneously until one of them finishes).
 */
class RaceCommand @JvmOverloads constructor(
    override val isInterruptable: Boolean = true,
    private vararg val commands: Command
) : CommandGroup(commands = commands) {

    override fun init() {
        commands.forEach { it.init() }
    }

    override fun execute() {
        commands.forEach { it.execute() }
        commands.filter { it.isFinished() }.forEach { it.end(false) }
    }

    override fun end(interrupted: Boolean) {
        if (!interrupted) commands.filter { !it.isFinished() }.forEach { it.end(true) }
        else commands.forEach { it.end(true) }
        super.end(interrupted)
    }

    override fun isFinished() = commands.any { it.isFinished() }
}