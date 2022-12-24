package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import kotlin.math.max
import kotlin.math.min

/**
 * Composite constraint representing the minimum of its constituent acceleration constraints.
 */
class MinAccelerationConstraint(
    val constraints: List<TrajectoryAccelerationConstraint>
) : TrajectoryAccelerationConstraint {
    constructor(vararg constraints: TrajectoryAccelerationConstraint) : this(constraints.toList())

    override fun get(deriv: Pose2d, lastDeriv: Pose2d, ds: Double, lastVel: Double): List<Pair<Double, Double>> {
        val sets = constraints.map { it[deriv, lastDeriv, ds, lastVel] }
        var currentSet: List<Pair<Double, Double>> = sets[0]
        sets.drop(1).forEach { set ->
            val newSet = ArrayList<Pair<Double, Double>>()
            for (range in set) {
                newSet += currentSet.intersection(range)
            }
            currentSet = newSet.ifEmpty {
                throw UnsatisfiableConstraint()
            }
        }
        return currentSet
    }

    private fun List<Pair<Double, Double>>.intersection(other: Pair<Double, Double>) =
        mapNotNull { it.intersection(other) }

    private fun Pair<Double, Double>.intersection(other: Pair<Double, Double>) =
        if (this intersects other) max(other.first, this.first) to min(other.second, this.second)
        else null

    private infix fun List<Pair<Double, Double>>.intersects(other: Pair<Double, Double>) =
        this.any { it intersects other }

    private infix fun Pair<Double, Double>.intersects(other: Pair<Double, Double>) =
        first in other || second in other || other.first in this || other.second in this

    private operator fun Pair<Double, Double>.contains(vel: Double) = vel in first..second
}