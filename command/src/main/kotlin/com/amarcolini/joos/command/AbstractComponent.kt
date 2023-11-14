package com.amarcolini.joos.command

import com.amarcolini.joos.dashboard.SuperTelemetry

/**
 * An abstract version of [Component] with more quality of life features.
 */
abstract class AbstractComponent : Component {
    /**
     * A list of subcomponents that this component may use. All subcomponents are
     * updated as well, as long as there is a call to `super.update()`.
     */
    @JvmField
    protected val subcomponents: MutableSet<Component> = HashSet()

    /**
     * The global [SuperTelemetry] instance.
     */
    @JvmField
    protected val telem: SuperTelemetry = CommandScheduler.telem

    override fun update() {
        subcomponents.forEach { it.update() }
    }
}