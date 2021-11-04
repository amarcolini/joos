package com.griffinrobotics.lib.drive

import com.griffinrobotics.lib.control.FeedforwardCoefficients
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.TankKinematics
import com.griffinrobotics.lib.localization.Localizer
import com.griffinrobotics.lib.localization.TankLocalizer
import com.griffinrobotics.lib.util.Angle

/**
 * This class provides the basic functionality of a tank/differential drive using [TankKinematics].
 *
 * @param feedforward motor feedforward coefficients
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 */
abstract class AbstractTankDrive constructor(
    var feedforward: FeedforwardCoefficients,
    private val trackWidth: Double
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
        val powers =
            Kinematics.calculateMotorFeedforward(velocities, accelerations, feedforward)
        setMotorPowers(powers[0], powers[1])
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val powers = TankKinematics.robotToWheelVelocities(drivePower, 1.0)
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
