package com.amarcolini.joos.geometry

import com.amarcolini.joos.serialization.format
import com.amarcolini.joos.util.*
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.*

/**
 * A class representing different angle units.
 */
enum class AngleUnit {
    /**
     * A unit of measure for angles equivalent to 1/360 of a full rotation.
     */
    Degrees,

    /**
     * A unit of measure for angles equivalent to 1/2pi of a full rotation.
     */
    Radians
}

/**
 * Class for representing angles.
 */
//@JsonSerialize(using = AngleSerializer::class)
//@JsonDeserialize(using = AngleDeserializer::class)
class Angle @JvmOverloads constructor(
    @JvmField var value: Double = 0.0,
    @JvmField var units: AngleUnit = defaultUnits
) : Comparable<Angle> {

    companion object Static {
        private const val deg2Rad = PI / 180
        private const val rad2Deg = 180 / PI

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
    val degrees: Double
        @JvmName("degrees") get() = when (units) {
            AngleUnit.Degrees -> value
            AngleUnit.Radians -> value * rad2Deg
        }

    /**
     * The measure of this angle in radians.
     */
    val radians: Double
        @JvmName("radians") get() = when (units) {
            AngleUnit.Degrees -> value * deg2Rad
            AngleUnit.Radians -> value
        }

    private fun getValue(units: AngleUnit) = when (units) {
        AngleUnit.Degrees -> degrees
        AngleUnit.Radians -> radians
    }

    /**
     * The measure of this angle in [defaultUnits].
     */
    val defaultValue: Double
        @JvmName("defaultValue") get() = getValue(defaultUnits)

    /**
     * Returns this angle clamped to `[0, 2pi]` in radians, or `[0, 360]` in degrees.
     */
    fun norm(): Angle =
        Angle(value.wrap(0.0, 360.deg.getValue(units)), units)

    /**
     * Returns this angle clamped to `[-pi, pi]` in radians, or `[-180, 180]` in degrees.
     */
    fun normDelta(): Angle =
        Angle(value.wrap(-180.deg.getValue(units), 180.deg.getValue(units)), units)

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
    fun abs(): Angle = Angle(abs(value), units)

    /**
     * Ensures that this angle lies in the specified range [min]..[max].
     */
    fun coerceIn(min: Angle, max: Angle): Angle =
        Angle(value.coerceIn(min.getValue(units), max.getValue(units)), units)

    /**
     * Ensures that this angle lies in the specified range [min]..[max], where [min] and [max] are in [defaultUnits].
     */
    fun coerceIn(min: Double, max: Double): Angle = coerceIn(
        Angle(min),
        Angle(max)
    )

    /**
     * Returns the shortest angle that can be added to this angle to get [other]
     * (e.g., 10° angleTo 360° = -10°).
     */
    infix fun angleTo(other: Angle): Angle =
        Angle(
            (other.value - getValue(other.units) + 180.deg.getValue(other.units)).mod(360.deg.getValue(other.units)) - 180.deg.getValue(
                other.units
            ), other.units
        )

    /**
     * Adds two angles.
     */
    operator fun plus(other: Angle): Angle =
        Angle(getValue(other.units) + other.value, other.units)

    /**
     * Adds two angles, where [other] is in [defaultUnits].
     */
    operator fun plus(other: Double): Angle = this + Angle(other)

    /**
     * Subtracts two angles.
     */
    operator fun minus(other: Angle): Angle =
        Angle(getValue(other.units) - other.value, other.units)

    /**
     * Subtracts two angles, where [other] is in [defaultUnits].
     */
    operator fun minus(other: Double): Angle = this - Angle(
        other
    )

    /**
     * Multiplies this angle by a scalar.
     */
    operator fun times(scalar: Double): Angle =
        Angle(value * scalar, units)

    /**
     * Divides this angle by a scalar.
     */
    operator fun div(scalar: Double): Angle =
        Angle(value / scalar, units)

    /**
     * Divides two angles.
     */
    operator fun div(other: Angle): Double = getValue(other.units) / other.value

    /**
     * Returns the negative of this angle.
     */
    operator fun unaryMinus(): Angle = Angle(-value, units)

    /**
     * Returns whether two angles are approximately equal (within [EPSILON]).
     */
    infix fun strictEpsilonEquals(other: Angle): Boolean =
        getValue(other.units) epsilonEquals other.value

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
        norm().getValue(other.units) epsilonEquals other.norm().value

    /**
     * Returns whether two angles are approximately equal (within [EPSILON]), but angles which
     * point in the same direction are considered equal as well (e.g., 0° = 360° = 720°).
     * [other] is in degrees or radians as specified by [defaultUnits].
     */
    infix fun epsilonEquals(other: Double): Boolean = this epsilonEquals Angle(other)

    override fun toString(): String = when (defaultUnits) {
        AngleUnit.Degrees -> "${degrees.format(3)}°"
        AngleUnit.Radians -> radians.format(3)
    }

    /**
     * Returns 1 if this angle is greater than [other], 0 if they are equal, and -1 if this angle
     * is less than [other].
     */
    override operator fun compareTo(other: Angle): Int {
        return if (this == other) 0
        else if (this.getValue(other.units) > other.value) 1 else -1
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Angle) return false
        if (this === other) return true
        return this.getValue(other.units) == other.value
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + units.hashCode()
        return result
    }
}