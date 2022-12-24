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
import com.amarcolini.joos.trajectory.constraints.TankConstraints
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad
import kotlin.math.abs
import kotlin.math.min


/**
 * A [Component] implementation of a tank drive.
 */
open class TankDrive @JvmOverloads constructor(
    private val left: MotorGroup,
    private val right: MotorGroup,
    final override val imu: Imu? = null,
    constraints: TankConstraints = TankConstraints(
        min(
            left.maxDistanceVelocity,
            right.maxDistanceVelocity
        )
    ),
    axialPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5)
) : DriveComponent() {

    private val wheels = listOf(left, right)

    /**
     * All the motors in this drive.
     */
    @JvmField
    val motors: MotorGroup = MotorGroup(left, right)

    final override val constraints: TankConstraints =
        if (constraints.maxWheelVel <= 0) constraints.copy(motors.maxDistanceVelocity)
        else constraints

    override var trajectoryFollower: TrajectoryFollower = TankPIDVAFollower(
        axialPID, headingPID, Pose2d(0.5, 0.5, 5.deg), 0.5
    )
    override var localizer: Localizer = TankLocalizer(
        { listOf(left.distance, right.distance) },
        { listOf(left.distanceVelocity, right.distanceVelocity) },
        constraints.trackWidth,
        this, imu != null
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        wheels.zip(
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
        wheels.zip(
            TankKinematics.robotToWheelVelocities(
                drivePower.copy(heading = drivePower.heading.value.rad), 2.0
            )
        ).forEach { (motor, speed) -> motor.setPower(speed) }
    }

    @JvmOverloads
    fun setWeightedDrivePower(
        drivePower: Pose2d,
        xWeight: Double = 1.0,
        headingWeight: Double = 1.0
    ) {
        var vel = drivePower

        if (abs(vel.x) + abs(vel.heading.value) > 1) {
            val denom =
                xWeight * abs(vel.x) + headingWeight * abs(vel.heading.value)
            vel = Pose2d(vel.x * xWeight, 0.0, vel.heading * headingWeight) / denom
        }

        setDrivePower(vel)
    }

    override fun setRunMode(runMode: Motor.RunMode) {
        wheels.forEach { it.runMode = runMode }
    }

    override fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior) {
        wheels.forEach { it.zeroPowerBehavior = zeroPowerBehavior }
    }

    override fun update() {
        super.update()
        wheels.forEach { it.update() }
    }
}