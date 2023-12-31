package com.amarcolini.joos.gamepad

import com.amarcolini.joos.command.Component
import com.qualcomm.robotcore.hardware.Gamepad
import java.util.function.BiFunction
import java.util.function.BooleanSupplier
import kotlin.reflect.KProperty0

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

    @JvmSynthetic
    operator fun invoke(buttons: MultipleGamepad.() -> KProperty0<Boolean>): BooleanSupplier =
        object : BooleanSupplier {
            private val supplier = buttons(this@MultipleGamepad)

            override fun getAsBoolean(): Boolean = supplier.get()
        }

    @JvmSynthetic
    operator fun invoke(buttons: MultipleGamepad.() -> Toggleable): Toggleable = buttons(this)

    fun <T> get(buttons: BiFunction<GamepadEx, GamepadEx, T>): T =
        buttons.apply(p1, p2)
}