package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.max
import com.amarcolini.joos.util.min
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.sign
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs

class CircularArc(
    val center: Vector2d,
    val radius: Double,
    startAngle: Angle,
    endAngle: Angle,
) : ParametricCurve() {
    private val diff = (endAngle - startAngle).coerceIn(-Angle.circle, Angle.circle)
    val startAngle = startAngle.norm()
    val endAngle = this.startAngle + diff
    private val circumference = abs(diff.radians * radius)
    private val direction = sign(diff)
    private val range =
        min(this.startAngle, this.endAngle)..max(this.startAngle, this.endAngle)

    companion object {
        @JvmStatic
        fun fromPoint(start: Vector2d, startTangent: Angle, radius: Double, turnAngle: Angle): CircularArc {
            val normAngle = (0.5 * PI * sign(turnAngle.normDelta())).rad
            val center = start + (startTangent + normAngle).vec() * radius
            val startAngle = (start - center).angle()
            val endAngle = startAngle + turnAngle
            return CircularArc(center, radius, startAngle, endAngle)
        }
    }

    override fun length(): Double = circumference

    override fun reparameterize() {
    }

    override fun project(query: Vector2d): Double {
        val angle = (query - center).angle().norm()
        return if (angle in range) angle.radians
        else listOf(startAngle.radians, endAngle.radians).minBy { internalGet(it) distTo query }
    }

    override fun reparam(s: Double): Double =
        s / radius * direction + startAngle.radians

    override fun internalGet(t: Double): Vector2d =
        t.rad.coerceIn(range).vec() * radius + center

    override fun internalDeriv(t: Double): Vector2d =
        t.rad.coerceIn(range).run { Vector2d(-sin(), cos()) * direction } * radius

    override fun internalSecondDeriv(t: Double): Vector2d =
        t.rad.coerceIn(range).run { Vector2d(-cos(), -sin()) } * radius

    override fun internalThirdDeriv(t: Double): Vector2d = -internalDeriv(t)
}