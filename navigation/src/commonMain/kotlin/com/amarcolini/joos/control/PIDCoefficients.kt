package com.amarcolini.joos.control

import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Proportional, integral, and derivative (PID) gains used by [PIDFController].
 *
 * @param kP proportional gain
 * @param kI integral gain
 * @param kD derivative gain
 * @param N low-pass filter frequency cutoff for derivative, in radians per second
 */
data class PIDCoefficients @JvmOverloads constructor(
    @JvmField var kP: Double = 0.0,
    @JvmField var kI: Double = 0.0,
    @JvmField var kD: Double = 0.0,
    @JvmField var N: Double = 100.0
)