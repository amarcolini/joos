package com.amarcolini.joos.profile

import com.amarcolini.joos.serialization.format
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.pow

/**
 * Kinematic state of a motion profile at any given time.
 */
@JsExport
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
            x + v * t + a / 2 * t.pow(2) + j / 6 * t.pow(3),
            v + a * t + j / 2 * t.pow(2),
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

    override fun toString() = "(x=${x.format(3)}, v=${v.format(3)}, a=${a.format(3)}, j=${j.format(3)})"
}