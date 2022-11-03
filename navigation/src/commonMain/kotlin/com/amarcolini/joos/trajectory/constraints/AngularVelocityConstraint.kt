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
    /**
     * Constructs a [TrajectoryVelocityConstraint] where [maxAngVel] is in degrees or radians as specified by
     * [Angle.defaultUnits].
     */
    constructor(maxAngVel: Double) : this(Angle(maxAngVel))

    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d): Double {
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