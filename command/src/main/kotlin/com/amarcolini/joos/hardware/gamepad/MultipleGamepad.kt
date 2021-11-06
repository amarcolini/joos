package com.amarcolini.joos.hardware.gamepad

import com.amarcolini.joos.command.CommandScheduler
import com.amarcolini.joos.command.Component
import com.qualcomm.robotcore.hardware.Gamepad

/**
 * A container class that holds two gamepads for convenience.
 */
class MultipleGamepad(
    @JvmField
    val p1: GamepadEx,
    @JvmField
    val p2: GamepadEx
) : Component {
    constructor(p1: Gamepad, p2: Gamepad) : this(GamepadEx(p1), GamepadEx(p2))

    override fun update() {
        p1.update()
        p2.update()
    }
}