package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
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
    override fun get(deriv: Pose2d, lastDeriv: Pose2d, ds: Double, lastVel: Double): List<Pair<Double, Double>> {
        val currentCurvature = deriv.heading.radians
        val lastCurvature = lastDeriv.heading.radians

        if (currentCurvature == lastCurvature) return listOf(0.0 to Double.POSITIVE_INFINITY)

        val part0 = (lastCurvature - currentCurvature) * lastVel
        val part1 = ((lastCurvature + currentCurvature) * lastVel).pow(2)
        val part2 = 8 * currentCurvature * actualMaxAngAccel * ds
        val denom = 1 / (2 * currentCurvature)

        val v1 = (part0 + sqrt(part1 + part2)) * denom
        val v2 = (part0 - sqrt(part1 + part2)) * denom
        val v1star = (part0 + sqrt(part1 - part2)) * denom
        val v2star = (part0 - sqrt(part1 - part2)) * denom

        val v1hat = -(2 * ds * actualMaxAngAccel) / (lastCurvature * lastVel) - lastVel
        val v2hat = (2 * ds * actualMaxAngAccel) / (lastCurvature * lastVel) - lastVel

        return when {
            currentCurvature > 0 ->
                if (part1 - part2 < 0) listOf(v2 to v1)
                else listOf(v2 to v2star, v1star to v1)
            currentCurvature < 0 ->
                if (part1 + part2 < 0) listOf(v1star to v2star)
                else listOf(v1star to v1, v2 to v2star)
            else -> when {
                lastCurvature > 0 -> listOf(v1hat to v2hat)
                lastCurvature < 0 -> listOf(v2hat to v1hat)
                else -> listOf(0.0 to Double.POSITIVE_INFINITY)
            }
        }
    }
}