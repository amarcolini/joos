package com.amarcolini.joos.command

import java.util.function.BiConsumer
import java.util.function.BooleanSupplier
import java.util.function.Consumer

/**
 * A state machine representing a complete action to be performed using any number of [Component]s.
 * Commands are usually run from a [CommandScheduler], but can be run independently if desired. Commands can be chained
 * together to form complex multi-step actions.
 */
abstract class Command {
    companion object Static {
        /**
         * Creates an [InstantCommand] out of the provided [runnable].
         */
        @JvmStatic
        fun of(runnable: Runnable) = InstantCommand(runnable)

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
     * The set of components that this Command uses.
     */
    open val requirements: Set<Component> = emptySet()

    /**
     * An internal property which stores this command's current [CommandScheduler]. This is useful when using multiple [CommandScheduler]s.
     */
    var scheduler: CommandScheduler? = null

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
     * Runs once when this command finishes / is interrupted.
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
    fun cancel() {
        if (scheduler?.isScheduled(this) == true) scheduler?.cancel(this)
    }

    /**
     * Returns whether this command is currently registered with a [CommandScheduler].
     */
    fun isScheduled(): Boolean = scheduler?.isScheduled(this) == true

    /**
     * Runs this command independently of a [CommandScheduler]. Initializes, executes and ends this command synchronously.
     *
     * *Note*: If this command does not end by itself, this method will run continuously.
     */
    fun run() {
        init()
        do execute() while (!isFinished())
        end(false)
    }

    //These are for chaining commands

    /**
     * Adds a command to run after this one.
     */
    infix fun then(other: Command) =
        SequentialCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run after this one.
     */
    infix fun then(runnable: Runnable) =
        this then InstantCommand(runnable)

    /**
     * Waits [duration] seconds after this command finishes.
     */
    infix fun wait(duration: Double) =
        this then WaitCommand(duration)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until they finish).
     */
    infix fun and(other: Command) =
        ParallelCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until they finish).
     */
    infix fun and(runnable: Runnable) =
        this and InstantCommand(runnable)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(other: Command) =
        RaceCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(runnable: Runnable) =
        this race InstantCommand(runnable)

    /**
     * Waits until [condition] returns true after this command finishes.
     */
    infix fun waitUntil(condition: BooleanSupplier) =
        this then FunctionalCommand(isFinished = condition)

    /**
     * Overrides this command's [isFinished] function to finish when [condition] returns true.
     */
    fun runUntil(condition: BooleanSupplier) =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            condition,
            isInterruptable,
            requirements
        )

    /**
     * Returns a [ListenerCommand] that runs the specified action when this command initializes.
     */
    fun onInit(action: Consumer<Command>) =
        ListenerCommand(this, action, {}, { _, _ -> })

    /**
     * Returns a [ListenerCommand] that runs the specified action whenever this command updates.
     */
    fun onExecute(action: Consumer<Command>) =
        ListenerCommand(this, {}, action, { _, _ -> })

    /**
     * Returns a [ListenerCommand] that runs the specified action when this command is ended.
     */
    fun onEnd(action: BiConsumer<Command, Boolean>) =
        ListenerCommand(this, {}, {}, action)

    /**
     * Adds [requirements] to this command's list of required components.
     */
    fun requires(requirements: Set<Component>) =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            this::isFinished,
            this.isInterruptable,
            this.requirements + requirements
        )
}