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
import com.amarcolini.joos.trajectory.constraints.UnsatisfiableConstraint
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
            { s, ds ->
                val t = path.reparam(s)
                val lastT = path.reparam(s - ds)
                velocityConstraint[
                        path[s, t],
                        path.deriv(s, t),
                        path.deriv(s - ds, lastT),
                        ds,
                        Pose2d()
                ]
            },
            { s, ds, lastVel ->
                val t = path.reparam(s)
                val lastT = path.reparam(s - ds)
                val result =
                    accelerationConstraint[path.deriv(s, t), path.deriv(s - ds, lastT), ds, lastVel].maxOf { it.second }
                if (result < 0) throw UnsatisfiableConstraint() else result
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