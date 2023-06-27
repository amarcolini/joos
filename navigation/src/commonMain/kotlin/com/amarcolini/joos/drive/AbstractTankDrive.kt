package com.amarcolini.joos.drive

import com.amarcolini.joos.control.FeedforwardCoefficients
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.TankKinematics
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.TankLocalizer
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * This class provides the basic functionality of a tank/differential drive using [TankKinematics].
 *
 * @param feedforward motor feedforward coefficients
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 */
@JsExport
abstract class AbstractTankDrive constructor(
    protected val feedforward: FeedforwardCoefficients,
    protected val trackWidth: Double
) : Drive() {

    override var localizer: Localizer = TankLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        trackWidth,
        this, true
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        val velocities = TankKinematics.robotToWheelVelocities(driveSignal.vel, trackWidth)
        val accelerations = TankKinematics.robotToWheelAccelerations(driveSignal.accel, trackWidth)
        val powers = feedforward.calculate(velocities, accelerations)
        setMotorPowers(powers[0], powers[1])
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val powers = TankKinematics.robotToWheelVelocities(drivePower, 2.0)
        setMotorPowers(powers[0], powers[1])
    }

    /**
     * Sets the following motor powers (normalized voltages). All arguments are on the interval `[-1.0, 1.0]`.
     */
    abstract fun setMotorPowers(left: Double, right: Double)

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