package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.LineSegment
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathSegment
import com.amarcolini.joos.path.heading.LinearHeading
import com.amarcolini.joos.path.heading.LinearInterpolator
import com.amarcolini.joos.profile.MotionProfile
import com.amarcolini.joos.util.epsilonEquals
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmField

/**
 * Trajectory composed of a list of trajectory segments.
 *
 * @param segments list of trajectory segments
 * @see TrajectorySegment
 */
@JsExport
class Trajectory(
    @JvmField val segments: List<TrajectorySegment>
) {
    /**
     * The path representing this trajectory, excluding turn and wait segments.
     */
    val path: Path

    init {
        if (segments.isEmpty()) throw IllegalArgumentException("A Trajectory cannot be initialized without segments.")
        val pathSegments = mutableListOf<PathSegment>()
        for (segment in segments) {
            if (segment is PathTrajectorySegment) pathSegments += segment.path.segments
        }
        if (pathSegments.isEmpty()) {
            val start = start()
            val end = end()
            pathSegments += PathSegment(
                LineSegment(start.vec(), end.vec()),
                LinearInterpolator(start.heading, start.heading angleTo end.heading)
            )
        }
        path = Path(pathSegments)
    }

    /**
     * @param segment single trajectory segment
     */
    @JsName("fromSingle")
    constructor(segment: TrajectorySegment) : this(listOf(segment))

    @JsName("fromPath")
    constructor(
        path: Path,
        profile: MotionProfile
    ) : this(PathTrajectorySegment(path, profile))

    /**
     * Returns the length of the trajectory.
     */
    fun length() = segments.sumOf { it.length() }

    /**
     * Returns the duration of the trajectory.
     */
    fun duration() = segments.sumOf { it.duration() }

    /**
     * Returns a pair containing the [TrajectorySegment] at [t] seconds along the path and the duration along that segment.
     */
    fun segment(t: Double): Pair<TrajectorySegment, Double> {
        if (t <= 0.0) {
            return segments.first() to 0.0
        }
        var remainingDuration = t
        for (segment in segments) {
            if (remainingDuration <= segment.duration()) {
                return segment to remainingDuration
            }
            remainingDuration -= segment.duration()
        }
        return segments.last() to segments.last().duration()
    }

    /**
     * Returns the pose [t] seconds into the trajectory.
     */
    operator fun get(t: Double): Pose2d {
        val (segment, remainingDuration) = segment(t)
        return segment[remainingDuration]
    }

    /**
     * Returns the distance traveled [t] seconds into the trajectory.
     */
    fun distance(t: Double): Double {
        if (t <= 0.0) {
            return 0.0
        }
        var distance = 0.0
        var remainingDuration = t
        for (segment in segments) {
            if (remainingDuration <= segment.duration()) {
                return distance + segment.distance(remainingDuration)
            }
            remainingDuration -= segment.duration()
            distance += segment.length()
        }
        return distance
    }

    /**
     * Returns the time in seconds where the trajectory has traveled [s] distance. Only
     * works if distance is always increasing.
     */
    fun reparam(s: Double): Double {
        var tLo = 0.0
        var tHi = duration()
        for (i in 1..50) {
            val tMid = 0.5 * (tLo + tHi)
            if (distance(tMid) > s) {
                tHi = tMid
            } else {
                tLo = tMid
            }
            if (tLo epsilonEquals tHi) break
        }
        return 0.5 * (tLo + tHi)
    }

    /**
     * Returns the pose derivative [t] seconds into the trajectory.
     */
    fun deriv(t: Double): Pose2d {
        val (segment, remainingDuration) = segment(t)
        return segment.deriv(remainingDuration)
    }

    /**
     * Returns the pose second derivative [t] seconds into the trajectory.
     */
    fun secondDeriv(t: Double): Pose2d {
        val (segment, remainingDuration) = segment(t)
        return segment.secondDeriv(remainingDuration)
    }

    /**
     * Returns the pose velocity [t] seconds into the trajectory.
     */
    fun velocity(t: Double): Pose2d {
        val (segment, remainingDuration) = segment(t)
        return segment.velocity(remainingDuration)
    }

    /**
     * Returns the pose acceleration [t] seconds into the trajectory.
     */
    fun acceleration(t: Double): Pose2d {
        val (segment, remainingDuration) = segment(t)
        return segment.acceleration(remainingDuration)
    }

    /**
     * Returns the start pose.
     */
    fun start() = segments.first().start()

    /**
     * Returns the end pose.
     */
    fun end() = segments.last().end()
}