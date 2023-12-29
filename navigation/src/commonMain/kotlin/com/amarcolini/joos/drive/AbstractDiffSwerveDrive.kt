package com.amarcolini.joos.drive

import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDController
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.DiffSwerveLocalizer
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.wrap
import kotlin.math.PI
import kotlin.math.abs

/**
 * This class provides the basic functionality of a differential swerve drive using [DiffSwerveKinematics].
 *
 * @param orientationPID Module orientation PID coefficients
 * @param trackWidth Lateral distance between pairs of wheels on different sides of the robot.
 */
abstract class AbstractDiffSwerveDrive(
    protected val orientationPID: PIDCoefficients,
    protected val trackWidth: Double,
    protected var feedforward: Feedforward,
    protected val externalHeadingSensor: AngleSensor? = null,
) : Drive() {
    override var localizer: Localizer = DiffSwerveLocalizer.withModuleSensors(
        ::getModuleOrientations,
        ::getGearPositions,
        ::getGearVelocities,
        trackWidth
    ).let { if (externalHeadingSensor != null) it.addHeadingSensor(externalHeadingSensor) else it }

    override fun setDriveSignal(driveSignal: DriveSignal) {
        val (leftVel, rightVel) = DiffSwerveKinematics.robotToWheelVelocities(
            driveSignal.vel,
            trackWidth
        )
        val (leftAccel, rightAccel) = DiffSwerveKinematics.robotToWheelAccelerations(
            driveSignal.vel,
            driveSignal.accel,
            trackWidth
        )
        val (leftOrientation, rightOrientation) = DiffSwerveKinematics.robotToModuleOrientations(
            driveSignal.vel,
            trackWidth
        )
        leftPower = feedforward.calculate(leftVel, leftAccel)
        rightPower = feedforward.calculate(rightVel, rightAccel)
        leftModuleController.setTarget(leftOrientation.radians)
        rightModuleController.setTarget(rightOrientation.radians)
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val actualDrivePower = drivePower.copy(heading = drivePower.heading.value.rad)
        val (leftPower, rightPower) =
            DiffSwerveKinematics.robotToWheelVelocities(actualDrivePower, 1.0)
        val (leftOrientation, rightOrientation) =
            DiffSwerveKinematics.robotToModuleOrientations(actualDrivePower, 1.0)
        this.leftPower = leftPower
        this.rightPower = rightPower
        leftModuleController.setTarget(leftOrientation.radians)
        rightModuleController.setTarget(rightOrientation.radians)
    }

    protected open fun getModuleOrientations(): Pair<Angle, Angle> {
        val (topLeft, bottomLeft, topRight, bottomRight) = getGearRotations()
        return Pair(
            DiffSwerveKinematics.gearToModuleOrientation(topLeft, bottomLeft),
            DiffSwerveKinematics.gearToModuleOrientation(topRight, bottomRight),
        )
    }

    protected val leftModuleController = PIDController(orientationPID)
    protected val rightModuleController = PIDController(orientationPID)

    init {
        leftModuleController.setInputBounds(-PI * 0.5, PI * 0.5)
        rightModuleController.setInputBounds(-PI * 0.5, PI * 0.5)
        leftModuleController.outputBounded = false
        rightModuleController.outputBounded = false
    }

    private var leftPower = 0.0
    private var rightPower = 0.0

    /**
     * Updates the module orientations. Should be called regularly.
     */
    fun updateModuleOrientations() {
        val (left, right) = getModuleOrientations()
        val leftControl = leftModuleController.update(left.radians)
        val rightControl = rightModuleController.update(right.radians)

        val leftDirection =
            if (abs((leftModuleController.targetPosition - left.radians).wrap(-PI, PI)) <= (PI * 0.5)) 1
            else -1
        val rightDirection =
            if (abs((rightModuleController.targetPosition - right.radians).wrap(-PI, PI)) <= (PI * 0.5)) 1
            else -1
        val powers = listOf(
            leftPower * leftDirection + leftControl,
            -leftPower * leftDirection + leftControl,
            rightPower * rightDirection + rightControl,
            -rightPower * rightDirection + rightControl
        )
        setMotorPowers(
            powers[0],
            powers[1],
            powers[2],
            powers[3],
        )
    }

    /**
     * Sets the following motor powers (normalized voltages). All arguments are on the interval `[-1.0, 1.0]`.
     */
    abstract fun setMotorPowers(
        topLeft: Double,
        bottomLeft: Double,
        topRight: Double,
        bottomRight: Double
    )

    /**
     * Returns the total rotation the gears. Angles should exactly match the ordering in
     * [setMotorPowers].
     */
    abstract fun getGearRotations(): List<Angle>

    /**
     * Returns the positions of the gears in linear distance units. Positions should exactly match the ordering in
     * [setMotorPowers].
     */
    abstract fun getGearPositions(): List<Double>

    /**
     * Returns the velocities of the gears in linear distance units. Velocities should exactly match the ordering in
     * [setMotorPowers].
     */
    open fun getGearVelocities(): List<Double>? = null
}