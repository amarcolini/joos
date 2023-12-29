package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.heading.HeadingInterpolation
import com.amarcolini.joos.path.heading.TangentHeading
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.trajectory.constraints.*
import com.amarcolini.joos.util.*
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Builder for trajectories with *dynamic* constraints.
 */
@JsExport
open class TrajectoryBuilder protected constructor(
    startPose: Pose2d,
    startDeriv: Pose2d,
    startSecondDeriv: Pose2d,
    @JvmField val baseVelConstraint: TrajectoryVelocityConstraint,
    @JvmField val baseAccelConstraint: TrajectoryAccelerationConstraint,
    @JvmField val baseAngVel: Angle,
    @JvmField val baseAngAccel: Angle,
    @JvmField val baseAngJerk: Angle,
    start: MotionState,
    private val resolution: Double
) : BaseTrajectoryBuilder<TrajectoryBuilder>(startPose, startDeriv, startSecondDeriv) {
    /**
     * Creates a builder from a start pose and tangent. This is the recommended constructor for creating
     * trajectories from rest.
     */
    @JvmOverloads
    @JsName("create")
    constructor(
        startPose: Pose2d = Pose2d(),
        startTangent: Angle = startPose.heading,
        baseVelConstraint: TrajectoryVelocityConstraint,
        baseAccelConstraint: TrajectoryAccelerationConstraint,
        baseAngVel: Angle,
        baseAngAccel: Angle,
        baseAngJerk: Angle = 0.rad,
        resolution: Double = 0.25
    ) : this(
        startPose,
        Pose2d(startTangent.vec()),
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
    @JsName("fromConstraints")
    constructor(
        startPose: Pose2d = Pose2d(),
        startTangent: Angle = startPose.heading,
        constraints: TrajectoryConstraints,
        resolution: Double = 0.25
    ) : this(
        startPose,
        startTangent,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel,
        constraints.maxAngAccel,
        constraints.maxAngJerk,
        resolution
    )

    /**
     * Create a builder from a start pose with a reversed tangent. This constructor is used to execute trajectories
     * backwards.
     */
    @JvmOverloads
    @JsName("createReversed")
    constructor(
        startPose: Pose2d,
        reversed: Boolean,
        baseVelConstraint: TrajectoryVelocityConstraint,
        baseAccelConstraint: TrajectoryAccelerationConstraint,
        baseAngVel: Angle,
        baseAngAccel: Angle,
        baseAngJerk: Angle = 0.rad,
        resolution: Double = 0.25
    ) : this(
        startPose,
        (startPose.heading + (if (reversed) 180.deg else 0.deg)).norm(),
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
    @JsName("fromConstraintsReversed")
    constructor(
        startPose: Pose2d, reversed: Boolean, constraints: TrajectoryConstraints, resolution: Double = 0.25
    ) : this(
        startPose,
        reversed,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel,
        constraints.maxAngAccel,
        constraints.maxAngJerk,
        resolution
    )

    /**
     * Create a builder from an active trajectory. This is useful for interrupting a live trajectory and smoothly
     * transitioning to a new one.
     */
    @JvmOverloads
    @JsName("fromTrajectory")
    constructor(
        trajectory: Trajectory,
        t: Double,
        baseVelConstraint: TrajectoryVelocityConstraint,
        baseAccelConstraint: TrajectoryAccelerationConstraint,
        baseAngVel: Angle,
        baseAngAccel: Angle,
        baseAngJerk: Angle = 0.rad,
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
            0.0, trajectory.velocity(t).vec().norm(), trajectory.acceleration(t).vec().norm()
        ),
        resolution
    )

    /**
     * Create a builder from an active trajectory. This is useful for interrupting a live trajectory and smoothly
     * transitioning to a new one.
     */
    @JvmOverloads
    @JsName("fromConstraintsTrajectory")
    constructor(
        trajectory: Trajectory, t: Double, constraints: TrajectoryConstraints, resolution: Double = 0.25
    ) : this(
        trajectory,
        t,
        constraints.velConstraint,
        constraints.accelConstraint,
        constraints.maxAngVel,
        constraints.maxAngAccel,
        constraints.maxAngJerk,
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
     * Sets the constraints for the following path segments.
     */
    @JsName("setContraintsSeparate")
    fun setConstraints(
        velConstraintOverride: TrajectoryVelocityConstraint, accelConstraintOverride: TrajectoryAccelerationConstraint
    ): TrajectoryBuilder {
        pushPath()
        currentVelConstraint = velConstraintOverride
        currentAccelConstraint = accelConstraintOverride
        return this
    }

    /**
     * Sets the velocity constraints for the following path segments.
     */
    fun setVelocityConstraints(
        velConstraintOverride: TrajectoryVelocityConstraint,
    ): TrajectoryBuilder {
        pushPath()
        currentVelConstraint = velConstraintOverride
        return this
    }

    /**
     * Sets the acceleration constraints for the following path segments.
     */
    fun setAccelConstraints(
        accelConstraintOverride: TrajectoryAccelerationConstraint
    ): TrajectoryBuilder {
        pushPath()
        currentAccelConstraint = accelConstraintOverride
        return this
    }

    /**
     * Sets the constraints for the following segments using the provided [constraints].
     * Sets both the path and angular constraints.
     */
    fun setConstraints(constraints: TrajectoryConstraints): TrajectoryBuilder {
        setConstraints(constraints.velConstraint, constraints.accelConstraint)
        setAngularConstraints(constraints.maxAngVel, constraints.maxAngAccel, constraints.maxAngJerk)
        return this
    }

    /**
     * Adds the provided constraints for the following path segments.
     */
    @JsName("addConstraintsSeparate")
    fun addConstraints(
        velConstraint: TrajectoryVelocityConstraint, accelConstraint: TrajectoryAccelerationConstraint
    ): TrajectoryBuilder {
        pushPath()
        currentVelConstraint = MinVelocityConstraint(currentVelConstraint, velConstraint)
        currentAccelConstraint = MinAccelerationConstraint(currentAccelConstraint, accelConstraint)
        return this
    }

    /**
     * Adds the provided velocity constraints for the following path segments.
     */
    fun addVelocityConstraints(
        velConstraint: TrajectoryVelocityConstraint,
    ): TrajectoryBuilder {
        pushPath()
        currentVelConstraint = MinVelocityConstraint(currentVelConstraint, velConstraint)
        return this
    }

    /**
     * Adds the provided acceleration constraints for the following path segments.
     */
    fun addAccelConstraints(
        accelConstraint: TrajectoryAccelerationConstraint
    ): TrajectoryBuilder {
        pushPath()
        currentAccelConstraint = MinAccelerationConstraint(currentAccelConstraint, accelConstraint)
        return this
    }

    /**
     * Adds the provided constraints to the following path and turn segments.
     */
    fun addConstraints(constraints: TrajectoryConstraints): TrajectoryBuilder {
        setConstraints(constraints.velConstraint, constraints.accelConstraint)
        setAngularConstraints(constraints.maxAngVel, constraints.maxAngAccel, constraints.maxAngJerk)
        return this
    }

    /**
     * Resets the path constraints to the default constructor-provided values.
     */
    fun resetConstraints(): TrajectoryBuilder {
        currentVelConstraint
        currentAccelConstraint = baseAccelConstraint
        return this
    }

    /**
     * Sets the angular constraints for the following turn segments.
     */
    @JvmOverloads
    fun setAngularConstraints(
        angVelOverride: Angle, angAccelOverride: Angle = baseAngAccel, angJerkOverride: Angle = baseAngJerk
    ): TrajectoryBuilder {
        currentAngVel = angVelOverride
        currentAngAccel = angAccelOverride
        currentAngJerk = angJerkOverride
        return this
    }

    /**
     * Adds the provided angular constraints for the following turn segments.
     */
    @JvmOverloads
    fun addAngularConstraints(
        angVel: Angle, angAccel: Angle = Double.POSITIVE_INFINITY.rad, angJerk: Angle = Double.POSITIVE_INFINITY.rad
    ): TrajectoryBuilder {
        currentAngVel = min(currentAngVel, angVel)
        currentAngAccel = min(currentAngAccel, angAccel)
        currentAngJerk = min(currentAngJerk, angJerk)
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
     * Resets all constraints to the default constructor-provided values.
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
    @JvmOverloads
    @JsName("turnCustom")
    fun turn(
        angle: Angle,
        angVelOverride: Angle,
        angAccelOverride: Angle = baseAngAccel,
        angJerkOverride: Angle = baseAngJerk
    ): TrajectoryBuilder {
        setAngularConstraints(angVelOverride, angAccelOverride, angJerkOverride)
        turn(angle)
        resetAngularConstraints()
        return this
    }

    /**
     * Adds a line segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param headingInterpolation desired heading interpolation
     * @param constraintsOverride segment-specific constraints
     * @see HeadingInterpolation
     */
    @JvmOverloads
    @JsName("addLineCustom")
    fun addLine(
        endPosition: Vector2d,
        headingInterpolation: HeadingInterpolation = TangentHeading,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ addLine(endPosition, headingInterpolation) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("lineToCustom")
    fun lineTo(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ lineTo(endPosition) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("lineToConstantHeadingCustom")
    fun lineToConstantHeading(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment(
        { lineToConstantHeading(endPosition) }, velConstraintOverride, accelConstraintOverride
    )

    /**
     * Adds a line segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("lineToLinearHeadingCustom")
    fun lineToLinearHeading(
        endPose: Pose2d,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ lineToLinearHeading(endPose) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("lineToSplineHeadingCustom")
    fun lineToSplineHeading(
        endPose: Pose2d,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ lineToSplineHeading(endPose) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a strafe path segment.
     *
     * @param endPosition end position
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("strafeToCustom")
    fun strafeTo(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ strafeTo(endPosition) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line straight forward.
     *
     * @param distance distance to travel forward
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("forwardCustom")
    fun forward(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ forward(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a line straight backward.
     *
     * @param distance distance to travel backward
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("backCustom")
    fun back(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ back(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a segment that strafes left in the robot reference frame.
     *
     * @param distance distance to strafe left
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("StrafeLeftCustom")
    fun strafeLeft(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ strafeLeft(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a segment that strafes right in the robot reference frame.
     *
     * @param distance distance to strafe right
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("strafeRightCustom")
    fun strafeRight(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment({ strafeRight(distance) }, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a spline segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent (negative = default magnitude)
     * @param endTangentMag the magnitude of the end tangent (negative = default magnitude)
     * @param headingInterpolation the desired heading interpolation
     * @param constraintsOverride segment-specific constraints
     * @see HeadingInterpolation
     */
    @JvmOverloads
    @JsName("addSplineCustom")
    fun addSpline(
        endPosition: Vector2d,
        endTangent: Angle,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0,
        headingInterpolation: HeadingInterpolation = TangentHeading,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment(
        { addSpline(endPosition, endTangent, startTangentMag, endTangentMag, headingInterpolation) },
        velConstraintOverride,
        accelConstraintOverride
    )

    /**
     * Adds a spline segment with tangent heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("splineToCustom")
    fun splineTo(
        endPosition: Vector2d,
        endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment(
        { splineTo(endPosition, endTangent) }, velConstraintOverride, accelConstraintOverride
    )

    /**
     * Adds a spline segment with constant heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("splineToConstantHeadingCustom")
    fun splineToConstantHeading(
        endPosition: Vector2d,
        endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment(
        { splineToConstantHeading(endPosition, endTangent) }, velConstraintOverride, accelConstraintOverride
    )

    /**
     * Adds a spline segment with linear heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("splineToLinearHeadingCustom")
    fun splineToLinearHeading(
        endPose: Pose2d,
        endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment(
        { splineToLinearHeading(endPose, endTangent) }, velConstraintOverride, accelConstraintOverride
    )

    /**
     * Adds a spline segment with spline heading interpolation.
     *
     * @param endPose end pose
     * @param endTangent end tangent
     * @param constraintsOverride segment-specific constraints
     */
    @JvmOverloads
    @JsName("splineToSplineHeadingCustom")
    fun splineToSplineHeading(
        endPose: Pose2d,
        endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint,
        accelConstraintOverride: TrajectoryAccelerationConstraint = baseAccelConstraint
    ) = addSegment(
        { splineToSplineHeading(endPose, endTangent) }, velConstraintOverride, accelConstraintOverride
    )

    override fun makePathSegment(path: Path): PathTrajectorySegment {
        return TrajectoryGenerator.generatePathTrajectorySegment(
            path, currentVelConstraint, currentAccelConstraint, currentMotionState, resolution = resolution
        )
    }

    override fun makeTurnSegment(pose: Pose2d, angle: Angle): TurnSegment = TrajectoryGenerator.generateTurnSegment(
        pose, angle, currentAngVel, currentAngAccel, currentAngJerk, true
    )
}