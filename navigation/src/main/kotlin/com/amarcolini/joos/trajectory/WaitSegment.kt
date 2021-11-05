package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.Angle

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

    override fun deriv(time: Double) = Pose2d(Angle.vec(pose.heading))
    override fun secondDeriv(time: Double) = Pose2d()
    override fun velocity(time: Double) = Pose2d()
    override fun acceleration(time: Double) = Pose2d()

    override fun start() = pose

    override fun end() = pose
}