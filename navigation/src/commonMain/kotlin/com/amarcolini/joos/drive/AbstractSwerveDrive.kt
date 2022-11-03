package com.amarcolini.joos.drive

import com.amarcolini.joos.control.FeedforwardCoefficients
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.SwerveLocalizer
import kotlin.jvm.JvmOverloads

/**
 * This class provides the basic functionality of a swerve drive using [SwerveKinematics].
 *
 * @param feedforward motor feedforward coefficients
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 */
abstract class AbstractSwerveDrive @JvmOverloads constructor(
    private val feedforward: FeedforwardCoefficients,
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
        val powers = feedforward.calculate(velocities, accelerations)
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
     * Sets the module orientations.
     */
    abstract fun setModuleOrientations(
        frontLeft: Angle,
        rearLeft: Angle,
        rearRight: Angle,
        frontRight: Angle
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
     * Returns the current module orientations. Orientations should exactly match the order in
     * [setModuleOrientations].
     */
    abstract fun getModuleOrientations(): List<Angle>
}