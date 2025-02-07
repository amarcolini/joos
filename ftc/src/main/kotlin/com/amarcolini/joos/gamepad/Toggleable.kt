package com.amarcolini.joos.gamepad

/**
 * Represents the state of any toggleable.
 */
abstract class Toggleable {
    abstract val state: Boolean
    abstract val lastState: Boolean
    open val isActive get() = state
    open val isNotActive get() = !state
    open val isJustActivated get() = state && !lastState
    open val isJustDeactivated get() = !state && lastState
    open val isJustChanged get() = state != lastState

    fun combine(other: Toggleable, operator: (Boolean, Boolean) -> Boolean) = object : Toggleable() {
        override val state: Boolean get() = operator(this@Toggleable.state, other.state)
        override val lastState: Boolean get() = operator(this@Toggleable.state, other.state)
        override val isActive: Boolean get() = operator(this@Toggleable.isActive, other.isActive)
        override val isNotActive: Boolean get() = operator(this@Toggleable.isNotActive, other.isNotActive)
        override val isJustActivated: Boolean get() = operator(this@Toggleable.isJustActivated, other.isJustActivated)
        override val isJustDeactivated: Boolean get() = operator(this@Toggleable.isJustDeactivated, other.isJustDeactivated)
        override val isJustChanged: Boolean get() = operator(this@Toggleable.isJustChanged, other.isJustChanged)
    }

    infix fun or(other: Toggleable) = combine(other) { a, b -> a || b }
    infix fun and(other: Toggleable) = combine(other) { a, b -> a && b }
}