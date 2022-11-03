package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.heading.ConstantInterpolator
import com.amarcolini.joos.path.heading.LinearInterpolator
import com.amarcolini.joos.path.heading.SplineInterpolator
import com.amarcolini.joos.path.heading.TangentInterpolator
import com.amarcolini.joos.util.deg
import kotlin.jvm.JvmOverloads

/**
 * Exception thrown by [PathBuilder].
 */
abstract class PathBuilderException : RuntimeException()

/**
 * Exception thrown when [PathBuilder] methods are chained illegally. This commonly arises when switching from
 * non-tangent interpolation back to tangent interpolation and when splicing paths.
 */
class PathContinuityViolationException : PathBuilderException()

/**
 * Exception thrown when empty path segments are requested.
 */
class EmptyPathSegmentException : PathBuilderException()

/**
 * Easy-to-use builder for creating [Path] instances.
 *
 * @param startPose start pose
 * @param startDeriv start derivative
 * @param startSecondDeriv start second derivative
 */
class PathBuilder(
    startPose: Pose2d,
    startDeriv: Pose2d,
    startSecondDeriv: Pose2d
) {
    @JvmOverloads
    constructor(startPose: Pose2d, startTangent: Angle = startPose.heading) :
            this(startPose, Pose2d(startTangent.vec()), Pose2d())

    constructor(startPose: Pose2d, reversed: Boolean) :
            this(startPose, (startPose.heading + Angle(if (reversed) 180.0 else 0.0)).norm())

    constructor(path: Path, s: Double) : this(path[s], path.deriv(s), path.secondDeriv(s))

    private var currentPose: Pose2d = startPose
    private var currentDeriv: Pose2d = startDeriv
    private var currentSecondDeriv: Pose2d = startSecondDeriv

    private var segments = mutableListOf<PathSegment>()

    private fun makeLine(end: Vector2d): LineSegment {
        val start = currentPose

        if (start.vec() epsilonEquals end) {
            throw EmptyPathSegmentException()
        }

        return LineSegment(start.vec(), end)
    }

    private fun makeSpline(endPosition: Vector2d, endTangent: Angle): QuinticSpline {
        if (currentPose.vec() epsilonEquals endPosition) {
            throw EmptyPathSegmentException()
        }

        val derivMag = (currentPose.vec() distTo endPosition)
        val startWaypoint =
            QuinticSpline.Knot(
                currentPose.vec(),
                currentDeriv.vec() * derivMag,
                currentSecondDeriv.vec()
            )
        val endWaypoint = QuinticSpline.Knot(endPosition, Vector2d.polar(derivMag, endTangent))

        return QuinticSpline(startWaypoint, endWaypoint)
    }

    private fun makeTangentInterpolator(curve: ParametricCurve): TangentInterpolator {
        //TODO: bring back smooth tangent splicing?
//        if (currentPose == null) {
//            val prevInterpolator = path!!.segment(s!!).first.interpolator
//            if (prevInterpolator !is TangentInterpolator) {
//                throw PathContinuityViolationException()
//            }
//            return TangentInterpolator(prevInterpolator.offset)
//        }

        val startHeading = curve.tangentAngle(0.0, 0.0)

        val interpolator = TangentInterpolator(currentPose.heading - startHeading)
        interpolator.init(curve)
        return interpolator
    }

    private fun makeConstantInterpolator(): ConstantInterpolator {
        val currentHeading = currentPose.heading

        return ConstantInterpolator(currentHeading)
    }

    private fun makeLinearInterpolator(endHeading: Angle): LinearInterpolator {
        val startHeading = currentPose.heading

        return LinearInterpolator(startHeading, (endHeading - startHeading).normDelta())
    }

    private fun makeSplineInterpolator(endHeading: Angle): SplineInterpolator {
        return SplineInterpolator(
            currentPose.heading,
            endHeading,
            currentDeriv.heading,
            currentSecondDeriv.heading,
            null,
            null
        )
    }

    private fun addSegment(segment: PathSegment): PathBuilder {
        if (segments.isNotEmpty()) {
//            val lastSegment = segments.last()
            if (!(currentPose epsilonEqualsHeading segment.start() &&
                        currentDeriv epsilonEquals segment.startDeriv() &&
                        currentSecondDeriv.vec() epsilonEquals segment.startSecondDeriv()
                    .vec())
            ) throw PathContinuityViolationException()
        }

        currentPose = segment.end()
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
    fun lineTo(endPosition: Vector2d): PathBuilder {
        val line = makeLine(endPosition)
        val interpolator = makeTangentInterpolator(line)

        addSegment(PathSegment(line, interpolator))

        return this
    }

    /**
     * Adds a line segment with constant heading interpolation.
     *
     * @param endPosition end position
     */
    fun lineToConstantHeading(endPosition: Vector2d): PathBuilder =
        addSegment(PathSegment(makeLine(endPosition), makeConstantInterpolator()))

    /**
     * Adds a strafe segment (i.e., a line segment with constant heading interpolation).
     *
     * @param endPosition end position
     */
    fun strafeTo(endPosition: Vector2d): PathBuilder = lineToConstantHeading(endPosition)

    /**
     * Adds a line segment with linear heading interpolation.
     *
     * @param endPose end pose
     */
    fun lineToLinearHeading(endPose: Pose2d): PathBuilder =
        addSegment(PathSegment(makeLine(endPose.vec()), makeLinearInterpolator(endPose.heading)))

    /**
     * Adds a line segment with spline heading interpolation.
     *
     * @param endPose end pose
     */
    fun lineToSplineHeading(endPose: Pose2d): PathBuilder =
        addSegment(PathSegment(makeLine(endPose.vec()), makeSplineInterpolator(endPose.heading)))

    /**
     * Adds a line straight forward.
     *
     * @param distance distance to travel forward
     */
    fun forward(distance: Double): PathBuilder =
        lineTo(currentPose.vec() + Vector2d.polar(distance, currentPose.heading))

    /**
     * Adds a line straight backward.
     *
     * @param distance distance to travel backward
     */
    fun back(distance: Double): PathBuilder = forward(-distance)

    /**
     * Adds a segment that strafes left in the robot reference frame.
     *
     * @param distance distance to strafe left
     */
    fun strafeLeft(distance: Double): PathBuilder =
        strafeTo(currentPose.vec() + Vector2d.polar(distance, currentPose.heading + 90.deg))

    /**
     * Adds a segment that strafes right in the robot reference frame.
     *
     * @param distance distance to strafe right
     */
    fun strafeRight(distance: Double): PathBuilder = strafeLeft(-distance)

    /**
     * Adds a spline segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     */
    fun splineTo(endPosition: Vector2d, endTangent: Angle): PathBuilder {
        val spline = makeSpline(endPosition, endTangent)
        val interpolator = makeTangentInterpolator(spline)

        return addSegment(PathSegment(spline, interpolator))
    }

    /**
     * Adds a spline segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent in [Angle.defaultUnits]
     */
    fun splineTo(endPosition: Vector2d, endTangent: Double): PathBuilder = splineTo(endPosition, Angle(endTangent))

    /**
     * Adds a spline segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     */
    fun splineToConstantHeading(endPosition: Vector2d, endTangent: Angle): PathBuilder =
        addSegment(PathSegment(makeSpline(endPosition, endTangent), makeConstantInterpolator()))

    /**
     * Adds a spline segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent in [Angle.defaultUnits]
     */
    fun splineToConstantHeading(endPosition: Vector2d, endTangent: Double): PathBuilder =
        splineToConstantHeading(endPosition, Angle(endTangent))

    /**
     * Adds a spline segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent
     */
    fun splineToLinearHeading(endPose: Pose2d, endTangent: Angle) =
        addSegment(
            PathSegment(
                makeSpline(endPose.vec(), endTangent),
                makeLinearInterpolator(endPose.heading)
            )
        )

    /**
     * Adds a spline segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent in [Angle.defaultUnits]
     */
    fun splineToLinearHeading(endPose: Pose2d, endTangent: Double) =
        splineToLinearHeading(endPose, Angle(endTangent))

    /**
     * Adds a spline segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent
     */
    fun splineToSplineHeading(endPose: Pose2d, endTangent: Angle) =
        addSegment(
            PathSegment(
                makeSpline(endPose.vec(), endTangent),
                makeSplineInterpolator(endPose.heading)
            )
        )

    /**
     * Adds a spline segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent in [Angle.defaultUnits]
     */
    fun splineToSplineHeading(endPose: Pose2d, endTangent: Double) =
        splineToSplineHeading(endPose, Angle(endTangent))

    /**
     * Constructs the [Path] instance.
     */
    fun build(): Path {
        return Path(segments)
    }
}