package com.amarcolini.joos.command

import kotlin.jvm.JvmStatic

/**
 * A class representing a basic unit of robot organization, encapsulating low-level robot hardware and providing
 * methods to be used by [Command]s. Components can be used to ensure that multiple commands are not using
 * the same hardware at the same time. Commands that use a component should include that component in their [Command.requirements] set.
 */
interface Component {
    companion object {
        /**
         * Creates a component using the provided [runnable].
         */
        @JvmStatic
        fun of(runnable: Runnable): Component =
            object : Component {
                override fun update() = runnable.run()
            }

        /**
         * Creates a component using the provided [runnable] and [defaultCommandSupplier].
         */
        @JvmStatic
        fun of(defaultCommandSupplier: () -> Command? = { null }, runnable: Runnable): Component =
            object : Component {
                override fun update() = runnable.run()
                override val defaultCommand get() = defaultCommandSupplier()
            }

        /**
         * Creates a component using the provided [runnable] and [defaultCommand].
         */
        @JvmStatic
        fun of(defaultCommand: Command, runnable: Runnable): Component =
            object : Component {
                override fun update() = runnable.run()
                override val defaultCommand = defaultCommand
            }
    }

    /**
     * Returns the default [Command] that should run when no other [Command] is using this component.
     */
    val defaultCommand: Command? get() = null

    /**
     * This method should be called repeatedly.
     */
    fun update() {}
}