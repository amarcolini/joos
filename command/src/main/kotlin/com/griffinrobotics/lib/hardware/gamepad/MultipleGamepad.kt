package com.griffinrobotics.lib.hardware.gamepad

import com.griffinrobotics.lib.command.CommandScheduler
import com.griffinrobotics.lib.command.Component

/**
 * A container class that holds two gamepads for convenience.
 */
class MultipleGamepad(
    @JvmField
    val p1: GamepadEx,
    @JvmField
    val p2: GamepadEx
) : Component {
    override fun update(scheduler: CommandScheduler) {
        p1.update(scheduler)
        p2.update(scheduler)
    }
}