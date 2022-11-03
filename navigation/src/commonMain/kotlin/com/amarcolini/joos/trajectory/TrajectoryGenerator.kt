package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.profile.MotionProfile
import com.amarcolini.joos.profile.MotionProfileGenerator
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.trajectory.constraints.TrajectoryAccelerationConstraint
import com.amarcolini.joos.trajectory.constraints.TrajectoryVelocityConstraint
import com.amarcolini.joos.util.epsilonEquals
import kotlin.jvm.JvmOverloads

/**
 * Trajectory generator for creating trajectories with dynamic and static constraints from paths.
 */
object TrajectoryGenerator {

    fun generateProfile(
        path: Path,
        velocityConstraint: TrajectoryVelocityConstraint,
        accelerationConstraint: TrajectoryAccelerationConstraint,
        start: MotionState,
        goal: MotionState,
        resolution: Double
    ): MotionProfile {
        return MotionProfileGenerator.generateMotionProfile(
            start,
            goal,
            { s ->
                val t = path.reparam(s)
                velocityConstraint[
                        s,
                        path[s, t],
                        path.deriv(s, t),
                        Pose2d()
                ]
            },
            { lastS, s, lastVel, dx ->
                accelerationConstraint[lastS, s, lastVel, dx, path]
            },
            resolution
        )
    }

    fun generateSimpleProfile(
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double,
        start: MotionState,
        goal: MotionState,
        overshoot: Boolean = false
    ): MotionProfile {
        return MotionProfileGenerator.generateSimpleMotionProfile(
            start,
            goal,
            maxProfileVel,
            maxProfileAccel,
            maxProfileJerk,
            overshoot
        )
    }

    // note: this assumes that the profile position is monotonic increasing
    fun displacementToTime(trajectory: Trajectory, s: Double): Double {
        var tLo = 0.0
        var tHi = trajectory.duration()
        while (!(tLo epsilonEquals tHi)) {
            val tMid = 0.5 * (tLo + tHi)
            if (trajectory.distance(tMid) > s) {
                tHi = tMid
            } else {
                tLo = tMid
            }
        }
        return 0.5 * (tLo + tHi)
    }

    fun pointToTime(trajectory: Trajectory, point: Vector2d) =
        displacementToTime(trajectory, trajectory.path.project(point))

    fun convertMarkers(
        segments: List<TrajectorySegment>,
        temporalMarkers: List<TemporalMarker>,
        displacementMarkers: List<DisplacementMarker>,
        spatialMarkers: List<SpatialMarker>
    ): List<TrajectoryMarker> {
        val trajectory = Trajectory(segments)
        return temporalMarkers.map { (producer, callback) ->
            TrajectoryMarker(producer.produce(trajectory.duration()), callback)
        } +
                displacementMarkers.map { (producer, callback) ->
                    TrajectoryMarker(
                        displacementToTime(trajectory, producer.produce(trajectory.length())),
                        callback
                    )
                } +
                spatialMarkers.map { (point, callback) ->
                    TrajectoryMarker(pointToTime(trajectory, point), callback)
                }
    }

    /**
     * Generates a trajectory.
     */
    fun generateTrajectory(
        segments: List<TrajectorySegment>,
        temporalMarkers: List<TemporalMarker>,
        displacementMarkers: List<DisplacementMarker>,
        spatialMarkers: List<SpatialMarker>
    ) = Trajectory(
        segments,
        convertMarkers(segments, temporalMarkers, displacementMarkers, spatialMarkers)
    )

    /**
     * Generates a dynamic constraint path trajectory segment.
     * @param path path
     * @param velocityConstraint trajectory velocity constraints
     * @param accelerationConstraint trajectory acceleration constraints
     * @param start start motion state
     * @param goal goal motion state
     * @param resolution dynamic profile sampling resolution
     */
    @JvmOverloads
    fun generatePathTrajectorySegment(
        path: Path,
        velocityConstraint: TrajectoryVelocityConstraint,
        accelerationConstraint: TrajectoryAccelerationConstraint,
        start: MotionState = MotionState(0.0, 0.0, 0.0),
        goal: MotionState = MotionState(path.length(), 0.0, 0.0),
        resolution: Double = 0.25
    ): PathTrajectorySegment {
        val profile = generateProfile(
            path,
            velocityConstraint,
            accelerationConstraint,
            start,
            goal,
            resolution
        )
        return PathTrajectorySegment(path, profile)
    }

    /**
     * Generates a simple constraint path trajectory segment.
     * @param path path
     * @param maxProfileVel maximum velocity
     * @param maxProfileAccel maximum acceleration
     * @param maxProfileJerk maximum jerk
     * @param start start motion state
     * @param goal goal motion state
     */
    @JvmOverloads
    fun generateSimplePathTrajectorySegment(
        path: Path,
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double,
        start: MotionState = MotionState(0.0, 0.0, 0.0, 0.0),
        goal: MotionState = MotionState(path.length(), 0.0, 0.0, 0.0),
    ): PathTrajectorySegment {
        val profile =
            generateSimpleProfile(maxProfileVel, maxProfileAccel, maxProfileJerk, start, goal)
        return PathTrajectorySegment(path, profile)
    }

    /**
     * Generates a turn segment.
     * @param pose pose to turn on
     * @param angle angle to turn
     * @param maxAngVel maximum angular velocity
     * @param maxAngAccel maximum angular acceleration
     * @param maxAngJerk maximum angular jerk
     */
    fun generateTurnSegment(
        pose: Pose2d,
        angle: Angle,
        maxAngVel: Angle,
        maxAngAccel: Angle,
        maxAngJerk: Angle,
        overshoot: Boolean = false
    ): TurnSegment {
        val profile =
            generateSimpleProfile(
                maxAngVel.radians, maxAngAccel.radians, maxAngJerk.radians,
                MotionState(0.0, 0.0, 0.0),
                MotionState(angle.radians, 0.0, 0.0),
                overshoot
            )
        return TurnSegment(pose, profile)
    }
}