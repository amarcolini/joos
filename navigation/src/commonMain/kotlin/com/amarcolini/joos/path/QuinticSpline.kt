package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.EPSILON
import com.amarcolini.joos.util.evaluatePolynomial
import com.amarcolini.joos.util.generatePascalsTriangle
import com.amarcolini.joos.util.isolateRoots
import kotlin.jvm.JvmOverloads
import kotlin.math.absoluteValue

/**
 * Quintic Bezier Spline
 */
class QuinticSpline(
    start: Knot,
    end: Knot
) : ParametricCurve() {
    //TODO: convex hull
    /**
     * X polynomial (`x(t)`).
     */
    val x: QuinticPolynomial =
        QuinticPolynomial(
            start.x,
            start.dx,
            start.d2x,
            end.x,
            end.dx,
            end.d2x
        )

    /**
     * Y polynomial (`y(t)`).
     */
    val y: QuinticPolynomial =
        QuinticPolynomial(
            start.y,
            start.dy,
            start.d2y,
            end.y,
            end.dy,
            end.d2y
        )

//    private val convexHull =
//        MonotoneChain(false).findHullVertices(x.controlPoints.mapIndexed { i, d -> Vector2D(d, y.controlPoints[i]) })
//            .map { Vector2d(it.x, it.y) }

    /**
     * Class for representing the end points of interpolated quintic splines.
     *
     * @param x x position
     * @param y y position
     * @param dx x derivative
     * @param dy y derivative
     * @param d2x x second derivative
     * @param d2y y second derivative
     */
    class Knot @JvmOverloads constructor(
        val x: Double,
        val y: Double,
        val dx: Double = 0.0,
        val dy: Double = 0.0,
        val d2x: Double = 0.0,
        val d2y: Double = 0.0
    ) {
        @JvmOverloads
        constructor(
            pos: Vector2d,
            deriv: Vector2d = Vector2d(),
            secondDeriv: Vector2d = Vector2d()
        ) : this(pos.x, pos.y, deriv.x, deriv.y, secondDeriv.x, secondDeriv.y)

        fun pos() = Vector2d(x, y)

        fun deriv() = Vector2d(dx, dy)

        fun secondDeriv() = Vector2d(d2x, d2y)
    }

    private val reparameterization by lazy { ArcLengthParameterization(0.0, 1.0) }

    override fun length(): Double = reparameterization.length

    override fun reparam(s: Double): Double = reparameterization.reparam(s)

    override fun internalGet(t: Double): Vector2d = Vector2d(x[t], y[t])

    override fun internalDeriv(t: Double): Vector2d = Vector2d(x.deriv(t), y.deriv(t))

    override fun internalSecondDeriv(t: Double): Vector2d = Vector2d(x.secondDeriv(t), y.secondDeriv(t))

    override fun internalThirdDeriv(t: Double): Vector2d = Vector2d(x.thirdDeriv(t), y.thirdDeriv(t))

    override fun reparameterize() {
        reparameterization
    }

    private val pascalsTriangle by lazy {
        generatePascalsTriangle(10)
    }

    val coeffs = x.coeffs.zip(y.coeffs).map {
        Vector2d(it.first, it.second)
    }

    val dcoeffs = x.dcoeffs.zip(y.dcoeffs).map {
        Vector2d(it.first, it.second)
    }

    private val invLeadingCoefficient by lazy {
        -(coeffs[0] dot dcoeffs[0])
    }

    private val preG by lazy {
        val raw = doubleArrayOf(
            1.0,
            -(coeffs[0] dot dcoeffs[1]) - (coeffs[1] dot dcoeffs[0]),
            -(coeffs[0] dot dcoeffs[2]) - (coeffs[1] dot dcoeffs[1]) - (coeffs[2] dot dcoeffs[0]),
            -(coeffs[0] dot dcoeffs[3]) - (coeffs[1] dot dcoeffs[2]) - (coeffs[2] dot dcoeffs[1]) - (coeffs[3] dot dcoeffs[0]),
            -(coeffs[0] dot dcoeffs[4]) - (coeffs[1] dot dcoeffs[3]) - (coeffs[2] dot dcoeffs[2]) - (coeffs[3] dot dcoeffs[1]) - (coeffs[4] dot dcoeffs[0]),
            -(coeffs[1] dot dcoeffs[4]) - (coeffs[2] dot dcoeffs[3]) - (coeffs[3] dot dcoeffs[2]) - (coeffs[4] dot dcoeffs[1]) - (coeffs[5] dot dcoeffs[0]),
            -(coeffs[2] dot dcoeffs[4]) - (coeffs[3] dot dcoeffs[3]) - (coeffs[4] dot dcoeffs[2]) - (coeffs[5] dot dcoeffs[1]),
            -(coeffs[3] dot dcoeffs[4]) - (coeffs[4] dot dcoeffs[3]) - (coeffs[5] dot dcoeffs[2]),
            -(coeffs[4] dot dcoeffs[4]) - (coeffs[5] dot dcoeffs[3]),
            -(coeffs[5] dot dcoeffs[4])
        )
        for (i in 1..raw.lastIndex) {
            raw[i] /= invLeadingCoefficient
        }
        raw
    }

    override fun project(query: Vector2d): Double = project(query, EPSILON)

    /**
     * Projection algorithm based on [this paper](https://inria.hal.science/file/index/docid/518379/filename/Xiao-DiaoChen2007c.pdf),
     * but with the Modified Uspensky algorithm instead of using a Sturm sequence.
     */
    fun project(p: Vector2d, tolerance: Double): Double {
        val g = preG.copyOf()
        g[5] = g[5] + (p dot dcoeffs[0]) / invLeadingCoefficient
        g[6] = g[6] + (p dot dcoeffs[1]) / invLeadingCoefficient
        g[7] = g[7] + (p dot dcoeffs[2]) / invLeadingCoefficient
        g[8] = g[8] + (p dot dcoeffs[3]) / invLeadingCoefficient
        g[9] = g[9] + (p dot coeffs[4]) / invLeadingCoefficient
        val roots = isolateRoots(g, pascalsTriangle).mapNotNull {
            if (!(evaluatePolynomial(g, it.start) < 0 && evaluatePolynomial(
                    g,
                    it.endInclusive
                ) > 0)
            ) return@mapNotNull null
            var a = it.start
            var b = it.endInclusive
            while (b - a >= tolerance) {
                val m = (a + b) / 2
                val result = evaluatePolynomial(g, m)
                if (result == 0.0) return@mapNotNull m to (p - internalGet(m)).squaredNorm()
                if (result < 0.0) a = m
                else b = m
            }
            val r = (a + b) / 2
            r to (p - internalGet(r)).squaredNorm()
        }
        val startDist = (p - Vector2d(x.controlPoints[0], y.controlPoints[0])).squaredNorm()
        val endDist = (p - Vector2d(x.controlPoints[5], y.controlPoints[5])).squaredNorm()
        if (roots.isEmpty()) return when {
            startDist > endDist -> 1.0
            else -> 0.0
        }
        return (roots + listOf(
            0.0 to startDist,
            1.0 to endDist
        )).minBy { it.second }.first
    }
}