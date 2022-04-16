package com.amarcolini.joos.trajectory.config

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.*
import com.amarcolini.joos.trajectory.constraints.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

/**
 * Configuration describing constraints and other robot-specific parameters.
 */
@JsonIgnoreProperties("velConstraint", "accelConstraint", "type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(GenericConstraints::class),
    JsonSubTypes.Type(MecanumConstraints::class),
    JsonSubTypes.Type(TankConstraints::class),
    JsonSubTypes.Type(SwerveConstraints::class),
    JsonSubTypes.Type(DiffSwerveConstraints::class)
)
interface TrajectoryConstraints {
    /**
     * Type of drivetrain.
     */
    enum class DriveType(val clazz: KClass<out TrajectoryConstraints>) {
        GENERIC(GenericConstraints::class),
        MECANUM(MecanumConstraints::class),
        SWERVE(SwerveConstraints::class),
        DIFF_SWERVE(DiffSwerveConstraints::class),
        TANK(TankConstraints::class)
    }

    val type: DriveType

    val velConstraint: TrajectoryVelocityConstraint
    val accelConstraint: TrajectoryAccelerationConstraint

    val maxAngVel: Angle
    val maxAngAccel: Angle
    val maxAngJerk: Angle
}

data class MecanumConstraints @JvmOverloads constructor(
    val maxWheelVel: Double = 100.0,
    val trackWidth: Double = 1.0,
    val wheelBase: Double = trackWidth,
    val lateralMultiplier: Double = 1.0,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            MecanumVelocityConstraint(maxWheelVel, trackWidth, wheelBase, lateralMultiplier),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.MECANUM
}

data class GenericConstraints @JvmOverloads constructor(
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TranslationalVelocityConstraint(maxVel),
            AngularVelocityConstraint(maxAngVel)
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.GENERIC
}

data class TankConstraints @JvmOverloads constructor(
    val maxWheelVel: Double = 100.0,
    val trackWidth: Double = 1.0,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TankVelocityConstraint(maxWheelVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.TANK
}

data class SwerveConstraints @JvmOverloads constructor(
    val maxWheelVel: Double = 100.0,
    val trackWidth: Double = 1.0,
    val wheelBase: Double = trackWidth,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            SwerveVelocityConstraint(maxWheelVel, trackWidth, wheelBase),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.SWERVE
}

data class DiffSwerveConstraints @JvmOverloads constructor(
    val maxGearVel: Double = 100.0,
    val trackWidth: Double = 1.0,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    override val type = TrajectoryConstraints.DriveType.DIFF_SWERVE

    override val velConstraint: TrajectoryVelocityConstraint = MinVelocityConstraint(
        listOf(
            DiffSwerveVelocityConstraint(maxGearVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )

    override val accelConstraint: TrajectoryAccelerationConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
}