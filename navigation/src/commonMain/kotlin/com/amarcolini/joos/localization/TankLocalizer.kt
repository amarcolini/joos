package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.TankKinematics
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for tank drives based on the drive encoders.
 *
 * @param getWheelPositions wheel positions in linear distance units
 * @param getWheelVelocities wheel velocities in linear distance units
 * @param getWheelVelocities lateral distance between pairs of wheels on different sides of the robot
 */
class TankLocalizer @JvmOverloads constructor(
    @JvmField val getWheelPositions: () -> List<Double>,
    @JvmField val getWheelVelocities: () -> List<Double>? = { null },
    private val trackWidth: Double,
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
        val wheelPositions = getWheelPositions()
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val robotPoseDelta =
                TankKinematics.wheelToRobotVelocities(wheelDeltas, trackWidth)
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                robotPoseDelta
            )
            lastRobotPoseDelta
        }

        val wheelVelocities = getWheelVelocities()
        if (wheelVelocities != null) {
            poseVelocity =
                TankKinematics.wheelToRobotVelocities(wheelVelocities, trackWidth)
        }

        lastWheelPositions = wheelPositions
    }
}