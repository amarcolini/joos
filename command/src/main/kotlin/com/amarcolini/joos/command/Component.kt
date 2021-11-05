package com.amarcolini.joos.command

/**
 * A class representing a basic unit of robot organization, encapsulating low-level robot hardware and providing
 * methods to be used by [Command]s. [CommandScheduler]s use components to ensure that multiple commands are not using
 * the same hardware at the same time. Commands that use a component should include that component in their [Command.requirements] set.
 */
interface Component {
    /**
     * Returns the default [Command] that will be automatically scheduled when no other [Command] is using this component.
     */
    fun getDefaultCommand(): Command? = null

    /**
     * This method is called repeatedly by a [CommandScheduler].
     *
     * @param scheduler the scheduler that called this method.
     */
    fun update(scheduler: CommandScheduler) {}
}