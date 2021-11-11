package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path

/**
 * Composite constraint representing the minimum of its constituent acceleration constraints.
 */
class MinAccelerationConstraint(
    val constraints: List<TrajectoryAccelerationConstraint>
) : TrajectoryAccelerationConstraint {
    override fun get(lastS: Double, s: Double, lastVel: Double, dx: Double, path: Path) =
        constraints.map { it[lastS, s, lastVel, dx, path] }.minOrNull()!!
}
