package com.amarcolini.joos.geometry

import com.amarcolini.joos.util.*
import net.sergeych.sprintf.format
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Class for representing 2D robot poses (x, y, and heading) and their derivatives.
 */
data class Pose2d @JvmOverloads constructor(
    @JvmField var x: Double = 0.0,
    @JvmField var y: Double = 0.0,
    @JvmField var heading: Angle = Angle()
) {
    /**
     * Constructs a pose where [heading] is in [Angle.defaultUnits].
     */
    constructor(x: Double, y: Double, heading: Double) : this(x, y, Angle(heading))

    @JvmOverloads
    constructor(pos: Vector2d, heading: Angle = Angle()) : this(pos.x, pos.y, heading)

    /**
     * Constructs a pose where [heading] is in [Angle.defaultUnits].
     */
    constructor(pos: Vector2d, heading: Double) : this(pos,
        Angle(heading)
    )

    /**
     * Returns this pose as a vector (i.e., without heading).
     */
    fun vec(): Vector2d = Vector2d(x, y)

    /**
     * Returns the vector representation of this pose's heading.
     */
    fun headingVec(): Vector2d = heading.vec()

    /**
     * Adds two poses.
     */
    operator fun plus(other: Pose2d): Pose2d =
        Pose2d(x + other.x, y + other.y, heading + other.heading)

    /**
     * Subtracts two poses.
     */
    operator fun minus(other: Pose2d): Pose2d =
        Pose2d(x - other.x, y - other.y, heading - other.heading)

    /**
     * Multiplies this pose by a scalar.
     */
    operator fun times(scalar: Double): Pose2d =
        Pose2d(scalar * x, scalar * y, scalar * heading)

    /**
     * Divides this pose by a scalar.
     */
    operator fun div(scalar: Double): Pose2d =
        Pose2d(x / scalar, y / scalar, heading / scalar)

    /**
     * Returns the negative of this pose.
     */
    operator fun unaryMinus(): Pose2d =
        Pose2d(-x, -y, -heading)

    /**
     * Returns whether two poses are approximately equal (within [EPSILON]).
     */
    infix fun epsilonEquals(other: Pose2d): Boolean =
        x epsilonEquals other.x && y epsilonEquals other.y && heading strictEpsilonEquals other.heading

    /**
     * Returns whether two poses are approximately equal (within [EPSILON]).
     */
    infix fun epsilonEqualsHeading(other: Pose2d): Boolean =
        x epsilonEquals other.x && y epsilonEquals other.y && heading epsilonEquals other.heading

    override fun toString(): String = "(%.3f, %.3f, $heading)".format(x, y)
}