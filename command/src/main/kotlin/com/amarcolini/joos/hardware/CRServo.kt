package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.WaitCommand
import com.amarcolini.joos.util.NanoClock
import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap

/**
 * A wrapper for the [CRServo] object in the FTC SDK.
 *
 * @param servo the servo for this wrapper to use
 * @param maxRPM the max RPM of the servo
 */
class CRServo constructor(
    private val servo: CRServo, @JvmField val maxRPM: Double
) : Component {
    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param maxRPM the maximum revolutions per minute of the motor
     */
    constructor(
        hMap: HardwareMap, id: String, maxRPM: Double
    ) : this(
        hMap.get(CRServo::class.java, id), maxRPM
    )

    /**
     * A class representing the different ways to measure motor speed.
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
     * Whether the motor is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the motor is reversed.
         */
        @JvmName("setReversed") set(value) {
            servo.direction = if (value) DcMotorSimple.Direction.REVERSE
            else DcMotorSimple.Direction.FORWARD
            field = value
        }
        @JvmName("isReversed") get() = servo.direction == DcMotorSimple.Direction.REVERSE

    private var speed: Double = 0.0

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
     * @param velocity the velocity to set
     * @param unit the units [velocity] is expressed in (revolutions per minute by default).
     */
    @JvmOverloads
    fun setSpeed(
        velocity: Double, unit: RotationUnit = RotationUnit.RPM
    ) {
        val multiplier = when (unit) {
            RotationUnit.RPM -> 1.0
            RotationUnit.DPS -> maxRPM * 6
            RotationUnit.RPS -> (maxRPM / 60) * 2 * Math.PI
        }
        val vel = velocity * multiplier
        speed = (vel / maxRPM).coerceIn(-1.0, 1.0)

        servo.power = speed
    }

    /**
     * Returns a command that runs the motor for a desired time.
     */
    @JvmOverloads
    fun runForSeconds(seconds: Double, speed: Double, unit: RotationUnit = RotationUnit.RPM): Command =
        WaitCommand(seconds)
            .onInit { setSpeed(speed, unit) }
            .onEnd { setSpeed(0.0) }


    /**
     * The percentage of power/velocity of the motor in the range `[-1.0, 1.0]`.
     */
    var power: Double = 0.0
        set(value) {
            setSpeed(value * maxRPM)
            field = value
        }
        get() = speed
}