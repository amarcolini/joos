package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.path.PathContinuityViolationException
import com.amarcolini.joos.path.heading.*
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads

/**
 * Easy-to-use builder for creating [Trajectory] instances.
 *
 * @param startPose start pose
 * @param startDeriv start derivative
 * @param startSecondDeriv start second derivative
 */
@JsExport
@Suppress("UNCHECKED_CAST")
abstract class BaseTrajectoryBuilder<T : BaseTrajectoryBuilder<T>> protected constructor(
    private val startPose: Pose2d,
    private val startDeriv: Pose2d,
    private val startSecondDeriv: Pose2d
) {
    private var pathBuilder: PathBuilder = PathBuilder(startPose, startDeriv, startSecondDeriv)

    internal var segments = mutableListOf<TrajectorySegment>()

    protected fun addPathSegment(add: () -> Unit) {
        try {
            add()
        } catch (e: PathContinuityViolationException) {
            pushPath()
            pathBuilder = PathBuilder(segments.last().end())
            add()
        }
    }

    protected fun pushPath() {
        val path = pathBuilder.build()
        if (path.segments.isNotEmpty())
            addSegment(makePathSegment(path))
    }

    protected fun addSegment(segment: TrajectorySegment) {
        if (segments.isNotEmpty()) {
            val lastSegment = segments.last()
            val end = lastSegment.duration()
            //TODO: Test whether not checking acceleration is a problem
            if (!(segment.start() epsilonEqualsHeading lastSegment.end()
                        && segment.velocity(0.0) epsilonEquals lastSegment.velocity(end)
//                        && segment.secondDeriv(0.0).vec() epsilonEquals lastSegment.secondDeriv(end).vec()
                        )
            ) throw PathContinuityViolationException()
        }
        segments += segment
    }

    /**
     * Adds a turn segment that turns the specified [angle].
     *
     * @param angle angle to turn
     */
    fun turn(angle: Angle): T {
        pushPath()
        val start = if (segments.isEmpty()) startPose else segments.last().end()
        addSegment(makeTurnSegment(start, angle))
        pathBuilder = PathBuilder(segments.last().end())
        return this as T
    }

    /**
     * Adds a turn segment that turns to the specified [angle] in the global coordinate space.
     *
     * @param angle angle to turn to
     */
    fun turnTo(angle: Angle): T {
        pushPath()
        val start = if (segments.isEmpty()) startPose else segments.last().end()
        addSegment(makeTurnSegment(start, (angle - start.heading).normDelta()))
        pathBuilder = PathBuilder(segments.last().end())
        return this as T
    }

    /**
     * Adds a wait segment that waits [seconds].
     */
    fun wait(seconds: Double): T {
        pushPath()
        val start = if (segments.isEmpty()) startPose else segments.last().end()
        addSegment(WaitSegment(start, seconds))
        pathBuilder = PathBuilder(segments.last().end())
        return this as T
    }

    /**
     * Adds a line segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param headingInterpolation desired heading interpolation
     * @see HeadingInterpolation
     */
    fun addLine(endPosition: Vector2d, headingInterpolation: HeadingInterpolation = TangentHeading): T {
        addPathSegment { pathBuilder.addLine(endPosition, headingInterpolation) }
        return this as T
    }

    fun lineTo(endPosition: Vector2d) = addLine(endPosition)
    fun lineToConstantHeading(endPosition: Vector2d) = addLine(endPosition, ConstantHeading)
    fun lineToLinearHeading(endPose: Pose2d) = addLine(endPose.vec(), LinearHeading(endPose.heading))
    fun lineToSplineHeading(endPose: Pose2d) = addLine(endPose.vec(), SplineHeading(endPose.heading))
    fun forward(distance: Double): T {
        addPathSegment { pathBuilder.forward(distance) }
        return this as T
    }

    fun back(distance: Double): T {
        addPathSegment { pathBuilder.back(distance) }
        return this as T
    }

    fun strafeLeft(distance: Double): T {
        addPathSegment { pathBuilder.strafeLeft(distance) }
        return this as T
    }

    fun strafeRight(distance: Double): T {
        addPathSegment { pathBuilder.strafeRight(distance) }
        return this as T
    }

    /**
     * Adds a spline segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent (negative = default magnitude)
     * @param endTangentMag the magnitude of the end tangent (negative = default magnitude)
     * @param headingInterpolation the desired heading interpolation
     * @see HeadingInterpolation
     */
    @JvmOverloads
    fun addSpline(
        endPosition: Vector2d,
        endTangent: Angle,
        headingInterpolation: HeadingInterpolation = TangentHeading,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0,
    ): T {
        addPathSegment {
            pathBuilder.addSpline(
                endPosition,
                endTangent,
                headingInterpolation,
                startTangentMag,
                endTangentMag,
            )
        }
        return this as T
    }

    fun splineTo(endPosition: Vector2d, endTangent: Angle) = addSpline(endPosition, endTangent)
    fun splineToConstantHeading(endPosition: Vector2d, endTangent: Angle) =
        addSpline(endPosition, endTangent, ConstantHeading)

    fun splineToLinearHeading(endPose: Pose2d, endTangent: Angle) =
        addSpline(endPose.vec(), endTangent, LinearHeading(endPose.heading))

    fun splineToSplineHeading(endPose: Pose2d, endTangent: Angle) =
        addSpline(endPose.vec(), endTangent, SplineHeading(endPose.heading))

    /**
     * Constructs the [Trajectory] instance.
     */
    fun build(): Trajectory {
        pushPath()
        return Trajectory(segments)
    }

    protected abstract fun makePathSegment(path: Path): PathTrajectorySegment

    protected abstract fun makeTurnSegment(pose: Pose2d, angle: Angle): TurnSegment
}