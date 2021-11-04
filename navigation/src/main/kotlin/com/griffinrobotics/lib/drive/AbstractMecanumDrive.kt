package com.griffinrobotics.lib.drive

import com.griffinrobotics.lib.control.FeedforwardCoefficients
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.MecanumKinematics
import com.griffinrobotics.lib.localization.Localizer
import com.griffinrobotics.lib.localization.MecanumLocalizer

/**
 * This class provides the basic functionality of a mecanum drive using [MecanumKinematics].
 *
 * @param feedforward motor feedforward coefficients
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 * @param lateralMultiplier lateral multiplier
 */
abstract class AbstractMecanumDrive @JvmOverloads constructor(
    var feedforward: FeedforwardCoefficients,
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth,
    private val lateralMultiplier: Double = 1.0
) : Drive() {

    override var localizer: Localizer = MecanumLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        trackWidth, wheelBase, lateralMultiplier,
        this, true
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        val velocities = MecanumKinematics.robotToWheelVelocities(
            driveSignal.vel,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )
        val accelerations = MecanumKinematics.robotToWheelAccelerations(
            driveSignal.accel,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )
        val powers =
            Kinematics.calculateMotorFeedforward(velocities, accelerations, feedforward)
        setMotorPowers(powers[0], powers[1], powers[2], powers[3])
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val powers = MecanumKinematics.robotToWheelVelocities(
            drivePower,
            1.0,
            1.0,
            lateralMultiplier
        )
        setMotorPowers(powers[0], powers[1], powers[2], powers[3])
    }

    /**
     * Sets the following motor powers (normalized voltages). All arguments are on the interval `[-1.0, 1.0]`.
     */
    abstract fun setMotorPowers(
        frontLeft: Double,
        rearLeft: Double,
        rearRight: Double,
        frontRight: Double
    )

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
