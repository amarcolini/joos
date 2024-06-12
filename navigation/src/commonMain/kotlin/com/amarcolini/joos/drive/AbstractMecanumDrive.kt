package com.amarcolini.joos.drive

import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.MecanumKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.MecanumLocalizer
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

/**
 * This class provides the basic functionality of a mecanum drive using [MecanumKinematics].
 *
 * @param trackWidth Lateral distance between pairs of wheels on different sides of the robot.
 * @param wheelBase Distance between pairs of wheels on the same side of the robot.
 * @param lateralMultiplier Lateral multiplier.
 */
abstract class AbstractMecanumDrive @JvmOverloads constructor(
    protected val trackWidth: Double,
    protected val wheelBase: Double = trackWidth,
    protected val lateralMultiplier: Double = 1.0,
    protected val externalHeadingSensor: AngleSensor? = null
) : Drive() {
    final override var localizer: Localizer = MecanumLocalizer.from(
        ::getWheelPositions,
        ::getWheelVelocities,
        trackWidth, wheelBase, lateralMultiplier,
    ).let { if (externalHeadingSensor != null) it.addHeadingSensor(externalHeadingSensor) else it }

    final override fun setDriveSignal(driveSignal: DriveSignal) {
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
        setWheelVelocities(velocities, accelerations)
    }

    final override fun setDrivePower(drivePower: Pose2d) {
        val powers = MecanumKinematics.robotToWheelVelocities(
            drivePower.copy(heading = drivePower.heading.value.rad),
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