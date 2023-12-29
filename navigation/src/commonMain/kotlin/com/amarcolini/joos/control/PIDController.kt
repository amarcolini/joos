package com.amarcolini.joos.control

import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.wrap
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.max

/**
 * PID controller.
 *
 * @param pid traditional PID coefficients
 * @param clock clock
 */
@JsExport
class PIDController @JvmOverloads constructor(
    pid: PIDCoefficients,
    private val clock: NanoClock = NanoClock.system
) {
    var pid: PIDCoefficients = pid
        set(value) {
            if (field != value) reset()
            field = value
        }

    private var errorSum: Double = 0.0
    private var isIntegrating: Boolean = true
    private var lastUpdateTimestamp: Double = Double.NaN

    var inputBounded: Boolean = false
    private var minInput: Double = 0.0
    private var maxInput: Double = 0.0

    var outputBounded: Boolean = false
    private var minOutput: Double = 0.0
    private var maxOutput: Double = 0.0

    /**
     * Target position (that is, the controller setpoint).
     */
    var targetPosition: Double = 0.0

    /**
     * Target velocity.
     */
    var targetVelocity: Double = 0.0

    /**
     * Error computed in the last call to [update].
     */
    var lastError: Double = 0.0
        private set

    /**
     * The position error considered tolerable for [isAtSetPoint] to return true.
     */
    var tolerance: Double = 0.05

    /**
     * Returns whether the controller is at the target position with an error within [tolerance].
     */
    fun isAtSetPoint() = abs(lastError) <= tolerance

    /**
     * Sets the target position, velocity, and acceleration.
     */
    @JvmOverloads
    fun setTarget(
        targetPosition: Double,
        targetVelocity: Double = this.targetVelocity
    ) {
        this.targetPosition = targetPosition
        this.targetVelocity = targetVelocity
    }

    /**
     * Sets bound on the input of the controller. The min and max values are considered modularly-equivalent (that is,
     * the input wraps around).
     *
     * @param min minimum input
     * @param max maximum input
     */
    fun setInputBounds(min: Double, max: Double) {
        if (min < max) {
            inputBounded = true
            minInput = min
            maxInput = max
        }
    }

    /**
     * Sets bounds on the output of the controller.
     *
     * @param min minimum output
     * @param max maximum output
     */
    fun setOutputBounds(min: Double, max: Double) {
        if (min < max) {
            outputBounded = true
            minOutput = min
            maxOutput = max
        }
    }

    private fun getPositionError(measuredPosition: Double): Double {
        var error = targetPosition - measuredPosition
        if (inputBounded) {
            error = error.wrap(minInput, maxInput)
        }
        return error
    }

    /**
     * Run a single iteration of the controller.
     *
     * @param measuredPosition measured position (feedback)
     * @param measuredVelocity measured velocity
     */
    @JvmOverloads
    fun update(
        measuredPosition: Double,
        measuredVelocity: Double? = null
    ): Double {
        val currentTimestamp = clock.seconds()
        val error = getPositionError(measuredPosition)
        return if (lastUpdateTimestamp.isNaN()) {
            lastError = error
            lastUpdateTimestamp = currentTimestamp
            0.0
        } else {
            val dt = currentTimestamp - lastUpdateTimestamp
            if (isIntegrating) {
                val newError = 0.5 * (error + lastError) * dt
                errorSum += newError
            }
            val errorDeriv =
                measuredVelocity?.let { targetVelocity - it } ?: ((error - lastError) / dt)

            lastError = error
            lastUpdateTimestamp = currentTimestamp

            val output = pid.kP * error + pid.kI * errorSum +
                    pid.kD * errorDeriv

            if (outputBounded) {
                val clamped = output.coerceIn(minOutput, maxOutput)
                if (isIntegrating && output != clamped && output - pid.kI * errorSum in minOutput..maxOutput) {
                    /*
                     If the controller output is saturated and the integral term is the reason, then we reduce the
                     integral term and stop integrating to prevent windup.
                     */
                    errorSum -= (output - clamped) / pid.kI
                }
                isIntegrating = output == clamped
                clamped
            } else {
                isIntegrating = true
                output
            }
        }
    }

    /**
     * Reset the controller's integral sum.
     */
    fun reset() {
        errorSum = 0.0
        isIntegrating = true
        lastError = 0.0
        lastUpdateTimestamp = Double.NaN
    }
}