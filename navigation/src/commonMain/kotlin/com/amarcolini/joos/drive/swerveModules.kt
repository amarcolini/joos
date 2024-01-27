package com.amarcolini.joos.drive

import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDController
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.wrap
import kotlin.math.PI
import kotlin.math.abs

abstract class SwerveModule {
    /**
     * Sets the wheel motor power (normalized voltage) on the interval `[-1.0, 1.0]`.
     */
    abstract fun setDrivePower(power: Double)

    /**
     * Sets the wheel velocity (and acceleration) of the wheel motor.
     */
    abstract fun setWheelVelocity(velocity: Double, acceleration: Double)

    abstract fun getModuleOrientation(): Angle

    abstract fun setModuleOrientation(angle: Angle)

    /**
     * Returns the positions of the wheel in linear distance units.
     */
    abstract fun getWheelPosition(): Double

    /**
     * Returns the velocity of the wheel in linear distance units.
     */
    open fun getWheelVelocity(): Double? = null

    /**
     * Updates the module.
     */
    abstract fun update()
}

/**
 * A basic [SwerveModule] implementation using a [PIDController] to hold module position.
 */
abstract class PIDSwerveModule(
    protected val pidCoefficients: PIDCoefficients
) : SwerveModule() {
    private var targetSpeed: (Double) -> Unit = { mult: Double ->
        setDrivePower(0.0)
    }
    protected val pidController = PIDController(pidCoefficients)

    init {
        pidController.setInputBounds(-PI / 2, PI / 2)
    }

    final override fun setModuleOrientation(angle: Angle) {
        pidController.targetPosition = angle.radians
    }

    final override fun setWheelVelocity(velocity: Double, acceleration: Double) {
        targetSpeed = {
            setCorrectedWheelVelocity(velocity * it, acceleration * it)
        }
    }

    /**
     * Sets the corrected wheel velocity (and acceleration) of the wheel motor.
     */
    abstract fun setCorrectedWheelVelocity(velocity: Double, acceleration: Double)

    final override fun setDrivePower(power: Double) {
        targetSpeed = {
            setCorrectedDrivePower(power * it)
        }
    }

    /**
     * Sets the corrected wheel motor power (normalized voltage) on the interval `[-1.0, 1.0]`.
     */
    abstract fun setCorrectedDrivePower(power: Double)

    override fun update() {
        val orientation = getModuleOrientation()
        val direction =
            if (abs((pidController.targetPosition - orientation.radians).wrap(-PI, PI)) <= (PI * 0.5)) 1.0
            else -1.0
        setModulePower(pidController.update(orientation.radians))
        targetSpeed.invoke(direction)
    }

    /**
     * Sets the power of the actuator which rotates the module.
     */
    abstract fun setModulePower(power: Double)
}

/**
 * A basic [SwerveModule] implementation using a [PIDController] to control a differential swerve module.
 */
abstract class PIDDiffSwerveModule(
    pidCoefficients: PIDCoefficients,
    protected var feedforward: Feedforward,
) : SwerveModule() {
    private var targetPower: Double = 0.0
    protected val pidController = PIDController(pidCoefficients)

    init {
        pidController.setInputBounds(-PI / 2, PI / 2)
    }

    final override fun setModuleOrientation(angle: Angle) {
        pidController.targetPosition = angle.radians
    }

    final override fun setWheelVelocity(velocity: Double, acceleration: Double) {
        targetPower = feedforward.calculate(velocity, acceleration)
    }

    final override fun setDrivePower(power: Double) {
        targetPower = power
    }

    override fun update() {
        val orientation = getModuleOrientation()
        val control = pidController.update(orientation.radians)
        val direction =
            if (abs((pidController.targetPosition - orientation.radians).wrap(-PI, PI)) <= (PI / 2)) 1
            else -1
        setMotorPowers(
            targetPower * direction + control,
            -targetPower * direction + control,
        )
    }

    override fun getWheelPosition(): Double {
        val positions = getGearPositions()
        return DiffSwerveKinematics.gearToWheelVelocities(positions[0], positions[1])
    }

    override fun getWheelVelocity(): Double? {
        val positions = getGearVelocities() ?: return null
        return DiffSwerveKinematics.gearToWheelVelocities(positions[0], positions[1])
    }

    override fun getModuleOrientation(): Angle {
        val (top, bottom) = getGearRotations()
        return DiffSwerveKinematics.gearToModuleOrientation(top, bottom)
    }

    /**
     * Sets the following motor powers (normalized voltages). All arguments are on the interval `[-1.0, 1.0]`.
     */
    abstract fun setMotorPowers(
        top: Double,
        bottom: Double,
    )

    /**
     * Returns the total rotation of the gears. Angles should exactly match the ordering in
     * [setMotorPowers].
     */
    abstract fun getGearRotations(): List<Angle>

    /**
     * Returns the positions of the gears in linear distance units. Positions should exactly match the ordering in
     * [setMotorPowers].
     */
    abstract fun getGearPositions(): List<Double>

    /**
     * Returns the velocities of the gears in linear distance units. Velocities should exactly match the ordering in
     * [setMotorPowers].
     */
    open fun getGearVelocities(): List<Double>? = null
}