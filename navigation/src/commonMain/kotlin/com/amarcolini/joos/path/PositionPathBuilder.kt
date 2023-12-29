package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.deg
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads

/**
 * Easy-to-use builder for creating [PositionPath] instances.
 * Note that this builder does not respect tangent continuity.
 *
 * @param startPos start position
 * @param startDeriv start derivative
 * @param startSecondDeriv start second derivative
 * @see PathBuilder
 */
@JsExport
class PositionPathBuilder(
    startPos: Vector2d,
    startDeriv: Vector2d,
    startSecondDeriv: Vector2d
) {
    @JsName("fromPos")
    constructor(startPos: Vector2d, startTangent: Angle) :
            this(startPos, startTangent.vec(), Vector2d())

    @JsName("fromPath")
    constructor(path: PositionPath, s: Double) : this(path[s], path.deriv(s), path.secondDeriv(s))

    private var currentPos: Vector2d = startPos
    private var currentDeriv: Vector2d = startDeriv
    private var currentSecondDeriv: Vector2d = startSecondDeriv

    private var segments = mutableListOf<ParametricCurve>()

    private fun makeLine(end: Vector2d): LineSegment {
        val start = currentPos

        if (start epsilonEquals end) {
            throw EmptyPathSegmentException()
        }

        return LineSegment(start, end)
    }

    private fun makeSpline(
        endPosition: Vector2d,
        endTangent: Angle,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0
    ): QuinticSpline {
        if (currentPos epsilonEquals endPosition) {
            throw EmptyPathSegmentException()
        }

        val derivMag = (currentPos distTo endPosition)
        val startWaypoint =
            QuinticSpline.Knot(
                currentPos,
                currentDeriv * if (startTangentMag >= 0.0) startTangentMag else derivMag,
                currentSecondDeriv
            )
        val endWaypoint = QuinticSpline.Knot(
            endPosition,
            Vector2d.polar(if (endTangentMag >= 0.0) endTangentMag else derivMag, endTangent)
        )

        return QuinticSpline(startWaypoint, endWaypoint)
    }

    private fun addSegment(segment: ParametricCurve): PositionPathBuilder {
        if (segments.isNotEmpty()) {
//            val lastSegment = segments.last()
            if (!(currentPos epsilonEquals segment.start())
            // We only care about C^0 continuity
//                        && currentDeriv epsilonEquals segment.startDeriv()
//                        && currentSecondDeriv epsilonEquals segment.startSecondDeriv())
            ) throw PathContinuityViolationException()
        }

        currentPos = segment.end()
        currentDeriv = segment.endDeriv()
        currentSecondDeriv = segment.endSecondDeriv()

        segments.add(segment)

        return this
    }

    /**
     * Adds a line segment with tangent heading interpolation.
     *
     * @param endPosition end position
     */
    fun lineTo(endPosition: Vector2d): PositionPathBuilder =
        addSegment(makeLine(endPosition))

    /**
     * Adds a line straight forward.
     *
     * @param distance distance to travel forward
     */
    fun forward(distance: Double): PositionPathBuilder =
        lineTo(currentPos + Vector2d.polar(distance, currentDeriv.angle()))

    /**
     * Adds a line straight backward.
     *
     * @param distance distance to travel backward
     */
    fun back(distance: Double): PositionPathBuilder = forward(-distance)

    /**
     * Adds a segment that travels left in the robot reference frame.
     *
     * @param distance distance to travel left
     */
    fun left(distance: Double): PositionPathBuilder =
        lineTo(currentPos + Vector2d.polar(distance, currentDeriv.angle() + 90.deg))

    /**
     * Adds a segment that travels right in the robot reference frame.
     *
     * @param distance distance to travel right
     */
    fun right(distance: Double): PositionPathBuilder = left(-distance)

    /**
     * Adds a spline segment.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     * @param startTangentMag the magnitude of the start tangent (negative = default magnitude)
     * @param endTangentMag the magnitude of the end tangent (negative = default magnitude)
     */
    @JvmOverloads
    fun splineTo(
        endPosition: Vector2d,
        endTangent: Angle,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0
    ): PositionPathBuilder =
        addSegment(makeSpline(endPosition, endTangent, startTangentMag, endTangentMag))

    /**
     * Adds a [CircularArc] segment that turns [angle].
     */
    fun turn(angle: Angle, radius: Double): PositionPathBuilder =
        addSegment(CircularArc.fromPoint(currentPos, currentDeriv.angle(), radius, angle))

    fun turnLeft(angle: Angle, radius: Double) = turn(angle, radius)
    fun turnRight(angle: Angle, radius: Double) = turn(-angle, radius)

    /**
     * Constructs the [PositionPath] instance.
     */
    fun build(): PositionPath {
        segments.forEach { it.reparameterize() }
        return PositionPath(segments)
    }

    /**
     * Constructs the [Path] instance without reparameterizing the curves.
     */
    fun preBuild(): PositionPath {
        return PositionPath(segments)
    }
}