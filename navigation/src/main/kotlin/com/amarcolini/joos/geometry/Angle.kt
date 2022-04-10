package com.amarcolini.joos.geometry

import com.amarcolini.joos.trajectory.config.AngleDeserializer
import com.amarcolini.joos.trajectory.config.AngleSerializer
import com.amarcolini.joos.util.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * A class representing different angle units.
 */
enum class AngleUnit {
    Degrees,
    Radians
}

/**
 * Class for representing angles.
 */
@JsonSerialize(using = AngleSerializer::class)
@JsonDeserialize(using = AngleDeserializer::class)
data class Angle @JvmOverloads constructor(
    private val value: Double = 0.0,
    private val units: AngleUnit = defaultUnits
) {

    companion object Static {
        /**
         * The units [Angle] uses when no units are provided. [AngleUnit.Degrees] by default.
         */
        @JvmField
        var defaultUnits: AngleUnit = AngleUnit.Degrees

        /**
         * Constructs an angle from the specified value in degrees.
         */
        @JvmStatic
        fun deg(value: Double): Angle = value.deg

        /**
         * Constructs an angle from the specified value in radians.
         */
        @JvmStatic
        fun rad(value: Double): Angle = value.rad
    }

    /**
     * The measure of this angle in degrees.
     */
    val degrees: Double by lazy {
        when (units) {
            AngleUnit.Degrees -> value
            AngleUnit.Radians -> Math.toDegrees(value)
        }
    }
        @JvmName("degrees") get

    /**
     * The measure of this angle in radians.
     */
    val radians: Double by lazy {
        when (units) {
            AngleUnit.Degrees -> Math.toRadians(value)
            AngleUnit.Radians -> value
        }
    }
        @JvmName("radians") get

    /**
     * The measure of this angle in [defaultUnits].
     */
    val defaultValue: Double
        @JvmName("defaultValue") get() = when (defaultUnits) {
            AngleUnit.Degrees -> degrees
            AngleUnit.Radians -> radians
        }

    /**
     * Returns this angle clamped to `[0, 2pi]` in radians, or `[0, 360]` in degrees.
     */
    fun norm(): Angle = Angle(defaultValue.wrap(0.0, 360.0))

    /**
     * Returns this angle clamped to `[-pi, pi]` in radians, or `[-180, 180]` in degrees.
     */
    fun normDelta(): Angle = Angle(defaultValue.wrap(-180.0, 180.0))

    /**
     * Returns the vector representation of this angle.
     */
    fun vec(): Vector2d = Vector2d.polar(1.0, norm())

    /**
     * Returns the cosine of this angle.
     */
    fun cos(): Double = cos(radians)

    /**
     * Returns the sine of this angle.
     */
    fun sin(): Double = sin(radians)

    /**
     * Returns the tangent of this angle.
     */
    fun tan(): Double = tan(radians)

    /**
     * Returns the absolute value of this angle.
     */
    fun abs(): Angle = Angle(abs(defaultValue))

    /**
     * Ensures that this angle lies in the specified range [min]..[max].
     */
    fun coerceIn(min: Angle, max: Angle): Angle = Angle(defaultValue.coerceIn(min.defaultValue, max.defaultValue))

    /**
     * Ensures that this angle lies in the specified range [min]..[max], where [min] and [max] are in [defaultUnits].
     */
    fun coerceIn(min: Double, max: Double): Angle = coerceIn(Angle(min), Angle(max))

    /**
     * Returns the shortest angle that can be added to this angle to get [other]
     * (e.g., 10° angleTo 360° = -10°).
     */
    infix fun angleTo(other: Angle): Angle = ((other.degrees - degrees + 180).mod(360.0) - 180).deg

    /**
     * Adds two angles.
     */
    operator fun plus(other: Angle): Angle = (radians + other.radians).rad

    /**
     * Adds two angles, where [other] is in [defaultUnits].
     */
    operator fun plus(other: Double): Angle = this + Angle(other)

    /**
     * Subtracts two angles.
     */
    operator fun minus(other: Angle): Angle = (radians - other.radians).rad

    /**
     * Subtracts two angles, where [other] is in [defaultUnits].
     */
    operator fun minus(other: Double): Angle = this - Angle(other)

    /**
     * Multiplies this angle by a scalar.
     */
    operator fun times(scalar: Double): Angle = (radians * scalar).rad

    /**
     * Divides this angle by a scalar.
     */
    operator fun div(scalar: Double): Angle = (radians / scalar).rad

    /**
     * Divides two angles.
     */
    operator fun div(other: Angle): Double = radians / other.radians

    /**
     * Returns the negative of this angle.
     */
    operator fun unaryMinus(): Angle = (-degrees).deg

    /**
     * Returns whether two angles are approximately equal (within [EPSILON]).
     */
    infix fun strictEpsilonEquals(other: Angle): Boolean =
        radians epsilonEquals other.radians

    /**
     * Returns whether two angles are approximately equal (within [EPSILON]). [other]
     * is in degrees or radians as specified by [defaultUnits].
     */
    infix fun strictEpsilonEquals(other: Double): Boolean =
        this strictEpsilonEquals Angle(other)

    /**
     * Returns whether two angles are approximately equal (within [EPSILON]), but angles which
     * point in the same direction are considered equal as well (e.g., 0° = 360° = 720°).
     */
    infix fun epsilonEquals(other: Angle): Boolean =
        norm().radians epsilonEquals other.norm().radians

    /**
     * Returns whether two angles are approximately equal (within [EPSILON]), but angles which
     * point in the same direction are considered equal as well (e.g., 0° = 360° = 720°).
     * [other] is in degrees or radians as specified by [defaultUnits].
     */
    infix fun epsilonEquals(other: Double): Boolean = this epsilonEquals Angle(other)

    override fun toString(): String =
        when (defaultUnits) {
            AngleUnit.Degrees -> String.format("%.3f°", degrees)
            AngleUnit.Radians -> String.format("%.3f", radians)
        }

    /**
     * Returns 1 if this angle is greater than [other], 0 if they are equal, and -1 if this angle
     * is less than [other].
     */
    operator fun compareTo(other: Angle): Int {
        return if (this == other) 0
        else if (this.degrees > other.degrees) 1 else -1
    }
}