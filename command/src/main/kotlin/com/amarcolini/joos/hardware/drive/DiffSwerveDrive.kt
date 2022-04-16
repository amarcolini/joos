package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDFController
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.HolonomicPIDVAFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
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
        leftModuleController.setInputBounds(-PI / 2, PI / 2)
        rightModuleController.setInputBounds(-PI / 2, PI / 2)
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
            driveSignal.accel,
            driveSignal.vel,
            constraints.trackWidth
        )
        leftModuleController.targetPosition = leftOrientation.radians
        rightModuleController.targetPosition = rightOrientation.radians
        leftModuleController.setOutputBounds(-abs(leftAngVel.radians), abs(leftAngVel.radians))
        rightModuleController.setOutputBounds(-abs(rightAngVel.radians), abs(rightAngVel.radians))
    }

    override fun setDrivePower(drivePower: Pose2d) {
        targetSpeeds = DiffSwerveKinematics.robotToWheelVelocities(drivePower, 1.0).map {
            it * constraints.maxGearVel
        }.zip(listOf(0.0, 0.0))
        val (leftOrientation, rightOrientation) = DiffSwerveKinematics.robotToModuleOrientations(
            drivePower, constraints.trackWidth
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
        val leftControl = leftModuleController.update(leftOrientation.radians)

        val (leftDV, leftDA) = speedsToDirectional(
            leftVel,
            leftAccel,
            leftModuleController.targetPosition,
            leftOrientation.radians
        )

        leftModule.first.setSpeed(leftDV + leftControl, leftDA, Motor.RotationUnit.UPS)
        leftModule.second.setSpeed(-leftDV + leftControl, -leftDA, Motor.RotationUnit.UPS)

        val (rightVel, rightAccel) = targetSpeeds[1]
        val rightOrientation = DiffSwerveKinematics.gearToModuleOrientation(
            rightModule.first.rotation,
            rightModule.second.rotation
        )
        val rightControl = rightModuleController.update(rightOrientation.radians)

        val (rightDV, rightDA) = speedsToDirectional(
            rightVel,
            rightAccel,
            rightModuleController.targetPosition,
            rightOrientation.radians
        )

        rightModule.first.setSpeed(rightDV + rightControl, rightDA, Motor.RotationUnit.UPS)
        rightModule.second.setSpeed(-rightDV + rightControl, -rightDA, Motor.RotationUnit.UPS)

        gears.forEach { it.update() }
    }

    fun getModuleOrientations(): List<Angle> {
        val (topLeft, bottomLeft, topRight, bottomRight) = getGearRotations()
        return listOf(
            DiffSwerveKinematics.gearToModuleOrientation(topLeft, bottomLeft),
            DiffSwerveKinematics.gearToModuleOrientation(topRight, bottomRight),
        )
    }

    fun getGearRotations(): List<Angle> = gears.map { it.rotation }

    /**
     * Computes the robot velocities depending on which direction the module is facing and which direction the module is trying to go
     * @param velocity the velocity of the module
     * @param acceleration the acceleration of the module
     * @param target the target orientation of the module
     * @param current the current orientation of the module
     *
     * @return the robot velocity and acceleration of the module according to the direction the module is facing
     */
    fun speedsToDirectional(
        velocity: Double,
        acceleration: Double,
        target: Double,
        current: Double
    ): Pair<Double, Double> {
        val sameHalf = abs(target.wrap(-PI, PI) - current.wrap(-PI, PI)) <= PI / 2

        return if (sameHalf) velocity to acceleration
        else -velocity to -acceleration

    }
}