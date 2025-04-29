package com.amarcolini.joos.command

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.PathFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.trajectory.Trajectory
import kotlin.jvm.JvmField

open class FollowPathCommand<T>(
    @JvmField val path: Path,
    @JvmField val pathFollower: PathFollower,
    @JvmField val driveComponent: T,
) : Command() where T : Drive, T : Component {
    final override val requirements: Set<Component> = setOf(driveComponent)

    final override fun init() {
        pathFollower.followPath(path)
        postInit()
    }

    open fun postInit() {}

    final override fun execute() {
        driveComponent.setDriveSignal(
            pathFollower.update(
                driveComponent.localizer.poseEstimate,
                driveComponent.localizer.poseVelocity
            )
        )
        postExecute()
    }

    open fun postExecute() {}

    final override fun isFinished() = !pathFollower.isFollowing()

    final override fun end(interrupted: Boolean) {
        driveComponent.setDriveSignal(DriveSignal())
    }
}

open class FollowTrajectoryCommand<T>(
    @JvmField val trajectory: Trajectory,
    @JvmField val trajectoryFollower: TrajectoryFollower,
    @JvmField val driveComponent: T,
) : Command() where T : Drive, T : Component {
    final override val requirements: Set<Component> = setOf(driveComponent)

    final override fun init() {
        trajectoryFollower.followTrajectory(trajectory)
        postInit()
    }

    open fun postInit() {}

    final override fun execute() {
        driveComponent.setDriveSignal(
            trajectoryFollower.update(
                driveComponent.localizer.poseEstimate,
                driveComponent.localizer.poseVelocity
            )
        )
        postExecute()
    }

    open fun postExecute() {}

    final override fun isFinished() = !trajectoryFollower.isFollowing()

    final override fun end(interrupted: Boolean) {
        driveComponent.setDriveSignal(DriveSignal())
    }
}