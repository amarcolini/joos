package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.CommandScheduler
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.command.FunctionalCommand
import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.constraints.TrajectoryConstraints

/**
 * A [Component] implementation of [Drive].
 */
abstract class DriveComponent : Drive(), Component {
    abstract var trajectoryFollower: TrajectoryFollower
    abstract val constraints: TrajectoryConstraints
    protected abstract val externalHeadingSensor: AngleSensor?
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
            val telemetry = CommandScheduler.telem
            val trajectory = getCurrentTrajectory()
            if (trajectory != null) {
                poseHistory.add(poseEstimate)
                if (poseHistoryLimit > -1 && poseHistory.size > poseHistoryLimit)
                    poseHistory.removeFirst()
                telemetry.drawSampledTrajectory(trajectory, pathColor, turnColor, waitColor)
                telemetry.drawPoseHistory(poseHistory, robotColor)
            }
            telemetry.fieldOverlay().setStrokeWidth(1)
            telemetry.drawRobot(poseEstimate, robotColor)
        }
    }

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     */
    @JvmOverloads
    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        startTangent: Angle = startPose.heading
    ): TrajectoryBuilder = TrajectoryBuilder(
        startPose,
        startTangent,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel, constraints.maxAngAccel, constraints.maxAngJerk
    )

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     *
     * @param startTangent the starting tangent in degrees or radians as specified by [Angle.defaultUnits]
     */
    @JvmOverloads
    fun trajectoryBuilder(startPose: Pose2d = poseEstimate, startTangent: Double): TrajectoryBuilder =
        trajectoryBuilder(startPose, Angle(startTangent))

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     */
    @JvmOverloads
    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        reversed: Boolean
    ): TrajectoryBuilder = TrajectoryBuilder(
        startPose,
        reversed,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel, constraints.maxAngAccel, constraints.maxAngJerk
    )

    /**
     * Returns a [Command] that follows the provided trajectory.
     */
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

    abstract fun setRunMode(runMode: Motor.RunMode)
    abstract fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior)
}