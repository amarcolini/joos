package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.*
import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import kotlin.Double.Companion.NaN

/**
 * A [Component] implementation of [Drive].
 */
abstract class DriveComponent : Drive(), Component {
    protected abstract val trajectoryFollower: TrajectoryFollower
    abstract val constants: TrajectoryConstraints
    protected abstract val imu: Imu?

    override fun update(scheduler: CommandScheduler) {
        updatePoseEstimate()
    }

    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        startHeading: Double = startPose.heading
    ) = TrajectoryBuilder(
        startPose,
        startHeading,
        constants.velConstraint,
        constants.accelConstraint,
        constants.maxAngVel, constants.maxAngAccel, constants.maxAngJerk
    )

    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        reversed: Boolean
    ) = TrajectoryBuilder(
        startPose,
        reversed,
        constants.velConstraint,
        constants.accelConstraint,
        constants.maxAngVel, constants.maxAngAccel, constants.maxAngJerk
    )

    fun followTrajectory(trajectory: Trajectory): Command {
        return if (!trajectoryFollower.isFollowing()) {
            FunctionalCommand(
                init = { trajectoryFollower.followTrajectory(trajectory) },
                execute = { setDriveSignal(trajectoryFollower.update(poseEstimate, poseVelocity)) },
                isFinished = { !trajectoryFollower.isFollowing() },
                end = { setDriveSignal(DriveSignal()) },
                requirements = setOf(this)
            )
        } else Command.emptyCommand()
    }

    final override val rawExternalHeading: Double
        get() = imu?.heading ?: NaN

    final override fun getExternalHeadingVelocity() = imu?.headingVelocity
}