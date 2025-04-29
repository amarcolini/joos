package com.amarcolini.joos.command

import com.amarcolini.joos.command.CommandScheduler.waitToScheduleCommands
import java.util.function.BooleanSupplier

/**
 * An interface that simplifies access to the [CommandScheduler] by implementing all its methods.
 */
interface CommandInterface {
    /**
     * Returns whether a command can currently be scheduled.
     */
    fun isAvailable(command: Command): Boolean = CommandScheduler.isAvailable(command)

    /**
     * Schedules commands for execution.
     *
     * @return `true` if all commands were successfully scheduled immediately, and `false` if they were not. Note that if
     * the scheduler is currently updating, this method will return `false`, but the scheduler will attempt to
     * schedule the commands when it can. If [waitToScheduleCommands] is `true`, all commands will be successfully scheduled.
     *
     * @see CommandScheduler.waitToScheduleCommands
     */
    fun schedule(vararg commands: Command): Boolean = CommandScheduler.schedule(*commands)

    /**
     * Schedules commands for execution.
     *
     * @return `true` if all commands were successfully scheduled immediately, and `false` if they were not. Note that if
     * the scheduler is currently updating, this method will return `false`, but the scheduler will attempt to
     * schedule the commands when it can. If [CommandScheduler.waitToScheduleCommands] is `true`, all commands will be successfully scheduled.
     *
     * @see CommandScheduler.waitToScheduleCommands
     */
    fun schedule(runnable: Runnable): Boolean = CommandScheduler.schedule(runnable)

    /**
     * Runs a command independently of the [CommandScheduler]. Initializes, executes and ends this command synchronously
     * while also updating all of its required components and updating [CommandScheduler.telem].
     *
     * *Note*: If this command does not end by itself, this method will run indefinitely.
     */
    fun runBlocking(command: Command) = CommandScheduler.runBlocking(command)

    /**
     * Schedules commands for execution.
     *
     * @return `true` if all commands were successfully scheduled immediately, and `false` if they were not. Note that if
     * the scheduler is currently updating, this method will return `false`, but the scheduler will attempt to
     * schedule the commands when it can. If [CommandScheduler.waitToScheduleCommands] is `true`, all commands will be successfully scheduled.
     *
     * @param repeat whether the provided [runnable] should run repeatedly or not
     * @see CommandScheduler.waitToScheduleCommands
     */
    fun schedule(repeat: Boolean, runnable: Runnable): Boolean = CommandScheduler.schedule(repeat, runnable)

    /**
     * Registers the given components to this CommandScheduler so that their update functions are called and their default commands are scheduled.
     */
    fun register(vararg components: Component) = CommandScheduler.register(*components)

    /**
     * Unregisters the given components from this CommandScheduler so that their update functions are no longer called and their default commands are no longer scheduled.
     */
    fun unregister(vararg components: Component) = CommandScheduler.unregister(*components)

    /**
     * Returns whether the given components are registered with this CommandScheduler.
     */
    fun isRegistered(vararg components: Component) = CommandScheduler.isRegistered(*components)

    /**
     * Cancels commands. Ignores whether a command is interruptable.
     */
    fun cancel(vararg commands: Command) = CommandScheduler.cancel(*commands)

    /**
     * Maps a condition to commands. If the condition returns true, the commands are scheduled.
     * A command can be mapped to multiple conditions.
     */
    fun map(condition: BooleanSupplier, vararg commands: Command) =
        CommandScheduler.map(condition, *commands)

    /**
     * Maps a condition to a runnable. If the condition returns true, a command is scheduled.
     * A command can be mapped to multiple conditions.
     */
    fun map(condition: BooleanSupplier, runnable: Runnable) =
        CommandScheduler.map(condition, runnable)

    /**
     * Removes commands from the list of mappings.
     */
    fun unmap(vararg commands: Command) = CommandScheduler.unmap(*commands)

    /**
     * Removes a condition from the list of mappings.
     */
    fun unmap(condition: BooleanSupplier) = CommandScheduler.unmap(condition)

    /**
     * Cancels all currently scheduled commands. Ignores whether a command is interruptable.
     */
    fun cancelAll() = CommandScheduler.cancelAll()

    /**
     * Returns whether all the given commands are scheduled.
     */
    fun isScheduled(vararg commands: Command) = CommandScheduler.isScheduled(*commands)

    /**
     * Returns the command currently requiring a given component.
     */
    fun requiring(component: Component) = CommandScheduler.requiring(component)

    /**
     * Stops the currently active OpMode.
     */
    fun stopOpMode() = CommandScheduler.stopOpMode()
}