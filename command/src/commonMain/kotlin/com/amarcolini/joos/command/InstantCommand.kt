package com.amarcolini.joos.command

/**
 * A command that runs as soon as it is scheduled.
 */
class InstantCommand(
    private val runnable: Runnable = Runnable {}
) : Command() {
    override fun init() = runnable.run()
    override var isInterruptable = true
    override fun isFinished(): Boolean = true
}