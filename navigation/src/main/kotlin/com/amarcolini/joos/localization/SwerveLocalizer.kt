package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.util.Angle

/**
 * Default localizer for swerve drives based on the drive encoder positions, module orientations, and (optionally) a
 * heading sensor.
 */
class SwerveLocalizer @JvmOverloads constructor(
    private val wheelPositions: () -> List<Double>,
    private val wheelVelocities: () -> List<Double>? = { null },
    private val moduleOrientations: () -> List<Double>,
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth,
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
        val moduleOrientations = moduleOrientations()
        val extHeading = if (useExternalHeading) drive.externalHeading else Double.NaN
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
            SwerveKinematics.wheelToRobotVelocities(
                it,
                moduleOrientations,
                wheelBase,
                trackWidth
            )
        }
        if (extHeadingVel != null) {
            poseVelocity = Pose2d((poseVelocity ?: return).vec(), extHeadingVel)
        }

        lastWheelPositions = wheelPositions
        lastExtHeading = extHeading
    }
}