package com.amarcolini.joos.path.heading

import com.amarcolini.joos.geometry.Angle

/**
 * Constant heading interpolator used for arbitrary holonomic translations.
 *
 * @param heading heading to maintain
 */
class ConstantInterpolator(val heading: Angle) : HeadingInterpolator() {
    override fun internalGet(s: Double, t: Double): Angle = heading.norm()

    override fun internalDeriv(s: Double, t: Double) = Angle()

    override fun internalSecondDeriv(s: Double, t: Double) = Angle()
}