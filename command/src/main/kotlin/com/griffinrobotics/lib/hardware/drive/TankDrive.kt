package com.griffinrobotics.lib.hardware.drive

import com.griffinrobotics.lib.command.Component
import com.griffinrobotics.lib.control.PIDCoefficients
import com.griffinrobotics.lib.drive.DriveSignal
import com.griffinrobotics.lib.followers.TankPIDVAFollower
import com.griffinrobotics.lib.followers.TrajectoryFollower
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.hardware.Imu
import com.griffinrobotics.lib.hardware.Motor
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.MecanumKinematics
import com.griffinrobotics.lib.kinematics.TankKinematics
import com.griffinrobotics.lib.localization.Localizer
import com.griffinrobotics.lib.localization.TankLocalizer
import com.griffinrobotics.lib.trajectory.config.TankConfig
import com.qualcomm.hardware.bosch.BNO055IMU
import kotlin.math.min

/**
 * A [Component] implementation of a tank drive.
 */
class TankDrive(
    private val left: Motor,
    private val right: Motor,
    override val imu: Imu? = null,
    override val constants: TankConfig = TankConfig(min(left.maxRPM, right.maxRPM)),
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
        constants.trackWidth,
        this, imu != null
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        motors.zip(
            TankKinematics.robotToWheelVelocities(
                driveSignal.vel,
                constants.trackWidth,
            ).zip(
                TankKinematics.robotToWheelAccelerations(
                    driveSignal.accel,
                    constants.trackWidth,
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