package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.FunctionalCommand
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
    abstract val constraints: TrajectoryConstraints
    protected abstract val imu: Imu?

    override fun update() {
        updatePoseEstimate()
    }

    @JvmOverloads
    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        startTangent: Double = startPose.heading
    ) = TrajectoryBuilder(
        startPose,
        startTangent,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel, constraints.maxAngAccel, constraints.maxAngJerk
    )

    @JvmOverloads
    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        reversed: Boolean
    ) = TrajectoryBuilder(
        startPose,
        reversed,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel, constraints.maxAngAccel, constraints.maxAngJerk
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

    override val rawExternalHeading: Double
        get() = imu?.heading ?: NaN

    override fun getExternalHeadingVelocity() = imu?.headingVelocity
}