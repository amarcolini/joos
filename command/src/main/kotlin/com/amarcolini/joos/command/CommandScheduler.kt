package com.amarcolini.joos.command

import android.content.Context
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.*
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import java.util.function.BooleanSupplier
import kotlin.reflect.full.hasAnnotation

/**
 * The main orchestrator for [Command]s and [Component]s.
 */
object CommandScheduler : OpModeManagerNotifier.Notifications {
    /**
     * The global telemetry object used for both FTC Dashboard and the Driver Station.
     */
    @JvmField
    val telemetry: SuperTelemetry = SuperTelemetry()

    /**
     * Resets [telemetry].
     */
    @JvmStatic
    fun resetTelemetry(): Unit = telemetry.reset()

    /**
     * The current gamepads being used, or null if no OpMode is active.
     */
    @JvmStatic
    var gamepad: MultipleGamepad? = null
        private set

    /**
     * The currently active OpMode, or null if no OpMode is active.
     */
    @JvmStatic
    var opMode: OpMode? = null
        internal set(value) {
            gamepad = if (value == null) {
                reset()
                null
            } else MultipleGamepad(value.gamepad1, value.gamepad2).also {
                gamepad?.unregister()
                telemetry.register(value.telemetry)
                it.register()
            }
            field = value
        }

    /**
     * Stops the currently active OpMode.
     */
    fun stopOpMode() {
        opMode?.requestOpModeStop()
    }

    /**
     * Whether the current OpMode is a teleop OpMode.
     */
    @JvmStatic
    val isInTeleOp: Boolean
        get() = opMode?.let { it::class.hasAnnotation<TeleOp>() } ?: false

    /**
     * Whether the current OpMode is an autonomous OpMode.
     */
    @JvmStatic
    val isInAutonomous: Boolean
        get() = opMode?.let { it::class.hasAnnotation<Autonomous>() } ?: false

    /**
     * The current hardware map, or null if no OpMode is active.
     */
    @JvmStatic
    val hMap: HardwareMap?
        get() = opMode?.hardwareMap

    /**
     * This method attaches itself to the robot controller event loop to automatically
     * add/remove telemetries from the global telemetry, register any gamepads or hardware maps, and provide
     * other useful features.
     */
    @OnCreateEventLoop
    @JvmStatic
    fun attachEventLoop(context: Context, eventLoop: FtcEventLoop) {
        eventLoop.opModeManager.registerListener(this)
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

    /**
     * Schedules commands for execution.
     *
     * @return `true` if all commands were successfully scheduled immediately, and `false` if they were not. Note that if
     * the scheduler is currently updating, this method will return `false`, but the scheduler will attempt to
     * schedule the commands when it can. If [schedulePolicy] is `true`, all commands will be successfully scheduled.
     *
     * @param repeat whether the provided [runnable] should run repeatedly or not
     * @see schedulePolicy
     */
    fun schedule(runnable: Runnable, repeat: Boolean): Boolean = schedule(BasicCommand(runnable).runUntil { !repeat })

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
        this.components += components
    }

    /**
     * Unregisters the given components from this CommandScheduler so that their update functions are no longer called and their default commands are no longer scheduled.
     */
    fun unregister(vararg components: Component) {
        this.components -= components.toSet()
    }

    /**
     * Returns whether the given components are registered with this CommandScheduler.
     */
    fun isRegistered(vararg components: Component): Boolean =
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
    fun map(condition: BooleanSupplier, vararg runnables: Runnable) {
        map(condition, *runnables.map { Command.of(it) }.toTypedArray())
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
    @JvmStatic
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
    fun isScheduled(vararg commands: Command): Boolean = scheduledCommands.containsAll(commands.asList())

    /**
     * Returns the command currently requiring a given component.
     */
    fun requiring(component: Component): Command? = requirements[component]

    override fun onOpModePreInit(opMode: OpMode) {
        this.opMode = opMode
    }

    override fun onOpModePreStart(opMode: OpMode) {}

    override fun onOpModePostStop(opMode: OpMode) {
        reset()
        this.opMode = null
        gamepad = null
    }
}