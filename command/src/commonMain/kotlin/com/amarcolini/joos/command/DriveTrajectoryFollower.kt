package com.amarcolini.joos.command

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
    fun trajectoryBuilder(): TrajectoryBuilder = trajectoryBuilder(localizer.poseEstimate)

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
        startPose: Pose2d = localizer.poseEstimate,
        startTangent: Angle = startPose.heading
    ): TrajectoryBuilder = TrajectoryBuilder(
        startPose,
        startTangent,
        constraints
    )

    /**
     * Returns a [TrajectoryBuilder] with the constraints of this drive.
     */
    fun trajectoryBuilder(
        startPose: Pose2d = localizer.poseEstimate,
        reversed: Boolean
    ): TrajectoryBuilder = TrajectoryBuilder(
        startPose,
        reversed,
        constraints
    )

    /**
     * Returns a [Command] that follows the provided trajectory.
     */
    fun followTrajectory(trajectory: Trajectory): FollowTrajectoryCommand = FollowTrajectoryCommand(
        trajectory, trajectoryFollower, this
    )
}