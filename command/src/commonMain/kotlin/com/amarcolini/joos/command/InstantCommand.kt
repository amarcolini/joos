package com.amarcolini.joos.command

/**
 * A command that runs as soon as it is scheduled.
 */
class InstantCommand(
    private val runnable: () -> Unit = {}
) : Command() {
    override fun init() = runnable()
    override var isInterruptable = true
    override fun isFinished(): Boolean = true
}