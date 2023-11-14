package com.amarcolini.joos.gamepad

/**
 * Represents the state of any toggleable.
 */
abstract class Toggleable {
    abstract val state: Boolean
    abstract val lastState: Boolean
    open val isActive get() = state
    open val isNotActive get() = !state
    open val justActivated @get:JvmName("justActivated") get() = state && !lastState
    open val justDeactivated @get:JvmName("justDeactivated") get() = !state && lastState
    open val justChanged @get:JvmName("justChanged") get() = state != lastState

    private fun combine(other: Toggleable, operator: (Boolean, Boolean) -> Boolean) = object : Toggleable() {
        override val state: Boolean get() = operator(this@Toggleable.state, other.state)
        override val lastState: Boolean get() = operator(this@Toggleable.state, other.state)
        override val isActive: Boolean get() = operator(this@Toggleable.isActive, other.isActive)
        override val isNotActive: Boolean get() = operator(this@Toggleable.isNotActive, other.isNotActive)
        override val justActivated: Boolean get() = operator(this@Toggleable.justActivated, other.justActivated)
        override val justDeactivated: Boolean get() = operator(this@Toggleable.justDeactivated, other.justDeactivated)
        override val justChanged: Boolean get() = operator(this@Toggleable.justChanged, other.justChanged)
    }

    infix fun or(other: Toggleable) = combine(other) { a, b -> a || b }
    infix fun and(other: Toggleable) = combine(other) { a, b -> a && b }
}