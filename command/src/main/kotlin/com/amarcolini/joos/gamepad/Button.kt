package com.amarcolini.joos.gamepad

/**
 * Represents the state of a button.
 */
class Button(
    state: Boolean = false,
) : Toggleable() {
    override var state: Boolean = state
        private set(value) {
            lastState = field
            field = value
        }
    override var lastState: Boolean = state
        private set

    fun update(newState: Boolean) {
        state = newState
    }

    fun toggle() {
        state = !state
    }
}