package com.amarcolini.joos.path.heading

import com.amarcolini.joos.util.Angle

/**
 * Tangent (system) interpolator for tank/differential and other nonholonomic drives.
 *
 * @param offset tangent heading offset
 */
class TangentInterpolator @JvmOverloads constructor(
    val offset: Double = 0.0
) : HeadingInterpolator() {
    override fun internalGet(s: Double, t: Double) = Angle.norm(offset + curve.tangentAngle(s, t))

    override fun internalDeriv(s: Double, t: Double) = curve.tangentAngleDeriv(s, t)

    override fun internalSecondDeriv(s: Double, t: Double) = curve.tangentAngleSecondDeriv(s, t)
}
