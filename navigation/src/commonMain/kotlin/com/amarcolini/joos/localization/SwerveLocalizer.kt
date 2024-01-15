package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.SwerveModule
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.SwerveKinematics
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for swerve drives based on the drive encoder positions and module orientations.
 *
 * @param modules the individual swerve modules
 * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
 */
class SwerveLocalizer(
    @JvmField val modules: List<SwerveModule>,
    private val modulePositions: List<Vector2d>
) : DeadReckoningLocalizer {
    /**
     * Default localizer for swerve drives based on the drive encoder positions and module orientations.
     *
     * @param modules the individual swerve modules
     * @param trackWidth lateral distance between pairs of modules on different sides of the robot
     * @param wheelBase distance between pairs of modules on the same side of the robot
     */
    @JvmOverloads
    constructor(
        modules: List<SwerveModule>,
        trackWidth: Double,
        wheelBase: Double = trackWidth,
    ) : this(
        modules,
        SwerveKinematics.getModulePositions(trackWidth, wheelBase)
    )

    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    override var lastRobotPoseDelta: Pose2d = Pose2d()
        private set

    override fun update() {
        val wheelPositions = modules.map(SwerveModule::getWheelPosition)
        val moduleOrientations = modules.map(SwerveModule::getModuleOrientation)
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val robotPoseDelta = SwerveKinematics.moduleToRobotVelocities(
                wheelDeltas,
                moduleOrientations,
                modulePositions
            )
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                robotPoseDelta
            )
            lastRobotPoseDelta = robotPoseDelta
        }

        val wheelVelocities =
            modules.mapNotNull(SwerveModule::getWheelVelocity).takeIf { it.size == modules.size }
        poseVelocity =
            if (wheelVelocities != null)
                SwerveKinematics.moduleToRobotVelocities(
                    wheelVelocities,
                    moduleOrientations,
                    modulePositions,
                )
            else null

        lastWheelPositions = wheelPositions
    }
}