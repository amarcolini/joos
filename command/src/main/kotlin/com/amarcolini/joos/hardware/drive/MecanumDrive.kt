package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.HolonomicPIDVAFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.kinematics.MecanumKinematics
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.MecanumLocalizer
import com.amarcolini.joos.trajectory.config.MecanumConstraints
import kotlin.math.abs

/**
 * A [Component] implementation of a mecanum drive.
 */
open class MecanumDrive @JvmOverloads constructor(
    private val frontLeft: Motor,
    private val backLeft: Motor,
    private val backRight: Motor,
    private val frontRight: Motor,
    final override val imu: Imu? = null,
    final override val constraints: MecanumConstraints = MecanumConstraints(
        listOf(
            frontLeft,
            backLeft,
            backRight,
            frontRight
        ).minOf { it.maxRPM }
    ),
    translationalPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5)
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
        constraints.trackWidth, constraints.wheelBase, constraints.lateralMultiplier,
        this, imu != null
    )

    private fun getWheelPositions() = motors.map { it.distance }

    private fun getWheelVelocities() = motors.map { it.distanceVelocity }

    override fun setDriveSignal(driveSignal: DriveSignal) {
        motors.zip(
            MecanumKinematics.robotToWheelVelocities(
                driveSignal.vel,
                constraints.trackWidth,
                constraints.wheelBase,
                constraints.lateralMultiplier
            ).zip(
                MecanumKinematics.robotToWheelAccelerations(
                    driveSignal.accel,
                    constraints.trackWidth,
                    constraints.wheelBase,
                    constraints.lateralMultiplier
                )
            )
        ).forEach { (motor, power) ->
            val (vel, accel) = power
            motor.setSpeed(vel, accel, Motor.RotationUnit.UPS)
        }
    }

    override fun setDrivePower(drivePower: Pose2d) {
        motors.zip(
            MecanumKinematics.robotToWheelVelocities(
                drivePower,
                1.0,
                1.0,
                constraints.lateralMultiplier
            )
        ).forEach { (motor, speed) -> motor.power = speed }
    }

    @JvmOverloads
    fun setWeightedDrivePower(
        drivePower: Pose2d,
        xWeight: Double = 1.0,
        yWeight: Double = 1.0,
        headingWeight: Double = 1.0
    ) {
        var vel = drivePower

        if (abs(vel.x) + abs(vel.y) + abs(vel.heading) > 1) {
            val denom =
                xWeight * abs(vel.x) + yWeight * abs(vel.y) + headingWeight * abs(vel.heading)
            vel = Pose2d(vel.x * xWeight, vel.y * yWeight, vel.heading * headingWeight) / denom
        }

        setDrivePower(vel)
    }

    override fun setRunMode(runMode: Motor.RunMode) {
        motors.forEach { it.runMode = runMode }
    }

    override fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior) {
        motors.forEach { it.zeroPowerBehavior = zeroPowerBehavior }
    }

    override fun update() {
        super.update()
        motors.forEach { it.update() }
    }
}