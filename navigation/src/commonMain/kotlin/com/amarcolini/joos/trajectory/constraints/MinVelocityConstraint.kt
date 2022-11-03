package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d

/**
 * Composite constraint representing the minimum of its constituent velocity constraints.
 */
class MinVelocityConstraint(
    val constraints: List<TrajectoryVelocityConstraint>
) : TrajectoryVelocityConstraint {
    constructor(vararg constraints: TrajectoryVelocityConstraint) : this(constraints.toList())

    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d) =
        constraints.minOf { it[s, pose, deriv, baseRobotVel] }
}