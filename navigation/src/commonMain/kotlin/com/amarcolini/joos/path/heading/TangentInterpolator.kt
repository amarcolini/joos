package com.amarcolini.joos.path.heading

import com.amarcolini.joos.geometry.Angle
import kotlin.jvm.JvmOverloads

/**
 * Tangent (system) interpolator for tank/differential and other nonholonomic drives.
 *
 * @param offset tangent heading offset
 */
class TangentInterpolator @JvmOverloads constructor(
    val offset: Angle = Angle()
) : HeadingInterpolator() {
    /**
     * Constructs a [TangentInterpolator] where [offset] is in degrees or radians as specified by [Angle.defaultUnits].
     */
    constructor(offset: Double) : this(Angle(offset))

    override fun internalGet(s: Double, t: Double): Angle = (offset + curve.tangentAngle(s, t)).norm()

    override fun internalDeriv(s: Double, t: Double): Angle = curve.tangentAngleDeriv(s, t)

    override fun internalSecondDeriv(s: Double, t: Double): Angle = curve.tangentAngleSecondDeriv(s, t)
}