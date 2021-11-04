package com.griffinrobotics.lib.trajectory

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.path.Path
import com.griffinrobotics.lib.profile.MotionProfile

/**
 * Trajectory segment backed by a [Path] and a [MotionProfile].
 *
 * @param path path
 * @param profile motion profile
 */
class PathTrajectorySegment constructor(
    val path: Path,
    val profile: MotionProfile,
) : TrajectorySegment {
    override fun duration() = profile.duration()

    override fun length() = path.length()

    override operator fun get(time: Double) = path[profile[time].x]
    override fun distance(time: Double) = profile[time].x

    override fun deriv(time: Double) = path.deriv(profile[time].x)

    override fun secondDeriv(time: Double) = path.secondDeriv(profile[time].x)

    override fun velocity(time: Double): Pose2d {
        val motionState = profile[time]
        return path.deriv(motionState.x) * motionState.v
    }

    override fun acceleration(time: Double): Pose2d {
        val motionState = profile[time]
        return path.secondDeriv(motionState.x) * motionState.v * motionState.v +
                path.deriv(motionState.x) * motionState.a
    }

    override fun start() = path[0.0]

    override fun end() = path[path.length()]
}