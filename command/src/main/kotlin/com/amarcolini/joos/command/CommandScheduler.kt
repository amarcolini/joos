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
         * The packet to use with FTC Dashboard to avoid conflicting packet sends. Is automatically sent
         * every update cycle.
         */
        @JvmStatic
        var packet = TelemetryPacket()
            private set

        /**
         * Sends [packet]. Is automatically called every update cycle.
         */
        @JvmStatic
        fun sendPacket() {
            try {
                FtcDashboard.getInstance().sendTelemetryPacket(packet)
            } catch (e: Exception) {

            }
            packet = TelemetryPacket()
        }
    }

    private val scheduledCommands = LinkedHashSet<Command>()

    private val components = LinkedHashSet<Component>()

    private val requirements = LinkedHashMap<Component, Command>()

    private val conditions = LinkedHashMap<BooleanSupplier, MutableSet<Command>>()

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
     * Schedules commands for execution. Returns whether all the commands were successfully scheduled.
     */
    fun schedule(vararg commands: Command): Boolean {
        var success = true
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

    fun update() {
        components.forEach { it.update() }

        for (command in scheduledCommands) {
            command.execute()

            if (command.isFinished()) {
                scheduledCommands.remove(command)
                requirements.keys.removeAll(command.requirements)
                command.end(false)
                command.scheduler = null
            }
        }

        conditions.filterKeys { it.asBoolean }.values.forEach { commands ->
            commands.forEach { schedule(it) }
        }

        for (component in components) {
            if (!requirements.containsKey(component)) {
                schedule(component.getDefaultCommand() ?: continue)
            }
        }

        sendPacket()
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
     * Maps a condition to commands. If the condition returns true, the commands are scheduled. A command can be mapped to multiple conditions.
     */
    fun map(condition: BooleanSupplier, vararg commands: Command) {
        conditions[condition] = commands.toMutableSet()
    }

    fun map(condition: BooleanSupplier, vararg commands: Runnable) {
        map(condition, *commands.map { Command.of(it) }.toTypedArray())
    }

    /**
     * Removes commands from the list of mappings.
     */
    fun unmap(vararg commands: Command) {
        conditions.values.forEach { it.removeAll(commands) }
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
    fun cancelAll() {
        scheduledCommands.forEach { cancel(it) }
    }

    /**
     * Resets this [CommandScheduler]. All commands, components, and conditions are cleared.
     */
    fun reset() {
        scheduledCommands.clear()
        components.clear()
        requirements.clear()
        conditions.clear()
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
