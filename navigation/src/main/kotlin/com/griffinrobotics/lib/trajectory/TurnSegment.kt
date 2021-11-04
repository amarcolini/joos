package com.griffinrobotics.lib.trajectory

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.geometry.Vector2d
import com.griffinrobotics.lib.profile.MotionProfile
import com.griffinrobotics.lib.util.Angle

/**
 * Trajectory segment representing a point turn.
 * @param pose start pose
 * @param profile heading motion profile
 */
class TurnSegment(
    private val pose: Pose2d,
    private val profile: MotionProfile
) : TrajectorySegment {
    override fun duration() = profile.duration()

    override fun length() = 0.0

    override fun get(time: Double) = Pose2d(
        pose.vec(),
        Angle.norm(pose.heading + profile[time].x)
    )

    override fun distance(time: Double) = 0.0

    override fun deriv(time: Double) = Pose2d(
        Angle.vec(get(time).heading),
        profile[time].v
    )

    override fun secondDeriv(time: Double) = acceleration(time)

    override fun velocity(time: Double) = Pose2d(
        Vector2d(),
        profile[time].v
    )

    override fun acceleration(time: Double) = Pose2d(
        Vector2d(),
        profile[time].a
    )

    override fun start() = pose

    override fun end() = Pose2d(
        pose.vec(),
        Angle.norm(pose.heading + profile.end().x)
    )
}