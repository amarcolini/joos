package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.rad
import kotlin.js.JsExport
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.pow

/**
 * Parametric curve with two components (x and y). These curves are reparameterized from an internal parameter (t) to
 * the arc length parameter (s). Note that the arc length reparameterization is lazy, meaning that it is computed only
 * when needed. To precompute, use [reparameterize].
 */
@JsExport
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
                deriv.norm().pow(4)
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
        return (pt1 + pt2) / deriv.norm().pow(9)
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
    fun tangentAngle(s: Double, t: Double = reparam(s)): Angle = deriv(s, t).angle()

    /**
     * Returns the derivative of the tangent angle [s] units along the curve.
     */
    @JvmOverloads
    fun tangentAngleDeriv(s: Double, t: Double = reparam(s)): Angle {
        val deriv = deriv(s, t)
        val secondDeriv = secondDeriv(s, t)
        return (deriv.x * secondDeriv.y - deriv.y * secondDeriv.x).rad
    }

    /**
     * Returns the second derivative of the tangent angle [s] units along the curve.
     */
    @JvmOverloads
    fun tangentAngleSecondDeriv(s: Double, t: Double = reparam(s)): Angle {
        val deriv = deriv(s, t)
        val thirdDeriv = thirdDeriv(s, t)
        return (deriv.x * thirdDeriv.y - deriv.y * thirdDeriv.x).rad
    }

    /**
     * Returns the length of the curve.
     */
    abstract fun length(): Double

    /**
     * If this curve can not trivially be reparameterized to an arc length parameter, it should be done here.
     *
     * @see ParametricCurve
     */
    abstract fun reparameterize()

    /**
     * From an arc length parameter ([s]), finds the corresponding internal parameter `t`.
     */
    internal abstract fun reparam(s: Double): Double

    internal abstract fun internalGet(t: Double): Vector2d
    internal abstract fun internalDeriv(t: Double): Vector2d
    internal abstract fun internalSecondDeriv(t: Double): Vector2d
    internal abstract fun internalThirdDeriv(t: Double): Vector2d

    /**
     * Computes the curvature of a parametric curve at the internal parameter [t].
     */
    @JvmOverloads
    fun curvature(s: Double, t: Double = reparam(s)): Double = tangentAngleDeriv(s, t).radians

    /**
     * Automatically reparameterizes this curve by computing many small samples. This is computationally
     * expensive and should be avoided unless no faster way is available.
     */
    inner class ArcLengthParameterization(
        private val tLo: Double,
        private val tHi: Double,
        maxSegmentLength: Double = 0.25,
        maxDepth: Int = 15,
        maxDeltaK: Double = 0.01
    ) {
        private val tSamples = mutableListOf(0.0)
        private val sSamples = mutableListOf(0.0)

        /**
         * Returns the computed curve length.
         */
        val length: Double
            @JvmName("length") get

        init {
            var length = 0.0
            fun parameterize(tLo: Double, tHi: Double, depth: Int) {
                val tMid = (tLo + tHi) * 0.5
                val vLo = internalGet(tLo)
                val vMid = internalGet(tMid)
                val vHi = internalGet(tHi)
                val deltaK = abs(curvature(0.0, tLo) - curvature(0.0, tHi))
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
            this.length = length
        }

        /**
         * Computes internal parameter vs curve length samples and estimates curve length.
         * @param tLo the lower bound of the internal parameter
         * @param tHi the upper bound of the internal parameter
         * @param maxSegmentLength the maximum distance between two samples
         * @param maxDepth the maximum number of times the curve can be divided into samples
         * @param maxDeltaK the maximum change in curvature between two samples
         */
        /**
         * Uses the samples computed by to find the value of the internal parameter `t` that
         * corresponds to the given length along the curve [s].
         * @param s distance travelled along the curve
         * @return
         */
        fun reparam(s: Double): Double {
            if (s <= 0.0) return tLo
            if (s >= length) return tHi
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
    }
}