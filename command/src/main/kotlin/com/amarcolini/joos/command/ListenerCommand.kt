package com.amarcolini.joos.command

import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * A command useful for adding listeners to other commands.
 * @param command the command to be run.
 * @param onInit the action to run when this command initializes.
 * @param onExecute the action to run whenever this command updates.
 * @param onEnd the action to run when this command is ended. Takes in whether it was interrupted as a parameter.
 */
class ListenerCommand @JvmOverloads constructor(
    val command: Command = emptyCommand(),
    private val onInit: Runnable = Runnable {},
    private val onExecute: Runnable = Runnable {},
    private val onEnd: Consumer<Boolean> = Consumer<Boolean> {}
) : Command() {
    override val requirements: Set<Component> = command.requirements

    override val isInterruptable: Boolean = command.isInterruptable

    override fun init() {
        command.init()
        onInit.run()
    }

    override fun execute() {
        command.execute()
        onExecute.run()
    }

    override fun end(interrupted: Boolean) {
        command.end(interrupted)
        onEnd.accept(interrupted)
    }

    override fun isFinished(): Boolean = command.isFinished()
}