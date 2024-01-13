package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.DCMotorFeedforward
import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.hardware.Motor.RunMode
import com.qualcomm.robotcore.hardware.HardwareMap

/**
 * A class that runs multiple motors together as a unit.
 */
class MotorGroup(private val motors: List<Motor>) : Component, List<Motor> by motors {
    constructor(vararg motors: Motor) : this(motors.toList())

    /**
     * Constructs a motor group out of several motor groups.
     */
    constructor(vararg motors: MotorGroup) :
            this(*motors.flatMap { it.motors.asIterable() }.toTypedArray())

    /**
     * @param hMap the hardware map from the OpMode
     * @param maxRPM the maximum revolutions per minute of all the motors
     * @param TPR the ticks per revolution of all the motors
     * @param ids the device ids from the RC config
     */
    @JvmOverloads
    constructor(hMap: HardwareMap, maxRPM: Double, TPR: Double = 1.0, vararg ids: String) : this(
        *ids.map { Motor(hMap, it, maxRPM, TPR) }.toTypedArray()
    )

    /**
     * @param hMap the hardware map from the OpMode
     * @param type the kind of all the motors
     * @param ids the device ids from the RC config
     */
    constructor(hMap: HardwareMap, type: Motor.Type, vararg ids: String) : this(
        *ids.map { Motor(hMap, it, type.maxRPM, type.TPR) }.toTypedArray()
    )

    /**
     * @param hMap the hardware map from the OpMode
     * @param maxRPM the maximum revolutions per minute of all the motors
     * @param TPR the ticks per revolution of all the motors
     * @param ids the device ids from the RC config and whether those motors should be reversed
     */
    @JvmOverloads
    @SafeVarargs
    constructor(hMap: HardwareMap, maxRPM: Double, TPR: Double = 1.0, vararg ids: Pair<String, Boolean>) : this(
        *ids.map { Motor(hMap, it.first, maxRPM, TPR).apply { reversed = it.second } }.toTypedArray()
    )

    /**
     * @param hMap the hardware map from the OpMode
     * @param type the kind of all the motors
     * @param ids the device ids from the RC config and whether those motors should be reversed
     */
    @SafeVarargs
    constructor(hMap: HardwareMap, type: Motor.Type, vararg ids: Pair<String, Boolean>) : this(
        hMap, type.maxRPM, type.TPR, *ids
    )

    private val states = motors.associateWith { it.reversed }

    /**
     * The maximum revolutions per minute that all motors in the group can achieve.
     */
    @JvmField
    val maxRPM: Double = motors.minOf { it.maxRPM }

    /**
     * The maximum distance velocity that all motors in the group can achieve.
     */
    val maxDistanceVelocity: Double get() = motors.minOf { it.maxDistanceVelocity }

    /**
     * The maximum ticks per second velocity that all motors in the group can achieve.
     */
    @JvmField
    val maxTPS: Double = motors.minOf { it.maxTPS }

    /**
     * A list of the individual rotations of each motor.
     */
    val rotation: List<Angle> get() = motors.map { it.rotation }

    /**
     * The average currentPosition that all motors in the group have travelled.
     * @see velocity
     * @see getPositions
     */
    val currentPosition: Double get() = getPositions().average()

    /**
     * The currentPosition that each motor in the group has travelled.
     * @see velocity
     * @see currentPosition
     * @see getVelocities
     */
    fun getPositions() = motors.map { it.currentPosition }

    /**
     * The average velocity of all motors in the group.
     */
    val velocity: Double get() = getVelocities().average()

    /**
     * The velocity of each motor in the group.
     * @see velocity
     * @see currentPosition
     * @see getPositions
     */
    fun getVelocities() = motors.map { it.velocity }

    /**
     * The average distance that all motors in the group have travelled.
     * @see distanceVelocity
     * @see getDistances
     */
    val distance: Double get() = getDistances().average()

    /**
     * The distance that each motor in the group has travelled.
     * @see distanceVelocity
     * @see distance
     * @see getDistanceVelocities
     */
    fun getDistances() = motors.map { it.distance }

    /**
     * The average distance velocity of all motors in the group.
     */
    val distanceVelocity: Double get() = getDistanceVelocities().average()

    /**
     * The distance velocity of each motor in the group.
     * @see distanceVelocity
     * @see distance
     * @see getDistances
     */
    fun getDistanceVelocities() = motors.map { it.distanceVelocity }

    /**
     * Whether the motor group is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the motor group is reversed.
         */
        @JvmSynthetic
        set(value) {
            motors.forEach {
                it.reversed = value != (states[it] == true)
            }
            field = value
        }
        @JvmName("isReversed")
        get

    fun setReversed(reversed: Boolean): MotorGroup {
        this.reversed = reversed
        return this
    }

    /**
     * Reverses the direction of the motor group.
     */
    fun reversed(): MotorGroup {
        reversed = !reversed
        return this
    }

