package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.FunctionalCommand
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.followers.PathFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder

interface DrivePathFollower : DriveComponent {
    val pathFollower: PathFollower

    /**
     * Returns a [PathBuilder] with the constraints of this drive.
     */
    fun pathBuilder(): PathBuilder = PathBuilder(poseEstimate)

    /**
     * Returns a [PathBuilder] with the constraints of this drive.
     */
    fun pathBuilder(startTangent: Angle): PathBuilder = PathBuilder(
        poseEstimate,
        startTangent
    )

    /**
     * Returns a [PathBuilder] with the constraints of this drive.
     */
    fun pathBuilder(reversed: Boolean): PathBuilder = PathBuilder(
        poseEstimate,
        reversed
    )

    /**
     * Returns a [Command] that follows the provided path.
     */
    fun followerPath(path: Path): Command {
        return if (!pathFollower.isFollowing()) {
            FunctionalCommand(
                init = { pathFollower.followPath(path) },
                execute = { setDriveSignal(pathFollower.update(poseEstimate, poseVelocity)) },
                isFinished = { !pathFollower.isFollowing() },
                end = { setDriveSignal(DriveSignal()) },
                requirements = setOf(this)
            )
        } else Command.emptyCommand()
    }

    /**
     * Returns the path currently being followed by this drive, if any.
     */
    fun getCurrentPath(): Path? =
        if (pathFollower.isFollowing()) pathFollower.path else null
}