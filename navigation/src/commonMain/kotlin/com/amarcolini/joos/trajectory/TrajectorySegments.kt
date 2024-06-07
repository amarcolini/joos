package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.profile.MotionProfile
import com.amarcolini.joos.util.rad

/**
 * Generic trajectory segment.
 */
interface TrajectorySegment {
    fun duration(): Double

    fun length(): Double

    operator fun get(time: Double): Pose2d

    fun distance(time: Double): Double

    fun deriv(time: Double): Pose2d

    fun secondDeriv(time: Double): Pose2d

    fun velocity(time: Double): Pose2d

    fun acceleration(time: Double): Pose2d

    fun start(): Pose2d

    fun end(): Pose2d
}

/**
 * Trajectory segment backed by a [Path] and a [MotionProfile].
 *
 * @param path path
 * @param profile motion profile
 */
class PathTrajectorySegment(
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

/**
 * Trajectory segment representing a point turn.
 * @param pose start pose
 * @param profile heading motion profile, where heading is specified in radians
 */
class TurnSegment(
    private val pose: Pose2d,
    private val profile: MotionProfile
) : TrajectorySegment {
    override fun duration() = profile.duration()

    override fun length() = 0.0

    override fun get(time: Double) = Pose2d(
        pose.vec(),
        (pose.heading + profile[time].x.rad).norm()
    )

    override fun distance(time: Double) = 0.0

    override fun deriv(time: Double) = Pose2d(
        get(time).heading.vec(),
        profile[time].v.rad
    )

    override fun secondDeriv(time: Double) = acceleration(time)

    override fun velocity(time: Double) = Pose2d(
        Vector2d(),
        profile[time].v.rad
    )

    override fun acceleration(time: Double) = Pose2d(
        Vector2d(),
        profile[time].a.rad
    )

    override fun start() = pose

    override fun end() = Pose2d(
        pose.vec(),
        (pose.heading + profile.end().x.rad).norm()
    )
}

/**
 * Static trajectory segment that holds a constant pose.
 *
 * @param pose pose to hold
 * @param duration duration in seconds
 */
class WaitSegment(private val pose: Pose2d, private val duration: Double) : TrajectorySegment {
    override fun duration() = duration

    override fun length() = 0.0

    override fun get(time: Double) = pose
    override fun distance(time: Double) = 0.0

    override fun deriv(time: Double) = Pose2d(pose.heading.vec())
    override fun secondDeriv(time: Double) = Pose2d()
    override fun velocity(time: Double) = Pose2d()
    override fun acceleration(time: Double) = Pose2d()

    override fun start() = pose

    override fun end() = pose
}