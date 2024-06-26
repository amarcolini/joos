package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.trajectory.constraints.UnsatisfiableConstraint
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.epsilonEquals
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads

private fun zeroPosition(state: MotionState) = MotionState(0.0, state.v, state.a, state.j)

/**
 * Builder for trajectories with *static* constraints.
 */
class SimpleTrajectoryBuilder(
    startPose: Pose2d,
    startDeriv: Pose2d,
    startSecondDeriv: Pose2d,
    private val maxProfileVel: Double,
    private val maxProfileAccel: Double,
    private val maxProfileJerk: Double,
    private val maxAngVel: Angle,
    private val maxAngAccel: Angle,
    private val maxAngJerk: Angle,
    private val start: MotionState
) : BaseTrajectoryBuilder<SimpleTrajectoryBuilder>(startPose, startDeriv, startSecondDeriv) {
    /**
     * Create a builder from a start pose and motion state. This is the recommended constructor for creating
     * trajectories from rest.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d = Pose2d(),
        startTangent: Angle = startPose.heading,
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double = 0.0,
        maxAngVel: Angle,
        maxAngAccel: Angle,
        maxAngJerk: Angle = 0.rad
    ) : this(
        startPose,
        Pose2d(startTangent.vec(), 0.rad),
        Pose2d(),
        maxProfileVel,
        maxProfileAccel,
        maxProfileJerk,
        maxAngVel,
        maxAngAccel,
        maxAngJerk,
        MotionState(0.0, 0.0, 0.0)
    )

    /**
     * Create a builder from a start pose with a reversed tangent. This constructor is used to execute trajectories
     * backwards.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d = Pose2d(),
        reversed: Boolean,
        maxProfileVel: Double,
        maxProfileAccel: Double,
        maxProfileJerk: Double = 0.0,
        maxAngVel: Angle,
        maxAngAccel: Angle,
        maxAngJerk: Angle = 0.deg
    ) : this(
        startPose,
        (startPose.heading + (if (reversed) 180.deg else 0.deg)).norm(),
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
        maxAngVel: Angle,
        maxAngAccel: Angle,
        maxAngJerk: Angle = 0.deg
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
            trajectory.velocity(t).vec().norm(),
            trajectory.acceleration(t).vec().norm()
        ),
    )

    private var currentMotionState = start

    override fun makePathSegment(path: Path) =
        TrajectoryGenerator.generateSimplePathTrajectorySegment(
            path,
            maxProfileVel,
            maxProfileAccel,
            maxProfileJerk,
            currentMotionState
        ).also { currentMotionState = it.profile.end() }

    override fun makeTurnSegment(pose: Pose2d, angle: Angle): TurnSegment {
        if (!(currentMotionState.v epsilonEquals 0.0 && currentMotionState.a epsilonEquals 0.0 && currentMotionState.j epsilonEquals 0.0))
            throw IllegalStateException("Cannot make turn segment unless robot is at rest.")
        return TrajectoryGenerator.generateTurnSegment(
            pose,
            angle,
            maxAngVel,
            maxAngAccel,
            maxAngJerk,
            true
        )
    }
}