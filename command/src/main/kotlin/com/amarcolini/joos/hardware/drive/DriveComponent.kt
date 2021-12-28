package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.CommandScheduler
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
 */
abstract class DriveComponent : Drive(), Component {
    protected abstract val trajectoryFollower: TrajectoryFollower
    abstract val constraints: TrajectoryConstraints
    protected abstract val imu: Imu?
    private val poseHistory = ArrayList<Pose2d>()
    var pathColor: String = "#4CAF50"
    var turnColor: String = "#7c4dff"
    var waitColor: String = "#dd2c00"
    var robotColor: String = "#3F51B5"

    /**
     * Whether current drive pose, trajectory, and pose history are to be displayed
     * on FTC Dashboard.
     */
    var dashboardEnabled: Boolean = true
    var poseHistoryLimit: Int = 100

    override fun update() {
        updatePoseEstimate()
        if (dashboardEnabled) {
            val trajectory = getCurrentTrajectory()
            if (trajectory != null) {
                poseHistory.add(poseEstimate)
                if (poseHistoryLimit > -1 && poseHistory.size > poseHistoryLimit)
                    poseHistory.removeFirst()
                DashboardUtil.drawSampledTrajectory(trajectory, pathColor, turnColor, waitColor)
                DashboardUtil.drawPoseHistory(poseHistory, robotColor)
            }
            CommandScheduler.packet.fieldOverlay().setStrokeWidth(1)
            DashboardUtil.drawRobot(poseEstimate, robotColor)
        }
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
        poseHistory.clear()
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

    abstract fun setRunMode(runMode: Motor.RunMode)
    abstract fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior)
}