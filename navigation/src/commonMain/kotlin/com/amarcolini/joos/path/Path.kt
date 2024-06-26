package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.DoubleProgression
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.roundToInt

/**
 * Path composed of a list of parametric curves and heading interpolators.
 *
 * @param segments list of path segments
 */
@JsExport
class Path(@JvmField val segments: List<PathSegment>) {

    /**
     * @param segment single path segment
     */
    @JsName("fromSingle")
    constructor(segment: PathSegment) : this(listOf(segment))

    init {
        if (segments.isEmpty()) throw IllegalArgumentException("A Path cannot be initialized without segments.")
    }

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
        segments.forEach { it.curve.reparameterize() }
    }

    /**
     * Returns a pair containing the [PathSegment] at [s] inches along the path and the length along that segment.
     */
    fun segment(s: Double): Pair<PathSegment, Double> {
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
     * Returns the pose [s] units along the path.
     */
    @JvmOverloads
    operator fun get(s: Double, t: Double = reparam(s)): Pose2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment[remainingDisplacement, t]
    }

    /**
     * Returns the pose derivative [s] units along the path.
     */
    @JvmOverloads
    fun deriv(s: Double, t: Double = reparam(s)): Pose2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment.deriv(remainingDisplacement, t)
    }

    /**
     * Returns the pose second derivative [s] units along the path.
     */
    @JvmOverloads
    fun secondDeriv(s: Double, t: Double = reparam(s)): Pose2d {
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
    fun internalGet(segmentIndex: Int, t: Double): Pose2d {
        val segment = segments[segmentIndex.coerceIn(0, segments.lastIndex)]
        return segment[0.0, t]
    }

    @JvmOverloads
    fun internalDeriv(s: Double, t: Double = reparam(s)): Pose2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment.internalDeriv(remainingDisplacement, t)
    }

    @JvmOverloads
    fun internalSecondDeriv(s: Double, t: Double = reparam(s)): Pose2d {
        val (segment, remainingDisplacement) = segment(s)
        return segment.internalSecondDeriv(remainingDisplacement, t)
    }

    @JvmOverloads
    fun curvature(s: Double, t: Double = reparam(s)): Double {
        val (segment, remainingDisplacement) = segment(s)
        return segment.curvature(remainingDisplacement, t)
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
            val t = it.curve.project(queryPoint)
            t to it.curve
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
            val pathPoint = get(s, t).vec()
            val deriv = deriv(s, t).vec()
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
     */
    fun project(queryPoint: Vector2d, ds: Double = 3.0): Double {
        val samples = (length() / ds).roundToInt()

        val guesses = DoubleProgression.fromClosedInterval(0.0, length(), samples)

        val results = guesses.map { fastProject(queryPoint, it) }

        return results.minByOrNull { this[it].vec().distTo(queryPoint) } ?: 0.0
    }

    /**
     * Returns the start pose.
     */
    fun start() = segments.first().start()

    /**
     * Returns the start pose derivative.
     */
    fun startDeriv() = segments.first().startDeriv()

    /**
     * Returns the start pose second derivative.
     */
    fun startSecondDeriv() = segments.first().startSecondDeriv()

    /**
     * Returns the end pose.
     */
    fun end() = segments.last().end()

    /**
     * Returns the end pose derivative.
     */
    fun endDeriv() = segments.last().startDeriv()

    /**
     * Returns the end pose second derivative.
     */
    fun endSecondDeriv() = segments.last().startDeriv()
}