package com.amarcolini.joos.command

import kotlin.jvm.JvmStatic

/**
 * A state machine representing a complete action to be performed using any number of [Component]s.
 * Commands are usually run from the [CommandScheduler], but can be run independently if desired. Commands can be chained
 * together to form complex multi-step actions.
 */
abstract class Command {

    companion object {
        /**
         * Creates a [BasicCommand] out of the provided [runnable].
         */
        @JvmStatic
        fun of(runnable: () -> Unit): BasicCommand = BasicCommand(runnable)

        /**
         * Creates a [SelectCommand] out of the provided [command]. Note that requirements must be explicitly
         * specified, as the [SelectCommand] is uncertain of what requirements it will have before being scheduled.
         */
        @JvmStatic
        fun select(vararg requirements: Component, command: () -> Command): SelectCommand =
            SelectCommand(command, *requirements)

        /**
         * Creates a command that does nothing.
         */
        @JvmStatic
        fun empty(): InstantCommand = InstantCommand {}
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
    open fun init() {}

    /**
     * Runs repeatedly until [isFinished] returns true.
     */
    open fun execute() {}

    /**
     * Runs once when this command finishes or is interrupted.
     */
    open fun end(interrupted: Boolean) {}

    /**
     * Returns whether this command is finished.
     */
    open fun isFinished(): Boolean = false

    //These are for chaining commands

    /**
     * Adds a command to run after this one.
     */
    infix fun then(other: Command): SequentialCommand = if (this is SequentialCommand)
        this.run { add(other) }
    else SequentialCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run after this one.
     */
    infix fun then(runnable: () -> Unit): SequentialCommand =
        this then BasicCommand(runnable)

    /**
     * Waits [duration] seconds after this command finishes.
     */
    infix fun wait(duration: Double): SequentialCommand =
        this then WaitCommand(duration)

    /**
     * Interrupts this command if it does not finish after [duration] seconds.
     */
    fun withTimeout(duration: Double): RaceCommand =
        this race WaitCommand(duration)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until they finish).
     */
    infix fun and(other: Command): ParallelCommand = if (this is ParallelCommand)
        this.run { add(other) }
    else ParallelCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until they finish).
     */
    infix fun and(runnable: () -> Unit): ParallelCommand =
        this and BasicCommand(runnable)

    /**
     * Adds a command to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(other: Command): RaceCommand = if (this is RaceCommand)
        this.run { add(other) }
    else RaceCommand(isInterruptable && other.isInterruptable, this, other)

    /**
     * Adds a runnable to run in parallel with this one (Both run simultaneously until one finishes).
     */
    infix fun race(runnable: () -> Unit): RaceCommand =
        this race BasicCommand(runnable)

    /**
     * Waits until [condition] returns true after this command finishes.
     */
    infix fun waitUntil(condition: () -> Boolean): SequentialCommand =
        this then FunctionalCommand(isFinished = condition)

    /**
     * Overrides this command's [isFinished] function to finish when [condition] returns true.
     */
    fun runUntil(condition: () -> Boolean): FunctionalCommand = if (this is FunctionalCommand)
        this.apply {
            isFinishedFunction = condition
        }
    else FunctionalCommand(
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
    fun stopWhen(condition: () -> Boolean): FunctionalCommand =
        runUntil { isFinished() || condition() }

    /**
     * Overrides this command's [isFinished] function to run until it is cancelled.
     */
    fun runForever(): FunctionalCommand = runUntil { false }

    /**
     * Overrides this command's [isFinished] function to run only once.
     */
    fun runOnce(): FunctionalCommand = runUntil { true }

    /**
     * Repeats this command [times] times.
     */
    fun repeat(times: Int): RepeatCommand = RepeatCommand(this, times)

    /**
     * Repeats this command indefinitely.
     */
    fun repeatForever(): RepeatCommand = RepeatCommand(this, -1)

    /**
     * Overrides this command's [init] function.
     */
    fun setInit(action: () -> Unit): FunctionalCommand = if (this is FunctionalCommand)
        this.apply { initFunction = action }
    else FunctionalCommand(
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
    fun setExecute(action: () -> Unit): FunctionalCommand = if (this is FunctionalCommand)
        this.apply { executeFunction = action }
    else FunctionalCommand(
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
    fun setEnd(action: (Boolean) -> Unit): FunctionalCommand = if (this is FunctionalCommand)
        this.apply { endFunction = action }
    else FunctionalCommand(
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
    fun onInit(action: () -> Unit): ListenerCommand = if (this is ListenerCommand)
        this.apply { onInit = action }
    else ListenerCommand(this, action, {}, {})

    /**
     * Returns a [ListenerCommand] that runs the specified action whenever this command updates.
     */
    fun onExecute(action: () -> Unit): ListenerCommand = if (this is ListenerCommand)
        this.apply { onExecute = action }
    else ListenerCommand(this, {}, action, {})

    /**
     * Returns a [ListenerCommand] that runs the specified action when this command is ended.
     */
    fun onEnd(action: (Boolean) -> Unit): ListenerCommand = if (this is ListenerCommand)
        this.apply { onEnd = action }
    else ListenerCommand(this, {}, {}, action)

    /**
     * Adds [requirements] to this command's list of required components.
     */
    fun requires(requirements: Set<Component>): FunctionalCommand = if (
        this is FunctionalCommand
    ) this.apply { this.requirements += requirements }
    else FunctionalCommand(
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
    fun requires(vararg requirements: Component): FunctionalCommand = if (
        this is FunctionalCommand
    ) this.apply { this.requirements += requirements }
    else FunctionalCommand(
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
    fun setInterruptable(interruptable: Boolean): Command = when (this) {
        is FunctionalCommand -> this.apply { isInterruptable = interruptable }
        is CommandGroup -> this.apply { isInterruptable = interruptable }
        else -> FunctionalCommand(
            this::init,
            this::execute,
            this::end,
            this::isFinished,
            interruptable,
            requirements
        )
    }
}