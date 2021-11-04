package com.griffinrobotics.lib.hardware.drive

import com.griffinrobotics.lib.command.Component
import com.griffinrobotics.lib.control.PIDCoefficients
import com.griffinrobotics.lib.drive.DriveSignal
import com.griffinrobotics.lib.followers.HolonomicPIDVAFollower
import com.griffinrobotics.lib.followers.TrajectoryFollower
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.hardware.Imu
import com.griffinrobotics.lib.hardware.Motor
import com.griffinrobotics.lib.hardware.Servo
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.MecanumKinematics
import com.griffinrobotics.lib.kinematics.SwerveKinematics
import com.griffinrobotics.lib.localization.Localizer
import com.griffinrobotics.lib.localization.SwerveLocalizer
import com.griffinrobotics.lib.trajectory.config.SwerveConfig
import com.qualcomm.hardware.bosch.BNO055IMU

/**
 * A [Component] implementation of a swerve drive.
 */
class SwerveDrive(
    private val frontLeft: Pair<Motor, Servo>,
    private val backLeft: Pair<Motor, Servo>,
    private val backRight: Pair<Motor, Servo>,
    private val frontRight: Pair<Motor, Servo>,
    override val imu: Imu? = null,
    override val constants: SwerveConfig = SwerveConfig(
        listOf(
            frontLeft,
            backLeft,
            backRight,
            frontRight
        ).minOf { it.first.maxRPM }
    ),
    translationalPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5)
) : DriveComponent() {
    private val motors =
        listOf(frontLeft.first, backLeft.first, backRight.first, frontRight.first)
    private val servos =
        listOf(frontLeft.second, frontRight.second, backRight.second, frontRight.second)

    override val trajectoryFollower = HolonomicPIDVAFollower(
        translationalPID, translationalPID,
        headingPID,
        Pose2d(0.5, 0.5, Math.toRadians(5.0)),
        0.5
    )
    override var localizer: Localizer = SwerveLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        ::getModuleOrientations,
        constants.trackWidth, constants.wheelBase,
        this, imu != null
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        motors.zip(
            SwerveKinematics.robotToWheelVelocities(
                driveSignal.vel,
                constants.trackWidth,
                constants.wheelBase,
            ).zip(
                SwerveKinematics.robotToWheelAccelerations(
                    driveSignal.vel,
                    driveSignal.accel,
                    constants.trackWidth,
                    constants.wheelBase,
                )
            )
        ).forEach { (motor, power) ->
            val (vel, accel) = power
            motor.set(vel / (motor.distancePerPulse * motor.maxTPS))
            motor.targetAcceleration = accel / motor.distancePerPulse
        }
        servos.zip(
            SwerveKinematics.robotToModuleOrientations(
                driveSignal.vel,
                constants.trackWidth,
                constants.wheelBase
            )
        ).forEach { (servo, orientation) ->
            servo.angle = orientation
        }
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val avg = (constants.trackWidth + constants.wheelBase) / 2.0
        motors.zip(
            SwerveKinematics.robotToWheelVelocities(
                drivePower,
                constants.trackWidth / avg,
                constants.wheelBase / avg,
            )
        ).forEach { (motor, speed) -> motor.set(speed) }
        servos.zip(
            SwerveKinematics.robotToModuleOrientations(
                drivePower,
                constants.trackWidth / avg,
                constants.wheelBase / avg
            )
        ).forEach { (servo, orientation) ->
            servo.angle = orientation
        }
    }

    private fun getWheelPositions(): List<Double> = motors.map { it.distance }

    private fun getWheelVelocities(): List<Double> = motors.map { it.distanceVelocity }

    private fun getModuleOrientations(): List<Double> = servos.map { it.angle }
}