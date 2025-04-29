package com.amarcolini.joos.command

/**
 * A command that executes the provided [runnable] once. Easily decorated.
 */
class BasicCommand(
    private val runnable: Runnable = Runnable {}
) : Command() {
    override fun execute() = runnable.run()
    override fun isFinished() = true

    override var isInterruptable = true
}