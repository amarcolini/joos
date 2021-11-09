package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import kotlin.math.pow

class AngularAccelerationConstraint(val maxAngAccel: Double) : TrajectoryAccelerationConstraint {
    override fun get(
        s: Double,
        pose: Pose2d,
        deriv: Pose2d,
        secondDeriv: Pose2d,
        baseRobotVel: Pose2d
    ): Double {
        val result = maxAngAccel * (1 / secondDeriv.vec().norm())
        return if (!result.isNaN()) result else Double.POSITIVE_INFINITY
    }
}