package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.kinematics.Kinematics
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Default localizer for differential swerve drives based on the drive encoder positions.
 *
 * @param gearRotations the total rotation of each gear
 * @param gearPositions the position of each gear in linear distance units
 * @param gearVelocities the current velocities of each gear in linear distance units
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 */
class DiffSwerveLocalizer @JvmOverloads constructor(
    @JvmField val gearRotations: () -> List<Angle>,
    @JvmField val gearPositions: () -> List<Double>,
    @JvmField val gearVelocities: () -> List<Double>? = { null },
    private val trackWidth: Double,
) : DeadReckoningLocalizer {
    companion object {
        /**
         * Uses [moduleOrientations] to get module orientations instead of gear rotations. Useful when using an
         * external sensor to measure module orientation.
         */
        @JvmOverloads
        @JvmStatic
        fun withModuleSensors(
            moduleOrientations: () -> Pair<Angle, Angle>,
            gearPositions: () -> List<Double>,
            gearVelocities: () -> List<Double>? = { null },
            trackWidth: Double,
        ) = DiffSwerveLocalizer(
            {
                val orientations = moduleOrientations()
                listOf(orientations.first, orientations.first, orientations.second, orientations.second)
            }, gearPositions, gearVelocities, trackWidth
        )
    }

    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastGearPositions = emptyList()
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastGearPositions = emptyList<Double>()
    override var lastRobotPoseDelta: Pose2d = Pose2d()
        private set

    override fun update() {
        val gearRotations = gearRotations()
        val gearPositions = gearPositions()
        if (lastGearPositions.isNotEmpty()) {
            val gearDeltas = gearPositions
                .zip(lastGearPositions)
                .map { it.first - it.second }
            val robotPoseDelta = DiffSwerveKinematics.gearToRobotVelocities(
                gearRotations,
                gearDeltas,
                trackWidth
            )
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                robotPoseDelta
            )
            lastRobotPoseDelta = robotPoseDelta
        }

        val gearVelocities = gearVelocities()
        poseVelocity =
            if (gearVelocities != null)
                DiffSwerveKinematics.gearToRobotVelocities(
                    gearRotations,
                    gearVelocities,
                    trackWidth
                )
            else null

        lastGearPositions = gearPositions
    }
}