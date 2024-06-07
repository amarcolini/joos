package com.amarcolini.joos.command

import java.util.function.BooleanSupplier
import java.util.function.Consumer

/**
 * A command whose properties can be defined externally. Useful for making simple inline commands or combining commands together.
 */
class FunctionalCommand @JvmOverloads constructor(
    @JvmField var init: Runnable = Runnable {},
    @JvmField var execute: Runnable = Runnable {},
    @JvmField var end: Consumer<Boolean> = Consumer {},
    @JvmField var isFinished: BooleanSupplier = BooleanSupplier { false },
    override var isInterruptable: Boolean = true,
    override var requirements: Set<Component> = emptySet()
) : Command() {
    constructor(
        init: Runnable = Runnable {},
        execute: Runnable = Runnable {},
        end: Consumer<Boolean> = Consumer {},
        isFinished: BooleanSupplier = BooleanSupplier { false },
        isInterruptable: Boolean = true,
        vararg requirements: Component
    ) : this(init, execute, end, isFinished, isInterruptable, requirements.toSet())

    override fun init() = init.run()
    override fun execute() = execute.run()
    override fun end(interrupted: Boolean) = end.accept(interrupted)
}