package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.path.Path
import org.apache.commons.math3.util.FastMath
import kotlin.math.sqrt

/**
 * Constraint limiting translational acceleration.
 */
class TranslationalAccelerationConstraint(
    val maxTranslationalAccel: Double
) : TrajectoryAccelerationConstraint {
    override fun get(lastS: Double, s: Double, lastVel: Double, dx: Double, path: Path): Double =
        sqrt(FastMath.pow(lastVel, 2) + 2 * maxTranslationalAccel * dx)
}
