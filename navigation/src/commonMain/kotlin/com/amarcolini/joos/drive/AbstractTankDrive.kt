package com.amarcolini.joos.drive

import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.TankKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.TankLocalizer
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

/**
 * This class provides the basic functionality of a tank/differential drive using [TankKinematics].
 *
 * @param trackWidth Lateral distance between pairs of wheels on different sides of the robot.
 */
abstract class AbstractTankDrive constructor(
    protected val trackWidth: Double,
    protected val externalHeadingSensor: AngleSensor? = null
) : Drive() {

    override var localizer: Localizer = TankLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        trackWidth,
    ).let { if (externalHeadingSensor != null) it.addHeadingSensor(externalHeadingSensor) else it }

    override fun setDriveSignal(driveSignal: DriveSignal) {
        val velocities = TankKinematics.robotToWheelVelocities(driveSignal.vel, trackWidth)
        val accelerations = TankKinematics.robotToWheelAccelerations(driveSignal.accel, trackWidth)
        setWheelVelocities(velocities, accelerations)
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val powers = TankKinematics.robotToWheelVelocities(drivePower.copy(heading = drivePower.heading.value.rad), 2.0)
        setMotorPowers(powers[0], powers[1])
    }

    /**
     * Sets the following motor powers (normalized voltages). All arguments are on the interval `[-1.0, 1.0]`.
     */
    abstract fun setMotorPowers(left: Double, right: Double)

    /**
     * Sets the wheel velocities (and accelerations) of each motor, in distance units per second. Velocities and accelerations
     * match the ordering in [setMotorPowers].
     */
    abstract fun setWheelVelocities(velocities: List<Double>, accelerations: List<Double>)

    /**
     * Returns the positions of the wheels in linear distance units. Positions should exactly match the ordering in
     * [setMotorPowers].
     */
    abstract fun getWheelPositions(): List<Double>

    /**
     * Returns the velocities of the wheels in linear distance units. Positions should exactly match the ordering in
     * [setMotorPowers].
     */
    open fun getWheelVelocities(): List<Double>? = null
}