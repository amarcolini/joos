package com.amarcolini.joos.hardware.gamepad

/**
 * Represents the state of a trigger.
 */
class Trigger @JvmOverloads constructor(
    value: Float = 0.0f,
    var threshold: Float = 0.5f
) : Toggleable() {
    var value: Float = value
        set(value) {
            lastValue = value
            field = value
        }
    var lastValue: Float = value
        private set

    override val state: Boolean
        get() = value >= threshold
    override val lastState: Boolean
        get() = lastValue >= threshold

    fun update(newValue: Float) {
        value = newValue
    }
}