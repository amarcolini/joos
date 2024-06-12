package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.MecanumKinematics
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Default localizer for mecanum drives based on the drive encoders.
 *
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 * @param lateralMultiplier lateral multiplier
 */
abstract class MecanumLocalizer @JvmOverloads constructor(
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth,
    private val lateralMultiplier: Double = 1.0,
) : DeadReckoningLocalizer {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun from(
            getWheelPositions: () -> List<Double>,
            getWheelVelocities: () -> List<Double>? = { null },
            trackWidth: Double,
            wheelBase: Double = trackWidth,
            lateralMultiplier: Double = 1.0,
        ): MecanumLocalizer = object : MecanumLocalizer(trackWidth, wheelBase, lateralMultiplier) {
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
            val robotPoseDelta = MecanumKinematics.wheelToRobotVelocities(
                wheelDeltas,
                trackWidth,
                wheelBase,
                lateralMultiplier
            )
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                robotPoseDelta
            )
            lastRobotPoseDelta = robotPoseDelta
        }

        val wheelVelocities = getWheelVelocities()
        poseVelocity =
            if (wheelVelocities != null)
                MecanumKinematics.wheelToRobotVelocities(
                    wheelVelocities,
                    trackWidth,
                    wheelBase,
                    lateralMultiplier
                )
            else null

        lastWheelPositions = wheelPositions
    }

    abstract fun getWheelPositions(): List<Double>

    open fun getWheelVelocities(): List<Double>? = null
}