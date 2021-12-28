package com.amarcolini.joos.geometry

import com.amarcolini.joos.util.Angle
import com.amarcolini.joos.util.epsilonEquals
import kotlin.math.cos
import kotlin.math.sin

/**
 * Class for representing 2D robot poses (x, y, and heading) and their derivatives.
 */
data class Pose2d @JvmOverloads constructor(
    @JvmField val x: Double = 0.0,
    @JvmField val y: Double = 0.0,
    @JvmField val heading: Double = 0.0
) {
    constructor(pos: Vector2d, heading: Double = 0.0) : this(pos.x, pos.y, heading)

    internal companion object {
        /**
         * Constructs a pose, but heading is represented in degrees.
         */
        @JvmStatic
        @JvmOverloads
        fun of(x: Double = 0.0, y: Double = 0.0, heading: Double = 0.0) =
            Pose2d(x, y, Math.toRadians(heading))
    }

    /**
     * Returns this pose as a vector (e.g., without heading).
     */
    fun vec() = Vector2d(x, y)

    /**
     * Returns the vector representation of this pose's heading.
     */
    fun headingVec() = Vector2d(cos(heading), sin(heading))

    /**
     * Adds two poses.
     */
    operator fun plus(other: Pose2d) =
        Pose2d(x + other.x, y + other.y, heading + other.heading)

    /**
     * Subtracts two poses.
     */
    operator fun minus(other: Pose2d) =
        Pose2d(x - other.x, y - other.y, heading - other.heading)

    /**
     * Multiplies two poses.
     */
    operator fun times(scalar: Double) =
        Pose2d(scalar * x, scalar * y, scalar * heading)

    /**
     * Divides two poses.
     */
    operator fun div(scalar: Double) =
        Pose2d(x / scalar, y / scalar, heading / scalar)

    /**
     * Returns the negative of this pose.
     */
    operator fun unaryMinus() = Pose2d(-x, -y, -heading)

    infix fun epsilonEquals(other: Pose2d) =
        x epsilonEquals other.x && y epsilonEquals other.y && heading epsilonEquals other.heading

    infix fun epsilonEqualsHeading(other: Pose2d) =
        x epsilonEquals other.x && y epsilonEquals other.y && Angle.normDelta(heading - other.heading) epsilonEquals 0.0

    override fun toString() =
        String.format("(%.3f, %.3f, %.3f°)", x, y, Math.toDegrees(heading))
}

operator fun Double.times(pose: Pose2d) = pose.times(this)

operator fun Double.div(pose: Pose2d) = pose.div(this)

/**
 * Converts this value from degrees to radians.
 */
val Number.rad: Double get() = Math.toRadians(this.toDouble())
