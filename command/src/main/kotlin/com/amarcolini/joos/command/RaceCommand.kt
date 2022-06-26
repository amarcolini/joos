package com.amarcolini.joos.command

/**
 * A command that runs commands in parallel until one of them finishes.
 */
class RaceCommand @JvmOverloads constructor(
    isInterruptable: Boolean = true,
    vararg commands: Command
) : CommandGroup(true, commands, isInterruptable) {
    private val commands = commands.toMutableList()
    override fun add(command: Command) {
        commands += command
        isInterruptable = isInterruptable && command.isInterruptable
    }

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
    }

    override fun isFinished(): Boolean = commands.any { it.isFinished() }
}