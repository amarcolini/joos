package com.amarcolini.joos.command

import com.amarcolini.joos.util.NanoClock
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * A [Command] that uses Kotlin's coroutine features to make writing commands look more synchronous. It is
 * **not** recommended to use this with Java.
 */
class CoroutineCommand(
    private val block: suspend CommandScope.() -> Unit
) : Command() {
    constructor(requirements: Set<Component>, block: suspend CommandScope.() -> Unit) : this(block) {
        this.requirements = requirements
    }

    constructor(vararg requirements: Component, block: suspend CommandScope.() -> Unit)
            : this(requirements.toSet(), block)

    private var scope: CommandScopeImpl? = null

    override var requirements: Set<Component> = setOf()

    override var isInterruptable: Boolean = true

    private var currentIsFinished = false
    override fun init() {
        currentIsFinished = false
        scope = CommandScopeImpl(block)
        scope?.update()
    }

    override fun execute() {
        scope?.update()
    }

    override fun isFinished(): Boolean = scope?.isFinished() != false

    /**
     * Sets a fallback action to be called if this scope gets cancelled.
     */
    fun ifInterrupted(action: () -> Unit) {
        onEnd = { if (it) action() }
    }

    var onEnd: (Boolean) -> Unit = {}
    override fun end(interrupted: Boolean) {
        scope?.cancelAll()
        onEnd(interrupted)
        scope = null
    }
}

/**
 * The scope for yielding in a [CoroutineCommand].
 */
@RestrictsSuspension
abstract class CommandScope internal constructor() {
    @RequiresOptIn
    annotation class Unsafe

    abstract inner class DeferredScope internal constructor() {
        /**
         * Sets a fallback action to be called if this scope gets cancelled.
         */
        abstract fun ifInterrupted(action: () -> Unit)

        abstract fun cancel()

        internal abstract fun isFinished(): Boolean

        internal abstract fun update()
    }

    /**
     * Yields to allow other commands to execute.
     *
     * @param isFinished whether this command is finished.
     */
    abstract suspend fun yield(isFinished: Boolean)

    /**
     * Runs a [command] synchronously (without actually blocking).
     */
    abstract suspend infix fun then(command: Command)

    /**
     * Starts another [CommandScope] that runs in parallel to the current scope.
     * **Note**: If this is used in conjunction with [then], it is impossible to guarantee that
     * all requirement constraints are obeyed. Use with caution.
     *
     * @return a [DeferredScope] which can be passed to [await] or [cancel].
     */
    @Unsafe
    abstract fun async(block: suspend CommandScope.() -> Unit): DeferredScope

    /**
     * Waits for the specified [scope] to finish.
     */
    abstract suspend fun await(scope: DeferredScope)

    /**
     * Waits for all [async] blocks to finish.
     */
    abstract suspend fun awaitAll()

    fun cancel(scope: DeferredScope) = scope.cancel()

    /**
     * Cancels all [async] blocks.
     */
    abstract fun cancelAll()

    /**
     * Equivalent to [then].
     */
    suspend operator fun Command.unaryPlus() = this@CommandScope.then(this)

    suspend fun wait(duration: Double, clock: NanoClock = NanoClock.system) = +WaitCommand(duration, clock)

    /**
     * Runs the following [block] synchronously until the specified [timeout] returns `true`.
     */
    suspend fun withTimeout(timeout: () -> Boolean, block: suspend CommandScope.() -> Unit) =
        +RaceCommand(
            FunctionalCommand(isFinished = timeout),
            CoroutineCommand(block)
        )

    /**
     * @see withTimeout
     */
    suspend fun withTimeout(
        duration: Double,
        clock: NanoClock = NanoClock.system,
        block: suspend CommandScope.() -> Unit
    ) = +RaceCommand(
        WaitCommand(duration, clock),
        CoroutineCommand(block)
    )
}

private class CommandScopeImpl(block: suspend CommandScope.() -> Unit) : CommandScope(), Continuation<Unit> {
    private enum class State {
        Active, Done, Failed
    }

    private val children = mutableListOf<DeferredScopeImpl>()
    private var state = State.Active
    private var currentIsFinished: Boolean = false
    private var nextStep: Continuation<Unit>? = block.createCoroutineUnintercepted(this, this)

    fun update() {
        if (!currentIsFinished || state == State.Active) {
            val step = nextStep
            nextStep = null
            if (step != null) step.resume(Unit)
            else state = State.Failed
            children.removeAll {
                it.update()
                it.isFinished()
            }
        }
        if (currentIsFinished || state != State.Active) {
            cancelAll()
        }
    }

    fun isFinished() = currentIsFinished || state != State.Active

    override suspend fun yield(isFinished: Boolean) {
        currentIsFinished = isFinished
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun then(command: Command) {
        command.init()
        yield(false)
        do {
            command.execute()
            val isFinished = command.isFinished()
            yield(false)
        } while (!isFinished)
    }

    inner class DeferredScopeImpl(block: suspend CommandScope.() -> Unit) : DeferredScope() {
        private val scope = CommandScopeImpl(block)
        private var interrupted = false
        private var ifInterrupted = {}
        override fun ifInterrupted(action: () -> Unit) {
            ifInterrupted = action
        }

        override fun isFinished() = if (interrupted) true else scope.isFinished()
        override fun update() = if (!interrupted) scope.update() else Unit

        override fun cancel() {
            if (!interrupted) {
                interrupted = true
                ifInterrupted.invoke()
            }
        }

        init {
            children += this
        }
    }

    @Unsafe
    override fun async(block: suspend CommandScope.() -> Unit): DeferredScope =
        DeferredScopeImpl(block)

    override suspend fun await(scope: DeferredScope) {
        while (!scope.isFinished()) yield(false)
    }

    override suspend fun awaitAll() {
        val current = children.toList()
        while (current.any { !it.isFinished() }) yield(false)
    }

    override fun cancelAll() {
        children.removeAll {
            it.cancel()
            true
        }
    }

    // Completion continuation implementation
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // just rethrow exception if it is there
        state = State.Done
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext
}