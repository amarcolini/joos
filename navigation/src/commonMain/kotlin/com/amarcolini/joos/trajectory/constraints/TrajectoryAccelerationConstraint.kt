package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d

/**
 * Motion profile acceleration constraint.
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
fun interface TrajectoryAccelerationConstraint {

    /**
     * Returns the range of profile velocities allowed by this acceleration constraint.
     *
     * @param deriv pose derivative
     * @param lastDeriv previous pose derivative
     * @param ds the change in displacement between the current and previous pose derivatives
     * @param lastVel previous profile velocity
     */
    operator fun get(
        deriv: Pose2d,
        lastDeriv: Pose2d,
        ds: Double,
        lastVel: Double
    ): List<Pair<Double, Double>>
}