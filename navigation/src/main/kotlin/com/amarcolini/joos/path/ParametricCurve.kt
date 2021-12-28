package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Vector2d
import org.apache.commons.math3.util.FastMath
import kotlin.math.abs

/**
 * Parametric curve with two components (x and y). These curves are reparameterized from an internal parameter (t) to
 * the arc length parameter (s).
 */
abstract class ParametricCurve {
    /**
     * Returns the vector [s] units along the curve.
     */
    @JvmOverloads
    operator fun get(s: Double, t: Double = reparam(s)): Vector2d = internalGet(t)

    /**
     * Returns the derivative [s] units along the curve.
     */
    @JvmOverloads
    fun deriv(s: Double, t: Double = reparam(s)): Vector2d {
        val deriv = internalDeriv(t)
        return deriv / deriv.norm()
    }

    /**
     * Returns the second derivative [s] units along the curve.
     */
    @JvmOverloads
    fun secondDeriv(s: Double, t: Double = reparam(s)): Vector2d {
        val deriv = internalDeriv(t)
        val secondDeriv = internalSecondDeriv(t)

        return ((secondDeriv * (deriv dot deriv)) - (deriv * (secondDeriv dot deriv))) /
                FastMath.pow(deriv.norm(), 4)
    }

    /**
     * Returns the third derivative [s] units along the curve.
     */
    @JvmOverloads
    fun thirdDeriv(s: Double, t: Double = reparam(s)): Vector2d {
        val deriv = internalDeriv(t)
        val secondDeriv = internalSecondDeriv(t)
        val thirdDeriv = internalThirdDeriv(t)

        val pt1 = (thirdDeriv * (deriv dot deriv)) - (deriv * (deriv dot thirdDeriv))
        val pt2 = (secondDeriv * (secondDeriv dot deriv)) - (deriv * (secondDeriv dot secondDeriv))
        return (pt1 + pt2) / FastMath.pow(deriv.norm(), 9)
    }

    /**
     * Returns the start vector.
     */
    fun start() = get(0.0, 0.0)

    /**
     * Returns the start vector.
     */
    fun startDeriv() = deriv(0.0, 0.0)

    /**
     * Returns the start second derivative.
     */
    fun startSecondDeriv() = secondDeriv(0.0, 0.0)

    /**
     * Returns the start third derivative.
     */
    fun startThirdDeriv() = thirdDeriv(0.0, 0.0)

    /**
     * Returns the end vector.
     */
    fun end() = get(length(), 1.0)

    /**
     * Returns the end derivative.
     */
    fun endDeriv() = deriv(length(), 1.0)

    /**
     * Returns the end second derivative.
     */
    fun endSecondDeriv() = secondDeriv(length(), 1.0)

    /**
     * Returns the end third derivative.
     */
    fun endThirdDeriv() = thirdDeriv(length(), 1.0)

    /**
     * Returns the angle of the tangent line [s] units along the curve.
     */
    @JvmOverloads
    fun tangentAngle(s: Double, t: Double = reparam(s)) = deriv(s, t).angle()

    /**
     * Returns the derivative of the tangent angle [s] units along the curve.
     */
    @JvmOverloads
    fun tangentAngleDeriv(s: Double, t: Double = reparam(s)): Double {
        val deriv = deriv(s, t)
        val secondDeriv = secondDeriv(s, t)
        return deriv.x * secondDeriv.y - deriv.y * secondDeriv.x
    }

    /**
     * Returns the second derivative of the tangent angle [s] units along the curve.
     */
    @JvmOverloads
    fun tangentAngleSecondDeriv(s: Double, t: Double = reparam(s)): Double {
        val deriv = deriv(s, t)
        val thirdDeriv = thirdDeriv(s, t)
        return deriv.x * thirdDeriv.y - deriv.y * thirdDeriv.x
    }

    /**
     * Returns the length of the curve.
     */
    abstract fun length(): Double

    internal abstract fun reparam(s: Double): Double

    internal abstract fun internalGet(t: Double): Vector2d
    internal abstract fun internalDeriv(t: Double): Vector2d
    internal abstract fun internalSecondDeriv(t: Double): Vector2d
    internal abstract fun internalThirdDeriv(t: Double): Vector2d

    /**
     * Computes the curvature of a parametric curve at the internal parameter [t].
     */
    @JvmOverloads
    fun curvature(s: Double, t: Double = reparam(s)): Double = secondDeriv(s, t).norm()

    private var length: Double = 0.0
    private val tSamples = mutableListOf(0.0)
    private val sSamples = mutableListOf(0.0)

    /**
     * Computes internal parameter vs curve length samples and estimates curve length.
     * @param tLo the lower bound of the internal parameter
     * @param tHi the upper bound of the internal parameter
     */
    protected fun internalParam(
        tLo: Double,
        tHi: Double,
        maxSegmentLength: Double = 0.25,
        maxDepth: Int = 15,
        maxDeltaK: Double = 0.01
    ) {
        var maxReachedDepth = 0
        fun parameterize(tLo: Double, tHi: Double, depth: Int) {
            if (depth > maxReachedDepth) maxReachedDepth = depth
            val tMid = (tLo + tHi) * 0.5
            val vLo = internalGet(tLo)
            val vMid = internalGet(tMid)
            val vHi = internalGet(tHi)
            val deltaK = abs(curvature(tLo) - curvature(tHi))
            //TODO: more accurate length estimation?
            val segmentLength = (vLo distTo vMid) + (vMid distTo vHi)

            if (depth < maxDepth && (deltaK > maxDeltaK || segmentLength > maxSegmentLength)) {
                parameterize(tLo, tMid, depth + 1)
                parameterize(tMid, tHi, depth + 1)
            } else {
                length += segmentLength
                sSamples.add(length)
                tSamples.add(tHi)
            }
        }
        parameterize(tLo, tHi, 0)
        println(maxReachedDepth)
    }

    /**
     * Uses the samples computed by [internalParam] to find the value of the internal parameter `t` that corresponds to the given length along the curve [s].
     * @param s distance travelled along the curve
     * @return
     */
    protected fun internalReparam(s: Double): Double {
        if (s <= 0.0) return 0.0
        if (s >= length) return 1.0
        var lo = 0
        var hi = sSamples.size
        while (lo <= hi) {
            val mid = (hi + lo) / 2
            when {
                s < sSamples[mid] -> {
                    hi = mid - 1
                }
                s > sSamples[mid] -> {
                    lo = mid + 1
                }
                else -> {
                    return tSamples[mid]
                }
            }
        }
        return tSamples[lo] + (s - sSamples[lo]) * (tSamples[hi] - tSamples[lo]) / (sSamples[hi] - sSamples[lo])
    }

    /**
     * Returns the curve length computed by [internalParam]
     */
    protected fun internalLength() = length
}
