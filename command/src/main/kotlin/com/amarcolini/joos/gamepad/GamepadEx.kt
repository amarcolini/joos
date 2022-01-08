package com.amarcolini.joos.gamepad

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.geometry.Vector2d
import com.qualcomm.robotcore.hardware.Gamepad

/**
 * A class that simplifies the use of [Gamepad]s.
 */
class GamepadEx(gamepad: Gamepad) : Component {
    val internal = gamepad

    @JvmField
    val a: Button = Button()

    @JvmField
    val b: Button = Button()

    @JvmField
    val back: Button = Button()

    @JvmField
    val circle: Button = Button()

    @JvmField
    val cross: Button = Button()

    @JvmField
    val dpad_down: Button = Button()

    @JvmField
    val dpad_left: Button = Button()

    @JvmField
    val dpad_right: Button = Button()

    @JvmField
    val dpad_up: Button = Button()

    @JvmField
    val guide: Button = Button()

    @JvmField
    val left_bumper: Button = Button()

    @JvmField
    val left_stick_button: Button = Button()

    @JvmField
    val left_trigger: Trigger = Trigger()

    @JvmField
    val options: Button = Button()

    @JvmField
    val ps: Button = Button()

    @JvmField
    val right_bumper: Button = Button()

    @JvmField
    val right_stick_button: Button = Button()

    @JvmField
    val right_trigger: Trigger = Trigger()

    @JvmField
    val share: Button = Button()

    @JvmField
    val square: Button = Button()

    @JvmField
    val start: Button = Button()

    @JvmField
    val touchpad: Button = Button()

    @JvmField
    val touchpad_finger_1: Button = Button()

    @JvmField
    val touchpad_finger_2: Button = Button()

    @JvmField
    val triangle: Button = Button()

    @JvmField
    val x: Button = Button()

    @JvmField
    val y: Button = Button()

    private var leftStick = getLeftStick()
    private var rightStick = getRightStick()
    var leftStickChanged: Boolean = false
        private set
        @JvmName("leftStickChanged") get
    var rightStickChanged: Boolean = false
        private set
        @JvmName("rightStickChanged") get

    override fun update() {
        a.update(internal.a)
        b.update(internal.b)
        back.update(internal.back)
        circle.update(internal.circle)
        cross.update(internal.cross)
        dpad_down.update(internal.dpad_down)
        dpad_left.update(internal.dpad_left)
        dpad_right.update(internal.dpad_right)
        dpad_up.update(internal.dpad_up)
        guide.update(internal.guide)
        left_bumper.update(internal.left_bumper)
        left_stick_button.update(internal.left_stick_button)
        left_trigger.update(internal.left_trigger)
        options.update(internal.options)
        ps.update(internal.ps)
        right_bumper.update(internal.right_bumper)
        right_stick_button.update(internal.right_stick_button)
        right_trigger.update(internal.right_trigger)
        share.update(internal.share)
        square.update(internal.square)
        start.update(internal.start)
        touchpad.update(internal.touchpad)
        touchpad_finger_1.update(internal.touchpad_finger_1)
        touchpad_finger_2.update(internal.touchpad_finger_2)
        triangle.update(internal.triangle)
        x.update(internal.x)
        y.update(internal.y)
        leftStickChanged = getLeftStick() epsilonEquals leftStick
        rightStickChanged = getRightStick() epsilonEquals rightStick
        leftStick = getLeftStick()
        rightStick = getRightStick()
    }

    fun getButton(button: GamepadButton): Toggleable = when (button) {
        GamepadButton.Y -> y
        GamepadButton.X -> x
        GamepadButton.A -> a
        GamepadButton.B -> b
        GamepadButton.LEFT_BUMPER -> left_bumper
        GamepadButton.RIGHT_BUMPER -> right_bumper
        GamepadButton.BACK -> back
        GamepadButton.START -> start
        GamepadButton.DPAD_UP -> dpad_up
        GamepadButton.DPAD_DOWN -> dpad_down
        GamepadButton.DPAD_LEFT -> dpad_left
        GamepadButton.DPAD_RIGHT -> dpad_right
        GamepadButton.LEFT_STICK_BUTTON -> left_stick_button
        GamepadButton.RIGHT_STICK_BUTTON -> right_stick_button
        GamepadButton.LEFT_TRIGGER -> left_trigger
        GamepadButton.RIGHT_TRIGGER -> right_trigger
        GamepadButton.CIRCLE -> circle
        GamepadButton.CROSS -> cross
        GamepadButton.GUIDE -> guide
        GamepadButton.OPTIONS -> options
        GamepadButton.PS -> ps
        GamepadButton.SHARE -> share
        GamepadButton.SQUARE -> square
        GamepadButton.TOUCHPAD -> touchpad
        GamepadButton.TOUCHPAD_FINGER_1 -> touchpad_finger_1
        GamepadButton.TOUCHPAD_FINGER_2 -> touchpad_finger_2
        GamepadButton.TRIANGLE -> triangle
    }

    fun isActive(button: GamepadButton) = getButton(button).isActive

    fun justActivated(button: GamepadButton) = getButton(button).justActivated

    fun justDeactivated(button: GamepadButton) = getButton(button).justDeactivated

    fun justChanged(button: GamepadButton) = getButton(button).justChanged

    fun getLeftStick() = Vector2d(
        internal.left_stick_x.toDouble(),
        internal.left_stick_y.toDouble()
    )

    fun getRightStick() = Vector2d(
        internal.right_stick_x.toDouble(),
        internal.right_stick_y.toDouble()
    )

    fun getTouchpadFinger1() = Vector2d(
        internal.touchpad_finger_1_x.toDouble(),
        internal.touchpad_finger_1_y.toDouble()
    )

    fun getTouchpadFinger2() = Vector2d(
        internal.touchpad_finger_2_x.toDouble(),
        internal.touchpad_finger_2_y.toDouble()
    )
}

