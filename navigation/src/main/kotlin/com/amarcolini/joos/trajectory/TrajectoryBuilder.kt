package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import com.amarcolini.joos.trajectory.constraints.TrajectoryAccelerationConstraint
import com.amarcolini.joos.trajectory.constraints.TrajectoryVelocityConstraint
import com.amarcolini.joos.util.Angle
import kotlin.math.PI

/**
 * Builder for trajectories with *dynamic* constraints.
 */
class TrajectoryBuilder private constructor(
    startPose: Pose2d,
    startDeriv: Pose2d,
    startSecondDeriv: Pose2d,
    private val baseVelConstraint: TrajectoryVelocityConstraint,
    private val baseAccelConstraint: TrajectoryAccelerationConstraint,
    private val baseAngVel: Double,
    private val baseAngAccel: Double,
    private val baseAngJerk: Double,
    start: MotionState,
    private val resolution: Double
) : BaseTrajectoryBuilder<TrajectoryBuilder>(startPose, startDeriv, startSecondDeriv) {
    /**
     * Creates a builder from a start pose and tangent. This is the recommended constructor for creating
     * trajectories from rest.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d,
        startTangent: Double = startPose.heading,
        baseVelConstraint: TrajectoryVelocityConstraint,
        baseAccelConstraint: TrajectoryAccelerationConstraint,
        baseAngVel: Double,
        baseAngAccel: Double,
        baseAngJerk: Double = 0.0,
        resolution: Double = 0.25
    ) : this(
        startPose,
        Pose2d(Angle.vec(Math.toRadians(startTangent)), 0.0),
        Pose2d(),
        baseVelConstraint,
        baseAccelConstraint,
        baseAngVel,
        baseAngAccel,
        baseAngJerk,
        MotionState(0.0, 0.0, 0.0),
        resolution
    )

    /**
     * Creates a builder from a start pose and tangent. This is the recommended constructor for creating
     * trajectories from rest.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d,
        startTangent: Double = startPose.heading,
        constraints: TrajectoryConstraints,
        resolution: Double = 0.25
    ) : this(
        startPose,
        startTangent,
        constraints.velConstraint,
        constraints.accelConstraint,
        Math.toRadians(constraints.maxAngVel),
        Math.toRadians(constraints.maxAngAccel),
        Math.toRadians(constraints.maxAngJerk),
        resolution
    )

    /**
     * Create a builder from a start pose with a reversed tangent. This constructor is used to execute trajectories
     * backwards.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d,
        reversed: Boolean,
        baseVelConstraint: TrajectoryVelocityConstraint,
        baseAccelConstraint: TrajectoryAccelerationConstraint,
        baseAngVel: Double,
        baseAngAccel: Double,
        baseAngJerk: Double = 0.0,
        resolution: Double = 0.25
    ) : this(
        startPose,
        Angle.norm(startPose.heading + if (reversed) PI else 0.0),
        baseVelConstraint,
        baseAccelConstraint,
        baseAngVel,
        baseAngAccel,
        baseAngJerk,
        resolution
    )

    /**
     * Create a builder from a start pose with a reversed tangent. This constructor is used to execute trajectories
     * backwards.
     */
    @JvmOverloads
    constructor(
        startPose: Pose2d,
        reversed: Boolean,
        constraints: TrajectoryConstraints,
        resolution: Double = 0.25
    ) : this(
        startPose,
        reversed,
        constraints.velConstraint,
        constraints.accelConstraint,
        Math.toRadians(constraints.maxAngVel),
        Math.toRadians(constraints.maxAngAccel),
        Math.toRadians(constraints.maxAngJerk),
        resolution
    )

    /**
     * Create a builder from an active trajectory. This is useful for interrupting a live trajectory and smoothly
     * transitioning to a new one.
     */
    @JvmOverloads
    constructor(
        trajectory: Trajectory,
        t: Double,
        baseVelConstraint: TrajectoryVelocityConstraint,
        baseAccelConstraint: TrajectoryAccelerationConstraint,
        baseAngVel: Double,
        baseAngAccel: Double,
        baseAngJerk: Double = 0.0,
        resolution: Double = 0.25
    ) : this(
        trajectory[t],
        trajectory.deriv(t),
        trajectory.secondDeriv(t),
        baseVelConstraint,
        baseAccelConstraint,
        baseAngVel,
        baseAngAccel,
        baseAngJerk,
        //TODO: fix splicing
        MotionState(
            0.0,
            (trajectory.velocity(t).vec() / trajectory.velocity(t).vec().norm()).angle(),
            (trajectory.acceleration(t).vec() / trajectory.acceleration(t).vec().norm()).angle()
        ),
        resolution
    )

    private var currentVelConstraint = baseVelConstraint
    private var currentAccelConstraint = baseAccelConstraint

    private var currentAngVel = baseAngVel
    private var currentAngAccel = baseAngAccel
    private var currentAngJerk = baseAngJerk
    private var currentMotionState = start

    private fun addSegment(
        add: () -> Unit,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint
    ): TrajectoryBuilder {
        setConstraints(velConstraintOverride, accelConstraintOverride)
        addPathSegment(add)
        pushPath()
        resetConstraints()
        return this
    }

    /**
     * Sets the constraints for the following segments.
     */
    @JvmOverloads
    fun setConstraints(
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ): TrajectoryBuilder {
        pushPath()
        currentVelConstraint = velConstraintOverride
        currentAccelConstraint = accelConstraintOverride
        return this
    }

    /**
     * Sets the constraints for the following segments using the provided [config].
     * Sets both the regular and angular constraints.
     */
    fun setConstraints(config: TrajectoryConstraints): TrajectoryBuilder {
        setConstraints(config.velConstraint, config.accelConstraint)
        setAngularConstraints(config.maxAngVel, config.maxAngAccel, config.maxAngJerk)
        return this
    }

    /**
     * Resets the constraints to the default constructor-provided values.
     */
    fun resetConstraints(): TrajectoryBuilder {
        currentVelConstraint = baseVelConstraint
        currentAccelConstraint = baseAccelConstraint
        return this
    }

    /**
     * Sets the angular constraints for the following turn segments.
     */
    @JvmOverloads
    fun setAngularConstraints(
        angVelOverride: Double,
        angAccelOverride: Double = Math.toDegrees(baseAngAccel),
        angJerkOverride: Double = Math.toDegrees(baseAngJerk)
    ): TrajectoryBuilder {
        currentAngVel = Math.toRadians(angVelOverride)
        currentAngAccel = Math.toRadians(angAccelOverride)
        currentAngJerk = Math.toRadians(angJerkOverride)
        return this
    }

    /**
     * Resets the angular constraints to the default constructor-provided values.
     */
    fun resetAngularConstraints(): TrajectoryBuilder {
        currentAngVel = baseAngVel
        currentAngAccel = baseAngAccel
        currentAngJerk = baseAngJerk
        return this
    }

    /**
     * Resets all constraints.
     */
    fun resetAllConstraints(): TrajectoryBuilder {
        resetConstraints()
        resetAngularConstraints()
        return this
    }

    /**
     * Adds a turn segment that turns [angle] degrees.
     *
     * @param angle angle to turn (in degrees)
     * @param constraintsOverride segment-specific constraints
     */
    fun turn(
        angle: Double,
        angVelOverride: Double,
        angAccelOverride: Double = Math.toDegrees(baseAngAccel),
        angJerkOverride: Double = Math.toDegrees(baseAngJerk)
    ): TrajectoryBuilder {
        setAngularConstraints(angVelOverride, angAccelOverride, angJerkOverride)
        turn(angle)
        resetAngularConstraints()
        return this
    }

    /**
     * Adds a line segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param constraintsOverride segment-specific constraints
     */
    fun lineTo(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ lineTo(endPosition) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param constraintsOverride segment-specific constraints
     */
    fun lineToConstantHeading(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment(
            { lineToConstantHeading(endPosition) },
            velConstraintOverride,
            accelConstraintOverride
        )

    /**
     * Adds a line segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param constraintsOverride segment-specific constraints
     */
    fun lineToLinearHeading(
        endPose: Pose2d,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ lineToLinearHeading(endPose) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param constraintsOverride segment-specific constraints
     */
    fun lineToSplineHeading(
        endPose: Pose2d,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ lineToSplineHeading(endPose) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a strafe path segment.
     *
     * @param endPosition end position
     * @param constraintsOverride segment-specific constraints
     */
    fun strafeTo(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ strafeTo(endPosition) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line straight forward.
     *
     * @param distance distance to travel forward
     * @param constraintsOverride segment-specific constraints
     */
    fun forward(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ forward(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line straight backward.
     *
     * @param distance distance to travel backward
     * @param constraintsOverride segment-specific constraints
     */
    fun back(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ back(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a segment that strafes left in the robot reference frame.
     *
     * @param distance distance to strafe left
     * @param constraintsOverride segment-specific constraints
     */
    fun strafeLeft(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ strafeLeft(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a segment that strafes right in the robot reference frame.
     *
     * @param distance distance to strafe right
     * @param constraintsOverride segment-specific constraints
     */
    fun strafeRight(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment({ strafeRight(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a spline segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent, in degrees
     * @param constraintsOverride segment-specific constraints
     */
    fun splineTo(
        endPosition: Vector2d,
        endTangent: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment(
            { splineTo(endPosition, endTangent) },
            velConstraintOverride,
            accelConstraintOverride
        )

    /**
     * Adds a spline segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent, in degrees
     * @param constraintsOverride segment-specific constraints
     */
    fun splineToConstantHeading(
        endPosition: Vector2d,
        endTangent: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment(
            { splineToConstantHeading(endPosition, endTangent) },
            velConstraintOverride,
            accelConstraintOverride
        )

    /**
     * Adds a spline segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent, in degrees
     * @param constraintsOverride segment-specific constraints
     */
    fun splineToLinearHeading(
        endPose: Pose2d,
        endTangent: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment(
            { splineToLinearHeading(endPose, endTangent) },
            velConstraintOverride,
            accelConstraintOverride
        )

    /**
     * Adds a spline segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param constraintsOverride segment-specific constraints
     */
    fun splineToSplineHeading(
        endPose: Pose2d,
        endTangent: Double,
        velConstraintOverride: TrajectoryVelocityConstraint = baseVelConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) =
        addSegment(
            { splineToSplineHeading(endPose, endTangent) },
            velConstraintOverride,
            accelConstraintOverride
        )

    override fun makePathSegment(path: Path): PathTrajectorySegment {
        return TrajectoryGenerator.generatePathTrajectorySegment(
            path,
            currentVelConstraint,
            currentAccelConstraint,
            currentMotionState,
            resolution = resolution
        )
    }

    override fun makeTurnSegment(pose: Pose2d, angle: Double) =
        TrajectoryGenerator.generateTurnSegment(
            pose,
            angle,
            currentAngVel,
            currentAngAccel,
            currentAngJerk,
            true
        )
}
