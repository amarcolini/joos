package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.TankKinematics
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Default localizer for tank drives based on the drive encoders.
 *
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 */
abstract class TankLocalizer(
    private val trackWidth: Double,
) : DeadReckoningLocalizer {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun from(
            getWheelPositions: () -> List<Double>,
            getWheelVelocities: () -> List<Double>? = { null },
            trackWidth: Double
        ): TankLocalizer = object : TankLocalizer(trackWidth) {
            override fun getWheelPositions(): List<Double> = getWheelPositions()

            override fun getWheelVelocities(): List<Double>? = getWheelVelocities()
        }
    }

    private var _poseEstimate = Pose2d()
    final override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            _poseEstimate = value
        }
    final override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    final override var lastRobotPoseDelta: Pose2d = Pose2d()
        private set

    final override fun update() {
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

    abstract fun getWheelPositions(): List<Double>

    open fun getWheelVelocities(): List<Double>? = null
}