package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.heading.HeadingInterpolator
import com.amarcolini.joos.path.heading.TangentInterpolator
import kotlin.jvm.JvmOverloads

/**
 * Path segment composed of a parametric curve and heading interpolator.
 *
 * @param curve parametric curve
 * @param interpolator heading interpolator
 */
class PathSegment @JvmOverloads constructor(
    val curve: ParametricCurve,
    val interpolator: HeadingInterpolator = TangentInterpolator()
) {
    init {
        interpolator.init(curve)
    }

    fun length() = curve.length()

    @JvmOverloads
    operator fun get(s: Double, t: Double = reparam(s)): Pose2d =
        Pose2d(curve[s, t], interpolator[s, t])

    @JvmOverloads
    fun deriv(s: Double, t: Double = reparam(s)): Pose2d =
        Pose2d(curve.deriv(s, t), interpolator.deriv(s, t))

    @JvmOverloads
    fun secondDeriv(s: Double, t: Double = reparam(s)): Pose2d =
        Pose2d(curve.secondDeriv(s, t), interpolator.secondDeriv(s, t))

    @JvmOverloads
    fun tangentAngle(s: Double, t: Double = reparam(s)): Angle = curve.tangentAngle(s, t)

    @JvmOverloads
    internal fun internalDeriv(s: Double, t: Double = reparam(s)) =
        Pose2d(curve.internalDeriv(t), interpolator.internalDeriv(s, t))

    @JvmOverloads
    internal fun internalSecondDeriv(s: Double, t: Double = reparam(s)) =
        Pose2d(curve.internalSecondDeriv(t), interpolator.internalDeriv(s, t))

    @JvmOverloads
    fun curvature(s: Double, t: Double = reparam(s)): Double = curve.curvature(s, t)

    fun reparam(s: Double): Double = curve.reparam(s)

    /**
     * Returns the start pose.
     */
    fun start(): Pose2d = get(0.0)

    /**
     * Returns the start pose derivative.
     */
    fun startDeriv(): Pose2d = deriv(0.0)

    /**
     * Returns the start pose second derivative.
     */
    fun startSecondDeriv(): Pose2d = secondDeriv(0.0)

    /**
     * Returns the start tangent angle.
     */
    fun startTangentAngle(): Angle = tangentAngle(0.0)

    internal fun startInternalDeriv() = internalDeriv(0.0)

    internal fun startInternalSecondDeriv() = internalSecondDeriv(0.0)

    /**
     * Returns the end pose.
     */
    fun end(): Pose2d = get(length())

    /**
     * Returns the end pose derivative.
     */
    fun endDeriv(): Pose2d = deriv(length())

    /**
     * Returns the end pose second derivative.
     */
    fun endSecondDeriv(): Pose2d = secondDeriv(length())

    /**
     * Returns the end tangent angle.
     */
    fun endTangentAngle(): Angle = tangentAngle(length())

    internal fun endInternalDeriv() = internalDeriv(length())

    internal fun endInternalSecondDeriv() = internalSecondDeriv(length())
}