    /**
     * Sets the velocity/accleration of all motors in encoder ticks per second.
     *
     * *Note*: Because of physical constraints, motors may move at different speeds.
     *
     * @param velocity The velocity to set.
     * @param acceleration The acceleration to set.
     */
    @JvmOverloads
    fun setTickVelocity(velocity: Double, acceleration: Double = 0.0) =
        motors.forEach { it.setTickVelocity(velocity, acceleration) }

    /**
     * Sets the velocities/accelerations of each motor in encoder ticks per second.
     *
     * *Note*: Because of physical constraints, motors may move at different speeds.
     *
     * @param velocities The velocities for each motor.
     * @param accelerations The accelerations for each motor.
     */
    @JvmOverloads
    fun setTickVelocities(velocities: List<Double>, accelerations: List<Double> = emptyList()) =
        motors.forEachIndexed { i, motor ->
            velocities.getOrNull(i)?.let { motor.setTickVelocity(it, accelerations.getOrElse(i) { 0.0 }) }
        }

    /**
     * Sets the velocity/acceleration of all motors in distance units per second.
     *
     * *Note*: Because of physical constraints, motors may move at different speeds.
     *
     * @param velocity The velocity to set.
     * @param acceleration The acceleration to set.
     */
    @JvmOverloads
    fun setDistanceVelocity(velocity: Double, acceleration: Double = 0.0) =
        motors.forEach { it.setDistanceVelocity(velocity, acceleration) }

    /**
     * Sets the velocities/accelerations of each motor in encoder ticks per second.
     *
     * *Note*: Because of physical constraints, motors may move at different speeds.
     *
     * @param velocities The velocities for each motor.
     * @param accelerations The accelerations for each motor.
     */
    @JvmOverloads
    fun setDistanceVelocities(velocities: List<Double>, accelerations: List<Double> = emptyList()) =
        motors.forEachIndexed { i, motor ->
            velocities.getOrNull(i)?.let { motor.setDistanceVelocity(it, accelerations.getOrElse(i) { 0.0 }) }
        }

    /**
     * Sets the velocity of all motors in revolutions per second.
     *
     * *Note*: Because of physical constraints, motors may move at different speeds.
     *
     * @param rpm The revolutions per second to set.
     */
    fun setRPM(rpm: Double) = motors.forEach { it.setRPM(rpm) }

    /**
     * Sets the percentage of power/velocity of all motors in the range `[-1.0, 1.0]`.
     *
     * *Note*: Since power is expressed as a percentage, motors may move at different speeds.
     */
    fun setPower(power: Double): Unit = motors.forEach { it.power = power }

    /**
     * Sets the percentage of power/velocity of each motor in the range `[-1.0, 1.0]`.
     *
     * *Note*: Since power is expressed as a percentage, motors may move at different speeds.
     */
    fun setPowers(powers: List<Double>): Unit = motors.zip(powers).forEach { (motor, power) -> motor.power = power }

    /**
     * Returns the previously set zero power behavior. Note that this may not accurately reflect the actual zero
     * power behavior of all the motors, since they are independent of each other.
     */
    var zeroPowerBehavior: Motor.ZeroPowerBehavior = Motor.ZeroPowerBehavior.FLOAT
        /**
         * Sets the distance per tick of all the motors in the group.
         */
        set(value) {
            motors.forEach { it.zeroPowerBehavior = value }
            field = value
        }

    /**
     * Returns the previously set run mode. Note that this may not accurately reflect the actual run mode
     * of all the motors, since they are independent of each other.
     */
    var runMode: RunMode = RunMode.RUN_WITHOUT_ENCODER
        /**
         * Sets the distance per tick of all the motors in the group.
         */
        set(value) {
            motors.forEach { it.runMode = value }
            field = value
        }

    /**
     * Returns the previously set PID coefficients. Note that this may not accurately reflect the actual coefficients
     * of all the motors, since they are independent of each other.
     */
    var veloCoefficients: PIDCoefficients = PIDCoefficients(1.0)
        /**
         * Sets the distance per tick of all the motors in the group.
         */
        set(value) {
            motors.forEach { it.veloCoefficients = value }
            field = value
        }

    /**
     * Returns the previously set feedforward. Note that this may not accurately reflect the actual feedforward
     * of all the motors, since they are independent of each other.
     */
    var feedforward: Feedforward = DCMotorFeedforward(1 / maxTPS)
        /**
         * Sets the feedforward of all the motors in the group.
         */
        set(value) {
            motors.forEach { it.feedforward = value }
            field = value
        }

    /**
     * Returns the previously set distance per tick. Note that this may not accurately reflect the actual distance
     * per tick of all the motors, since they are independent of each other.
     */
    var distancePerTick: Double = 1.0
        /**
         * Sets the distance per tick of all the motors in the group.
         */
        set(value) {
            motors.forEach { it.distancePerTick = value }
            field = value
        }

    /**
     * Resets the encoders of all the motors in the group.
     */
    fun resetEncoders(): Unit = motors.forEach { it.resetEncoder() }

    /**
     * Updates [RunMode.RUN_USING_ENCODER]. Running this method is
     * not necessary for [RunMode.RUN_WITHOUT_ENCODER].
     */
    override fun update() {
        motors.forEach { it.update() }
    }
}