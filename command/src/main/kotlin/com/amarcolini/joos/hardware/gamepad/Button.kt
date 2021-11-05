package com.amarcolini.joos.hardware.gamepad

class Button(
    state: Boolean = false,
) : Toggleable() {
    override var state: Boolean = state
        set(value) {
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