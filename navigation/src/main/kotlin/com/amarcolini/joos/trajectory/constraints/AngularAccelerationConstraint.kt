package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.path.Path
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Constraint limiting angular acceleration.
 */
class AngularAccelerationConstraint(val maxAngAccel: Double) : TrajectoryAccelerationConstraint {
    override fun get(lastS: Double, s: Double, lastVel: Double, dx: Double, path: Path): Double {
        val currentCurvature = path.curvature(s)
        val lastCurvature = path.curvature(lastS)
        return sqrt(
            (lastCurvature * lastVel.pow(2) + 2 * maxAngAccel * dx) /
                    currentCurvature
        )
    }
}