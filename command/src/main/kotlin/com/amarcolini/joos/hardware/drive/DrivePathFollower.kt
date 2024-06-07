package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.followers.PathFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder

interface DrivePathFollower : DriveComponent {
    val pathFollower: PathFollower

    /**
     * Returns a [PathBuilder] with the constraints of this drive.
     */
    fun pathBuilder(): PathBuilder = PathBuilder(localizer.poseEstimate)

    /**
     * Returns a [PathBuilder] with the constraints of this drive.
     */
    fun pathBuilder(startTangent: Angle): PathBuilder = PathBuilder(
        localizer.poseEstimate,
        startTangent
    )

    /**
     * Returns a [PathBuilder] with the constraints of this drive.
     */
    fun pathBuilder(reversed: Boolean): PathBuilder = PathBuilder(
        localizer.poseEstimate,
        reversed
    )

    /**
     * Returns a [FollowPathCommand] that follows the provided path.
     */
    fun followPath(path: Path): FollowPathCommand = FollowPathCommand(
        path, pathFollower, this
    )
}