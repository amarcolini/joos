package com.amarcolini.joos.command

/**
 * A command that runs as soon as it is scheduled.
 */
class InstantCommand(
    private val runnable: Runnable
) : Command() {
    override fun init() = runnable.run()
    override fun isFinished() = true

    override var isInterruptable = true
}