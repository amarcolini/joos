package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.TankPIDVAFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.kinematics.TankKinematics
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.TankLocalizer
import com.amarcolini.joos.trajectory.config.TankConstraints
import kotlin.math.min

/**
 * A [Component] implementation of a tank drive.
 */
class TankDrive @JvmOverloads constructor(
    private val left: Motor,
    private val right: Motor,
    override val imu: Imu? = null,
    override val constraints: TankConstraints = TankConstraints(min(left.maxRPM, right.maxRPM)),
    translationalPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5)
) : DriveComponent() {

    private val motors = listOf(left, right)

    override val trajectoryFollower: TrajectoryFollower = TankPIDVAFollower(
        translationalPID, headingPID, Pose2d(0.5, 0.5, Math.toRadians(5.0)), 0.5
    )
    override var localizer: Localizer = TankLocalizer(
        { listOf(left.distance, right.distance) },
        { listOf(left.distanceVelocity, right.distanceVelocity) },
        constraints.trackWidth,
        this, imu != null
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        motors.zip(
            TankKinematics.robotToWheelVelocities(
                driveSignal.vel,
                constraints.trackWidth,
            ).zip(
                TankKinematics.robotToWheelAccelerations(
                    driveSignal.accel,
                    constraints.trackWidth,
                )
            )
        ).forEach { (motor, power) ->
            val (vel, accel) = power
            motor.set(vel / (motor.distancePerPulse * motor.maxTPS))
            motor.targetAcceleration = accel / motor.distancePerPulse
        }
    }

    override fun setDrivePower(drivePower: Pose2d) {
        motors.zip(
            TankKinematics.robotToWheelVelocities(drivePower, 1.0)
        ).forEach { (motor, speed) -> motor.set(speed) }
    }
}