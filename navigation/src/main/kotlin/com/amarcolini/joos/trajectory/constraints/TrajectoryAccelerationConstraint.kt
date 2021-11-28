package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.path.Path
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Motion profile acceleration constraint.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
fun interface TrajectoryAccelerationConstraint {

    /**
     * Returns the maximum profile velocity allowed by this acceleration constraint.
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
