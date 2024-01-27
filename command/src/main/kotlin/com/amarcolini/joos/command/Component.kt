package com.amarcolini.joos.command

import java.util.function.Supplier

/**
 * A class representing a basic unit of robot organization, encapsulating low-level robot hardware and providing
 * methods to be used by [Command]s. The [CommandScheduler] uses components to ensure that multiple commands are not using
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
        @JvmSynthetic
        fun of(defaultCommandSupplier: () -> Command? = { null }, runnable: () -> Unit): Component =
            object : Component {
                override fun update() = runnable()
                override val defaultCommand get() = defaultCommandSupplier()
            }

        /**
         * Creates a component using the provided [runnable] and [defaultCommand].
         */
        @JvmSynthetic
        fun of(defaultCommand: Command, runnable: () -> Unit): Component =
            object : Component {
                override fun update() = runnable()
                override val defaultCommand = defaultCommand
            }

        /**
         * Creates a component using the provided [runnable] and [defaultCommandSupplier].
         */
        @JvmStatic
        fun of(runnable: Runnable, defaultCommandSupplier: Supplier<Command?>): Component =
            object : Component {
                override fun update() = runnable.run()
                override val defaultCommand get() = defaultCommandSupplier.get()
            }

        /**
         * Creates a component using the provided [runnable] and [defaultCommand].
         */
        @JvmStatic
        fun of(runnable: Runnable, defaultCommand: Command): Component =
            object : Component {
                override fun update() = runnable.run()
                override val defaultCommand = defaultCommand
            }
    }

    /**
     * Returns the default [Command] that will be automatically scheduled when no other [Command] is using this component.
     */
    val defaultCommand: Command? get() = null

    /**
     * This method is called repeatedly by the [CommandScheduler].
     */
    fun update() {}

    /**
     * Unregisters this component.
     */
    fun unregister() = CommandScheduler.unregister(this)

    /**
     * Registers this component.
     */
    fun register() = CommandScheduler.register(this)
}