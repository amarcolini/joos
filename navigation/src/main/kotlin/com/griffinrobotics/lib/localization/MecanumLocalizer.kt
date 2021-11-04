package com.griffinrobotics.lib.localization

import com.griffinrobotics.lib.drive.Drive
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.MecanumKinematics
import com.griffinrobotics.lib.util.Angle

/**
 * Default localizer for mecanum drives based on the drive encoders and (optionally) a heading sensor.
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
            lastExtHeading = Double.NaN
            if (useExternalHeading) drive.externalHeading = value.heading
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    private var lastExtHeading = Double.NaN

    override fun update() {
        val wheelPositions = wheelPositions()
        val extHeading = if (useExternalHeading) drive.externalHeading else Double.NaN
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
            val finalHeadingDelta = if (!extHeading.isNaN()) {
                Angle.normDelta(extHeading - lastExtHeading)
            } else {
                robotPoseDelta.heading
            }
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                Pose2d(robotPoseDelta.vec(), finalHeadingDelta)
            )
        }

        val wheelVelocities = wheelVelocities()
        val extHeadingVel = drive.getExternalHeadingVelocity()
        poseVelocity = wheelVelocities?.let {
            MecanumKinematics.wheelToRobotVelocities(
                it,
                trackWidth,
                wheelBase,
                lateralMultiplier
            )
        }
        if (extHeadingVel != null) {
            poseVelocity = Pose2d((poseVelocity ?: return).vec(), extHeadingVel)
        }

        lastWheelPositions = wheelPositions
        lastExtHeading = extHeading
    }
}