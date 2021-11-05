package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d

/**
 * Constraint limiting profile acceleration.
 */
class ProfileAccelerationConstraint(
    val maxProfileAccel: Double
) : TrajectoryAccelerationConstraint {
    override fun get(
        s: Double,
        pose: Pose2d,
        deriv: Pose2d,
        baseRobotVel: Pose2d
    ) = maxProfileAccel
}
