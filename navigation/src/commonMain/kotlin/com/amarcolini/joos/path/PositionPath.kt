package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.DoubleProgression
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads
import kotlin.math.roundToInt

/**
 * Path composed of a list of parametric curves.
 *
 * @param segments list of curves.
 */
@JsExport
class PositionPath(val segments: List<ParametricCurve>) {

    /**
     * @param segment single curve.
     */
    @JsName("fromSingle")
    constructor(segment: ParametricCurve) : this(listOf(segment))

    /**
     * Returns the length of the path.
     */
    fun length() = segments.sumOf { it.length() }

    /**
     * Calls [ParametricCurve.reparameterize] on all the curves in this path.
     *
     * @see ParametricCurve
     */
    fun reparameterize() {
        segments.forEach { it.reparameterize() }
    }

    /**
     * Returns a pair containing the [ParametricCurve] at [s] inches along the path and the length along that segment.
     */
    fun segment(s: Double): Pair<ParametricCurve, Double> {
        if (s <= 0.0) {
            return segments.first() to 0.0
        }
        var remainingDisplacement = s
        for (segment in segments) {
            if (remainingDisplacement <= segment.length()) {
                return segment to remainingDisplacement
            }
            remainingDisplacement -= segment.length()
        }
        return segments.last() to segments.last().length()
    }

    /**
     * Returns the position [s] units along the path.
     */
    @JvmOverloads
    operator fun get(s: Double, t: Double = reparam(s)): Vector2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment[remainingDisplacement, t]
    }

    /**
     * Returns the position derivative [s] units along the path.
     */
    @JvmOverloads
    fun deriv(s: Double, t: Double = reparam(s)): Vector2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment.deriv(remainingDisplacement, t)
    }

    /**
     * Returns the position second derivative [s] units along the path.
     */
    @JvmOverloads
    fun secondDeriv(s: Double, t: Double = reparam(s)): Vector2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment.secondDeriv(remainingDisplacement, t)
    }

    /**
     * Returns the angle of the tangent line [s] units along the path.
     */
    @JvmOverloads
    fun tangentAngle(s: Double, t: Double = reparam(s)): Angle {
        val (segment, remainingDisplacement) = segment(s)
        return segment.tangentAngle(remainingDisplacement, t)
    }

    /**
     * Returns the pose along the segment specified by [segmentIndex] at the internal parameter [t].
     */
    fun internalGet(segmentIndex: Int, t: Double): Vector2d {
        val segment = segments[segmentIndex.coerceIn(0, segments.lastIndex)]
        return segment[0.0, t]
    }

    @JvmOverloads
    fun internalDeriv(s: Double, t: Double = reparam(s)): Vector2d {
        val (segment, _) = segment(s)
        return segment.internalDeriv(t)
    }

    @JvmOverloads
    fun internalSecondDeriv(s: Double, t: Double = reparam(s)): Vector2d {
        val (segment, _) = segment(s)
        return segment.internalSecondDeriv(t)
    }

    @JvmOverloads
    fun curvature(s: Double, t: Double = reparam(s)): Double {
        val (segment, _) = segment(s)
        return segment.curvature(t)
    }

    fun reparam(s: Double): Double {
        val (segment, remainingDisplacement) = segment(s)
        return segment.reparam(remainingDisplacement)
    }

    /**
     * Projects [queryPoint] onto the current path by calling [ParametricCurve.project] on every
     * segment and comparing the results. May be faster or slower than [fastProject] depending on the path.
     */
    fun compositeProject(queryPoint: Vector2d): Pair<Double, ParametricCurve> =
        segments.map {
            val t = it.project(queryPoint)
            t to it
        }.minBy { (it.second.internalGet(it.first) - queryPoint).squaredNorm() }

    /**
     * Project [queryPoint] onto the current path using the iterative method described
     * [here](http://www.geometrie.tugraz.at/wallner/sproj.pdf).
     *
     * @param queryPoint query queryPoint
     * @param projectGuess guess for the projected queryPoint's s along the path
     */
    fun fastProject(queryPoint: Vector2d, projectGuess: Double = length() / 2.0, iterations: Int = 10): Double {
        // we use the first-order method (since we already compute the arc length param)
        return (1..iterations).fold(projectGuess) { s, _ ->
            val t = reparam(s)
            val pathPoint = get(s, t)
            val deriv = deriv(s, t)
            val ds = (queryPoint - pathPoint) dot deriv
            (s + ds).coerceIn(0.0, length())
        }
    }

    /**
     * Project [queryPoint] onto the current path by applying [fastProject] with various
     * guesses along the path.
     *
     * @param queryPoint query queryPoint
     * @param ds spacing between guesses
     * @return the displacement along the path where [queryPoint] is projected to.
     */
    fun project(queryPoint: Vector2d, ds: Double = 3.0): Double {
        val samples = (length() / ds).roundToInt()

        val guesses = DoubleProgression.fromClosedInterval(0.0, length(), samples)

        val results = guesses.map { fastProject(queryPoint, it) }

        return results.minByOrNull { this[it].distTo(queryPoint) } ?: 0.0
    }

    /**
     * Returns the start position.
     */
    fun start() = get(0.0)

    /**
     * Returns the start position derivative.
     */
    fun startDeriv() = deriv(0.0)

    /**
     * Returns the start position second derivative.
     */
    fun startSecondDeriv() = secondDeriv(0.0)

    /**
     * Returns the end position.
     */
    fun end() = get(length())

    /**
     * Returns the end position derivative.
     */
    fun endDeriv() = deriv(length())

    /**
     * Returns the end position second derivative.
     */
    fun endSecondDeriv() = secondDeriv(length())
}