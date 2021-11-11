package com.amarcolini.joos.profile

/**
 * Motion profile acceleration constraint.
 */
fun interface AccelerationConstraint {

    /**
     * Returns the maximum profile velocity at displacement [s] using [lastS], [lastVel], and [dx].
     * @param s the current displacement
     * @param lastS the previous displacement
     * @param lastVel the last profile velocity
     * @param dx the distance between the current and previous velocities
     */
    operator fun get(lastS: Double, s: Double, lastVel: Double, dx: Double): Double
}
