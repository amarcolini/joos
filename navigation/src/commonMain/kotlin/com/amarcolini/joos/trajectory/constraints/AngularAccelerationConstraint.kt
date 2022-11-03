package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.path.Path
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Constraint limiting angular acceleration.
 */
class AngularAccelerationConstraint(val maxAngAccel: Angle) : TrajectoryAccelerationConstraint {
    /**
     * Constructs an [AngularAccelerationConstraint] where [maxAngAccel] is in degrees or radians as specified by
     * [Angle.defaultUnits].
     */
    constructor(maxAngAccel: Double) : this(Angle(maxAngAccel))

    private val actualMaxAngAccel = maxAngAccel.radians
    override fun get(lastS: Double, s: Double, lastVel: Double, dx: Double, path: Path): Double {
        val currentCurvature = path.deriv(s).heading.radians
        val lastCurvature = path.deriv(lastS).heading.radians

        val part1 = ((lastCurvature + currentCurvature) * lastVel).pow(2)
        val part2 = 8 * currentCurvature * actualMaxAngAccel * dx

        val v1 = ((lastCurvature - currentCurvature) * lastVel + sqrt(part1 + part2)) /
                (2 * currentCurvature)
        val v2star = (lastCurvature - currentCurvature) * lastVel - sqrt(part1 - part2) /
                (2 * currentCurvature)
        val v1hat = -(2 * dx * actualMaxAngAccel) / (lastCurvature * lastVel) - lastVel
        val v2hat = (2 * dx * actualMaxAngAccel) / (lastCurvature * lastVel) - lastVel

        return when {
            currentCurvature > 0 ->
                if (part1 - part2 < 0) v1
                else max(v2star, v1)
            currentCurvature < 0 ->
                if (part1 + part2 < 0) v2star
                else max(v1, v2star)
            else -> when {
                lastCurvature > 0 -> v2hat
                lastCurvature < 0 -> v1hat
                else -> Double.POSITIVE_INFINITY
            }
        }
    }
}