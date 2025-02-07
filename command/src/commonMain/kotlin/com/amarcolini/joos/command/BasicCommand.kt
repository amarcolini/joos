package com.amarcolini.joos.command

/**
 * A command that executes the provided [runnable] once. Easily decorated.
 */
class BasicCommand(
    private val runnable: () -> Unit = {}
) : Command() {
    override fun execute() = runnable()
    override fun isFinished() = true

    override var isInterruptable = true
}