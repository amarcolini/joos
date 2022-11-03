package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.MecanumKinematics
import com.amarcolini.joos.util.*
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for mecanum drives based on the drive encoders and (optionally) a heading sensor.
 *
 * @param wheelPositions wheel positions in linear distance units
 * @param wheelVelocities wheel velocities in linear distance units
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 * @param lateralMultiplier lateral multiplier
 * @param drive the drive this localizer is using
 * @param useExternalHeading whether to use [drive]'s external heading sensor
 */
class MecanumLocalizer @JvmOverloads constructor(
    private val wheelPositions: () -> List<Double>,
    private val wheelVelocities: () -> List<Double>? = { null },
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth,
    private val lateralMultiplier: Double = 1.0,
    private val drive: Drive,
    private val useExternalHeading: Boolean = true
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            lastExtHeading = null
            if (useExternalHeading) drive.externalHeading = value.heading
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    private var lastExtHeading: Angle? = null

    override fun update() {
        val wheelPositions = wheelPositions()
        val extHeading: Angle? = if (useExternalHeading) drive.externalHeading else null
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
        val extHeadingVel = if (useExternalHeading) drive.getExternalHeadingVelocity() else null
        poseVelocity =
            if (wheelVelocities != null)
                MecanumKinematics.wheelToRobotVelocities(
                    wheelVelocities,
                    trackWidth,
                    wheelBase,
                    lateralMultiplier
                ).let {
                    if (extHeadingVel != null) Pose2d(it.vec(), extHeadingVel)
                    else it
                }
            else null

        lastWheelPositions = wheelPositions
        lastExtHeading = extHeading
    }
}