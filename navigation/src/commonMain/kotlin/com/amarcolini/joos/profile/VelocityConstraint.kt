package com.amarcolini.joos.profile

/**
 * Motion profile velocity constraint.
 */
fun interface VelocityConstraint {

    /**
     * Returns the maximum profile velocity at displacement [s] and change in displacement [ds].
     */
    operator fun get(s: Double, ds: Double): Double
}