package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.path.PathContinuityViolationException

/**
 * Easy-to-use builder for creating [Trajectory] instances.
 *
 * @param startPose start pose
 * @param startDeriv start derivative
 * @param startSecondDeriv start second derivative
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseTrajectoryBuilder<T : BaseTrajectoryBuilder<T>> protected constructor(
    private val startPose: Pose2d,
    private val startDeriv: Pose2d,
    private val startSecondDeriv: Pose2d
) {
    private var pathBuilder: PathBuilder = PathBuilder(startPose, startDeriv, startSecondDeriv)

    private val temporalMarkers = mutableListOf<TemporalMarker>()
    private val displacementMarkers = mutableListOf<DisplacementMarker>()
    private val spatialMarkers = mutableListOf<SpatialMarker>()

    private var segments = mutableListOf<TrajectorySegment>()

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
     * Adds a turn segment that turns [angle] degrees.
     *
     * @param angle angle to turn (in degrees)
     */
    fun turn(angle: Double): T {
        pushPath()
        val start = if (segments.isEmpty()) startPose else segments.last().end()
        addSegment(makeTurnSegment(start, Math.toRadians(angle)))
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
     * Adds a line segment with tangent heading interpolation.
     *
     * @param endPosition end position
     */
    fun lineTo(endPosition: Vector2d): T {
        addPathSegment { pathBuilder.lineTo(endPosition) }

        return this as T
    }

    /**
     * Adds a line segment with constant heading interpolation.
     *
     * @param endPosition end position
     */
    fun lineToConstantHeading(endPosition: Vector2d): T {
        addPathSegment { pathBuilder.lineToConstantHeading(endPosition) }

        return this as T
    }

    /**
     * Adds a line segment with linear heading interpolation.
     *
     * @param endPose end pose
     */
    fun lineToLinearHeading(endPose: Pose2d): T {
        addPathSegment { pathBuilder.lineToLinearHeading(endPose) }

        return this as T
    }

    /**
     * Adds a line segment with spline heading interpolation.
     *
     * @param endPose end pose
     */
    fun lineToSplineHeading(endPose: Pose2d): T {
        addPathSegment { pathBuilder.lineToSplineHeading(endPose) }

        return this as T
    }

    /**
     * Adds a strafe path segment.
     *
     * @param endPosition end position
     */
    fun strafeTo(endPosition: Vector2d): T {
        addPathSegment { pathBuilder.strafeTo(endPosition) }

        return this as T
    }

    /**
     * Adds a line straight forward.
     *
     * @param distance distance to travel forward
     */
    fun forward(distance: Double): T {
        addPathSegment { pathBuilder.forward(distance) }

        return this as T
    }

    /**
     * Adds a line straight backward.
     *
     * @param distance distance to travel backward
     */
    fun back(distance: Double): T {
        addPathSegment { pathBuilder.back(distance) }

        return this as T
    }

    /**
     * Adds a segment that strafes left in the robot reference frame.
     *
     * @param distance distance to strafe left
     */
    fun strafeLeft(distance: Double): T {
        addPathSegment { pathBuilder.strafeLeft(distance) }

        return this as T
    }

    /**
     * Adds a segment that strafes right in the robot reference frame.
     *
     * @param distance distance to strafe right
     */
    fun strafeRight(distance: Double): T {
        addPathSegment { pathBuilder.strafeRight(distance) }

        return this as T
    }

    /**
     * Adds a spline segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent, in degrees
     */
    fun splineTo(endPosition: Vector2d, endTangent: Double): T {
        addPathSegment { pathBuilder.splineTo(endPosition, Math.toRadians(endTangent)) }

        return this as T
    }

    /**
     * Adds a spline segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent, in degrees
     */
    fun splineToConstantHeading(endPosition: Vector2d, endTangent: Double): T {
        addPathSegment {
            pathBuilder.splineToConstantHeading(
                endPosition,
                Math.toRadians(endTangent)
            )
        }

        return this as T
    }

    /**
     * Adds a spline segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent, in degrees
     */
    fun splineToLinearHeading(endPose: Pose2d, endTangent: Double): T {
        addPathSegment { pathBuilder.splineToLinearHeading(endPose, Math.toRadians(endTangent)) }

        return this as T
    }

    /**
     * Adds a spline segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent, in degrees
     */
    fun splineToSplineHeading(endPose: Pose2d, endTangent: Double): T {
        addPathSegment { pathBuilder.splineToSplineHeading(endPose, Math.toRadians(endTangent)) }

        return this as T
    }

    /**
     * Adds a marker to the trajectory at [time].
     */
    fun addTemporalMarker(time: Double, callback: MarkerCallback) =
        addTemporalMarker(0.0, time, callback)

    /**
     * Adds a marker to the trajectory at [scale] * trajectory duration + [offset].
     */
    fun addTemporalMarker(scale: Double, offset: Double, callback: MarkerCallback) =
        addTemporalMarker({ scale * it + offset }, callback)

    /**
     * Adds a marker to the trajectory at [time] evaluated with the trajectory duration.
     */
    fun addTemporalMarker(time: (Double) -> Double, callback: MarkerCallback): T {
        temporalMarkers.add(TemporalMarker(time, callback))

        return this as T
    }

    /**
     * Adds a marker that will be triggered at the closest trajectory point to [point].
     */
    fun addSpatialMarker(point: Vector2d, callback: MarkerCallback): T {
        spatialMarkers.add(SpatialMarker(point, callback))

        return this as T
    }

    /**
     * Adds a marker at the current position of the trajectory.
     */
    fun addDisplacementMarker(callback: MarkerCallback) =
        addDisplacementMarker(pathBuilder.build().length(), callback)

    /**
     * Adds a marker to the trajectory at [displacement].
     */
    fun addDisplacementMarker(displacement: Double, callback: MarkerCallback) =
        addDisplacementMarker(0.0, displacement, callback)

    /**
     * Adds a marker to the trajectory at [scale] * path length + [offset].
     */
    fun addDisplacementMarker(scale: Double, offset: Double, callback: MarkerCallback) =
        addDisplacementMarker({ scale * it + offset }, callback)

    /**
     * Adds a marker to the trajectory at [displacement] evaluated with path length.
     */
    fun addDisplacementMarker(displacement: (Double) -> Double, callback: MarkerCallback): T {
        displacementMarkers.add(DisplacementMarker(displacement, callback))

        return this as T
    }

    /**
     * Constructs the [Trajectory] instance.
     */
    fun build(): Trajectory {
        pushPath()
        return TrajectoryGenerator.generateTrajectory(
            segments, temporalMarkers, displacementMarkers, spatialMarkers
        )
    }

    protected abstract fun makePathSegment(path: Path): PathTrajectorySegment

    protected abstract fun makeTurnSegment(pose: Pose2d, angle: Double): TurnSegment
}
