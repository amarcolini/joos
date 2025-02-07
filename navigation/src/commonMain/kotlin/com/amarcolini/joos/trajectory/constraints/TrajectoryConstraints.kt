package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.util.deg
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.min

/**
 * Configuration describing constraints and other robot-specific parameters.
 */
@JsExport
interface TrajectoryConstraints {
    @Transient
    val velConstraint: TrajectoryVelocityConstraint

    @Transient
    val accelConstraint: TrajectoryAccelerationConstraint

    @Transient
    val decelConstraint: TrajectoryAccelerationConstraint

    val maxAngVel: Angle
    val maxAngAccel: Angle
    val maxAngJerk: Angle
}

private class DriveTrajectoryConstraints(
    maxVel: Double,
    maxAccel: Double,
    maxDecel: Double,
    override val maxAngVel: Angle,
    override val maxAngAccel: Angle,
    override val maxAngJerk: Angle,
    additionalVelConstraint: TrajectoryVelocityConstraint?
) : TrajectoryConstraints {

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOfNotNull(
            additionalVelConstraint,
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, min(maxAccel, maxDecel))
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )

    @Transient
    override val decelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxDecel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
data class MecanumConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val wheelBase: Double,
    @JvmField val lateralMultiplier: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    @JvmField val maxDecel: Double = maxAccel,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints by DriveTrajectoryConstraints(
    maxVel, maxAccel, maxDecel,
    maxAngVel, maxAngAccel, maxAngJerk,
    DriveVelocityConstraint.forMecanum(maxWheelVel, trackWidth, wheelBase, lateralMultiplier)
)

@Serializable
@JsExport
data class GenericConstraints @JvmOverloads constructor(
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    @JvmField val maxDecel: Double = maxAccel,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints by DriveTrajectoryConstraints(
    maxVel, maxAccel, maxDecel,
    maxAngVel, maxAngAccel, maxAngJerk,
    null
)

@Serializable
@JsExport
data class TankConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    @JvmField val maxDecel: Double = maxAccel,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints by DriveTrajectoryConstraints(
    maxVel, maxAccel, maxDecel,
    maxAngVel, maxAngAccel, maxAngJerk,
    DriveVelocityConstraint.forTank(maxWheelVel, trackWidth)
)

@Serializable
data class SwerveConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val wheelBase: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    @JvmField val maxDecel: Double = maxAccel,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints by DriveTrajectoryConstraints(
    maxVel, maxAccel, maxDecel,
    maxAngVel, maxAngAccel, maxAngJerk,
    DriveVelocityConstraint.forSwerve(maxWheelVel, SwerveKinematics.getModulePositions(trackWidth, wheelBase))
)

@Serializable
data class DiffSwerveConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    @JvmField val maxDecel: Double = maxAccel,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints by DriveTrajectoryConstraints(
    maxVel, maxAccel, maxDecel,
    maxAngVel, maxAngAccel, maxAngJerk,
    DriveVelocityConstraint.forSwerve(maxWheelVel, SwerveKinematics.getModulePositions(trackWidth))
)