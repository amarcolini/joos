package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.util.Matrix3x3
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmField

/**
 * Localizer based on three unpowered tracking omni wheels.
 *
 * @param wheelPoses wheel poses relative to the center of the robot (positive X points forward on the robot)
 */
abstract class ThreeTrackingWheelLocalizer(
    @JvmField val wheelPoses: List<Pose2d>
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
    private var lastWheelPositions = emptyList<Double>()

    private val forwardMatrix: Matrix3x3

    init {
        require(wheelPoses.size == 3) { "3 wheel positions must be provided" }

        val inverseMatrix = Matrix3x3(Array(3) {
            val orientationVector = wheelPoses[it].headingVec()
            val positionVector = wheelPoses[it].vec()
            doubleArrayOf(
                orientationVector.x,
                orientationVector.y,
                positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
            )
        })
        val forwardMatrix = inverseMatrix.getInverse()
            ?: throw IllegalArgumentException("The specified configuration cannot support full localization")
        this.forwardMatrix = forwardMatrix
    }

    private fun calculatePoseDelta(wheelDeltas: List<Double>): Pose2d {
        val rawPoseDelta = forwardMatrix * wheelDeltas
        return Pose2d(
            rawPoseDelta[0],
            rawPoseDelta[1],
            rawPoseDelta[2].rad
        )
    }

    override fun update() {
        val wheelPositions = getWheelPositions()
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val robotPoseDelta = calculatePoseDelta(wheelDeltas)
            _poseEstimate = Kinematics.relativeOdometryUpdate(_poseEstimate, robotPoseDelta)
        }

        val wheelVelocities = getWheelVelocities()
        if (wheelVelocities != null) {
            poseVelocity = calculatePoseDelta(wheelVelocities)
        }

        lastWheelPositions = wheelPositions
    }

    /**
     * Returns the positions of the tracking wheels in the desired distance units (not encoder counts!)
     */
    abstract fun getWheelPositions(): List<Double>

    /**
     * Returns the velocities of the tracking wheels in the desired distance units (not encoder counts!)
     */
    open fun getWheelVelocities(): List<Double>? = null
}