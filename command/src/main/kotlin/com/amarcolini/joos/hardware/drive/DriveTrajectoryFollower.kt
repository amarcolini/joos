package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.FunctionalCommand
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.constraints.TrajectoryConstraints

interface DriveTrajectoryFollower : DriveComponent {
    val trajectoryFollower: TrajectoryFollower
    val constraints: TrajectoryConstraints

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     */
    fun trajectoryBuilder(): TrajectoryBuilder = trajectoryBuilder(poseEstimate)

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     */
    fun trajectoryBuilder(
        startPose: Pose2d,
    ): TrajectoryBuilder = trajectoryBuilder(startPose, startPose.heading)

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     */
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
     */
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
        return if (!trajectoryFollower.isFollowing()) {
            FunctionalCommand(
                init = { trajectoryFollower.followTrajectory(trajectory) },
                execute = { setDriveSignal(trajectoryFollower.update(poseEstimate, poseVelocity)) },
                isFinished = { !trajectoryFollower.isFollowing() },
                end = { setDriveSignal(DriveSignal()) },
                requirements = setOf(this)
            )
        } else Command.empty()
    }

    /**
     * Returns the trajectory currently being followed by this drive, if any.
     */
    fun getCurrentTrajectory(): Trajectory? =
        if (trajectoryFollower.isFollowing()) trajectoryFollower.trajectory else null
}