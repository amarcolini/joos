package com.amarcolini.joos.path.heading

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads

/**
 * Tangent (system) interpolator for tank/differential and other nonholonomic drives.
 *
 * @param offset tangent heading offset
 */
class TangentInterpolator @JvmOverloads constructor(
    val offset: Angle = 0.rad
) : HeadingInterpolator() {
    override fun internalGet(s: Double, t: Double): Angle = (offset + curve.tangentAngle(s, t)).norm()

    override fun internalDeriv(s: Double, t: Double): Angle = curve.tangentAngleDeriv(s, t)

    override fun internalSecondDeriv(s: Double, t: Double): Angle = curve.tangentAngleSecondDeriv(s, t)
}