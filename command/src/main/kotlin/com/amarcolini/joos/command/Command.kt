package com.amarcolini.joos.command

import java.util.function.BiConsumer
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A state machine representing a complete action to be performed using any number of [Component]s.
 * Commands are usually run from a [CommandScheduler], but can be run independently if desired. Commands can be chained
 * together to form complex multi-step actions.
 */
abstract class Command {
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
     * The set of components that this Command uses.
     */
    open val requirements: Set<Component> = emptySet()

    /**
     * An internal property which stores this command's current [CommandScheduler]. This is useful when using multiple [CommandScheduler]s.
     */
    var scheduler: CommandScheduler? = null
        internal set

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
     * Runs this command independently of a [CommandScheduler]. Initializes, executes and ends this command synchronously
     * while also updating all of its required components and sending [CommandScheduler.packet].
     *
     * *Note*: If this command does not end by itself, this method will run continuously.
     */
    fun run() {
        requirements.forEach { it.update() }
        init()
        CommandScheduler.sendPacket()
        do {
            requirements.forEach { it.update() }
            execute()
            CommandScheduler.sendPacket()
        } while (!isFinished())
        end(false)
        CommandScheduler.sendPacket()
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
        this then BasicCommand(runnable)

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
        this and BasicCommand(runnable)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(other: Command) =
        RaceCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(runnable: Runnable) =
        this race BasicCommand(runnable)

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
     * Overrides this command's [isFinished] function to finish when [condition] is true.
     */
    fun runUntil(condition: Boolean) =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            { condition },
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

    /**
     * Adds [requirements] to this command's list of required components.
     */
    fun requires(vararg requirements: Component) =
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
    fun isInterruptable(interruptable: Boolean) =
        FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            this::isFinished,
            interruptable,
            requirements
        )
}