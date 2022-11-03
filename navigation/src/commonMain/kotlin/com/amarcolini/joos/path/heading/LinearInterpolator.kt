package com.amarcolini.joos.path.heading

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.times

/**
 * Linear heading interpolator for time-optimal transitions between poses.
 *
 * @param startHeading start heading
 * @param angle angle to sweep through (can be greater than a revolution)
 */
class LinearInterpolator(private val startHeading: Angle, private val angle: Angle) : HeadingInterpolator() {
    override fun internalGet(s: Double, t: Double) =
        (startHeading + s / curve.length() * angle).norm()

    override fun internalDeriv(s: Double, t: Double): Angle = angle / curve.length()

    override fun internalSecondDeriv(s: Double, t: Double): Angle = Angle()
}