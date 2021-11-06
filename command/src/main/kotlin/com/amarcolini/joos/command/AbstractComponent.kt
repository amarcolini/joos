package com.amarcolini.joos.command

/**
 * An abstract version of [Component] with more quality of life features.
 */
abstract class AbstractComponent : Component {
    /**
     * The scheduler currently using this component.
     */
    var scheduler: CommandScheduler? = null
        internal set
}