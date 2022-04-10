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
 * @param maxRPM the maximum revolutions per minute of the servo
 */
class CRServo constructor(
    private val servo: CRServo, @JvmField val maxRPM: Double
) : Component {
    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param maxRPM the maximum revolutions per minute of the servo
     */
    constructor(
        hMap: HardwareMap,
        id: String,
        maxRPM: Double
    ) : this(hMap.get(CRServo::class.java, id), maxRPM)

    /**
     * A class representing the different ways to measure servo speed.
     */
    enum class RotationUnit {
        /**
         * Revolutions per minute.
         */
        RPM,

        /**
         * Degrees per second.
         */
        DPS,

        /**
         * Radians per second.
         */
        RPS
    }

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
     * Sets the speed of the servo.
     *
     * @param speed the speed to set
     * @param unit the units [speed] are expressed in ([RotationUnit.RPM] by default).
     */
    @JvmOverloads
    fun setSpeed(speed: Double, unit: RotationUnit = RotationUnit.RPM) {
        //Converts the provided speed into revolutions per minute
        val multiplier = when (unit) {
            RotationUnit.RPM -> 1.0
            RotationUnit.DPS -> maxRPM * 6
            RotationUnit.RPS -> maxRPM / 30 * PI
        }
        val rpm = speed * multiplier
        servo.power = (rpm / maxRPM).coerceIn(-1.0, 1.0)
    }

    /**
     * Sets the speed of the servo.
     *
     * @param speed the speed to set, in units per second
     */
    fun setSpeed(speed: Angle) {
        servo.power = (speed.degrees / 6 / maxRPM).coerceIn(-1.0, 1.0)
    }

    /**
     * Returns a command that runs the servo for a desired time.
     */
    @JvmOverloads
    fun runFor(seconds: Double, speed: Double, unit: RotationUnit = RotationUnit.RPM): Command =
        WaitCommand(seconds)
            .onInit { setSpeed(speed, unit) }
            .onEnd { setSpeed(0.0) }
            .requires(this)

    /**
     * Returns a command that runs the servo for a desired time.
     */
    fun runFor(seconds: Double, speed: Angle): Command =
        WaitCommand(seconds)
            .onInit { setSpeed(speed) }
            .onEnd { setSpeed(0.0) }
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