package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.abs
import kotlin.math.max

/**
 * Constraint limiting angular velocity.
 */
class AngularVelocityConstraint(
    val maxAngVel: Angle
) : TrajectoryVelocityConstraint {
    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val omega0 = baseRobotVel.heading
        if (abs(omega0) >= maxAngVel) {
            throw UnsatisfiableConstraint()
        }

        return max(
            (maxAngVel - omega0) / deriv.heading,
            (-maxAngVel - omega0) / deriv.heading
        )
    }
}