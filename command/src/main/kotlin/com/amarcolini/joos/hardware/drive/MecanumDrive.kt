package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.HolonomicPIDVAFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.kinematics.MecanumKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.MecanumLocalizer
import com.amarcolini.joos.trajectory.constraints.MecanumConstraints
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad
import kotlin.math.abs

/**
 * A [Component] implementation of a mecanum drive.
 */
open class MecanumDrive @JvmOverloads constructor(
    private val frontLeft: Motor,
    private val backLeft: Motor,
    private val backRight: Motor,
    private val frontRight: Motor,
    final override val externalHeadingSensor: AngleSensor? = null,
    constraints: MecanumConstraints = MecanumConstraints(
        listOf(
            frontLeft,
            backLeft,
            backRight,
            frontRight
        ).minOf { it.maxDistanceVelocity }, 1.0, 1.0, 1.0
    ),
    translationalPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5)
) : DriveComponent() {
    /**
     * Constructs a mecanum drive from a [MotorGroup] that contains all of its motors.
     */
    @JvmOverloads
    constructor(
        motors: MotorGroup,
        externalHeadingSensor: AngleSensor? = null,
        constraints: MecanumConstraints = MecanumConstraints(motors.maxDistanceVelocity, 1.0, 1.0, 1.0),
        translationalPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5),
        headingPID: PIDCoefficients = PIDCoefficients(1.0, 0.0, 0.5)
    ) : this(
        motors[0],
        motors[1],
        motors[2],
        motors[3],
        externalHeadingSensor, constraints, translationalPID, headingPID
    )

    private val wheels = listOf(frontLeft, backLeft, backRight, frontRight)

    /**
     * All the motors in this drive.
     */
    @JvmField
    val motors: MotorGroup = MotorGroup(frontLeft, backLeft, backRight, frontRight)

    final override val constraints: MecanumConstraints =
        if (constraints.maxWheelVel <= 0) constraints.copy(maxWheelVel = motors.maxDistanceVelocity)
        else constraints

    override var trajectoryFollower: TrajectoryFollower = HolonomicPIDVAFollower(
        translationalPID, translationalPID,
        headingPID,
        Pose2d(0.5, 0.5, 5.deg),
        0.5
    )

    override var localizer: Localizer = MecanumLocalizer(
        ::getWheelPositions,
        ::getWheelVelocities,
        constraints.trackWidth, constraints.wheelBase, constraints.lateralMultiplier,
    ).let { if (externalHeadingSensor != null) it.addHeadingSensor(externalHeadingSensor) else it }

    private fun getWheelPositions() = wheels.map { it.distance }

    private fun getWheelVelocities() = wheels.map { it.distanceVelocity }

    override fun setDriveSignal(driveSignal: DriveSignal) {
        wheels.zip(
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
            motor.setDistanceVelocity(vel, accel)
        }
    }

    override fun setDrivePower(drivePower: Pose2d) {
        wheels.zip(
            MecanumKinematics.robotToWheelVelocities(
                drivePower.copy(heading = drivePower.heading.value.rad),
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

        if (abs(vel.x) + abs(vel.y) + abs(vel.heading.value) > 1) {
            val denom =
                xWeight * abs(vel.x) + yWeight * abs(vel.y) + headingWeight * abs(vel.heading.value)
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

    override fun update() {
        super.update()
        wheels.forEach { it.update() }
    }
}