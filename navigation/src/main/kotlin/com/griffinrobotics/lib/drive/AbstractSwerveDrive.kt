package com.griffinrobotics.lib.drive

import com.griffinrobotics.lib.control.FeedforwardCoefficients
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.SwerveKinematics
import com.griffinrobotics.lib.localization.Localizer
import com.griffinrobotics.lib.localization.SwerveLocalizer
import com.griffinrobotics.lib.util.Angle

/**
 * This class provides the basic functionality of a swerve drive using [SwerveKinematics].
 *
 * @param feedforward motor feedforward coefficients
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 */
abstract class AbstractSwerveDrive @JvmOverloads constructor(
    var feedforward: FeedforwardCoefficients,
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth
) : Drive() {

    override var localizer: Localizer = SwerveLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        ::getModuleOrientations,
        trackWidth, wheelBase,
        this, true
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        val velocities = SwerveKinematics.robotToWheelVelocities(
            driveSignal.vel,
            trackWidth,
            wheelBase
        )
        val accelerations = SwerveKinematics.robotToWheelAccelerations(
            driveSignal.vel,
            driveSignal.accel,
            trackWidth,
            wheelBase
        )
        val powers =
            Kinematics.calculateMotorFeedforward(velocities, accelerations, feedforward)
        val orientations = SwerveKinematics.robotToModuleOrientations(
            driveSignal.vel,
            trackWidth,
            wheelBase
        )
        setMotorPowers(powers[0], powers[1], powers[2], powers[3])
        setModuleOrientations(orientations[0], orientations[1], orientations[2], orientations[3])
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val avg = (trackWidth + wheelBase) / 2.0
        val powers =
            SwerveKinematics.robotToWheelVelocities(drivePower, trackWidth / avg, wheelBase / avg)
        val orientations = SwerveKinematics.robotToModuleOrientations(
            drivePower,
            trackWidth / avg,
            wheelBase / avg
        )
        setMotorPowers(powers[0], powers[1], powers[2], powers[3])
        setModuleOrientations(orientations[0], orientations[1], orientations[2], orientations[3])
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
     * Sets the module orientations. All values are in radians.
     */
    abstract fun setModuleOrientations(
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

    /**
     * Returns the current module orientations in radians. Orientations should exactly match the order in
     * [setModuleOrientations].
     */
    abstract fun getModuleOrientations(): List<Double>
}
