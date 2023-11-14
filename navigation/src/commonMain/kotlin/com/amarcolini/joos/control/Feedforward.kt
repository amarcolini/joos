package com.amarcolini.joos.control

import kotlin.js.JsName

/**
 * Feedforward model for basic velocity/acceleration control.
 */
abstract class Feedforward {
    /**
     * Computes the feedforward output for the desired velocities and accelerations.
     */
    @JsName("calculateMultiple")
    fun calculate(
        vels: List<Double>,
        accels: List<Double>
    ) =
        vels.zip(accels)
            .map { (vel, accel) -> calculate(vel, accel) }

    /**
     * Computes the feedforward output for the desired velocity and acceleration
     * on top of the given base output.
     */
    abstract fun calculate(vel: Double, accel: Double, base: Double): Double

    /**
     * Computes the feedforward output for the desired velocity and acceleration.
     */
    fun calculate(vel: Double, accel: Double) = calculate(vel, accel, 0.0)
}