package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.profile.MotionProfile
import com.amarcolini.joos.util.rad

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