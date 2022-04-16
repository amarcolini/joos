package com.amarcolini.joos.drive

import com.amarcolini.joos.control.FeedforwardCoefficients
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDFController
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.localization.DiffSwerveLocalizer
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.util.wrap
import kotlin.math.PI

/**
 * This class provides the basic functionality of a differential swerve drive using [DiffSwerveKinematics].
 *
 * @param feedforward motor feedforward coefficients
 * @param orientationPID module orientation PID coefficients
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 */
abstract class AbstractDiffSwerveDrive(
    val feedforward: FeedforwardCoefficients,
    val orientationPID: PIDCoefficients,
    private val trackWidth: Double,
) : Drive() {

    override var localizer: Localizer = DiffSwerveLocalizer(
        ::getGearRotations,
        ::getGearPositions,
        ::getGearVelocities,
        trackWidth, this, true
    )

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
        val (leftAngVel, rightAngVel) = DiffSwerveKinematics.robotToModuleAngularVelocities(
            driveSignal.vel,
            driveSignal.accel,
            trackWidth
        )

        this.leftVel = leftVel
        this.leftAccel = leftAccel
        this.rightVel = rightVel
        this.rightAccel = rightAccel
        leftModuleController.setTarget(leftOrientation.radians)
        rightModuleController.setTarget(rightOrientation.radians)
        leftModuleController.setOutputBounds(-leftAngVel.radians, leftAngVel.radians)
        rightModuleController.setOutputBounds(-rightAngVel.radians, rightAngVel.radians)
    }

    override fun setDrivePower(drivePower: Pose2d) {
        val (leftVel, rightVel) =
            DiffSwerveKinematics.robotToWheelVelocities(drivePower, 1.0)
        val (leftOrientation, rightOrientation) =
            DiffSwerveKinematics.robotToModuleOrientations(drivePower, 1.0)
        this.leftVel = leftVel
        this.leftAccel = 0.0
        this.rightVel = rightVel
        this.rightAccel = 0.0
        leftModuleController.setTarget(leftOrientation.radians)
        rightModuleController.setTarget(rightOrientation.radians)
        leftModuleController.outputBounded = false
        rightModuleController.outputBounded = false
    }

    private fun getModuleOrientations(): List<Angle> {
        val (topLeft, bottomLeft, topRight, bottomRight) = getGearRotations()
        return listOf(
            DiffSwerveKinematics.gearToModuleOrientation(topLeft, bottomLeft),
            DiffSwerveKinematics.gearToModuleOrientation(topRight, bottomRight),
        )
    }

    private val leftModuleController = PIDFController(orientationPID)
    private val rightModuleController = PIDFController(orientationPID)

    init {
        leftModuleController.setInputBounds(-PI * 0.5, PI * 0.5)
        rightModuleController.setInputBounds(-PI * 0.5, PI * 0.5)
    }

    private var leftVel = 0.0
    private var leftAccel = 0.0
    private var rightVel = 0.0
    private var rightAccel = 0.0

    /**
     * Updates the module orientations. Should be called regularly.
     */
    fun updateModuleOrientations() {
        val (left, right) = getModuleOrientations()
        val leftControl = leftModuleController.update(left.radians)
        val rightControl = rightModuleController.update(right.radians)

        val leftDirection =
            if ((leftModuleController.targetPosition - left.radians).wrap(-PI, PI) <= (PI * 0.5)) 1 else -1
        val rightDirection =
            if ((rightModuleController.targetPosition - right.radians).wrap(-PI, PI) <= (PI * 0.5)) 1 else -1
        val velocities = listOf(
            leftVel * leftDirection + leftControl, -leftVel * leftDirection + leftControl,
            rightVel * rightDirection + rightControl, -rightVel * rightDirection + rightControl
        )
        val accelerations = listOf(
            leftAccel * leftDirection, -leftAccel * leftDirection,
            rightAccel * rightDirection, -rightAccel * rightDirection
        )
        val powers = Kinematics.calculateMotorFeedforward(velocities, accelerations, feedforward)
        setMotorPowers(powers[0], powers[1], powers[2], powers[3])
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