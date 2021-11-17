package com.amarcolini.joos.util

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Various math utilities.
 */
object MathUtil {

    /**
     * Returns the real solutions to the quadratic ax^2 + bx + c.
     */
    @JvmStatic
    fun solveQuadratic(a: Double, b: Double, c: Double): List<Double> {
        val disc = b * b - 4 * a * c
        return when {
            disc epsilonEquals 0.0 -> listOf(-b / (2 * a))
            disc > 0.0 -> listOf(
                (-b + sqrt(disc)) / (2 * a),
                (-b - sqrt(disc)) / (2 * a)
            )
            else -> emptyList()
        }
    }

    /**
     * Ensures that [n] lies in the range [min]..[max],
     * where [min] and [max] are modularly-equivalent (that is, [n] wraps around).
     */
    @JvmStatic
    fun wrap(n: Double, min: Double, max: Double): Double =
        if (n < min) max - (min - n) % (max - min)
        else min + (n - min) % (max - min)

    /**
     * Ensures that [n] lies in the range [min]..[max],
     * where [min] and [max] are modularly-equivalent (that is, [n] wraps around).
     */
    @JvmStatic
    fun wrap(n: Int, min: Int, max: Int): Int =
        if (n < min) max - (min - n) % (max - min)
        else min + (n - min) % (max - min)
}

const val EPSILON = 1e-6

infix fun Double.epsilonEquals(other: Double) = abs(this - other) < EPSILON
fun Double.wrap(min: Double, max: Double) = MathUtil.wrap(this, min, max)
fun Int.wrap(min: Int, max: Int) = MathUtil.wrap(this, min, max)
