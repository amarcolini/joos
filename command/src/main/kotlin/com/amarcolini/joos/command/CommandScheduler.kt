package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import java.util.function.BooleanSupplier

/**
 * The main orchestrator for [Command]s and [Component]s.
 */
open class CommandScheduler {

    companion object Static {
        /**
         * The global telemetry object used for both FtcDashboard and the Driver Station.
         */
        @JvmField
        val telemetry = SuperTelemetry()

        @JvmStatic
        fun resetTelemetry() = telemetry.reset()
    }

    private val scheduledCommands = LinkedHashSet<Command>()

    private val components = LinkedHashSet<Component>()

    private val requirements = LinkedHashMap<Component, Command>()

    private val conditions = LinkedHashMap<BooleanSupplier, MutableSet<Command>>()

    /**
     * A list storing all actions that couldn't be completed at the time they were called because
     * the scheduler was busy.
     */
    private val actionCache = LinkedHashSet<() -> Boolean>()

    /**
     * Command scheduling policy. If true, all commands which cannot currently be scheduled will be scheduled as soon as
     * they can be schedule. If false (default behavior), all commands which cannot currently be scheduled will not be scheduled.
     */
    var schedulePolicy: Boolean = false

    /**
     * Returns whether a command can currently be scheduled.
     */
    fun isAvailable(command: Command): Boolean =
        requirements.filter { (key, value) -> command.requirements.contains(key) && !value.isInterruptable }
            .isEmpty()

    private fun initCommand(command: Command) {
        command.scheduler = this
        command.init()
        scheduledCommands.add(command)
        command.requirements.forEach { requirements[it] = command }
    }

    /**
     * Schedules commands for execution.
     *
     * @return `true` if all commands were successfully scheduled immediately, and `false` if they were not. Note that if
     * the scheduler is currently updating, this method will return `false`, but the scheduler will attempt to
     * schedule the commands when it can. If [schedulePolicy] is `true`, all commands will be successfully scheduled.
     *
     * @see schedulePolicy
     */
    fun schedule(vararg commands: Command): Boolean {
        var success = true
        if (isBusy) {
            actionCache += { schedule(*commands) || schedulePolicy }
            return false
        }
        for (command in commands) {
            success = success && if (!command.isScheduled() && isAvailable(command)) {
                requirements.filterKeys { key -> command.requirements.contains(key) }
                    .forEach { (_, command) ->
                        scheduledCommands.remove(command)
                        command.end(true)
                        command.scheduler = null
                    }
                initCommand(command)
                true
            } else false
        }
        return success
    }

    /**
     * Schedules commands for execution.
     *
     * @return `true` if all commands were successfully scheduled immediately, and `false` if they were not. Note that if
     * the scheduler is currently updating, this method will return `false`, but the scheduler will attempt to
     * schedule the commands when it can. If [schedulePolicy] is `true`, all commands will be successfully scheduled.
     *
     * @see schedulePolicy
     */
    fun schedule(vararg runnables: Runnable): Boolean = schedule(*runnables.map { BasicCommand(it) }.toTypedArray())

    private var isBusy = false
    fun update() {
        //Updates all registered components
        components.forEach { it.update() }

        //The predicate for removeIf is applied to all commands, acting like a for loop
        isBusy = true
        scheduledCommands.removeIf {
            //Executes all scheduled commands
            it.execute()
            //Computes whether they are finished
            val finished = it.isFinished()
            //Ends and frees up their requirements if they are finished
            if (finished) {
                requirements.keys.removeAll(it.requirements)
                it.end(false)
                it.scheduler = null
            }
            //Removes them if they are finished
            finished
        }
        isBusy = false

        //Schedules the necessary commands mapped to a condition
        conditions.filterKeys { it.asBoolean }.values.forEach { commands ->
            commands.forEach { schedule(it) }
        }

        //Schedules default commands, if possible
        for (component in components) {
            if (!requirements.containsKey(component)) {
                schedule(component.getDefaultCommand() ?: continue)
            }
        }

        telemetry.update()

        //Updates action cache
        actionCache.removeIf { it() }
    }

    /**
     * Registers the given components to this CommandScheduler so that their update functions are called and their default commands are scheduled.
     */
    fun register(vararg components: Component) {
        components.forEach {
            if (it is AbstractComponent) it.scheduler = this
        }
        this.components += components
    }

    /**
     * Unregisters the given components from this CommandScheduler so that their update functions are no longer called and their default commands are no longer scheduled.
     */
    fun unregister(vararg components: Component) {
        components.forEach {
            if (it is AbstractComponent && it.scheduler == this) it.scheduler = null
        }
        this.components -= components
    }

    /**
     * Returns whether the given components are registered with this CommandScheduler.
     */
    fun isRegistered(vararg components: Component) =
        this.components.containsAll(components.toList())

    /**
     * Cancels commands. Ignores whether a command is interruptable.
     */
    fun cancel(vararg commands: Command) {
        if (isBusy) {
            actionCache += {
                cancel(*commands)
                true
            }
            return
        }
        for (command in commands) {
            if (isScheduled(command)) {
                scheduledCommands.remove(command)
                requirements.keys.removeAll(command.requirements)
                command.end(true)
                command.scheduler = null
            }
        }
    }

    /**
     * Maps a condition to commands. If the condition returns true, the commands are scheduled.
     * A command can be mapped to multiple conditions.
     */
    fun map(condition: BooleanSupplier, vararg commands: Command) {
        conditions[condition] = commands.toMutableSet()
    }

    /**
     * Maps a condition to commands. If the condition returns true, the commands are scheduled.
     * A command can be mapped to multiple conditions.
     */
    fun map(condition: BooleanSupplier, vararg commands: Runnable) {
        map(condition, *commands.map { Command.of(it) }.toTypedArray())
    }

    /**
     * Removes commands from the list of mappings.
     */
    fun unmap(vararg commands: Command) {
        conditions.values.forEach { it.removeAll(commands.toSet()) }
    }

    /**
     * Removes a condition from the list of mappings.
     */
    fun unmap(condition: BooleanSupplier) {
        conditions.remove(condition)
    }

    /**
     * Cancels all currently scheduled commands. Ignores whether a command is interruptable.
     */
    fun cancelAll() = cancel(*scheduledCommands.toTypedArray())

    /**
     * Resets this [CommandScheduler]. The telemetry is reset, all commands are cancelled, and all commands, components, and conditions are cleared.
     */
    fun reset() {
        val reset = {
            cancelAll()
            scheduledCommands.clear()
            components.clear()
            requirements.clear()
            conditions.clear()
            resetTelemetry()
            true
        }
        if (isBusy) actionCache += reset
        else reset()
    }

    /**
     * Returns whether all the given commands are scheduled.
     */
    fun isScheduled(vararg commands: Command) = scheduledCommands.containsAll(commands.asList())

    /**
     * Returns the command currently requiring a given component.
     */
    fun requiring(component: Component) = requirements[component]
}