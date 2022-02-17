package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.FeedforwardCoefficients
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.hardware.Motor.RunMode
import com.qualcomm.robotcore.hardware.HardwareMap

/**
 * A class that runs multiple motors together as a unit.
 */
class MotorGroup(private vararg val motors: Motor) : Component {
    /**
     * Constructs a motor group out of several motor groups.
     */
    constructor(vararg motors: MotorGroup) :
            this(*motors.flatMap { it.motors.asIterable() }.toTypedArray())

    /**
     * @param hMap the hardware map from the OpMode
     * @param maxRPM the maximum revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     * @param ids the device ids from the RC config
     */
    @JvmOverloads
    constructor(hMap: HardwareMap, maxRPM: Double, TPR: Double = 1.0, vararg ids: String) : this(
        *ids.map { Motor(hMap, it, maxRPM, TPR) }.toTypedArray()
    )


    /**
     * @param hMap the hardware map from the OpMode
     * @param maxRPM the revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     * @param wheelRadius the radius of the wheel the motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     * @param ids the device ids from the RC config
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        maxRPM: Double,
        TPR: Double = 1.0,
        wheelRadius: Double,
        gearRatio: Double,
        vararg ids: String,
    ) : this(
        *ids.map { Motor(hMap, it, maxRPM, TPR, wheelRadius, gearRatio) }.toTypedArray()
    )

    private val states = motors.map { it to it.reversed }.toMap()

    /**
     * The maximum revolutions per minute that all motors in the group can achieve.
     */
    @JvmField
    val maxRPM = motors.minOf { it.maxRPM }

    /**
     * The maximum ticks per second velocity that all motors in the group can achieve.
     */
    @JvmField
    val maxTPS: Double = motors.minOf { it.maxTPS }

    /**
     * The maximum distance travelled that all motors in the group have travelled.
     * @see distanceVelocity
     */
    val distance get() = motors.minOf { it.distance }

    /**
     * The minimum distance velocity out of all motors in the group.
     */
    val distanceVelocity get() = motors.minOf { it.distanceVelocity }

    /**
     * Whether the motor group is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the motor group is reversed.
         */
        @JvmName("setReversed")
        set(value) {
            motors.forEach {
                it.reversed = value != (states[it] == true)
            }
            field = value
        }
        @JvmName("isReversed")
        get

    /**
     * Reverses the direction of the motor group.
     */
    fun reversed(): MotorGroup {
        reversed = !reversed
        return this
    }

    /**
     * Sets the speed of the motor.
     *
     * @param velocity the velocity to set
     * @param acceleration the acceleration to set
     * @param unit the units [velocity] and [acceleration] are expressed in (revolutions per minute by default)
     */
    @JvmOverloads
    fun setSpeed(
        velocity: Double,
        acceleration: Double = 0.0,
        unit: Motor.RotationUnit = Motor.RotationUnit.RPM
    ) = motors.forEach { it.setSpeed(velocity, acceleration, unit) }

    /**
     * Sets the percentage of power/velocity of the motor group in the range `[-1.0, 1.0]`.
     *
     * *Note*: Since power is expressed as a percentage, motors may move at different speeds.
     */
    fun setPower(power: Double) = motors.forEach { it.power = power }

    var zeroPowerBehavior: Motor.ZeroPowerBehavior = Motor.ZeroPowerBehavior.FLOAT
        set(value) {
            motors.forEach { it.zeroPowerBehavior = value }
            field = value
        }

    var runMode: RunMode = RunMode.RUN_WITHOUT_ENCODER
        set(value) {
            motors.forEach { it.runMode = value }
            field = value
        }

    /**
     * PID coefficients used in [RunMode.RUN_USING_ENCODER].
     */
    var veloCoefficients = PIDCoefficients(1.0)
        set(value) {
            motors.forEach { it.veloCoefficients = value }
            field = value
        }

    /**
     * PID coefficients used in [RunMode.RUN_TO_POSITION].
     */
    var positionCoefficients = PIDCoefficients(1.0)
        set(value) {
            motors.forEach { it.positionCoefficients = value }
            field = value
        }

    /**
     * Feedforward coefficients used in both [RunMode.RUN_USING_ENCODER] and [RunMode.RUN_WITHOUT_ENCODER].
     */
    var feedforwardCoefficients = FeedforwardCoefficients(1.0)
        set(value) {
            motors.forEach { it.feedforwardCoefficients = value }
            field = value
        }

    /**
     * The target position used by [RunMode.RUN_TO_POSITION].
     */
    var targetPosition: Int = 0
        set(value) {
            motors.forEach { it.targetPosition = value }
            field = value
        }

    /**
     * The position error considered tolerable for [RunMode.RUN_TO_POSITION] to be considered at the set point.
     */
    var positionTolerance: Int = 10
        set(value) {
            motors.forEach { it.positionTolerance = value }
            field = value
        }

    /**
     * Returns a command that runs all the motors in the group until all of them have reached the desired position.
     */
    fun goToPosition(position: Int) = Command.of {
        runMode = RunMode.RUN_TO_POSITION
        targetPosition = position
    }
        .onInit {
            runMode = RunMode.RUN_TO_POSITION
            targetPosition = position
        }
        .requires(this)
        .runUntil { !isBusy() }
        .onEnd { setSpeed(0.0) }

    /**
     * Returns a command that runs all the motors in the group until all of them have reached the desired distance.
     */
    fun goToDistance(distance: Double) = Command.of {
        runMode = RunMode.RUN_TO_POSITION
        motors.forEach {
            it.targetPosition = (distance / it.distancePerRev * it.TPR).toInt()
        }
    }
        .onInit {
            runMode = RunMode.RUN_TO_POSITION
            motors.forEach {
                it.targetPosition = (distance / it.distancePerRev * it.TPR).toInt()
            }
        }
        .requires(this)
        .runUntil { !isBusy() }
        .onEnd { setSpeed(0.0) }

    /**
     * Resets the encoders of all the motors in the group.
     */
    fun resetEncoder() = motors.forEach { it.resetEncoder() }

    /**
     * Returns whether any of the motors in the group are currently moving towards the desired setpoint using [RunMode.RUN_TO_POSITION].
     */
    fun isBusy() = motors.any { it.isBusy() }

    /**
     * Updates both [RunMode.RUN_USING_ENCODER] and [RunMode.RUN_TO_POSITION]. Running this method is
     * not necessary for [RunMode.RUN_WITHOUT_ENCODER].
     */
    override fun update() {
        motors.forEach { it.update() }
    }
}