package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.TankKinematics
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for tank drives based on the drive encoders and (optionally) a heading sensor.
 *
 * @param wheelPositions wheel positions in linear distance units
 * @param wheelVelocities wheel velocities in linear distance units
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param externalHeadingSensor An optional external heading sensor to use for heading measurement
 */
class TankLocalizer @JvmOverloads constructor(
    private val wheelPositions: () -> List<Double>,
    private val wheelVelocities: () -> List<Double>? = { null },
    private val trackWidth: Double,
    private val externalHeadingSensor: AngleSensor? = null
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            lastExtHeading = null
            externalHeadingSensor?.setAngle(value.heading)
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    private var lastExtHeading: Angle? = null

    override fun update() {
        val wheelPositions = wheelPositions()
        val extHeading: Angle? = externalHeadingSensor?.getAngle()
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val robotPoseDelta =
                TankKinematics.wheelToRobotVelocities(wheelDeltas, trackWidth)
            val lastExtHeading = lastExtHeading
            val finalHeadingDelta = if (extHeading != null && lastExtHeading != null)
                (extHeading - lastExtHeading).normDelta()
            else robotPoseDelta.heading
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                Pose2d(robotPoseDelta.vec(), finalHeadingDelta)
            )
        }

        val wheelVelocities = wheelVelocities()
        val extHeadingVel = externalHeadingSensor?.getAngularVelocity()
        if (wheelVelocities != null) {
            poseVelocity =
                TankKinematics.wheelToRobotVelocities(wheelVelocities, trackWidth).let {
                    if (extHeadingVel != null) Pose2d(it.vec(), extHeadingVel)
                    else it
                }
        }

        lastWheelPositions = wheelPositions
        lastExtHeading = extHeading
    }
}