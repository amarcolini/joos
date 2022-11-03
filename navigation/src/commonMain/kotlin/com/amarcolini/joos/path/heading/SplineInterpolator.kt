package com.amarcolini.joos.path.heading

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.path.ParametricCurve
import com.amarcolini.joos.path.QuinticPolynomial
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads

/**
 * Spline heading interpolator for transitioning smoothly between headings without violating continuity (and hence
 * allowing for integration into longer profiles).
 *
 * @param startHeading start heading
 * @param endHeading end heading
 * @param startHeadingDeriv start heading deriv (advanced)
 * @param startHeadingSecondDeriv start heading second deriv (advanced)
 * @param endHeadingDeriv start heading deriv (advanced)
 * @param endHeadingSecondDeriv start heading second deriv (advanced)
 */
// note: the spline parameter is transformed linearly into a pseudo-arclength parameter
class SplineInterpolator @JvmOverloads constructor(
    private val startHeading: Angle,
    private val endHeading: Angle,
    private val startHeadingDeriv: Angle? = null,
    private val startHeadingSecondDeriv: Angle? = null,
    private val endHeadingDeriv: Angle? = null,
    private val endHeadingSecondDeriv: Angle? = null
) : HeadingInterpolator() {
    private val tangentInterpolator = TangentInterpolator()
    private lateinit var headingSpline: QuinticPolynomial

    override fun init(curve: ParametricCurve) {
        super.init(curve)

        tangentInterpolator.init(this.curve)

        val len = curve.length()

        val headingDelta = (endHeading - startHeading).normDelta()

        headingSpline = QuinticPolynomial(
            0.0,
            ((startHeadingDeriv ?: curve.tangentAngleDeriv(0.0, 0.0)) * len).radians,
            ((startHeadingSecondDeriv ?: curve.tangentAngleSecondDeriv(0.0, 0.0)) * len * len).radians,
            headingDelta.radians,
            ((endHeadingDeriv ?: curve.tangentAngleDeriv(len, 1.0)) * len).radians,
            ((endHeadingSecondDeriv ?: curve.tangentAngleSecondDeriv(len, 1.0)) * len * len).radians
        )
    }

    override fun internalGet(s: Double, t: Double) =
        (startHeading + (headingSpline[s / curve.length()]).rad).norm()

    override fun internalDeriv(s: Double, t: Double): Angle {
        val len = curve.length()
        return (headingSpline.deriv(s / len) / len).rad
    }

    override fun internalSecondDeriv(s: Double, t: Double): Angle {
        val len = curve.length()
        return (headingSpline.secondDeriv(s / len) / (len * len)).rad
    }
}