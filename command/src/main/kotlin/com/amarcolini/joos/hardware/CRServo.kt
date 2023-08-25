package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.WaitCommand
import com.amarcolini.joos.geometry.Angle
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.math.PI

/**
 * A wrapper for the [CRServo] object in the FTC SDK.
 *
 * @param servo the servo for this wrapper to use
 */
class CRServo constructor(
    private val servo: CRServo
) : Component {
    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     */
    constructor(
        hMap: HardwareMap,
        id: String
    ) : this(hMap.get(CRServo::class.java, id))

    /**
     * Whether the servo is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the servo is reversed.
         */
        @JvmName("setReversed") set(value) {
            servo.direction = if (value) DcMotorSimple.Direction.REVERSE
            else DcMotorSimple.Direction.FORWARD
            field = value
        }
        @JvmName("isReversed") get() = servo.direction == DcMotorSimple.Direction.REVERSE

    /**
     * Reverses the direction of the servo.
     */
    fun reversed(): com.amarcolini.joos.hardware.CRServo {
        reversed = !reversed
        return this
    }

    /**
     * Returns a command that runs the servo for a desired time.
     */
    fun runFor(seconds: Double, power: Double): Command =
        WaitCommand(seconds)
            .onInit { this.power = power }
            .onEnd { this.power = 0.0 }
            .requires(this)

    /**
     * The percentage of velocity of the servo in the range `[-1.0, 1.0]`.
     */
    var power: Double = 0.0
        set(value) {
            servo.power = value
            field = value
        }
        get() = servo.power
}