package com.amarcolini.joos.trajectory.constraints

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path

/**
 * Motion profile acceleration constraint.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
fun interface TrajectoryAccelerationConstraint {

    /**
     * Returns the maximum profile acceleration.
     *
     * @param lastS previous path displacement
     * @param s path displacement
     * @param lastVel previous profile velocity
     * @param dx distance between current and previous velocities
     */
    operator fun get(
        lastS: Double,
        s: Double,
        lastVel: Double,
        dx: Double,
        path: Path
    ): Double
}
