package com.amarcolini.joos.command

import java.util.function.BooleanSupplier
import java.util.function.Consumer

/**
 * A command whose properties can be defined externally. Useful for making simple inline commands or combining commands together.
 */
class FunctionalCommand @JvmOverloads constructor(
    var init: Runnable = Runnable {},
    var execute: Runnable = Runnable {},
    var end: Consumer<Boolean> = Consumer {},
    var isFinished: BooleanSupplier = BooleanSupplier { false },
    override var isInterruptable: Boolean = true,
    override var requirements: Set<Component> = emptySet()
) : Command() {
    override fun init() = init.run()
    override fun execute() = execute.run()
    override fun end(interrupted: Boolean) = end.accept(interrupted)

    override fun isFinished() = isFinished.asBoolean
}