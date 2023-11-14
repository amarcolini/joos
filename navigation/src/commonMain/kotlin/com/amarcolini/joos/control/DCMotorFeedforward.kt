package com.amarcolini.joos.control

import com.amarcolini.joos.util.epsilonEquals
import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.sign

/**
 * Feedforward gains for DC motor velocity control.
 * @param kV velocity gain
 * @param kA acceleration gain
 * @param kStatic additive constant
 */
@JsExport
data class DCMotorFeedforward @JvmOverloads constructor(
    @JvmField var kV: Double = 0.0,
    @JvmField var kA: Double = 0.0,
    @JvmField var kStatic: Double = 0.0
) : Feedforward() {
    /**
     * Computes the motor feedforward (i.e., open loop power) for the given set of coefficients
     * on top of the given base output.
     */
    override fun calculate(
        vel: Double,
        accel: Double,
        base: Double
    ): Double {
        val basePower = vel * kV + accel * kA + base
        return if (basePower epsilonEquals 0.0) 0.0
        else basePower + sign(basePower) * kStatic
    }
}