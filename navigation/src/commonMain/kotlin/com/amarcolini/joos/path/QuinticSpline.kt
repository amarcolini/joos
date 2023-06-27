package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Vector2d
import kotlin.jvm.JvmOverloads

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
}