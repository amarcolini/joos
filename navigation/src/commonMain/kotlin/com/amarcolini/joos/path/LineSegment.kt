package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Vector2d

/**
 * Parametric representation of a line.
 *
 * @param start start vector
 * @param end end vector
 */
class LineSegment(private val start: Vector2d, end: Vector2d) : ParametricCurve() {
    private val diff = end - start
    private val length by lazy { diff.norm() }

    override fun length() = length

    override fun reparameterize() {
        length
    }

    override fun project(query: Vector2d): Double =
        (((query - start) dot diff) / (diff dot diff)).coerceIn(0.0, 1.0)

    override fun internalGet(t: Double) = start + diff * t

    override fun internalDeriv(t: Double) = diff / length()

    override fun internalSecondDeriv(t: Double) = Vector2d(0.0, 0.0)

    override fun internalThirdDeriv(t: Double) = Vector2d(0.0, 0.0)

    override fun reparam(s: Double) = s / length

    override fun toString() = "(${start.x}+${diff.x}*t,${start.y}+${diff.y}*t)"
}