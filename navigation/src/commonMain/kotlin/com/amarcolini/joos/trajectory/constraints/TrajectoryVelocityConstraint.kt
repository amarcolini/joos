package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d

/**
 * Motion profile velocity constraint.
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
fun interface TrajectoryVelocityConstraint {
    /**
     * Returns the maximum profile velocity.
     *
     * @param pose pose
     * @param deriv pose derivative
     * @param lastDeriv previous pose derivative
     * @param ds the change in displacement between the current and previous pose derivatives
     * @param baseRobotVel additive base velocity
     */
    operator fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double
}