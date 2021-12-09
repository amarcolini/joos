package com.amarcolini.joos.hardware.drive

import com.acmerobotics.dashboard.config.Config
import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.FunctionalCommand
import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import com.amarcolini.joos.util.DashboardUtil
import kotlin.Double.Companion.NaN

/**
 * A [Component] implementation of [Drive].
 *
 * @param useDashboard whether current drive pose, trajectory, and pose history are to be displayed
 * on FTC Dashboard.
 */
@Config
abstract class DriveComponent(private val useDashboard: Boolean = true) : Drive(), Component {
    protected abstract val trajectoryFollower: TrajectoryFollower
    abstract val constraints: TrajectoryConstraints
    protected abstract val imu: Imu?
    private val poseHistory = ArrayList<Pose2d>()

    companion object {
        @JvmStatic
        val POSE_HISTORY_LIMIT = 100
    }

    override fun update() {
        updatePoseEstimate()
        getCurrentTrajectory()?.path?.let { DashboardUtil.drawSampledPath(path = it) }
        if (trajectoryFollower.isFollowing()) poseHistory += poseEstimate
        if (POSE_HISTORY_LIMIT > -1 && poseHistory.size > POSE_HISTORY_LIMIT) poseHistory.removeFirst()
        if (trajectoryFollower.isFollowing()) DashboardUtil.drawPoseHistory(poseHistory = poseHistory)
        DashboardUtil.drawRobot(pose = poseEstimate)
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

    /**
     * Returns the trajectory currently being followed by this drive, if any.
     */
    fun getCurrentTrajectory(): Trajectory? =
        if (trajectoryFollower.isFollowing()) trajectoryFollower.trajectory else null

    override val rawExternalHeading: Double
        get() = imu?.heading ?: NaN

    override fun getExternalHeadingVelocity() = imu?.headingVelocity

    abstract fun setWeightedDrivePower(drivePower: Pose2d)
    abstract fun setRunMode(runMode: Motor.RunMode)
    abstract fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior)
}