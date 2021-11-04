package com.griffinrobotics.lib.command

import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * A command useful for adding listeners to other commands.
 * @param command the command to be run.
 * @param onInit the action to run when this command initializes. Takes in [command] as a parameter.
 * @param onExecute the action to run whenever this command updates. Takes in [command] as a parameter.
 * @param onEnd the action to run when this command is ended. Takes in [command] and whether it was interrupted as parameters.
 */
class ListenerCommand @JvmOverloads constructor(
    val command: Command = empty(),
    val onInit: Consumer<Command>,
    val onExecute: Consumer<Command>,
    val onEnd: BiConsumer<Command, Boolean>
) : Command() {
    override val requirements = command.requirements

    override val isInterruptable = command.isInterruptable

    override fun init() {
        command.init()
        onInit.accept(command)
    }

    override fun execute() {
        command.execute()
        onExecute.accept(command)
    }

    override fun end(interrupted: Boolean) {
        command.end(interrupted)
        onEnd.accept(command, interrupted)
    }

    override fun isFinished() = command.isFinished()
}