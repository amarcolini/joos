package com.griffinrobotics.lib.hardware

import com.griffinrobotics.lib.command.CommandScheduler
import com.griffinrobotics.lib.command.Component
import com.griffinrobotics.lib.control.FeedforwardCoefficients
import com.griffinrobotics.lib.control.PIDCoefficients
import com.griffinrobotics.lib.control.PIDFController
import com.griffinrobotics.lib.util.NanoClock
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.math.*


/**
 * A wrapper for the [DcMotor] object in the FTC SDK.
 *
 * @param motors the motors for this wrapper to use
 * @param CPR the counts per revolution of the motor
 * @param maxRPM the revolutions per minute of the motor
 */
class Motor @JvmOverloads constructor(
    val maxRPM: Double,
    val CPR: Double = 1.0,
    private vararg val motors: DcMotor
) : Component {
    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param CPR the counts per revolution of the motor
     * @param maxRPM the maximum revolutions per minute of the motor
     */
    @JvmOverloads
    constructor(hMap: HardwareMap, id: String, maxRPM: Double, CPR: Double = 1.0) : this(
        maxRPM,
        CPR,
        hMap.get(DcMotor::class.java, id)
    )

    /**
     * @param motors the motors for this wrapper to use
     * @param CPR the counts per revolution of the motor
     * @param maxRPM the revolutions per minute of the motor
     * @param wheelRadius the radius of the wheel this motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel
     */
    @JvmOverloads
    constructor(
        maxRPM: Double,
        CPR: Double = 1.0,
        wheelRadius: Double,
        gearRatio: Double,
        vararg motors: DcMotor
    ) : this(
        maxRPM, CPR, *motors
    ) {
        distancePerPulse = (wheelRadius * 2 * PI * gearRatio / CPR)
    }

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param CPR the counts per revolution of the motor
     * @param maxRPM the revolutions per minute of the motor
     * @param wheelRadius the radius of the wheel this motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        maxRPM: Double,
        CPR: Double = 1.0,
        wheelRadius: Double,
        gearRatio: Double
    ) : this(maxRPM, CPR, wheelRadius, gearRatio, hMap.get(DcMotor::class.java, id))

    enum class RunMode {
        /**
         * Controls the velocity of the motor using feedforward and/or encoders.
         */
        VelocityControl,

        /**
         * Uses an encoder to move the motor to the desired setpoint.
         */
        PositionControl,

        /**
         * Sets the raw voltage given to the motor.
         */
        RawPower
    }

    enum class ZeroPowerBehavior {
        BRAKE, FLOAT, UNKNOWN
    }

    /**
     * The encoder object for the motor.
     *
     * @param getPosition  the position supplier which just points to the
     * current position of the motor in ticks
     */
    inner class Encoder(
        private val getPosition: () -> Int,
        private val clock: NanoClock = NanoClock.system()
    ) {
        private var resetVal = 0
        private var lastPosition = 0
        var reversed: Boolean = false
        private var lastTimeStamp: Double = clock.seconds()
        private var veloEstimate = 0.0

        /**
         * The distance per pulse of the encoder in units per tick.
         */
        var distancePerPulse = 1.0

        /**
         * The current position of the encoder
         */
        val position: Int
            get() {
                val currentPosition = getPosition()
                if (currentPosition != lastPosition) {
                    val currentTime = clock.seconds()
                    val dt = currentTime - lastTimeStamp
                    veloEstimate = (currentPosition - lastPosition) / dt
                    lastPosition = getPosition()
                    lastTimeStamp = currentTime
                }
                return (if (reversed) -1.0 else 1.0 * (currentPosition - resetVal)).toInt()
            }

        /**
         * The distance traveled by the encoder.
         */
        val distance: Double
            get() = distancePerPulse * position

        /**
         * The velocity of the encoder in units per second. Computed using [distance].
         */
        val distanceVelocity: Double
            get() = distancePerPulse * correctedVelocity

        /**
         * Resets the encoder without having to stop the motor.
         */
        fun reset() {
            resetVal = getPosition()
        }

        /**
         * The number of revolutions turned by the encoder.
         */
        val revolutions: Double
            get() = getPosition() / CPR
        val rawVelocity: Double
            get() = velocity

        /**
         * The corrected velocity of the encoder, accounting for overflow.
         */
        val correctedVelocity: Double
            get() {
                var real = rawVelocity
                while (abs(veloEstimate - real) > 0x10000 / 2.0) {
                    real += sign(veloEstimate - real) * 0x10000
                }
                return real
            }
    }

    val encoder = Encoder(motors[0]::getCurrentPosition)

    var runMode: RunMode = RunMode.RawPower
    var zeroPowerBehavior: ZeroPowerBehavior = ZeroPowerBehavior.FLOAT
        set(value) {
            motors.forEach { it.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.valueOf(value.name) }
            field = ZeroPowerBehavior.valueOf(motors[0].zeroPowerBehavior.name)
        }
        get() = ZeroPowerBehavior.valueOf(motors[0].zeroPowerBehavior.name)

    var veloCoefficients = PIDCoefficients(1.0)
        set(value) {
            veloController.pid = value
            field = value
        }
    var positionCoefficients = PIDCoefficients(1.0)
        set(value) {
            positionController.pid = value
            field = value
        }
    var feedforwardCoefficients = FeedforwardCoefficients(1.0)
        set(value) {
            veloController.feedforward = value
            field = value
        }

    /**
     * The target position used by [RunMode.PositionControl]
     */
    var targetPosition: Int = 0
        set(value) {
            positionController.targetPosition = value.toDouble()
            field = value
        }

    var reversed: Boolean = false
        set(value) {
            motors.forEach {
                it.direction =
                    if (reversed) DcMotorSimple.Direction.REVERSE
                    else DcMotorSimple.Direction.FORWARD
            }
            field = value
        }
        get() = motors[0].direction == DcMotorSimple.Direction.REVERSE

    /**
     * The position error considered tolerable for [RunMode.PositionControl] to be considered at the set point.
     */
    var positionTolerance: Double = 0.05
        set(value) {
            positionController.tolerance = positionTolerance
            field = value
        }


    private var veloController = PIDFController(veloCoefficients)
    private var positionController = PIDFController(positionCoefficients)

    private var speed: Double = 0.0
    var targetAcceleration: Double = 0.0
        set(value) {
            veloController.targetAcceleration = value
            field = value
        }

    /**
     * The maximum achievable ticks per second velocity of the motor.
     */
    val maxTPS: Double = CPR * (maxRPM / 60)

    /**
     * Sets the speed of the motor.
     *
     * @param output the percentage of power/velocity to set. Should be in the range `[-1.0, 1.0]`
     */
    fun set(output: Double) {
        speed = min(1.0, max(-1.0, output))
        when (runMode) {
            RunMode.VelocityControl -> {
                veloController.targetPosition = speed * maxTPS
                veloController.targetVelocity = speed * maxTPS
                motors.forEach {
                    it.power =
                        veloController.update(correctedVelocity) / maxTPS
                }
            }
            RunMode.PositionControl -> {
                motors.forEach {
                    it.power =
                        speed * positionController.update(currentPosition.toDouble())
                }
            }
            RunMode.RawPower -> motors.forEach { it.power = speed }
        }
    }

    /**
     * Sets the velocity of the motor in rotations per second.
     * @see set
     */
    fun setVelocity(output: Double) {
        set(output / maxRPM)
    }

    override fun update(scheduler: CommandScheduler) {
        when (runMode) {
            RunMode.VelocityControl -> motors.forEach {
                it.power =
                    veloController.update(velocity) / maxTPS
            }
            RunMode.PositionControl -> motors.forEach {
                it.power =
                    speed * positionController.update(encoder.position.toDouble())
            }
            else -> return
        }
    }

    /**
     * The distance per pulse of the encoder in units per tick.
     */
    var distancePerPulse: Double = encoder.distancePerPulse
        get() = encoder.distancePerPulse
        set(value) {
            encoder.distancePerPulse = distancePerPulse
            field = value
        }

    /**
     * Returns whether the motor is currently moving towards the desired setpoint using [RunMode.PositionControl].
     */
    fun isBusy() = runMode == RunMode.PositionControl && !positionController.isAtSetPoint()

    /**
     * The velocity of the motor in encoder ticks per second using only the motor power and [maxTPS].
     */
    val velocity
        get() = motors[0].power * maxTPS

    /**
     * The encoder velocity corrected for overflow.
     */
    val correctedVelocity
        get() = encoder.correctedVelocity

    /**
     * The current position of the encoder.
     */
    val currentPosition
        get() = encoder.position

    /**
     * Resets the encoder.
     */
    fun resetEncoder() = encoder.reset()

    /**
     * The distance travelled by the motor. Computed using the encoder and its distance per pulse.
     */
    val distance
        get() = encoder.distance

    /**
     * The velocity of the motor in units per second. Computed using [distance].
     */
    val distanceVelocity
        get() = encoder.distanceVelocity

    /**
     * Returns the percentage of power currently set (in the range `[-1.0, 1.0]`).
     */
    fun get() = motors[0].power
}