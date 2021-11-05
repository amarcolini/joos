package com.amarcolini.joos.util

import com.amarcolini.joos.geometry.Vector2d
import kotlin.math.PI

/**
 * Various utilities for working with angles.
 */
object Angle {
    private const val TAU = PI * 2

    /**
     * Returns [angle] clamped to `[0, 2pi]`.
     *
     * @param angle angle measure in radians
     */
    @JvmStatic
    fun norm(angle: Double): Double {
        var modifiedAngle = angle % TAU

        modifiedAngle = (modifiedAngle + TAU) % TAU

        return modifiedAngle
    }

    /**
     * Returns [angleDelta] clamped to `[-pi, pi]`.
     *
     * @param angleDelta angle delta in radians
     */
    @JvmStatic
    fun normDelta(angleDelta: Double): Double {
        var modifiedAngleDelta = norm(angleDelta)

        if (modifiedAngleDelta > PI) {
            modifiedAngleDelta -= TAU
        }

        return modifiedAngleDelta
    }

    /**
     * Returns a normalized vector representation of [angle].
     *
     * @param angle angle measure in radians
     */
    @JvmStatic
    fun vec(angle: Double): Vector2d = Vector2d.polar(1.0, norm(angle))
}
