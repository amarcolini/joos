package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDFController
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.HolonomicPIDVAFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.localization.DiffSwerveLocalizer
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.trajectory.config.DiffSwerveConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.wrap
import kotlin.math.PI
import kotlin.math.abs

/**
 * A [Component] implementation of a differential swerve drive.
 */
open class DiffSwerveDrive(
    private val leftModule: Pair<Motor, Motor>,
    private val rightModule: Pair<Motor, Motor>,
    final override val imu: Imu? = null,
    constraints: DiffSwerveConstraints = DiffSwerveConstraints(
        listOf(
            leftModule.first,
            leftModule.second,
            rightModule.first,
            rightModule.second
        ).minOf { it.maxDistanceVelocity }
    ),
    moduleOrientationPID: PIDCoefficients,
    translationalPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5),
    headingPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5)
) : DriveComponent() {
    /**
     * Constructs a differential swerve drive from two [MotorGroup]s containing the motors of each module.
     */
    @JvmOverloads
    constructor(
        leftModule: MotorGroup,
        rightModule: MotorGroup,
        imu: Imu? = null,
        constraints: DiffSwerveConstraints = DiffSwerveConstraints(
            listOf(leftModule, rightModule).minOf { it.maxDistanceVelocity }
        ),
        moduleOrientationPID: PIDCoefficients,
        translationalPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5),
        headingPID: PIDCoefficients = PIDCoefficients(4.0, 0.0, 0.5)
    ) : this(
        leftModule.motors[0] to leftModule.motors[1],
        rightModule.motors[0] to rightModule.motors[1],
        imu, constraints, moduleOrientationPID, translationalPID, headingPID
    )

    private val gears = listOf(leftModule.first, leftModule.second, rightModule.first, rightModule.second)

    /**
     * All the motors in this drive.
     */
    @JvmField
    val motors: MotorGroup = MotorGroup(leftModule.first, leftModule.second, rightModule.first, rightModule.second)

    final override val constraints: DiffSwerveConstraints =
        if (constraints.maxGearVel <= 0) constraints.copy(motors.maxDistanceVelocity)
        else constraints

    override val trajectoryFollower: TrajectoryFollower = HolonomicPIDVAFollower(
        translationalPID, translationalPID, headingPID, Pose2d(0.5, 0.5, 5.deg), 0.5
    )

    override fun setRunMode(runMode: Motor.RunMode): Unit = gears.forEach { it.runMode = runMode }

    override fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior): Unit =
        gears.forEach { it.zeroPowerBehavior = zeroPowerBehavior }

    override var localizer: Localizer = DiffSwerveLocalizer(
        { gears.map { it.rotation } }, { gears.map { it.distance } }, { gears.map { it.distanceVelocity } },
        constraints.trackWidth, this, imu != null
    )

    private val leftModuleController = PIDFController(moduleOrientationPID)
    private val rightModuleController = PIDFController(moduleOrientationPID)

    init {
        leftModuleController.setInputBounds(-PI * 0.5, PI * 0.5)
        rightModuleController.setInputBounds(-PI * 0.5, PI * 0.5)
        leftModuleController.outputBounded = false
        rightModuleController.outputBounded = false
    }

    private var targetSpeeds: List<Pair<Double, Double>> = listOf(
        0.0 to 0.0,
        0.0 to 0.0
    )

    override fun setDriveSignal(driveSignal: DriveSignal) {
        targetSpeeds = DiffSwerveKinematics.robotToWheelVelocities(
            driveSignal.vel,
            constraints.trackWidth
        ).zip(
            DiffSwerveKinematics.robotToWheelAccelerations(
                driveSignal.vel,
                driveSignal.accel,
                constraints.trackWidth
            )
        )
        val (leftOrientation, rightOrientation) = DiffSwerveKinematics.robotToModuleOrientations(
            driveSignal.vel,
            constraints.trackWidth
        )
        leftModuleController.targetPosition = leftOrientation.radians
        rightModuleController.targetPosition = rightOrientation.radians
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val power = drivePower.copy(heading = drivePower.heading.value.rad)
        targetSpeeds = DiffSwerveKinematics.robotToWheelVelocities(power, 1.0).map {
            it * constraints.maxGearVel
        }.zip(listOf(0.0, 0.0))
        val (leftOrientation, rightOrientation) = DiffSwerveKinematics.robotToModuleOrientations(
            power, 1.0
        )
        leftModuleController.targetPosition = leftOrientation.radians
        rightModuleController.targetPosition = rightOrientation.radians
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

    override fun update() {
        super.update()

        val (leftVel, leftAccel) = targetSpeeds[0]
        val leftOrientation = DiffSwerveKinematics.gearToModuleOrientation(
            leftModule.first.rotation,
            leftModule.second.rotation
        )
        val leftDirection =
            if (abs((leftModuleController.targetPosition - leftOrientation.radians).wrap(-PI, PI)) <= (PI * 0.5)) 1
            else -1
        val leftControl = leftModuleController.update(leftOrientation.radians)
        leftModule.first.setSpeed(
            leftVel * leftDirection + leftControl,
            leftAccel * leftDirection,
            Motor.RotationUnit.UPS
        )
        leftModule.second.setSpeed(
            -leftVel * leftDirection + leftControl,
            -leftAccel * leftDirection,
            Motor.RotationUnit.UPS
        )

        val (rightVel, rightAccel) = targetSpeeds[1]
        val rightOrientation = DiffSwerveKinematics.gearToModuleOrientation(
            rightModule.first.rotation,
            rightModule.second.rotation
        )
        val rightDirection =
            if (abs((rightModuleController.targetPosition - rightOrientation.radians).wrap(-PI, PI)) <= (PI * 0.5)) 1
            else -1
        val rightControl = rightModuleController.update(rightOrientation.radians)
        rightModule.first.setSpeed(
            rightVel * rightDirection + rightControl,
            rightAccel * rightDirection,
            Motor.RotationUnit.UPS
        )
        rightModule.second.setSpeed(
            -rightVel * rightDirection + rightControl,
            -rightAccel * rightDirection,
            Motor.RotationUnit.UPS
        )

        gears.forEach { it.update() }
    }
}