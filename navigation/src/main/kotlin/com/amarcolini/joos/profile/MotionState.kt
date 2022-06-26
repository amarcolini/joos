package com.amarcolini.joos.profile

import org.apache.commons.math3.util.FastMath

/**
 * Kinematic state of a motion profile at any given time.
 */
class MotionState @JvmOverloads constructor(
    @JvmField val x: Double,
    @JvmField val v: Double,
    @JvmField val a: Double = 0.0,
    @JvmField val j: Double = 0.0
) {

    /**
     * Returns the [MotionState] at time [t].
     */
    operator fun get(t: Double) =
        MotionState(
            x + v * t + a / 2 * FastMath.pow(t, 2) + j / 6 * FastMath.pow(t, 3),
            v + a * t + j / 2 * FastMath.pow(t, 2),
            a + j * t,
            j
        )

    /**
     * Returns a flipped (negated) version of the state.
     */
    fun flipped() = MotionState(-x, -v, -a, -j)

    /**
     * Returns the state with velocity, acceleration, and jerk zeroed.
     */
    fun stationary() = MotionState(x, 0.0, 0.0, 0.0)

    override fun toString() = String.format("(x=%.3f, v=%.3f, a=%.3f, j=%.3f)", x, v, a, j)
}