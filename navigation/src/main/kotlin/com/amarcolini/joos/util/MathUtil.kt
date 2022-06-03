package com.amarcolini.joos.util

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.sign
import kotlin.math.*

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

    /**
     * Ensures that [n] lies in the range [min]..[max].
     */
    @JvmStatic
    fun clamp(n: Double, min: Double, max: Double): Double = n.coerceIn(min, max)

    @JvmStatic
    fun cos(angle: Angle): Double = angle.cos()

    @JvmStatic
    fun sin(angle: Angle): Double = angle.sin()

    @JvmStatic
    fun tan(angle: Angle): Double = angle.tan()

    @JvmStatic
    fun abs(angle: Angle): Angle = angle.abs()

    @JvmStatic
    fun min(angle1: Angle, angle2: Angle): Angle = min(angle1.radians, angle2.radians).rad

    @JvmStatic
    fun max(angle1: Angle, angle2: Angle): Angle = max(angle1.radians, angle2.radians).rad

    @JvmStatic
    fun sign(angle: Angle): Double = sign(angle.radians)
}

fun cos(angle: Angle): Double = angle.cos()
fun sin(angle: Angle): Double = angle.sin()
fun tan(angle: Angle): Double = angle.tan()
fun abs(angle: Angle): Angle = angle.abs()
fun min(angle1: Angle, angle2: Angle): Angle = min(angle1.radians, angle2.radians).rad
fun max(angle1: Angle, angle2: Angle): Angle = max(angle1.radians, angle2.radians).rad
fun sign(angle: Angle): Double = sign(angle.radians)

/**
 * The accuracy with which `epsilonEquals` operates : `0.000001`.
 */
const val EPSILON = 1e-6

/**
 * Returns whether two doubles are approximately equal (within [EPSILON]).
 */
infix fun Double.epsilonEquals(other: Double): Boolean = abs(this - other) < EPSILON

/**
 * @see MathUtil.wrap
 */
fun Double.wrap(min: Double, max: Double): Double = MathUtil.wrap(this, min, max)

/**
 * @see MathUtil.wrap
 */
fun Int.wrap(min: Int, max: Int): Int = MathUtil.wrap(this, min, max)


/**
 * Multiplies [angle] by the provided scalar.
 */
operator fun Double.times(angle: Angle): Angle = angle * this

/**
 * Adds this double and [angle], where this double is in [Angle.defaultUnits].
 */
operator fun Double.plus(angle: Angle): Angle = angle + this

/**
 * Adds [angle] from this double, where this double is in [Angle.defaultUnits].
 */
operator fun Double.minus(angle: Angle): Angle = Angle(this) - angle

/**
 * Divides this double by [angle], where this double is in [Angle.defaultUnits].
 */
operator fun Double.div(angle: Angle): Double = Angle(this) / angle

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