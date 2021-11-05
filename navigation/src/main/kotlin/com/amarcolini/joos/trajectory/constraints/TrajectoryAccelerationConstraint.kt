package com.amarcolini.joos.trajectory.constraints

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.amarcolini.joos.geometry.Pose2d

/**
 * Motion profile acceleration constraint.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
fun interface TrajectoryAccelerationConstraint {

    /**
     * Returns the maximum profile acceleration.
     *
     * @param s path displacement
     * @param pose pose
     * @param deriv pose derivative
     * @param baseRobotVel additive base velocity
     */
    operator fun get(
        s: Double,
        pose: Pose2d,
        deriv: Pose2d,
        baseRobotVel: Pose2d
    ): Double
}
