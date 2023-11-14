package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import kotlin.math.sqrt

/**
 * Constraint limiting translational acceleration.
 */
class TranslationalAccelerationConstraint(
    val maxTranslationalAccel: Double
) : TrajectoryAccelerationConstraint {
    override fun get(deriv: Pose2d, lastDeriv: Pose2d, ds: Double, lastVel: Double): IntervalSet {
        val p1 = lastVel * lastVel
        val p2 = 2 * maxTranslationalAccel * ds
        val min = if (p1 > p2) sqrt(p1 - p2) else 0.0
        return listOf(min to sqrt(p1 + p2))
    }
}