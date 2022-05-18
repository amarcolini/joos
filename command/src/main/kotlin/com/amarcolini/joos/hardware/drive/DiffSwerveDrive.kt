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
import com.amarcolini.joos.util.deg
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
    final override val constraints: DiffSwerveConstraints = DiffSwerveConstraints(
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

    private val gears = listOf(leftModule.first, leftModule.second, rightModule.first, rightModule.second)

    /**
     * All the motors in this drive.
     */
    @JvmField
    val motors: MotorGroup = MotorGroup(leftModule.first, leftModule.second, rightModule.first, rightModule.second)

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
        val (leftAngVel, rightAngVel) = DiffSwerveKinematics.robotToModuleAngularVelocities(
            driveSignal.vel,
            driveSignal.accel,
            constraints.trackWidth
        )
        leftModuleController.targetPosition = leftOrientation.radians
        rightModuleController.targetPosition = rightOrientation.radians
    }

    override fun setDrivePower(drivePower: Pose2d) {
        targetSpeeds = DiffSwerveKinematics.robotToWheelVelocities(drivePower, 1.0).map {
            it * constraints.maxGearVel
        }.zip(listOf(0.0, 0.0))
        val (leftOrientation, rightOrientation) = DiffSwerveKinematics.robotToModuleOrientations(
            drivePower, 1.0
        )
        leftModuleController.targetPosition = leftOrientation.radians
        rightModuleController.targetPosition = rightOrientation.radians
        leftModuleController.outputBounded = false
        rightModuleController.outputBounded = false
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
