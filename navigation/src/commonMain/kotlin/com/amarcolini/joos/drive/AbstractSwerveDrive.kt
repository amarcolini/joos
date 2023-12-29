package com.amarcolini.joos.drive

import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.SwerveLocalizer
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

/**
 * This class provides the basic functionality of a swerve drive using [SwerveKinematics].
 *
 * @param trackWidth Lateral distance between pairs of wheels on different sides of the robot.
 * @param wheelBase Distance between pairs of wheels on the same side of the robot.
 */
abstract class AbstractSwerveDrive @JvmOverloads constructor(
    protected val trackWidth: Double,
    protected val wheelBase: Double = trackWidth,
    protected val externalHeadingSensor: AngleSensor? = null
) : Drive() {

    override var localizer: Localizer = SwerveLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        ::getModuleOrientations,
        trackWidth, wheelBase,
    ).let { if (externalHeadingSensor != null) it.addHeadingSensor(externalHeadingSensor) else it }

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
        val orientations = SwerveKinematics.robotToModuleOrientations(
            driveSignal.vel,
            trackWidth,
            wheelBase
        )
        setWheelVelocities(velocities, accelerations)
        setModuleOrientations(orientations[0], orientations[1], orientations[2], orientations[3])
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val actualDrivePower = drivePower.copy(heading = drivePower.heading.value.rad)
        val avg = (trackWidth + wheelBase) / 2.0
        val powers =
            SwerveKinematics.robotToWheelVelocities(actualDrivePower, trackWidth / avg, wheelBase / avg)
        val orientations = SwerveKinematics.robotToModuleOrientations(
            actualDrivePower,
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
        backLeft: Double,
        backRight: Double,
        frontRight: Double
    )

    /**
     * Sets the wheel velocities (and accelerations) of each motor, in distance units per second. Velocities and accelerations
     * match the ordering in [setMotorPowers].
     */
    abstract fun setWheelVelocities(velocities: List<Double>, accelerations: List<Double>)

    /**
     * Sets the module orientations.
     */
    abstract fun setModuleOrientations(
        frontLeft: Angle,
        backLeft: Angle,
        backRight: Angle,
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