package com.amarcolini.joos.command

import kotlin.jvm.JvmField

/**
 * A type of command that runs multiple commands.
 *
 * @see ParallelCommand
 * @see RaceCommand
 * @see SequentialCommand
 */
abstract class CommandGroup(
    val commands: List<Command>,
) : Command() {
    final override var isInterruptable: Boolean = !commands.any { !it.isInterruptable }

    abstract override val requirements: Set<Component>

    /**
     * Ensures that no commands share the same requirements. Useful for groups like [ParallelCommand].
     */
    protected fun intersectRequirements(commands: List<Command>): Set<Component> {
        val requirements = mutableSetOf<Component>()
        for (command in commands) {
            if ((requirements intersect command.requirements).isNotEmpty()) throw IllegalArgumentException(
                "Multiple commands in a command group cannot require the same components."
            )
            requirements += command.requirements
        }
        return requirements
    }

    abstract fun internalInit()

    /**
     * Command scheduling policy. If true, all markers which cannot currently be scheduled will be scheduled as soon as
     * they can be scheduled. If false (default behavior), all commands which cannot currently be scheduled will not be scheduled.
     */
    protected var waitToScheduleCommands: Boolean = false

    @JvmField
    protected val scheduleQueue = mutableListOf<Command>()
    private val _scheduledCommands = mutableListOf<Command>()
    protected val scheduledCommands: List<Command> = _scheduledCommands
    private val scheduledRequirements = mutableMapOf<Component, Command>()

    final override fun init() {
        internalInit()
        scheduleCommands()
    }

    protected fun cancelAll() {
        scheduledRequirements.clear()
        _scheduledCommands.toList().forEach { it.end(true) }
        _scheduledCommands.clear()
    }

    protected fun cancel(command: Command) {
        scheduledRequirements.keys.removeAll(command.requirements)
        command.end(true)
        _scheduledCommands.remove(command)
    }

    private fun scheduleCommands() {
        scheduleQueue.removeAll(scheduleQueue.toList().mapNotNull { command ->
            val isAvailable = command.requirements.none { scheduledRequirements[it]?.isInterruptable == false }
            if (isAvailable) {
                //Cancels all commands with the same requirements
                command.requirements.forEach {
                    val cancelled = scheduledRequirements[it]
                    scheduledRequirements[it] = command
                    if (cancelled != null) {
                        _scheduledCommands.remove(cancelled)
                        cancelled.end(true)
                    }
                }
                command.init()
                _scheduledCommands.add(command)
            }
            if (isAvailable || !waitToScheduleCommands) command else null
        })
    }

    final override fun execute() {
        scheduleCommands()
        _scheduledCommands.removeAll(_scheduledCommands.toList().mapNotNull {
            it.execute()
            val finished = it.isFinished()
            if (finished) {
                scheduledRequirements.keys.removeAll(it.requirements)
                it.end(false)
            }
            if (finished) it else null
        })
    }

    final override fun end(interrupted: Boolean) {
        if (interrupted) cancelAll()
    }

    abstract override fun isFinished(): Boolean

    /**
     * Adds a command to this group.
     */
    abstract fun add(command: Command): CommandGroup

    /**
     * Adds a runnable to this group.
     */
    fun add(runnable: () -> Unit): CommandGroup = add(of(runnable))
}