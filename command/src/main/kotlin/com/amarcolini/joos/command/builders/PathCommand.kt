package com.amarcolini.joos.command.builders

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.path.Path

/**
 * A command wrapper that also contains a [Path].
 */
class PathCommand(@JvmField val command: Command, @JvmField val path: Path) : Command() {
    override val isInterruptable: Boolean
        get() = command.isInterruptable

    override val requirements: Set<Component>
        get() = command.requirements

    fun start() = path.start()
    fun end() = path.end()

    override fun execute() {
        command.execute()
    }

    override fun init() {
        command.init()
    }

    override fun isFinished(): Boolean {
        return command.isFinished()
    }

    override fun end(interrupted: Boolean) {
        command.end(interrupted)
    }
}