package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.TankPIDVAFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.kinematics.TankKinematics
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.TankLocalizer
import com.amarcolini.joos.trajectory.config.TankConstraints
import kotlin.math.abs
import kotlin.math.min


/**
 * A [Component] implementation of a tank drive.
 */
open class TankDrive @JvmOverloads constructor(
    private val left: MotorGroup,
    private val right: MotorGroup,
    final override val imu: Imu? = null,
    final override val constraints: TankConstraints = TankConstraints(
        min(
            left.maxRPM,
            right.maxRPM
        )
    ),
    axialPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5)
) : DriveComponent() {

    private val motors = listOf(left, right)

    override val trajectoryFollower: TrajectoryFollower = TankPIDVAFollower(
        axialPID, headingPID, Pose2d(0.5, 0.5, Math.toRadians(5.0)), 0.5
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
            motor.setSpeed(vel, accel, Motor.RotationUnit.UPS)
        }
    }

    override fun setDrivePower(drivePower: Pose2d) {
        motors.zip(
            TankKinematics.robotToWheelVelocities(drivePower, 1.0)
        ).forEach { (motor, speed) -> motor.setPower(speed) }
    }

    override fun setWeightedDrivePower(drivePower: Pose2d) {
        var vel = Pose2d(drivePower.x, 0.0, drivePower.heading)

        val denom = abs(vel.x) + abs(vel.heading)
        if (denom > 1) vel /= (denom)

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