package com.amarcolini.joos.util

import com.amarcolini.joos.geometry.Vector2d
import kotlin.math.PI

/**
 * Various utilities for working with angles.
 */
object Angle {
    private const val TAU = PI * 2

    /**
     * Returns [angle] clamped to `[0, 2pi]` if using radians, or `[0, 360]` if using degrees.
     *
     * @param angle angle measure in radians or degrees
     * @param inDegrees whether [angle] is in degrees or radians
     */
    @JvmStatic
    @JvmOverloads
    fun norm(angle: Double, inDegrees: Boolean = false): Double {
        val max = if (inDegrees) 360.0 else TAU
        var modifiedAngle = angle % max

        modifiedAngle = (modifiedAngle + max) % max

        return modifiedAngle
    }

    /**
     * Returns [angleDelta] clamped to `[-pi, pi]` if using radians, or `[-180, 180]` if using degrees.
     *
     * @param angleDelta angle delta in radians or degrees
     * @param inDegrees whether [angleDelta] is in degrees or radians
     */
    @JvmStatic
    @JvmOverloads
    fun normDelta(angleDelta: Double, inDegrees: Boolean = false): Double {
        var modifiedAngleDelta = norm(angleDelta, inDegrees)
        val max = if (inDegrees) 360.0 else TAU

        if (modifiedAngleDelta > max / 2) {
            modifiedAngleDelta -= max
        }

        return modifiedAngleDelta
    }

    /**
     * Returns a normalized vector representation of [angle].
     *
     * @param angle angle measure in radians or degrees
     * @param inDegrees whether [angle] is in degrees or radians
     */
    @JvmStatic
    @JvmOverloads
    fun vec(angle: Double, inDegrees: Boolean = false): Vector2d =
        Vector2d.polar(1.0, norm(angle, inDegrees))
}
