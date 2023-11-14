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

    override fun get(deriv: Pose2d, lastDeriv: Pose2d, ds: Double, lastVel: Double): IntervalSet {
        val sets = constraints.map { it[deriv, lastDeriv, ds, lastVel] }
        var currentSet: IntervalSet = sets[0]
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
}