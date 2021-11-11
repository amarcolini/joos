package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.path.Path
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Constraint limiting translational acceleration.
 */
class TranslationalAccelerationConstraint(
    val maxTranslationalAccel: Double
) : TrajectoryAccelerationConstraint {
    override fun get(lastS: Double, s: Double, lastVel: Double, dx: Double, path: Path): Double =
        sqrt(lastVel.pow(2) + 2 * maxTranslationalAccel * dx)
}
