package com.griffinrobotics.lib.hardware.drive

import com.griffinrobotics.lib.command.Component
import com.griffinrobotics.lib.control.PIDCoefficients
import com.griffinrobotics.lib.drive.DriveSignal
import com.griffinrobotics.lib.followers.HolonomicPIDVAFollower
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.hardware.Imu
import com.griffinrobotics.lib.hardware.Motor
import com.griffinrobotics.lib.kinematics.MecanumKinematics
import com.griffinrobotics.lib.localization.Localizer
import com.griffinrobotics.lib.localization.MecanumLocalizer
import com.griffinrobotics.lib.trajectory.config.MecanumConfig
import com.qualcomm.hardware.bosch.BNO055IMU
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import kotlin.math.abs

/**
 * A [Component] implementation of a mecanum drive.
 */
class MecanumDrive @JvmOverloads constructor(
    private val frontLeft: Motor,
    private val backLeft: Motor,
    private val backRight: Motor,
    private val frontRight: Motor,
    override val imu: Imu? = null,
    override val constants: MecanumConfig = MecanumConfig(
        listOf(
            frontLeft,
            backLeft,
            backRight,
            frontRight
        ).minOf { it.maxRPM }
    ),
    translationalPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5)
) : DriveComponent() {

    private val motors = listOf(frontLeft, backLeft, backRight, frontRight)

    override val trajectoryFollower = HolonomicPIDVAFollower(
        translationalPID, translationalPID,
        headingPID,
        Pose2d(0.5, 0.5, Math.toRadians(5.0)),
        0.5
    )

    override var localizer: Localizer = MecanumLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        constants.trackWidth, constants.wheelBase, constants.lateralMultiplier,
        this, imu != null
    )

    private fun getWheelPositions() = motors.map { it.distance }

    private fun getWheelVelocities() = motors.map { it.distanceVelocity }

    override fun setDriveSignal(driveSignal: DriveSignal) {
        motors.zip(
            MecanumKinematics.robotToWheelVelocities(
                driveSignal.vel,
                constants.trackWidth,
                constants.wheelBase,
                constants.lateralMultiplier
            ).zip(
                MecanumKinematics.robotToWheelAccelerations(
                    driveSignal.accel,
                    constants.trackWidth,
                    constants.wheelBase,
                    constants.lateralMultiplier
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
            MecanumKinematics.robotToWheelVelocities(
                drivePower,
                1.0,
                1.0,
                constants.lateralMultiplier
            )
        ).forEach { (motor, speed) -> motor.set(speed) }
    }

    fun setWeightedDrivePower(drivePower: Pose2d) {
        var vel = drivePower

        val denom = abs(vel.x) + abs(vel.y) + abs(vel.heading)
        if (denom > 1) vel /= (denom)

        setDrivePower(vel)
    }
}