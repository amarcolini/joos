package com.amarcolini.joos.command

/**
 * An abstract version of [Component] with more quality of life features.
 */
abstract class AbstractComponent : Component {
    /**
     * The scheduler currently using this component.
     */
    var scheduler: CommandScheduler? = null
        internal set(value) {
            subcomponents.forEach {
                if (it is AbstractComponent) it.scheduler = value
            }
            field = value
        }

    /**
     * A list of subcomponents that this component may use. All subcomponents are
     * updated as well, as long as there is a call to `super.update()`.
     */
    @JvmField
    protected val subcomponents: MutableList<Component> = ArrayList()

    override fun update() {
        subcomponents.forEach { it.update() }
    }
}