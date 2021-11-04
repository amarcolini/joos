package com.griffinrobotics.lib.trajectory

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.path.Path
import com.griffinrobotics.lib.profile.MotionState
import com.griffinrobotics.lib.util.Angle
import kotlin.math.PI

private fun zeroPosition(state: MotionState) = MotionState(0.0, state.v, state.a, state.j)

/**
 * Builder for trajectories with *static* constraints.
 */
class SimpleTrajectoryBuilder private constructor(
    startPose: Pose2d,
    startDeriv: Pose2d,
    startSecondDeriv: Pose2d,
    private val maxProfileVel: Double,
    private val maxProfileAccel: Double,
    private val maxProfileJerk: Double,
    private val maxAngVel: Double,
    private val maxAngAccel: Double,
    private val maxAngJerk: Double,
    private val start: MotionState
) : BaseTrajectoryBuilder<SimpleTrajectoryBuilder>(startPose, startDeriv, startSecondDeriv) {
    /**
     * Create a builder from a start pose and motion state. This is the recommended constructor for creating
     * trajectories from rest.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d,
        startTangent: Double = startPose.heading,
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double = 0.0,
        maxAngVel: Double,
        maxAngAccel: Double,
        maxAngJerk: Double = 0.0
    ) : this(
        startPose,
        Pose2d(Angle.vec(startTangent), 0.0),
        Pose2d(),
        maxProfileVel,
        maxProfileAccel,
        maxProfileJerk,
        maxAngVel,
        maxAngAccel,
        maxAngJerk,
        MotionState(0.0, 0.0, 0.0)
    )

    constructor(
        startPose: Pose2d,
        reversed: Boolean,
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double = 0.0,
        maxAngVel: Double,
        maxAngAccel: Double,
        maxAngJerk: Double = 0.0
    ) : this(
        startPose,
        Angle.norm(startPose.heading + if (reversed) PI else 0.0),
        maxProfileVel,
        maxProfileAccel,
        maxProfileJerk,
        maxAngVel,
        maxAngAccel,
        maxAngJerk,
    )

    /**
     * Create a builder from an active trajectory. This is useful for interrupting a live trajectory and smoothly
     * transitioning to a new one.
     */
    @JvmOverloads
    constructor(
        trajectory: Trajectory,
        t: Double,
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double = 0.0,
        maxAngVel: Double,
        maxAngAccel: Double,
        maxAngJerk: Double = 0.0
    ) : this(
        trajectory[t],
        trajectory.deriv(t),
        trajectory.secondDeriv(t),
        maxProfileVel,
        maxProfileAccel,
        maxProfileJerk,
        maxAngVel,
        maxAngAccel,
        maxAngJerk,
        //TODO: fix splicing
        MotionState(
            0.0,
            trajectory.velocity(0.0).vec().norm(),
            trajectory.acceleration(0.0).vec().norm()
        )
    )

    override fun makePathSegment(path: Path) =
        TrajectoryGenerator.generateSimplePathTrajectorySegment(
            path,
            maxProfileVel,
            maxProfileAccel,
            maxProfileJerk,
        )

    override fun makeTurnSegment(pose: Pose2d, angle: Double) =
        TrajectoryGenerator.generateTurnSegment(
            pose,
            angle,
            maxAngVel,
            maxAngAccel,
            maxAngJerk,
            true
        )
}
