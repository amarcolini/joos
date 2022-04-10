package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.HolonomicPIDVAFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.hardware.Servo
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.SwerveLocalizer
import com.amarcolini.joos.trajectory.config.SwerveConstraints
import com.amarcolini.joos.util.deg
import kotlin.math.abs

/**
 * A [Component] implementation of a swerve drive.
 */
open class SwerveDrive @JvmOverloads constructor(
    private val frontLeft: Pair<Motor, Servo>,
    private val backLeft: Pair<Motor, Servo>,
    private val backRight: Pair<Motor, Servo>,
    private val frontRight: Pair<Motor, Servo>,
    final override val imu: Imu? = null,
    final override val constraints: SwerveConstraints = SwerveConstraints(
        listOf(
            frontLeft,
            backLeft,
            backRight,
            frontRight
        ).minOf { it.first.maxDistanceVelocity }
    ),
    translationalPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5)
) : DriveComponent() {
    private val wheels =
        listOf(frontLeft.first, backLeft.first, backRight.first, frontRight.first)
    private val servos =
        listOf(frontLeft.second, frontRight.second, backRight.second, frontRight.second)

    /**
     * All the motors in this drive.
     */
    @JvmField
    val motors: MotorGroup = MotorGroup(frontLeft.first, backLeft.first, backRight.first, frontRight.first)

    override val trajectoryFollower = HolonomicPIDVAFollower(
        translationalPID, translationalPID,
        headingPID,
        Pose2d(0.5, 0.5, 5.deg),
        0.5
    )
    override var localizer: Localizer = SwerveLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        ::getModuleOrientations,
        constraints.trackWidth, constraints.wheelBase,
        this, imu != null
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        wheels.zip(
            SwerveKinematics.robotToWheelVelocities(
                driveSignal.vel,
                constraints.trackWidth,
                constraints.wheelBase,
            ).zip(
                SwerveKinematics.robotToWheelAccelerations(
                    driveSignal.vel,
                    driveSignal.accel,
                    constraints.trackWidth,
                    constraints.wheelBase,
                )
            )
        ).forEach { (motor, power) ->
            val (vel, accel) = power
            motor.setSpeed(vel, accel, Motor.RotationUnit.UPS)
        }
        servos.zip(
            SwerveKinematics.robotToModuleOrientations(
                driveSignal.vel,
                constraints.trackWidth,
                constraints.wheelBase
            )
        ).forEach { (servo, orientation) ->
            servo.angle = orientation
        }
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val avg = (constraints.trackWidth + constraints.wheelBase) / 2.0
        wheels.zip(
            SwerveKinematics.robotToWheelVelocities(
                drivePower,
                constraints.trackWidth / avg,
                constraints.wheelBase / avg,
            )
        ).forEach { (motor, speed) -> motor.power = speed }
        servos.zip(
            SwerveKinematics.robotToModuleOrientations(
                drivePower,
                constraints.trackWidth / avg,
                constraints.wheelBase / avg
            )
        ).forEach { (servo, orientation) ->
            servo.angle = orientation
        }
    }

    @JvmOverloads
    fun setWeightedDrivePower(
        drivePower: Pose2d,
        xWeight: Double = 1.0,
        yWeight: Double = 1.0,
        headingWeight: Double = 1.0
    ) {
        var vel = drivePower

        if (abs(vel.x) + abs(vel.y) + abs(vel.heading.radians) > 1) {
            val denom =
                xWeight * abs(vel.x) + yWeight * abs(vel.y) + headingWeight * abs(vel.heading.radians)
            vel = Pose2d(vel.x * xWeight, vel.y * yWeight, vel.heading * headingWeight) / denom
        }

        setDrivePower(vel)
    }

    override fun setRunMode(runMode: Motor.RunMode) {
        wheels.forEach { it.runMode = runMode }
    }

    override fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior) {
        wheels.forEach { it.zeroPowerBehavior = zeroPowerBehavior }
    }

    private fun getWheelPositions(): List<Double> = wheels.map { it.distance }

    private fun getWheelVelocities(): List<Double> = wheels.map { it.distanceVelocity }

    private fun getModuleOrientations(): List<Angle> = servos.map { it.angle }

    override fun update() {
        super.update()
        wheels.forEach { it.update() }
    }
}