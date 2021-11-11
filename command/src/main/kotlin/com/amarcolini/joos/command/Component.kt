package com.amarcolini.joos.command

import java.util.function.Supplier

/**
 * A class representing a basic unit of robot organization, encapsulating low-level robot hardware and providing
 * methods to be used by [Command]s. [CommandScheduler]s use components to ensure that multiple commands are not using
 * the same hardware at the same time. Commands that use a component should include that component in their [Command.requirements] set.
 */
interface Component {
    companion object {
        /**
         * Creates a component using the provided [runnable] and [defaultCommand].
         */
        @JvmStatic
        @JvmOverloads
        fun of(defaultCommand: Supplier<Command?> = Supplier { null }, runnable: Runnable) =
            object : Component {
                override fun update() = runnable.run()
                override fun getDefaultCommand() = defaultCommand.get()
            }
    }

    /**
     * Returns the default [Command] that will be automatically scheduled when no other [Command] is using this component.
     */
    fun getDefaultCommand(): Command? = null

    /**
     * This method is called repeatedly by a [CommandScheduler].
     */
    fun update() {}
}