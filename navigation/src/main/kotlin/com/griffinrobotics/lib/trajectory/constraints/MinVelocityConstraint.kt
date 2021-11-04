package com.griffinrobotics.lib.trajectory.constraints

import com.griffinrobotics.lib.geometry.Pose2d

/**
 * Composite constraint representing the minimum of its constituent velocity constraints.
 */
class MinVelocityConstraint(
    val constraints: List<TrajectoryVelocityConstraint>
) : TrajectoryVelocityConstraint {
    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d) =
        constraints.map { it[s, pose, deriv, baseRobotVel] }.minOrNull()!!
}
