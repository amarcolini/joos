package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.util.deg
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Configuration describing constraints and other robot-specific parameters.
 */
@JsExport
interface TrajectoryConstraints {
    @Transient
    val velConstraint: TrajectoryVelocityConstraint

    @Transient
    val accelConstraint: TrajectoryAccelerationConstraint

    val maxAngVel: Angle
    val maxAngAccel: Angle
    val maxAngJerk: Angle
}

@Serializable
data class MecanumConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val wheelBase: Double,
    @JvmField val lateralMultiplier: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            DriveVelocityConstraint.forMecanum(maxWheelVel, trackWidth, wheelBase, lateralMultiplier),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
@JsExport
data class GenericConstraints @JvmOverloads constructor(
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TranslationalVelocityConstraint(maxVel),
            AngularVelocityConstraint(maxAngVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
@JsExport
data class TankConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            DriveVelocityConstraint.forTank(maxWheelVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
data class SwerveConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val wheelBase: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            DriveVelocityConstraint.forSwerve(maxWheelVel, SwerveKinematics.getModulePositions(trackWidth, wheelBase)),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
data class DiffSwerveConstraints @JvmOverloads constructor(
    @JvmField val maxGearVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {

    override val velConstraint: TrajectoryVelocityConstraint = MinVelocityConstraint(
        listOf(
            DriveVelocityConstraint.forSwerve(maxGearVel, SwerveKinematics.getModulePositions(trackWidth)),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    override val accelConstraint: TrajectoryAccelerationConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}