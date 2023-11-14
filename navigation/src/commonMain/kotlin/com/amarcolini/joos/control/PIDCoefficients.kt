package com.amarcolini.joos.control

import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Proportional, integral, and derivative (PID) gains used by [PIDController].
 *
 * @param kP proportional gain
 * @param kI integral gain
 * @param kD derivative gain
 */
@JsExport
data class PIDCoefficients @JvmOverloads constructor(
    @JvmField var kP: Double = 0.0,
    @JvmField var kI: Double = 0.0,
    @JvmField var kD: Double = 0.0,
)