package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.PathFollower
import com.amarcolini.joos.followers.TrajectoryFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.trajectory.Trajectory

/**
 * An interface representing a [Component] implementation of [Drive].
 */
interface DriveComponent : Component {
    var localizer: Localizer

    override fun update() {
        localizer.update()
        motors.update()
    }

    /**
     * All the motors in this drive.
     */
    val motors: MotorGroup

    fun setDriveSignal(driveSignal: DriveSignal)
    fun setDrivePower(drivePower: Pose2d)
}

/**
 * An abstract class representing a [Component] implementation of [Drive].
 */
abstract class ComponentDrive : Drive(), DriveComponent {
    companion object {
        @JvmStatic
        fun from(driveComponent: DriveComponent): ComponentDrive =
            object : ComponentDrive(), DriveComponent by driveComponent {}
    }
}

class FollowPathCommand(
    val path: Path,
    val pathFollower: PathFollower,
    val driveComponent: DriveComponent,
) : Command() {
    override val requirements = setOf(driveComponent)

    override fun init() {
        pathFollower.followPath(path)
    }

    override fun execute() {
        driveComponent.setDriveSignal(
            pathFollower.update(
                driveComponent.localizer.poseEstimate,
                driveComponent.localizer.poseVelocity
            )
        )
    }

    override fun isFinished() = !pathFollower.isFollowing()

    override fun end(interrupted: Boolean) {
        driveComponent.setDriveSignal(DriveSignal())
    }
}

open class FollowTrajectoryCommand(
    val trajectory: Trajectory,
    val trajectoryFollower: TrajectoryFollower,
    val driveComponent: DriveComponent,
) : Command() {
    final override val requirements = setOf(driveComponent)

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