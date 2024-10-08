package com.amarcolini.joos.path

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.heading.*
import com.amarcolini.joos.util.deg
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads

/**
 * Exception thrown by [PathBuilder] and [PositionPathBuilder].
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
 * @see PositionPathBuilder
 */
class PathBuilder(
    startPose: Pose2d,
    startDeriv: Pose2d,
    startSecondDeriv: Pose2d
) {
    @JvmOverloads
    @JsName("fromPose")
    constructor(startPose: Pose2d, startTangent: Angle = startPose.heading) :
            this(startPose, Pose2d(startTangent.vec()), Pose2d())

    @JsName("fromReversed")
    constructor(startPose: Pose2d, reversed: Boolean) :
            this(startPose, (startPose.heading + (if (reversed) 180.deg else 0.deg)).norm())

    @JsName("fromPath")
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

    private fun makeSpline(
        endPosition: Vector2d,
        endTangent: Angle,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0
    ): QuinticSpline {
        if (currentPose.vec() epsilonEquals endPosition) {
            throw EmptyPathSegmentException()
        }

        val derivMag = (currentPose.vec() distTo endPosition)
        val startWaypoint =
            QuinticSpline.Knot(
                currentPose.vec(),
                currentDeriv.vec() * if (startTangentMag >= 0.0) startTangentMag else derivMag,
                currentSecondDeriv.vec()
            )
        val endWaypoint = QuinticSpline.Knot(
            endPosition,
            Vector2d.polar(if (endTangentMag >= 0.0) endTangentMag else derivMag, endTangent)
        )

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
     * Adds a line segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param headingInterpolation desired heading interpolation
     * @see HeadingInterpolation
     */
    fun addLine(endPosition: Vector2d, headingInterpolation: HeadingInterpolation = TangentHeading): PathBuilder {
        val line = makeLine(endPosition)
        val interpolator = when (headingInterpolation) {
            is TangentHeading -> makeTangentInterpolator(line)
            is ConstantHeading -> makeConstantInterpolator()
            is LinearHeading -> makeLinearInterpolator(headingInterpolation.target)
            is SplineHeading -> makeSplineInterpolator(headingInterpolation.target)
        }

        return addSegment(PathSegment(line, interpolator))
    }

    fun lineTo(endPosition: Vector2d) = addLine(endPosition, TangentHeading)
    fun lineToConstantHeading(endPosition: Vector2d) = addLine(endPosition, ConstantHeading)
    fun lineToLinearHeading(endPose: Pose2d) = addLine(endPose.vec(), LinearHeading(endPose.heading))
    fun lineToSplineHeading(endPose: Pose2d) = addLine(endPose.vec(), SplineHeading(endPose.heading))
    fun forward(distance: Double) =
        lineTo(currentPose.vec() + Vector2d.polar(distance, currentDeriv.vec().angle()))

    fun back(distance: Double) = forward(-distance)
    fun strafeLeft(distance: Double) =
        lineToConstantHeading(currentPose.vec() + Vector2d.polar(distance, currentPose.heading + 90.deg))

    fun strafeRight(distance: Double) = strafeLeft(-distance)

    /**
     * Adds a spline segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     * @param startTangentMag the magnitude of the start tangent (negative = default magnitude)
     * @param endTangentMag the magnitude of the end tangent (negative = default magnitude)
     * @param headingInterpolation desired heading interpolation
     * @see HeadingInterpolation
     */
    @JvmOverloads
    fun addSpline(
        endPosition: Vector2d,
        endTangent: Angle,
        headingInterpolation: HeadingInterpolation = TangentHeading,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0,
    ): PathBuilder {
        val spline = makeSpline(endPosition, endTangent, startTangentMag, endTangentMag)
        val interpolator = when (headingInterpolation) {
            is TangentHeading -> makeTangentInterpolator(spline)
            is ConstantHeading -> makeConstantInterpolator()
            is LinearHeading -> makeLinearInterpolator(headingInterpolation.target)
            is SplineHeading -> makeSplineInterpolator(headingInterpolation.target)
        }

        return addSegment(PathSegment(spline, interpolator))
    }

    fun splineTo(endPosition: Vector2d, endTangent: Angle): PathBuilder = addSpline(endPosition, endTangent)
    fun splineToConstantHeading(endPosition: Vector2d, endTangent: Angle): PathBuilder =
        addSpline(endPosition, endTangent, ConstantHeading)
    fun splineToLinearHeading(endPose: Pose2d, endTangent: Angle) =
        addSpline(endPose.vec(), endTangent, LinearHeading(endPose.heading))
    fun splineToSplineHeading(endPose: Pose2d, endTangent: Angle) =
        addSpline(endPose.vec(), endTangent, SplineHeading(endPose.heading))

    /**
     * Constructs the [Path] instance.
     */
    fun build(): Path {
        return Path(segments).also { it.reparameterize() }
    }

    /**
     * Constructs the [Path] instance without reparameterizing the curves.
     */
    fun preBuild(): Path {
        return Path(segments)
    }

    fun currentSegments(): List<PathSegment> = segments
}