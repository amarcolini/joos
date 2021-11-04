package com.griffinrobotics.lib.hardware.gamepad

/**
 * Represents the state of any toggleable.
 */
abstract class Toggleable {
    abstract val state: Boolean
    abstract val lastState: Boolean
    val isActive get() = state
    val justActivated @JvmName("justActivated") get() = state && !lastState
    val justDeactivated @JvmName("justDeactivated") get() = !state && lastState
    val justChanged @JvmName("justChanged") get() = state != lastState
}