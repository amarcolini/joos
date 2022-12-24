package com.amarcolini.joos.profile

/**
 * Motion profile acceleration constraint.
 */
fun interface AccelerationConstraint {

    /**
     * Returns the maximum profile velocity at displacement [s] using [ds] and [lastVel].
     * @param s the current displacement
     * @param ds the change in displacement from the last profile velocity
     * @param lastVel the last profile velocity
     */
    operator fun get(s: Double, ds: Double, lastVel: Double): Double
}