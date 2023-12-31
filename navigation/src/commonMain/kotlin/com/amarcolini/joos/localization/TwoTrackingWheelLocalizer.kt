package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.util.Matrix3x3
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmField

/**
 * Localizer based on two unpowered tracking omni wheels and an orientation sensor.
 *
 * @param wheelPoses wheel poses relative to the center of the robot (positive X points forward on the robot)
 */
abstract class TwoTrackingWheelLocalizer(
    @JvmField val wheelPoses: List<Pose2d>
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            lastHeading = null
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
    private var lastWheelPositions = emptyList<Double>()
    private var lastHeading: Angle? = null

    private val forwardMatrix: Matrix3x3

    init {
        require(wheelPoses.size == 2) { "2 wheel poses must be provided" }

        val inverseMatrix = Matrix3x3(Array(3) {
            if (it == 0 || it == 1) {
                val orientationVector = wheelPoses[it].headingVec()
                val positionVector = wheelPoses[it].vec()
                doubleArrayOf(
                    orientationVector.x,
                    orientationVector.y,
                    positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
                )
            } else doubleArrayOf(0.0, 0.0, 1.0)
        })
        val forwardMatrix = inverseMatrix.getInverse()
        require(forwardMatrix != null) {
            "The specified configuration cannot support full localization"
        }
        this.forwardMatrix = forwardMatrix
    }

    private fun calculatePoseDelta(wheelDeltas: List<Double>, headingDelta: Angle): Pose2d {
        val rawPoseDelta = forwardMatrix * (wheelDeltas + headingDelta.radians)
        return Pose2d(
            rawPoseDelta[0],
            rawPoseDelta[1],
            rawPoseDelta[2].rad
        )
    }

    override fun update() {
        val wheelPositions = getWheelPositions()
        val heading = getHeading()
        val lastHeading = lastHeading
        if (lastWheelPositions.isNotEmpty() && lastHeading != null) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val headingDelta = (heading - lastHeading).normDelta()
            val robotPoseDelta = calculatePoseDelta(wheelDeltas, headingDelta)
            _poseEstimate = Kinematics.relativeOdometryUpdate(_poseEstimate, robotPoseDelta)
        }

        val wheelVelocities = getWheelVelocities()
        val headingVelocity = getHeadingVelocity()
        if (wheelVelocities != null && headingVelocity != null) {
            poseVelocity = calculatePoseDelta(wheelVelocities, headingVelocity)
        }

        lastWheelPositions = wheelPositions
        this.lastHeading = heading
    }

    /**
     * Returns the positions of the tracking wheels in the desired distance units (not encoder counts!)
     */
    abstract fun getWheelPositions(): List<Double>

    /**
     * Returns the velocities of the tracking wheels in the desired distance units (not encoder counts!)
     */
    open fun getWheelVelocities(): List<Double>? = null

    /**
     * Returns the heading of the robot (usually from a gyroscope or IMU).
     */
    abstract fun getHeading(): Angle

    /**
     * Returns the heading of the robot (usually from a gyroscope or IMU).
     */
    open fun getHeadingVelocity(): Angle? = null
}