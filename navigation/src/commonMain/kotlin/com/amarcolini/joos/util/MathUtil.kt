@file:JvmName("MathUtil")
@file:JvmMultifileClass

package com.amarcolini.joos.util

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic
import kotlin.math.*

/**
 * Returns the real solutions to the quadratic \(ax^2 + bx + c\).
 *
 * @usesMathJax
 */
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
fun wrap(n: Double, min: Double, max: Double): Double =
    if (n < min) max - (min - n) % (max - min)
    else min + (n - min) % (max - min)

/**
 * Ensures that [n] lies in the range [min]..[max],
 * where [min] and [max] are modularly-equivalent (that is, [n] wraps around).
 */
fun wrap(n: Int, min: Int, max: Int): Int =
    if (n < min) max - (min - n) % (max - min)
    else min + (n - min) % (max - min)

/**
 * Ensures that [n] lies in the range [min]..[max].
 */
fun clamp(n: Double, min: Double, max: Double): Double = n.coerceIn(min, max)

/**
 * Does a linear regression of the form `y = mx + b`.
 *
 * @return `m` and `b`
 */
fun doLinearRegression(x: List<Double>, y: List<Double>): Pair<Double, Double> {
    require(x.size == y.size) { "x and y lists must have the same size!" }
    val xsum = x.sum()
    val ysum = y.sum()
    val xysum = x.zip(y).sumOf { it.first * it.second }
    val x2sum = x.sumOf { it * it }
    val slope = (x.size * xysum - xsum * ysum) / (x.size * x2sum - xsum * xsum)
    val intercept = (ysum - slope * xsum) / x.size
    return slope to intercept
}

/**
 * Does a linear regression of the form `y = mx`.
 *
 * @return `m`
 */
fun doLinearRegressionNoIntercept(x: List<Double>, y: List<Double>): Double {
    val numerator = x.zip(y).sumOf { it.first * it.second }
    val denominator = x.sumOf { it * it }
    return numerator / denominator
}

/**
 * Generates [n] rows of Pascal's triangle.
 */
fun generatePascalsTriangle(n: Int): Array<IntArray> =
    Array(n) { index ->
        var c = 1
        IntArray(index + 1) { i ->
            val result = c
            c = c * (index - i) / (i + 1)
            result
        }
    }

/**
 * Given f(x) = [polynomial], computes f(x+1).
 */
internal fun translate(
    polynomial: DoubleArray,
    pascalsTriangle: Array<IntArray> = generatePascalsTriangle(polynomial.size)
): DoubleArray =
    DoubleArray(polynomial.size) { i ->
        var c = 0.0
        for (j in 0..i) {
            c += polynomial[j] * pascalsTriangle[polynomial.lastIndex - j][i - j]
        }
        c
    }

/**
 * Given f(x) = [polynomial], returns the number of sign variations in the coefficients of
 * (x+1)^n * f(1/(x+1)), where n is the degree of [polynomial].
 */
internal fun signVar(polynomial: DoubleArray, pascalsTriangle: Array<IntArray>): Int {
    var signVar = 0
    val last = polynomial.lastIndex
    var lastSign = sign(polynomial[last] * pascalsTriangle[last][0])
    for (i in 1..last) {
        var c = 0.0
        for (j in 0..i) {
            val k = last - j
            c += polynomial[k] * pascalsTriangle[k][i - j]
        }
        val sign = sign(c)
        if (sign != lastSign) signVar++
        lastSign = sign
    }
    return signVar
}

/**
 * Returns a list of intervals that each contain a root of [polynomial] that lies in [0, 1]. Uses
 * the Modified Uspensky algorithm described [here](https://en.wikipedia.org/wiki/Real-root_isolation#Pseudocode).
 *
 * @param polynomial A list of all the coefficients of the polynomial (including coefficients of 0),
 * starting from the leading coefficient.
 * @param pascalsTriangle A representation of Pascal's triangle generated using [generatePascalsTriangle]. It
 * is recommended to compute this once and pass it to each consecutive function call to improve performance.
 */
fun isolateRoots(
    polynomial: Polynomial,
    pascalsTriangle: Array<IntArray> = generatePascalsTriangle(polynomial.coeffs.size)
): List<ClosedFloatingPointRange<Double>> {
    var n = polynomial.coeffs.size
    val l = arrayListOf(Triple(0, 0, polynomial.coeffs.copyOf()))
    val isol = ArrayList<Triple<Int, Int, Int>>()
    while (l.isNotEmpty()) {
        var (c, k, q) = l.removeAt(0)
        if (q[q.lastIndex] == 0.0) {
            q = q.copyOfRange(0, q.size - 2)
            n--
            isol += Triple(c, k, 0)
        }
        val v = signVar(q, pascalsTriangle)
        if (v == 1) isol += Triple(c, k, 1)
        if (v > 1) {
            for (i in q.indices) {
                q[i] *= (1 shl i).toDouble()
            }
            l += Triple(2 * c, k + 1, q)
            l += Triple(2 * c + 1, k + 1, translate(q, pascalsTriangle))
        }
    }
    return isol.map { (c, k, h) ->
        val denom = (1 shl k).toDouble()
        c / denom..(c + h) / denom
    }
}

fun cos(angle: Angle): Double = angle.cos()
fun sin(angle: Angle): Double = angle.sin()
fun tan(angle: Angle): Double = angle.tan()
fun abs(angle: Angle): Angle = angle.abs()
fun min(a: Angle, b: Angle): Angle = Angle(min(a.getValue(b.units), b.value), b.units)
fun max(a: Angle, b: Angle): Angle = Angle(max(a.getValue(b.units), b.value), b.units)
fun sign(angle: Angle): Double = sign(angle.value)

/**
 * The accuracy with which `epsilonEquals` operates : `0.000001`.
 */
const val EPSILON = 1e-6

/**
 * Returns whether two doubles are approximately equal (within [EPSILON]).
 */
infix fun Double.epsilonEquals(other: Double): Boolean = abs(this - other) < EPSILON

/**
 * @see wrap
 */
@JvmName("wrap1")
@JvmSynthetic
fun Double.wrap(min: Double, max: Double): Double = wrap(this, min, max)

/**
 * @see wrap
 */
@JvmName("wrap1")
@JvmSynthetic
fun Double.wrap(bounds: Pair<Double, Double>): Double = wrap(this, bounds.first, bounds.second)

/**
 * @see wrap
 */
@JvmName("wrap1")
@JvmSynthetic
fun Int.wrap(min: Int, max: Int): Int = wrap(this, min, max)


/**
 * Multiplies [angle] by the provided scalar.
 */
operator fun Double.times(angle: Angle): Angle = angle * this

/**
 * Creates an [Angle] from the specified value in radians.
 */
val Number.rad: Angle get() = Angle(this.toDouble(), AngleUnit.Radians)

/**
 * Creates an [Angle] from the specified value in degrees.
 */
val Number.deg: Angle get() = Angle(this.toDouble(), AngleUnit.Degrees)

/**
 * Multiplies [pose] by the provided scalar.
 */
operator fun Double.times(pose: Pose2d): Pose2d = pose.times(this)

/**
 * Multiplies [vector] by the provided scalar.
 */
operator fun Double.times(vector: Vector2d): Vector2d = vector.times(this)