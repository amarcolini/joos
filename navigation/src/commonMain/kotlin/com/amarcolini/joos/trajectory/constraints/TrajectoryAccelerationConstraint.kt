package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import kotlin.js.JsExport
import kotlin.math.max
import kotlin.math.min

/**
 * Motion profile acceleration constraint.
 */
@JsExport
fun interface TrajectoryAccelerationConstraint {

    /**
     * Returns the range of profile velocities allowed by this acceleration constraint.
     *
     * @param deriv pose derivative
     * @param lastDeriv previous pose derivative
     * @param ds the change in displacement between the current and previous pose derivatives
     * @param lastVel previous profile velocity
     */
    operator fun get(
        deriv: Pose2d,
        lastDeriv: Pose2d,
        ds: Double,
        lastVel: Double
    ): IntervalSet
}

typealias Interval = Pair<Double, Double>

typealias IntervalSet = List<Interval>

fun IntervalSet.intersection(other: Interval) =
    mapNotNull { it.intersection(other) }

fun Interval.intersection(other: Interval) =
    if (this intersects other) max(other.first, this.first) to min(other.second, this.second)
    else null

infix fun IntervalSet.intersects(other: Interval) =
    this.any { it intersects other }

infix fun Interval.intersects(other: Interval) =
    first in other || second in other || other.first in this || other.second in this

operator fun Interval.contains(vel: Double) = vel in first..second