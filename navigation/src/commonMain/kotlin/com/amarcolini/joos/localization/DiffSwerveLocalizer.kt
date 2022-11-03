package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.kinematics.Kinematics
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for differential swerve drives based on the drive encoder positions and (optionally) a
 * heading sensor.
 *
 * @param gearRotations the total rotation of each gear
 * @param gearPositions the position of each gear in linear distance units
 * @param gearVelocities the current velocities of each gear in linear distance units
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param drive the drive this localizer is using
 * @param useExternalHeading whether to use [drive]'s external heading sensor
 */
class DiffSwerveLocalizer @JvmOverloads constructor(
    private val gearRotations: () -> List<Angle>,
    private val gearPositions: () -> List<Double>,
    private val gearVelocities: () -> List<Double>? = { null },
    private val trackWidth: Double,
    private val drive: Drive,
    private val useExternalHeading: Boolean = true
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastGearPositions = emptyList()
            lastExtHeading = null
            if (useExternalHeading) drive.externalHeading = value.heading
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastGearPositions = emptyList<Double>()
    private var lastExtHeading: Angle? = null

    override fun update() {
        val gearRotations = gearRotations()
        val gearPositions = gearPositions()
        val extHeading: Angle? = if (useExternalHeading) drive.externalHeading else null
        if (lastGearPositions.isNotEmpty()) {
            val gearDeltas = gearPositions
                .zip(lastGearPositions)
                .map { it.first - it.second }
            val robotPoseDelta = DiffSwerveKinematics.gearToRobotVelocities(
                gearRotations,
                gearDeltas,
                trackWidth
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

        val gearVelocities = gearVelocities()
        val extHeadingVel = if (useExternalHeading) drive.getExternalHeadingVelocity() else null
        poseVelocity =
            if (gearVelocities != null)
                DiffSwerveKinematics.gearToRobotVelocities(
                    gearRotations,
                    gearVelocities,
                    trackWidth
                ).let {
                    if (extHeadingVel != null) Pose2d(it.vec(), extHeadingVel)
                    else it
                }
            else null

        lastGearPositions = gearPositions
        lastExtHeading = extHeading
    }
}