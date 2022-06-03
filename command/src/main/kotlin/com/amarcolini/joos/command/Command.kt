package com.amarcolini.joos.command

import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A state machine representing a complete action to be performed using any number of [Component]s.
 * Commands are usually run from the [CommandScheduler], but can be run independently if desired. Commands can be chained
 * together to form complex multi-step actions.
 */
abstract class Command : CommandInterface {
    @JvmField
    protected val telemetry: SuperTelemetry = CommandScheduler.telemetry

    companion object {
        /**
         * Creates a [BasicCommand] out of the provided [runnable].
         */
        @JvmStatic
        fun of(runnable: Runnable) = BasicCommand(runnable)

        /**
         * Creates a [SelectCommand] out of the provided [command].
         */
        @JvmStatic
        fun select(command: Supplier<Command>) = SelectCommand(command)

        /**
         * Creates a command that does nothing.
         */
        @JvmStatic
        fun emptyCommand() = InstantCommand {}
    }

    /**
     * Whether this command can be interrupted by another command.
     */
    open val isInterruptable: Boolean = true

    /**
     * The set of components that this command uses.
     */
    open val requirements: Set<Component> = emptySet()

    /**
     * Runs once when first scheduled.
     */
    open fun init() {
        if (!isScheduled()) return
    }

    /**
     * Runs repeatedly until [isFinished] returns true.
     */
    open fun execute() {
        if (!isScheduled()) return
    }

    /**
     * Runs once when this command finishes or is interrupted.
     */
    open fun end(interrupted: Boolean) {
        if (!isScheduled()) return
    }

    /**
     * Returns whether this command is finished.
     */
    open fun isFinished(): Boolean = false

    /**
     * Cancels this command.
     */
    fun cancel() = cancel(this)

    /**
     * Schedules this command.
     */
    fun schedule() = schedule(this)

    /**
     * Returns whether this command is currently registered with the [CommandScheduler].
     */
    fun isScheduled(): Boolean = isScheduled(this)

    /**
     * Runs this command independently of the [CommandScheduler]. Initializes, executes and ends this command synchronously
     * while also updating all of its required components and updating [CommandScheduler.telemetry].
     *
     * *Note*: If this command does not end by itself, this method will run continuously.
     */
    fun run() {
        requirements.forEach { it.update() }
        init()
        CommandScheduler.telemetry.update()
        do {
            requirements.forEach { it.update() }
            execute()
            CommandScheduler.telemetry.update()
        } while (!isFinished())
        end(false)
        CommandScheduler.telemetry.update()
    }

    //These are for chaining commands

    /**
     * Adds a command to run after this one.
     */
    infix fun then(other: Command): SequentialCommand =
        SequentialCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run after this one.
     */
    infix fun then(runnable: Runnable): SequentialCommand =
        this then BasicCommand(runnable)

    /**
     * Waits [duration] seconds after this command finishes.
     */
    infix fun wait(duration: Double): SequentialCommand =
        this then WaitCommand(duration)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until they finish).
     */
    infix fun and(other: Command): ParallelCommand =
        ParallelCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until they finish).
     */
    infix fun and(runnable: Runnable): ParallelCommand =
        this and BasicCommand(runnable)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(other: Command): RaceCommand =
        RaceCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(runnable: Runnable): RaceCommand =
        this race BasicCommand(runnable)

    /**
     * Waits until [condition] returns true after this command finishes.
     */
    infix fun waitUntil(condition: BooleanSupplier): SequentialCommand =
        this then FunctionalCommand(isFinished = condition)

    /**
     * Overrides this command's [isFinished] function to finish when [condition] returns true.
     */
    fun runUntil(condition: BooleanSupplier): FunctionalCommand =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            condition,
            isInterruptable,
            requirements
        )

    /**
     * Overrides this command's [isFinished] function to finish when [condition] returns true, or, if it doesn't,
     * when this command would normally finish.
     */
    fun stopWhen(condition: BooleanSupplier): FunctionalCommand =
        runUntil { isFinished() || condition.asBoolean }

    /**
     * Overrides this command's [isFinished] function to run until it is cancelled.
     */
    fun runForever(): FunctionalCommand = runUntil { false }

    /**
     * Overrides this command's [isFinished] function to run only once.
     */
    fun runOnce(): FunctionalCommand = runUntil { true }

    /**
     * Overrides this command's [init] function.
     */
    fun init(action: Runnable): FunctionalCommand = FunctionalCommand(
        action,
        this::execute,
        this::end,
        this::isFinished,
        isInterruptable,
        requirements
    )

    /**
     * Overrides this command's [execute] function.
     */
    fun execute(action: Runnable): FunctionalCommand = FunctionalCommand(
        this::init,
        action,
        this::end,
        this::isFinished,
        isInterruptable,
        requirements
    )

    /**
     * Overrides this command's [end] function.
     */
    fun end(action: Consumer<Boolean>): FunctionalCommand = FunctionalCommand(
        this::init,
        this::execute,
        action,
        this::isFinished,
        isInterruptable,
        requirements
    )

    /**
     * Returns a [ListenerCommand] that runs the specified action when this command initializes.
     */
    fun onInit(action: Runnable): ListenerCommand =
        ListenerCommand(this, action, {}, {})

    /**
     * Returns a [ListenerCommand] that runs the specified action whenever this command updates.
     */
    fun onExecute(action: Runnable): ListenerCommand =
        ListenerCommand(this, {}, action, {})

    /**
     * Returns a [ListenerCommand] that runs the specified action when this command is ended.
     */
    fun onEnd(action: Consumer<Boolean>): ListenerCommand =
        ListenerCommand(this, {}, {}, action)

    /**
     * Adds [requirements] to this command's list of required components.
     */
    fun requires(requirements: Set<Component>): FunctionalCommand =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            this::isFinished,
            this.isInterruptable,
            this.requirements + requirements
        )

    /**
     * Adds [requirements] to this command's list of required components.
     */
    fun requires(vararg requirements: Component): FunctionalCommand =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            this::isFinished,
            this.isInterruptable,
            this.requirements + requirements
        )

    /**
     * Sets whether this command is interruptable.
     */
    fun isInterruptable(interruptable: Boolean): FunctionalCommand =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            this::isFinished,
            interruptable,
            requirements
        )
}