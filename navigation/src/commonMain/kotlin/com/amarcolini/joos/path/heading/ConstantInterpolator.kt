package com.amarcolini.joos.path.heading

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.rad

/**
 * Constant heading interpolator used for arbitrary holonomic translations.
 *
 * @param heading heading to maintain
 */
class ConstantInterpolator(val heading: Angle) : HeadingInterpolator() {
    override fun internalGet(s: Double, t: Double): Angle = heading.norm()

    override fun internalDeriv(s: Double, t: Double) = 0.rad

    override fun internalSecondDeriv(s: Double, t: Double) = 0.rad
}