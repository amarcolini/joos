package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.SwerveKinematics
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for swerve drives based on the drive encoder positions and module orientations.
 *
 * @param wheelPositions wheel positions in linear distance units
 * @param wheelVelocities wheel velocities in linear distance units
 * @param moduleOrientations module orientations
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 */
class SwerveLocalizer @JvmOverloads constructor(
    private val wheelPositions: () -> List<Double>,
    private val wheelVelocities: () -> List<Double>? = { null },
    private val moduleOrientations: () -> List<Angle>,
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth,
) : DeadReckoningLocalizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    override var lastRobotPoseDelta: Pose2d = Pose2d()
        private set

    override fun update() {
        val wheelPositions = wheelPositions()
        val moduleOrientations = moduleOrientations()
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val robotPoseDelta = SwerveKinematics.wheelToRobotVelocities(
                wheelDeltas,
                moduleOrientations,
                wheelBase,
                trackWidth
            )
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                robotPoseDelta
            )
            lastRobotPoseDelta = robotPoseDelta
        }

        val wheelVelocities = wheelVelocities()
        poseVelocity =
            if (wheelVelocities != null)
                SwerveKinematics.wheelToRobotVelocities(
                    wheelVelocities,
                    moduleOrientations,
                    wheelBase,
                    trackWidth
                )
            else null

        lastWheelPositions = wheelPositions
    }
}