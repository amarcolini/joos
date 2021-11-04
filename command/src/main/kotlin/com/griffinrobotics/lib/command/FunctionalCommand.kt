package com.griffinrobotics.lib.command

/**
 * A command whose properties can be defined externally. Useful for making simple inline commands or combining commands together.
 */
class FunctionalCommand(
    var init: () -> Unit = {},
    var execute: () -> Unit = {},
    var end: (Boolean) -> Unit = {},
    var isFinished: () -> Boolean = { false },
    override var isInterruptable: Boolean = true,
    override var requirements: Set<Component> = emptySet()
) : Command() {
    override fun init() = init.invoke()
    override fun execute() = execute.invoke()
    override fun end(interrupted: Boolean) = end.invoke(interrupted)

    override fun isFinished() = isFinished.invoke()
}