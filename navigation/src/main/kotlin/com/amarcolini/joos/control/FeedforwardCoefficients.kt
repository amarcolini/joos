package com.amarcolini.joos.control

import com.amarcolini.joos.util.epsilonEquals
import kotlin.math.sign

/**
 * Feedforward gains for DC motor velocity control.
 * @param kV velocity gain
 * @param kA acceleration gain
 * @param kStatic additive constant
 */
data class FeedforwardCoefficients @JvmOverloads constructor(
    @JvmField var kV: Double = 0.0,
    @JvmField var kA: Double = 0.0,
    @JvmField var kStatic: Double = 0.0
) {
    /**
     * Computes the motor feedforward (i.e., open loop powers) for the given set of coefficients.
     */
    fun calculate(
        vels: List<Double>,
        accels: List<Double>
    ) =
        vels.zip(accels)
            .map { (vel, accel) -> calculate(vel, accel) }

    /**
     * Computes the motor feedforward (i.e., open loop power) for the given set of coefficients
     * on top of the given base output.
     */
    @JvmOverloads
    fun calculate(
        vel: Double,
        accel: Double,
        base: Double = 0.0
    ): Double {
        val basePower = vel * kV + accel * kA + base
        return if (basePower epsilonEquals 0.0) 0.0
        else basePower + sign(basePower) * kStatic
    }
}